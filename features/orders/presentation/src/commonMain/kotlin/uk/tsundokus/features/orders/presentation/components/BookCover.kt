package uk.tsundokus.features.orders.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource
import uk.tsundokus.core.data.AppBuildInfo
import uk.tsundokus.features.orders.domain.models.OrderStatus

/**
 * Where the server serves a book's cover. The same URL for every device and every order with that
 * ISBN, so each cover is downloaded once and then comes from the image cache.
 */
fun coverUrl(isbn: String): String = "${AppBuildInfo.baseUrl.trimEnd('/')}/api/books/$isbn/cover.jpg"

/** Books are roughly 1:1.4; every cover is drawn at that ratio so nothing shifts once it loads. */
private const val COVER_ASPECT = 1.4f

/**
 * A book cover of fixed size. [placeholder] is drawn underneath and shows until the image arrives —
 * or for good, offline with nothing cached — so the layout never waits on the network or jumps.
 * Only call this for an ISBN the server has a cover for: that is what keeps lists from asking for
 * covers that do not exist.
 */
@Composable
fun BookCover(
    isbn: String,
    width: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    placeholder: @Composable () -> Unit = {},
) {
    Box(
        modifier =
            modifier
                .size(width, width * COVER_ASPECT)
                .clip(RoundedCornerShape(width * 0.12f))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        placeholder()
        AsyncImage(
            model = coverUrl(isbn),
            contentDescription = contentDescription,
            // Coil decodes at this laid-out size, so a list of thumbnails holds thumbnails in memory,
            // not full images.
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
    }
}

/**
 * The leading visual of an order row: its cover when there is one, with the status as a coloured dot
 * in the corner; otherwise the status tile, as before.
 */
@Composable
fun OrderThumbnail(
    isbn: String,
    hasCover: Boolean,
    status: OrderStatus,
    modifier: Modifier = Modifier,
) {
    if (!hasCover || isbn.isBlank()) {
        StatusTile(status = status, modifier = modifier)
        return
    }
    val statusLabel = stringResource(status.labelRes)
    // The dot alone says nothing to a screen reader, so the status is announced for the whole thumbnail.
    Box(modifier = modifier.semantics { contentDescription = statusLabel }) {
        BookCover(
            isbn = isbn,
            width = 40.dp,
            placeholder = { StatusTile(status = status, size = 28.dp) },
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(12.dp)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .clip(CircleShape)
                    .background(status.accentColor()),
        )
    }
}
