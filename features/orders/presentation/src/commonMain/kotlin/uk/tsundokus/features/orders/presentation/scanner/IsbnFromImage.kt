package uk.tsundokus.features.orders.presentation.scanner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.isbn_from_image_choose_title
import tsundokuapp.features.orders.presentation.generated.resources.isbn_from_image_failed
import tsundokuapp.features.orders.presentation.generated.resources.isbn_from_image_none_found
import tsundokuapp.features.orders.presentation.generated.resources.isbn_from_image_pick_another
import tsundokuapp.features.orders.presentation.generated.resources.isbn_from_image_reading
import tsundokuapp.features.orders.presentation.generated.resources.isbn_shared_text_none_found
import tsundokuapp.features.orders.presentation.generated.resources.scan_to_receive_cancel
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.core.domain.logging.TsundokuLogger
import uk.tsundokus.features.orders.domain.models.Isbn
import uk.tsundokus.features.orders.presentation.components.BookCover
import uk.tsundokus.features.orders.presentation.navigation.SharedIsbnSource

private const val TAG = "IsbnReading"

/** Where reading an ISBN off an image, or out of shared text, has got to. */
internal sealed interface IsbnReadingState {
    data object Reading : IsbnReadingState

    /** More than one book, as on an order of several volumes. */
    data class Choosing(val isbns: List<String>) : IsbnReadingState

    data class NoneFound(val fromImage: Boolean) : IsbnReadingState

    data object Failed : IsbnReadingState
}

/**
 * Finds the ISBN in a screenshot or photo (of a shop's order confirmation, say) or in shared text. One
 * ISBN goes straight to the form; several are offered to choose from, in [IsbnReadingDialog].
 */
@Stable
internal class IsbnReading(
    private val reader: ImageTextReader?,
    private val scope: CoroutineScope,
    private val logger: TsundokuLogger,
    private val onIsbn: (isbn: String) -> Unit,
) {
    var state by mutableStateOf<IsbnReadingState?>(null)
        private set

    private var job: Job? = null

    /** Whether this platform can read text in images; shared text is read everywhere. */
    val canReadImages: Boolean get() = reader != null

    fun readImage(image: PlatformFile) {
        val reader = reader ?: return
        job?.cancel()
        state = IsbnReadingState.Reading
        job =
            scope.launch {
                val text =
                    try {
                        reader.read(image)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logger.error(TAG, "Could not read text in the image", e)
                        state = IsbnReadingState.Failed
                        return@launch
                    }
                settle(Isbn.findAll(text), fromImage = true)
            }
    }

    fun readText(text: String) = settle(Isbn.findAll(text), fromImage = false)

    /** An image that could not even be opened, such as a shared one whose access has lapsed. */
    fun fail() {
        logger.warning(TAG, "A shared image could not be opened")
        state = IsbnReadingState.Failed
    }

    fun choose(isbn: String) {
        state = null
        onIsbn(isbn)
    }

    fun dismiss() {
        job?.cancel()
        state = null
    }

    private fun settle(
        isbns: List<String>,
        fromImage: Boolean,
    ) {
        when (isbns.size) {
            0 -> state = IsbnReadingState.NoneFound(fromImage)
            1 -> choose(isbns.single())
            else -> state = IsbnReadingState.Choosing(isbns)
        }
    }
}

@Composable
internal fun rememberIsbnReading(onIsbn: (isbn: String) -> Unit): IsbnReading {
    val reader = rememberImageTextReader()
    val scope = rememberCoroutineScope()
    val logger = koinInject<TsundokuLogger>()
    val currentOnIsbn by rememberUpdatedState(onIsbn)
    return remember(reader, scope, logger) { IsbnReading(reader, scope, logger) { currentOnIsbn(it) } }
}

/** Opens the platform's image picker and reads the picked image; null where images cannot be read. */
@Composable
internal fun rememberIsbnImagePicker(reading: IsbnReading): (() -> Unit)? {
    val picker = rememberFilePickerLauncher(type = FileKitType.Image) { image -> image?.let(reading::readImage) }
    return if (reading.canReadImages) picker::launch else null
}

/** Reads what was shared into the app, once: not again when the screen is recreated. */
@Composable
internal fun ReadSharedIsbn(
    shared: SharedIsbnSource?,
    reading: IsbnReading,
) {
    var handled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(shared) {
        if (shared == null || handled) return@LaunchedEffect
        handled = true
        when (shared) {
            is SharedIsbnSource.Text -> reading.readText(shared.text)
            is SharedIsbnSource.Image -> platformFileOf(shared.uri)?.let(reading::readImage) ?: reading.fail()
        }
    }
}

/** A file the platform handed over by URI, such as an image shared into the app. */
internal expect fun platformFileOf(uri: String): PlatformFile?

@Composable
internal fun IsbnReadingDialog(
    reading: IsbnReading,
    onPickAnother: (() -> Unit)?,
) {
    val state = reading.state ?: return
    IsbnReadingDialog(
        state = state,
        onIsbn = reading::choose,
        onPickAnother =
            onPickAnother?.let {
                {
                    reading.dismiss()
                    it()
                }
            },
        onDismiss = reading::dismiss,
    )
}

@Composable
private fun IsbnReadingDialog(
    state: IsbnReadingState,
    onIsbn: (isbn: String) -> Unit,
    onPickAnother: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title =
            (state as? IsbnReadingState.Choosing)?.let {
                { Text(stringResource(Res.string.isbn_from_image_choose_title)) }
            },
        text = {
            when (state) {
                IsbnReadingState.Reading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text(stringResource(Res.string.isbn_from_image_reading))
                    }
                }

                is IsbnReadingState.Choosing -> {
                    IsbnChoices(isbns = state.isbns, onIsbn = onIsbn)
                }

                is IsbnReadingState.NoneFound -> {
                    val message =
                        if (state.fromImage) {
                            Res.string.isbn_from_image_none_found
                        } else {
                            Res.string.isbn_shared_text_none_found
                        }
                    Text(stringResource(message))
                }

                IsbnReadingState.Failed -> {
                    Text(stringResource(Res.string.isbn_from_image_failed))
                }
            }
        },
        confirmButton = {
            val offerAnother = state is IsbnReadingState.NoneFound || state == IsbnReadingState.Failed
            if (offerAnother && onPickAnother != null) {
                TextButton(onClick = onPickAnother) {
                    Text(stringResource(Res.string.isbn_from_image_pick_another))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.scan_to_receive_cancel)) }
        },
    )
}

/** Each ISBN with its cover, which is what tells the volumes of one order apart at a glance. */
@Composable
private fun IsbnChoices(
    isbns: List<String>,
    onIsbn: (isbn: String) -> Unit,
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        isbns.forEach { isbn ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onIsbn(isbn) }
                        .padding(vertical = 8.dp),
            ) {
                BookCover(isbn = isbn, width = 40.dp)
                Text(text = isbn, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@PreviewThemes
@Composable
private fun IsbnReadingDialogChoosingPreview() {
    TsundokuTheme {
        Surface {
            IsbnReadingDialog(
                state = IsbnReadingState.Choosing(listOf("9784088820453", "9780439420891")),
                onIsbn = {},
                onPickAnother = {},
                onDismiss = {},
            )
        }
    }
}

@PreviewThemes
@Composable
private fun IsbnReadingDialogNoneFoundPreview() {
    TsundokuTheme {
        Surface {
            IsbnReadingDialog(
                state = IsbnReadingState.NoneFound(fromImage = true),
                onIsbn = {},
                onPickAnother = {},
                onDismiss = {},
            )
        }
    }
}
