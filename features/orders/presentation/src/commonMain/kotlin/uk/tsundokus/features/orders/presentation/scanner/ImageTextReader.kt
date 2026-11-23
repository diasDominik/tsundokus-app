package uk.tsundokus.features.orders.presentation.scanner

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.PlatformFile

/** Reads the printed text in an image: a screenshot of an order, a photo of a book's imprint page. */
fun interface ImageTextReader {
    /** Every line of text found in [image], one per line. Throws when the image cannot be read. */
    suspend fun read(image: PlatformFile): String
}

/**
 * The platform's on-device text recognition, or null where there is none (desktop and web, where the
 * order page is open in a browser on the same machine and its ISBN can be copied instead).
 */
@Composable
internal expect fun rememberImageTextReader(): ImageTextReader?
