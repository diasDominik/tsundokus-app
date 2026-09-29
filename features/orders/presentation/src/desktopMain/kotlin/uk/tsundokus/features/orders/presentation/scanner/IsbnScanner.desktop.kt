package uk.tsundokus.features.orders.presentation.scanner

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * No camera scanning on desktop: KScan's desktop decoder pulls in OpenCV's native libraries for every
 * OS, hundreds of megabytes, for a webcam that is poor at reading barcodes anyway. The ISBN is typed
 * instead — which a USB barcode scanner does, since it presents itself as a keyboard.
 */
actual fun isCameraScanningSupported(): Boolean = false

@Composable
internal actual fun PlatformIsbnCamera(
    accept: (isbn: String) -> Boolean,
    onIsbn: (isbn: String) -> Unit,
    onError: (message: String) -> Unit,
    modifier: Modifier,
) = Unit
