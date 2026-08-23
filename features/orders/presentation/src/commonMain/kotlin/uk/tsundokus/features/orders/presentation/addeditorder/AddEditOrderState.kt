package uk.tsundokus.features.orders.presentation.addeditorder

import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState

data class AddEditOrderState(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val publisher: String = "",
    val volume: String = "",
    val store: String = "",
    val price: String = "",
    val currency: AppCurrency = AppCurrency.EUR,
    val status: OrderStatus = OrderStatus.ORDERED,
    val readState: ReadState = ReadState.WANT,
    val orderDate: String = "",
    val releaseDate: String = "",
    val shipDate: String = "",
    val eta: String = "",
    val receivedDate: String = "",
    val delayedTo: String = "",
    val isEdit: Boolean = false,
    val errors: Set<OrderFormField> = emptySet(),
    val isSaving: Boolean = false,
    /** Whether the user has changed anything since the form opened. Drives the discard guard. */
    val isDirty: Boolean = false,
) {
    /**
     * The form's content with the transient bookkeeping stripped, so two states can be compared for
     * "did the user actually change anything". Copying rather than listing the fields keeps this
     * correct when a field is added to the form.
     */
    internal fun formOnly(): AddEditOrderState = copy(errors = emptySet(), isSaving = false, isDirty = false)
}

/**
 * The fields the form can report an error on. Volume and release date stay optional and unvalidated;
 * the status dates are optional but must still describe a possible sequence of events when present.
 */
enum class OrderFormField {
    TITLE,
    AUTHOR,
    PUBLISHER,
    STORE,
    PRICE,
    ORDER_DATE,
    SHIP_DATE,
    ETA,
    RECEIVED_DATE,
    DELAYED_TO,
}
