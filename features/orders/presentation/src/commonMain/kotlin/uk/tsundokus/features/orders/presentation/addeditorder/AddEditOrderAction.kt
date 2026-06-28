package uk.tsundokus.features.orders.presentation.addeditorder

import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState

sealed interface AddEditOrderAction {
    data class OnTitleChange(val value: String) : AddEditOrderAction

    data class OnAuthorChange(val value: String) : AddEditOrderAction

    data class OnPublisherChange(val value: String) : AddEditOrderAction

    data class OnVolumeChange(val value: String) : AddEditOrderAction

    data class OnStoreChange(val value: String) : AddEditOrderAction

    data class OnPriceChange(val value: String) : AddEditOrderAction

    data class OnOrderDateChange(val value: String) : AddEditOrderAction

    data class OnReleaseDateChange(val value: String) : AddEditOrderAction

    data class OnShipDateChange(val value: String) : AddEditOrderAction

    data class OnEtaChange(val value: String) : AddEditOrderAction

    data class OnReceivedDateChange(val value: String) : AddEditOrderAction

    data class OnDelayedToChange(val value: String) : AddEditOrderAction

    data class OnCurrencySelected(val currency: AppCurrency) : AddEditOrderAction

    data class OnStatusSelected(val status: OrderStatus) : AddEditOrderAction

    data class OnReadStateSelected(val readState: ReadState) : AddEditOrderAction

    data object OnSave : AddEditOrderAction

    data object OnDelete : AddEditOrderAction
}
