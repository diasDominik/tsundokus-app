package uk.tsundokus.features.orders.presentation.scanner

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

/**
 * The camera preview, reading printed text frame by frame with ML Kit until one ISBN holds steady
 * (see [IsbnTextSightings]). The analyser keeps only the latest frame, so a slow read skips frames
 * rather than falling behind the picture.
 */
@Composable
internal fun TextIsbnCameraView(
    accept: (isbn: String) -> Boolean,
    onIsbn: (isbn: String) -> Unit,
    onError: (message: String) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentAccept by rememberUpdatedState(accept)
    val currentOnIsbn by rememberUpdatedState(onIsbn)
    val currentOnError by rememberUpdatedState(onError)
    val controller = remember(context) { LifecycleCameraController(context) }

    DisposableEffect(controller, lifecycleOwner) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val executor = Executors.newSingleThreadExecutor()
        val sightings = IsbnTextSightings { currentAccept(it) }
        var found = false

        controller.setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        controller.setImageAnalysisAnalyzer(executor) { frame ->
            readFrame(frame) { image ->
                recognizer
                    .process(image)
                    // ML Kit calls back on the main thread, so [found] is only touched there.
                    .addOnSuccessListener { text ->
                        val isbn = sightings.onFrame(text.text)
                        if (isbn != null && !found) {
                            found = true
                            currentOnIsbn(isbn)
                        }
                    }.addOnCompleteListener { frame.close() }
            }
        }
        try {
            controller.bindToLifecycle(lifecycleOwner)
        } catch (e: IllegalStateException) {
            currentOnError(e.message.orEmpty())
        }

        onDispose {
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
            executor.shutdown()
            recognizer.close()
        }
    }

    AndroidView(
        factory = { viewContext -> PreviewView(viewContext).apply { this.controller = controller } },
        modifier = modifier,
    )
}

/** Hands the frame to [read] upright, or closes it straight away when it carries no image. */
@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun readFrame(
    frame: ImageProxy,
    read: (InputImage) -> Unit,
) {
    val media = frame.image
    if (media == null) {
        frame.close()
        return
    }
    read(InputImage.fromMediaImage(media, frame.imageInfo.rotationDegrees))
}
