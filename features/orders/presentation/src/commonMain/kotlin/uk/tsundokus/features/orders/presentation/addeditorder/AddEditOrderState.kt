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
    val titleError: Boolean = false,
    val isSaving: Boolean = false,
)
