package uk.tsundokus.features.orders.presentation.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerView
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.scanner_permission_denied
import tsundokuapp.features.orders.presentation.generated.resources.scanner_permission_open_settings
import uk.tsundokus.features.orders.domain.models.Isbn
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual fun isCameraScanningSupported(): Boolean = true

@Composable
internal actual fun PlatformIsbnCamera(
    accept: (isbn: String) -> Boolean,
    onIsbn: (isbn: String) -> Unit,
    onError: (message: String) -> Unit,
    modifier: Modifier,
) {
    var status by remember { mutableStateOf(AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) }

    // Asked for straight away: the user opened the scanner, so the camera is what they want. iOS only
    // ever shows this prompt once; after that the answer can only change in Settings, and changing
    // it there restarts the app, so there is nothing to re-check on return.
    LaunchedEffect(Unit) {
        if (status == AVAuthorizationStatusNotDetermined) {
            val granted =
                suspendCoroutine { continuation ->
                    AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { continuation.resume(it) }
                }
            status = if (granted) AVAuthorizationStatusAuthorized else AVAuthorizationStatusDenied
        }
    }

    when (status) {
        AVAuthorizationStatusAuthorized -> {
            KScanIsbnView(accept = accept, onIsbn = onIsbn, onError = onError, modifier = modifier)
        }

        // The system prompt is up; the black viewfinder behind it is enough.
        AVAuthorizationStatusNotDetermined -> {}

        else -> {
            ScannerMessage(
                text = stringResource(Res.string.scanner_permission_denied),
                actionLabel = stringResource(Res.string.scanner_permission_open_settings),
                onAction = {
                    NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let { url ->
                        UIApplication.sharedApplication.openURL(
                            url,
                            options = emptyMap<Any?, Any>(),
                            completionHandler = null,
                        )
                    }
                },
            )
        }
    }
}

/** The KScan preview, narrowed to barcodes that are ISBNs. */
@Composable
private fun KScanIsbnView(
    accept: (isbn: String) -> Boolean,
    onIsbn: (isbn: String) -> Unit,
    onError: (message: String) -> Unit,
    modifier: Modifier,
) {
    ScannerView(
        // A book's ISBN barcode is always EAN-13; asking for nothing else keeps the decoder quick.
        codeTypes = listOf(BarcodeFormat.FORMAT_EAN_13),
        modifier = modifier,
        // Rejecting here keeps the camera running, so a non-ISBN code in view is simply passed over.
        filter = { barcode -> Isbn.normalize(barcode.data)?.let(accept) == true },
        result = { result ->
            when (result) {
                is BarcodeResult.OnSuccess -> Isbn.normalize(result.barcode.data)?.let(onIsbn)
                is BarcodeResult.OnFailed -> onError(result.exception.message.orEmpty())
            }
        },
    )
}
