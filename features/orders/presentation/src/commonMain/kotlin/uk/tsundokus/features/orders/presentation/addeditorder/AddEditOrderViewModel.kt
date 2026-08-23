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

@KoinViewModel
class AddEditOrderViewModel(
    @InjectedParam private val orderId: String?,
    private val orderRepository: OrderRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AddEditOrderState(isEdit = orderId != null))
    val state = _state.asStateFlow()

    private val eventChannel = Channel<AddEditOrderEvent>()
    val events = eventChannel.receiveAsFlow()

    private var originalCreatedAt = 0L

    init {
        if (orderId != null) {
            viewModelScope.launch {
                populate(orderRepository.getOrderById(orderId).filterNotNull().first())
            }
        }
    }

    fun onAction(action: AddEditOrderAction) {
        when (action) {
            is AddEditOrderAction.OnTitleChange -> {
                _state.update {
                    it.copy(
                        title = action.value,
                        titleError = false,
                    )
                }
            }

            is AddEditOrderAction.OnAuthorChange -> {
                _state.update { it.copy(author = action.value) }
            }

            is AddEditOrderAction.OnPublisherChange -> {
                _state.update { it.copy(publisher = action.value) }
            }

            is AddEditOrderAction.OnVolumeChange -> {
                _state.update { it.copy(volume = action.value) }
            }

            is AddEditOrderAction.OnStoreChange -> {
                _state.update { it.copy(store = action.value) }
            }

            is AddEditOrderAction.OnPriceChange -> {
                _state.update { it.copy(price = OrderValidator.sanitizePrice(action.value)) }
            }

            is AddEditOrderAction.OnOrderDateChange -> {
                _state.update { it.copy(orderDate = action.value) }
            }

            is AddEditOrderAction.OnReleaseDateChange -> {
                _state.update { it.copy(releaseDate = action.value) }
            }

            is AddEditOrderAction.OnShipDateChange -> {
                _state.update { it.copy(shipDate = action.value) }
            }

            is AddEditOrderAction.OnEtaChange -> {
                _state.update { it.copy(eta = action.value) }
            }

            is AddEditOrderAction.OnReceivedDateChange -> {
                _state.update { it.copy(receivedDate = action.value) }
            }

            is AddEditOrderAction.OnDelayedToChange -> {
                _state.update { it.copy(delayedTo = action.value) }
            }

            is AddEditOrderAction.OnCurrencySelected -> {
                _state.update { it.copy(currency = action.currency) }
            }

            is AddEditOrderAction.OnStatusSelected -> {
                _state.update { it.copy(status = action.status) }
            }

            is AddEditOrderAction.OnReadStateSelected -> {
                _state.update { it.copy(readState = action.readState) }
            }

            AddEditOrderAction.OnSave -> {
                save()
            }

            AddEditOrderAction.OnDelete -> {
                delete()
            }
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
    }

    private fun save() {
        val current = _state.value
        if (!OrderValidator.isTitleValid(current.title)) {
            _state.update { it.copy(titleError = true) }
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
