package uk.tsundokus.features.orders.presentation.orderdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.core.domain.util.onFailure
import uk.tsundokus.core.domain.util.onSuccess
import uk.tsundokus.core.presentation.util.UiText
import uk.tsundokus.core.presentation.util.toUiText
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.domain.order.OrderRepository
import uk.tsundokus.features.orders.presentation.components.TimelineNode
import uk.tsundokus.features.orders.presentation.components.TimelineNodeState
import uk.tsundokus.features.orders.presentation.components.fmtDate
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class OrderDetailViewModel(
    @InjectedParam private val orderId: String,
    private val orderRepository: OrderRepository,
) : ViewModel() {
    private val eventChannel = Channel<OrderDetailEvent>()
    val events = eventChannel.receiveAsFlow()

    val state: StateFlow<OrderDetailState> =
        orderRepository
            .getOrderById(orderId)
            .map { order ->
                if (order == null) {
                    OrderDetailState(isLoading = false, order = null)
                } else {
                    OrderDetailState(
                        isLoading = false,
                        order = order,
                        timeline = buildTimeline(order),
                        primaryAction = primaryActionFor(order),
                    )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = OrderDetailState(),
            )

    fun onPrimaryAction() {
        val action = state.value.primaryAction ?: return
        viewModelScope.launch {
            when (action) {
                PrimaryAction.MARK_SHIPPED -> {
                    orderRepository
                        .setStatus(orderId, OrderStatus.SHIPPED)
                        .handle(UiText.DynamicString("Marked ${OrderStatus.SHIPPED.label}"))
                }

                PrimaryAction.MARK_RECEIVED -> {
                    orderRepository
                        .setStatus(orderId, OrderStatus.RECEIVED)
                        .handle(UiText.DynamicString("Marked ${OrderStatus.RECEIVED.label}"))
                }

                PrimaryAction.MARK_AS_READ -> {
                    orderRepository
                        .setReadState(orderId, ReadState.READ)
                        .handle(UiText.DynamicString(ReadState.READ.fullLabel))
                }
            }
        }
    }

    fun onSetReadState(readState: ReadState) {
        val current = state.value.order ?: return
        if (current.readState == readState) return
        viewModelScope.launch {
            orderRepository.setReadState(orderId, readState).handle(UiText.DynamicString(readState.fullLabel))
        }
    }

    fun onDelete() {
        viewModelScope.launch {
            orderRepository
                .deleteOrder(orderId)
                .onSuccess {
                    eventChannel.send(OrderDetailEvent.ShowMessage(UiText.DynamicString("Order deleted")))
                    eventChannel.send(OrderDetailEvent.Deleted)
                }.onFailure { error ->
                    eventChannel.send(OrderDetailEvent.ShowMessage(error.toUiText()))
                }
        }
    }

    private suspend fun Result<Order, DataError.Remote>.handle(successMessage: UiText) {
        onSuccess {
            eventChannel.send(OrderDetailEvent.ShowMessage(successMessage))
        }.onFailure { error ->
            eventChannel.send(OrderDetailEvent.ShowMessage(error.toUiText()))
        }
    }
}

private fun primaryActionFor(order: Order): PrimaryAction? =
    when (order.status) {
        OrderStatus.ORDERED -> PrimaryAction.MARK_SHIPPED
        OrderStatus.SHIPPED, OrderStatus.DELAYED -> PrimaryAction.MARK_RECEIVED
        OrderStatus.RECEIVED -> if (order.readState != ReadState.READ) PrimaryAction.MARK_AS_READ else null
        OrderStatus.CANCELLED -> null
    }

private fun buildTimeline(order: Order): List<TimelineNode> {
    fun display(value: String): String = if (value.isBlank()) "—" else fmtDate(value)
    return when (order.status) {
        OrderStatus.CANCELLED -> {
            listOf(
                TimelineNode("Ordered", display(order.orderDate), TimelineNodeState.DONE),
                TimelineNode("Cancelled", display(order.orderDate), TimelineNodeState.CANCEL),
            )
        }

        OrderStatus.RECEIVED -> {
            listOf(
                TimelineNode("Ordered", display(order.orderDate), TimelineNodeState.DONE),
                TimelineNode("Shipped", display(order.shipDate.ifBlank { order.eta }), TimelineNodeState.DONE),
                TimelineNode("Received", display(order.receivedDate), TimelineNodeState.DONE),
            )
        }

        OrderStatus.SHIPPED -> {
            listOf(
                TimelineNode("Ordered", display(order.orderDate), TimelineNodeState.DONE),
                TimelineNode("Shipped", display(order.shipDate.ifBlank { order.eta }), TimelineNodeState.DONE),
                TimelineNode("Arriving", display(order.eta), TimelineNodeState.CURRENT),
            )
        }

        OrderStatus.DELAYED -> {
            listOf(
                TimelineNode("Ordered", display(order.orderDate), TimelineNodeState.DONE),
                TimelineNode("Delayed", display(order.delayedTo), TimelineNodeState.DELAY),
                TimelineNode("Expected", display(order.delayedTo), TimelineNodeState.CURRENT),
            )
        }

        OrderStatus.ORDERED -> {
            listOf(
                TimelineNode("Ordered", display(order.orderDate), TimelineNodeState.DONE),
                TimelineNode("Awaiting shipment", display(order.releaseDate), TimelineNodeState.CURRENT),
                TimelineNode("Delivered", "—", TimelineNodeState.TODO),
            )
        }
    }
}
