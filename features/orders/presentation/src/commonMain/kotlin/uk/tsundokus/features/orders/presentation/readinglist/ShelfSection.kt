package uk.tsundokus.features.orders.presentation.readinglist

import org.jetbrains.compose.resources.StringResource
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.reading_list_section_on_the_way
import tsundokuapp.features.orders.presentation.generated.resources.reading_list_section_pile
import tsundokuapp.features.orders.presentation.generated.resources.reading_list_section_read
import tsundokuapp.features.orders.presentation.generated.resources.reading_list_section_reading
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState

/**
 * Where an order sits on the reading shelf, in the order the shelf shows them. "Want to read" alone
 * mixed two different things: books on the user's own pile, ready to pick up, and books still in
 * the post that cannot be read yet. Splitting them makes the pile here the same pile Stats counts.
 */
enum class ShelfSection(
    val labelRes: StringResource,
) {
    READING(Res.string.reading_list_section_reading),
    PILE(Res.string.reading_list_section_pile),
    ON_THE_WAY(Res.string.reading_list_section_on_the_way),
    READ(Res.string.reading_list_section_read),
}

val Order.shelfSection: ShelfSection
    get() =
        when {
            readState == ReadState.READING -> ShelfSection.READING
            readState == ReadState.READ -> ShelfSection.READ
            status == OrderStatus.RECEIVED -> ShelfSection.PILE
            else -> ShelfSection.ON_THE_WAY
        }
