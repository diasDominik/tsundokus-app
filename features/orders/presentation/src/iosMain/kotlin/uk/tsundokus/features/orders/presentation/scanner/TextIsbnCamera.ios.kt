package uk.tsundokus.features.orders.presentation.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetHigh
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.AVFoundation.AVCaptureVideoOrientation
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeLeft
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeRight
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoOrientationPortraitUpsideDown
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.CoreMedia.CMSampleBufferRef
import platform.ImageIO.kCGImagePropertyOrientationUp
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIInterfaceOrientationLandscapeLeft
import platform.UIKit.UIInterfaceOrientationLandscapeRight
import platform.UIKit.UIInterfaceOrientationPortraitUpsideDown
import platform.UIKit.UIViewController
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create

/**
 * The camera preview, reading printed text frame by frame with Vision until one ISBN holds steady
 * (see [IsbnTextSightings]). Late frames are dropped while one is being read, so reading never falls
 * behind the picture. (VisionKit's ready-made live text scanner is Swift-only, out of Kotlin's reach.)
 */
@Composable
internal fun TextIsbnCameraView(
    accept: (isbn: String) -> Boolean,
    onIsbn: (isbn: String) -> Unit,
    onError: (message: String) -> Unit,
    modifier: Modifier,
) {
    val currentAccept by rememberUpdatedState(accept)
    val currentOnIsbn by rememberUpdatedState(onIsbn)
    val currentOnError by rememberUpdatedState(onError)
    val controller =
        remember {
            TextCameraViewController(
                sightings = IsbnTextSightings { currentAccept(it) },
                onIsbn = { currentOnIsbn(it) },
                onError = { currentOnError(it) },
            )
        }
    DisposableEffect(controller) { onDispose { controller.dispose() } }

    UIKitViewController(factory = { controller }, modifier = modifier)
}

@OptIn(ExperimentalForeignApi::class)
private class TextCameraViewController(
    private val sightings: IsbnTextSightings,
    private val onIsbn: (isbn: String) -> Unit,
    private val onError: (message: String) -> Unit,
) : UIViewController(null, null),
    AVCaptureVideoDataOutputSampleBufferDelegateProtocol {
    private val session = AVCaptureSession()
    private val frames = dispatch_queue_create("uk.tsundokus.isbn-text", null)
    private var previewLayer: AVCaptureVideoPreviewLayer? = null
    private var output: AVCaptureVideoDataOutput? = null

    // Set on the frame queue once an ISBN is confirmed; frames still queued are then skipped.
    private var found = false

    // The session stops on a background queue, so a frame can still arrive after disposal.
    private var disposed = false

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.blackColor
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        val input = device?.let { AVCaptureDeviceInput.deviceInputWithDevice(it, null) }
        if (input == null || !session.canAddInput(input)) {
            onError("No camera to read with")
            return
        }
        session.addInput(input)
        // Plenty for printed text, and a frame small enough for Vision to read quickly.
        session.sessionPreset = AVCaptureSessionPresetHigh

        val videoOutput =
            AVCaptureVideoDataOutput().apply {
                alwaysDiscardsLateVideoFrames = true
                setSampleBufferDelegate(this@TextCameraViewController, frames)
            }
        if (!session.canAddOutput(videoOutput)) {
            onError("The camera could not be read from")
            return
        }
        session.addOutput(videoOutput)
        output = videoOutput

        previewLayer =
            AVCaptureVideoPreviewLayer.layerWithSession(session).also { layer ->
                layer.videoGravity = AVLayerVideoGravityResizeAspectFill
                layer.frame = view.layer.bounds
                view.layer.addSublayer(layer)
            }
        updateOrientation()
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        offMainQueue { if (!session.isRunning()) session.startRunning() }
    }

    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)
        offMainQueue { if (session.isRunning()) session.stopRunning() }
    }

    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        previewLayer?.frame = view.layer.bounds
        updateOrientation()
    }

    /**
     * Turns both the preview and the frames handed to Vision to the way the screen faces, so the text
     * in a frame is always upright.
     */
    private fun updateOrientation() {
        val orientation: AVCaptureVideoOrientation =
            when (UIApplication.sharedApplication.statusBarOrientation) {
                UIInterfaceOrientationLandscapeLeft -> AVCaptureVideoOrientationLandscapeLeft
                UIInterfaceOrientationLandscapeRight -> AVCaptureVideoOrientationLandscapeRight
                UIInterfaceOrientationPortraitUpsideDown -> AVCaptureVideoOrientationPortraitUpsideDown
                else -> AVCaptureVideoOrientationPortrait
            }
        previewLayer?.connection?.videoOrientation = orientation
        output?.connectionWithMediaType(AVMediaTypeVideo)?.videoOrientation = orientation
    }

    @ObjCSignatureOverride
    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputSampleBuffer: CMSampleBufferRef?,
        fromConnection: AVCaptureConnection,
    ) {
        if (found || disposed || didOutputSampleBuffer == null) return
        val request =
            VNRecognizeTextRequest().apply {
                recognitionLevel = VNRequestTextRecognitionLevelAccurate
                // Correction is for words; it would only "fix" a number into something else.
                usesLanguageCorrection = false
            }
        val handler =
            VNImageRequestHandler(
                cMSampleBuffer = didOutputSampleBuffer,
                orientation = kCGImagePropertyOrientationUp,
                options = emptyMap<Any?, Any?>(),
            )
        if (!handler.performRequests(listOf(request), null)) return
        val text =
            request.results
                .orEmpty()
                .filterIsInstance<VNRecognizedTextObservation>()
                .mapNotNull { (it.topCandidates(1u).firstOrNull() as? VNRecognizedText)?.string }
                .joinToString("\n")
        val isbn = sightings.onFrame(text) ?: return
        found = true
        dispatch_async(dispatch_get_main_queue()) { if (!disposed) onIsbn(isbn) }
    }

    fun dispose() {
        disposed = true
        previewLayer?.removeFromSuperlayer()
        offMainQueue {
            if (session.isRunning()) session.stopRunning()
            output?.setSampleBufferDelegate(null, null)
        }
    }

    // Starting and stopping a session blocks, so it is kept off the main queue.
    private fun offMainQueue(block: () -> Unit) {
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u), block)
    }
}
