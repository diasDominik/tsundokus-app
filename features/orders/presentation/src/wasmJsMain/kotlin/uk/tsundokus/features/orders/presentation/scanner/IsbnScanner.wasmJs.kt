package uk.tsundokus.features.orders.presentation.scanner

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerView
import uk.tsundokus.features.orders.domain.models.Isbn

actual fun isCameraScanningSupported(): Boolean = true

/**
 * The browser asks for the camera itself on first use, and a refusal (or a page not served over
 * https) comes back as a failed result, shown by [IsbnScanner] with a retry.
 */
@Composable
internal actual fun PlatformIsbnCamera(
    accept: (isbn: String) -> Boolean,
    onIsbn: (isbn: String) -> Unit,
    onError: (message: String) -> Unit,
    modifier: Modifier,
) {
    ScannerView(
        codeTypes = listOf(BarcodeFormat.FORMAT_EAN_13),
        modifier = modifier,
        filter = { barcode -> Isbn.normalize(barcode.data)?.let(accept) == true },
        result = { result ->
            when (result) {
                is BarcodeResult.OnSuccess -> Isbn.normalize(result.barcode.data)?.let(onIsbn)
                is BarcodeResult.OnFailed -> onError(result.exception.message.orEmpty())
            }
        },
    )
}
