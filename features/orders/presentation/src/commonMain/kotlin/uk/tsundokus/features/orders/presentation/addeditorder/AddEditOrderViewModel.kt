package uk.tsundokus.features.orders.presentation.addeditorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_deleted
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_saved_added
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_saved_updated
import uk.tsundokus.core.domain.preferences.AppPreferencesRepository
import uk.tsundokus.core.domain.util.onFailure
import uk.tsundokus.core.domain.util.onSuccess
import uk.tsundokus.core.presentation.util.UiText
import uk.tsundokus.core.presentation.util.toUiText
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.order.OrderRepository
import uk.tsundokus.features.orders.domain.validation.OrderValidator
import uk.tsundokus.features.orders.presentation.components.nowEpochMillis
import uk.tsundokus.features.orders.presentation.components.todayIso
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val DATE_FIELDS =
    setOf(
        OrderFormField.ORDER_DATE,
        OrderFormField.SHIP_DATE,
        OrderFormField.ETA,
        OrderFormField.RECEIVED_DATE,
        OrderFormField.DELAYED_TO,
    )

@KoinViewModel
class AddEditOrderViewModel(
    @InjectedParam private val orderId: String?,
    private val orderRepository: OrderRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AddEditOrderState(isEdit = orderId != null))
    val state = _state.asStateFlow()

    private val eventChannel = Channel<AddEditOrderEvent>()
    val events = eventChannel.receiveAsFlow()

    private var originalCreatedAt = 0L

    /**
     * The form as it was last handed to the user — on open, and again after a successful save.
     * Everything that follows is compared against it to decide whether there is anything to lose.
     */
    private var pristine = _state.value

    init {
        if (orderId != null) {
            viewModelScope.launch {
                populate(orderRepository.getOrderById(orderId).filterNotNull().first())
            }
        } else {
            // A new order starts in the currency the user picked in settings, not a hardcoded one.
            viewModelScope.launch {
                val currency = appPreferencesRepository.currency().first()
                _state.update { it.copy(currency = currency) }
                markPristine()
            }
        }
    }

    fun onAction(action: AddEditOrderAction) {
        when (action) {
            is AddEditOrderAction.OnTitleChange -> {
                updateForm { it.copy(title = action.value).clearing(OrderFormField.TITLE) }
            }

            is AddEditOrderAction.OnAuthorChange -> {
                updateForm { it.copy(author = action.value).clearing(OrderFormField.AUTHOR) }
            }

            is AddEditOrderAction.OnPublisherChange -> {
                updateForm { it.copy(publisher = action.value).clearing(OrderFormField.PUBLISHER) }
            }

            is AddEditOrderAction.OnVolumeChange -> {
                updateForm { it.copy(volume = action.value) }
            }

            is AddEditOrderAction.OnStoreChange -> {
                updateForm { it.copy(store = action.value).clearing(OrderFormField.STORE) }
            }

            is AddEditOrderAction.OnPriceChange -> {
                updateForm {
                    it.copy(price = OrderValidator.sanitizePrice(action.value)).clearing(OrderFormField.PRICE)
                }
            }

            is AddEditOrderAction.OnOrderDateChange -> {
                updateForm { it.copy(orderDate = action.value).clearingDates() }
            }

            is AddEditOrderAction.OnReleaseDateChange -> {
                updateForm { it.copy(releaseDate = action.value) }
            }

            is AddEditOrderAction.OnShipDateChange -> {
                updateForm { it.copy(shipDate = action.value).clearingDates() }
            }

            is AddEditOrderAction.OnEtaChange -> {
                updateForm { it.copy(eta = action.value).clearingDates() }
            }

            is AddEditOrderAction.OnReceivedDateChange -> {
                updateForm { it.copy(receivedDate = action.value).clearingDates() }
            }

            is AddEditOrderAction.OnDelayedToChange -> {
                updateForm { it.copy(delayedTo = action.value).clearingDates() }
            }

            is AddEditOrderAction.OnCurrencySelected -> {
                updateForm { it.copy(currency = action.currency) }
            }

            is AddEditOrderAction.OnStatusSelected -> {
                updateForm { it.copy(status = action.status) }
            }

            is AddEditOrderAction.OnReadStateSelected -> {
                updateForm { it.copy(readState = action.readState) }
            }

            AddEditOrderAction.OnSave -> {
                save()
            }

            AddEditOrderAction.OnDelete -> {
                delete()
            }
        }
    }

    private fun AddEditOrderState.clearing(field: OrderFormField): AddEditOrderState =
        if (field in errors) copy(errors = errors - field) else this

    /**
     * Clears every date error, not just the edited field's. The date rules are relations between
     * fields, so moving the order date can resolve an error flagged on the received date; leaving
     * the others standing would show an error the form no longer has.
     */
    private fun AddEditOrderState.clearingDates(): AddEditOrderState = copy(errors = errors - DATE_FIELDS)

    /**
     * Applies a user edit and re-derives [AddEditOrderState.isDirty] from it. Comparing against
     * [pristine] rather than latching a flag means undoing an edit by hand — retyping the original
     * title, re-picking the original status — correctly leaves nothing to discard.
     */
    private fun updateForm(transform: (AddEditOrderState) -> AddEditOrderState) {
        _state.update { current ->
            val next = transform(current)
            next.copy(isDirty = next.formOnly() != pristine.formOnly())
        }
    }

    /** Adopts the current form as the new baseline: nothing here counts as unsaved any more. */
    private fun markPristine() {
        pristine = _state.value
        _state.update { it.copy(isDirty = false) }
    }

    /** Every required field, checked together so the user sees all that is missing at once. */
    private fun AddEditOrderState.validate(): Set<OrderFormField> =
        buildSet {
            if (!OrderValidator.isTitleValid(title)) add(OrderFormField.TITLE)
            if (!OrderValidator.isRequiredTextValid(author)) add(OrderFormField.AUTHOR)
            if (!OrderValidator.isRequiredTextValid(publisher)) add(OrderFormField.PUBLISHER)
            if (!OrderValidator.isRequiredTextValid(store)) add(OrderFormField.STORE)
            if (!OrderValidator.isPriceValid(price)) add(OrderFormField.PRICE)
            if (!OrderValidator.isRequiredDateValid(orderDate)) add(OrderFormField.ORDER_DATE)
            addAll(impossibleDates())
        }

    /**
     * Dates that describe a sequence that cannot have happened: nothing about an order can precede
     * the day it was ordered, and it cannot arrive before it shipped. Each offending field is
     * flagged rather than the order date, so the error sits on the value the user should change.
     */
    private fun AddEditOrderState.impossibleDates(): Set<OrderFormField> =
        buildSet {
            if (OrderValidator.isBefore(shipDate, orderDate)) add(OrderFormField.SHIP_DATE)
            if (OrderValidator.isBefore(eta, orderDate)) add(OrderFormField.ETA)
            if (OrderValidator.isBefore(delayedTo, orderDate)) add(OrderFormField.DELAYED_TO)
            if (OrderValidator.isBefore(receivedDate, orderDate) ||
                OrderValidator.isBefore(receivedDate, shipDate)
            ) {
                add(OrderFormField.RECEIVED_DATE)
            }
        }

    private fun populate(order: Order) {
        originalCreatedAt = order.createdAt
        _state.update {
            it.copy(
                id = order.id,
                title = order.title,
                author = order.author,
                publisher = order.publisher,
                volume = order.volume,
                store = order.store,
                price = if (order.price == 0.0) "" else order.price.toString(),
                currency = order.currency,
                status = order.status,
                readState = order.readState,
                orderDate = order.orderDate,
                releaseDate = order.releaseDate,
                shipDate = order.shipDate,
                eta = order.eta,
                receivedDate = order.receivedDate,
                delayedTo = order.delayedTo,
                isEdit = true,
            )
        }
        // The loaded order is the baseline an edit is measured against, not the blank form.
        markPristine()
    }

    private fun save() {
        val current = _state.value
        val errors = current.validate()
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val order = current.toOrder()
            val result =
                if (current.isEdit) {
                    orderRepository.updateOrder(
                        order,
                    )
                } else {
                    orderRepository.createOrder(order)
                }
            result
                .onSuccess {
                    _state.update { it.copy(isSaving = false) }
                    // Saved: there is nothing left to warn about on the way out.
                    markPristine()
                    val message =
                        if (current.isEdit) {
                            Res.string.add_edit_order_saved_updated
                        } else {
                            Res.string.add_edit_order_saved_added
                        }
                    eventChannel.send(AddEditOrderEvent.Saved(UiText.Resource(message)))
                }.onFailure { error ->
                    _state.update { it.copy(isSaving = false) }
                    eventChannel.send(AddEditOrderEvent.ShowError(error.toUiText()))
                }
        }
    }

    private fun delete() {
        val id = _state.value.id
        if (id.isBlank()) return
        viewModelScope.launch {
            orderRepository
                .deleteOrder(id)
                .onSuccess {
                    eventChannel.send(
                        AddEditOrderEvent.Deleted(UiText.Resource(Res.string.add_edit_order_deleted)),
                    )
                }.onFailure { error ->
                    eventChannel.send(AddEditOrderEvent.ShowError(error.toUiText()))
                }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun AddEditOrderState.toOrder(): Order {
        val resolvedReceivedDate =
            if (status == OrderStatus.RECEIVED && receivedDate.isBlank()) todayIso() else receivedDate.trim()
        return Order(
            // A new order gets a client-minted UUID so its create is an idempotent upsert the outbox
            // can safely replay; an edit keeps the existing id.
            id = id.ifBlank { Uuid.random().toString() },
            title = title.trim(),
            author = author.trim(),
            publisher = publisher.trim(),
            volume = volume.trim(),
            store = store.trim(),
            price = price.toDoubleOrNull() ?: 0.0,
            currency = currency,
            status = status,
            readState = readState,
            orderDate = orderDate.trim(),
            releaseDate = releaseDate.trim(),
            shipDate = shipDate.trim(),
            eta = eta.trim(),
            receivedDate = resolvedReceivedDate,
            delayedTo = delayedTo.trim(),
            createdAt = if (isEdit) originalCreatedAt else nowEpochMillis(),
        )
    }
}
