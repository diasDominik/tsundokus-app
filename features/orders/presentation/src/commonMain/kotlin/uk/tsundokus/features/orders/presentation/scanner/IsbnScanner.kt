package uk.tsundokus.features.orders.presentation.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.scanner_error
import tsundokuapp.features.orders.presentation.generated.resources.scanner_retry

/** Whether this platform can scan with a camera. Desktop cannot; it types the ISBN instead. */
expect fun isCameraScanningSupported(): Boolean

/**
 * The platform's camera preview, reporting each ISBN it reads as ISBN-13 digits. Handles the camera
 * permission itself. Stops after the first ISBN [accept]s, like the KScan view underneath it;
 * [IsbnScanner] re-arms it. Never composed where [isCameraScanningSupported] is false.
 */
@Composable
internal expect fun PlatformIsbnCamera(
    accept: (isbn: String) -> Boolean,
    onIsbn: (isbn: String) -> Unit,
    onError: (message: String) -> Unit,
    modifier: Modifier,
)

/**
 * A camera viewfinder that reads ISBN barcodes, one book after another. Only EAN-13 codes that are
 * valid ISBNs count, so the second barcode on a Japanese volume is skipped rather than misread.
 *
 * [ignoreIsbn] is the book just handled: the camera re-arms right after a read while that book is
 * usually still in view, and without it would read the same cover again straight away.
 *
 * On web the camera preview is an HTML element drawn above the Compose canvas, so nothing should be
 * laid over this; put controls beside it.
 */
@Composable
fun IsbnScanner(
    onIsbn: (isbn: String) -> Unit,
    modifier: Modifier = Modifier,
    ignoreIsbn: String? = null,
) {
    // Keying the camera on this restarts it: the view underneath stops after its first read.
    var generation by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    val currentIgnore by rememberUpdatedState(ignoreIsbn)
    val currentOnIsbn by rememberUpdatedState(onIsbn)

    Box(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        val message = error
        if (message != null) {
            ScannerMessage(
                text = stringResource(Res.string.scanner_error, message),
                actionLabel = stringResource(Res.string.scanner_retry),
                onAction = {
                    error = null
                    generation++
                },
            )
        } else {
            key(generation) {
                PlatformIsbnCamera(
                    accept = { isbn -> isbn != currentIgnore },
                    onIsbn = { isbn ->
                        generation++
                        currentOnIsbn(isbn)
                    },
                    onError = { error = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/** A line of text in the viewfinder's place, with an optional action — permission, errors. */
@Composable
internal fun ScannerMessage(
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = MaterialTheme.colorScheme.inversePrimary)
            }
        }
    }
}
