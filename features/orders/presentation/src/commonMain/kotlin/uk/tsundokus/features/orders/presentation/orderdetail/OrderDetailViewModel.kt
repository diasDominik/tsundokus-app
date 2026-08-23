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
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_deleted
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_marked_status
import tsundokuapp.features.orders.presentation.generated.resources.timeline_arriving
import tsundokuapp.features.orders.presentation.generated.resources.timeline_awaiting_shipment
import tsundokuapp.features.orders.presentation.generated.resources.timeline_cancelled
import tsundokuapp.features.orders.presentation.generated.resources.timeline_delayed
import tsundokuapp.features.orders.presentation.generated.resources.timeline_delivered
import tsundokuapp.features.orders.presentation.generated.resources.timeline_expected
import tsundokuapp.features.orders.presentation.generated.resources.timeline_ordered
import tsundokuapp.features.orders.presentation.generated.resources.timeline_received
import tsundokuapp.features.orders.presentation.generated.resources.timeline_shipped
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
import uk.tsundokus.features.orders.presentation.components.fullLabelRes
import uk.tsundokus.features.orders.presentation.components.labelRes
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
                        .handle(markedStatus(OrderStatus.SHIPPED))
                }

                PrimaryAction.MARK_RECEIVED -> {
                    orderRepository
                        .setStatus(orderId, OrderStatus.RECEIVED)
                        .handle(markedStatus(OrderStatus.RECEIVED))
                }

                PrimaryAction.MARK_AS_READ -> {
                    orderRepository
                        .setReadState(orderId, ReadState.READ)
                        .handle(UiText.Resource(ReadState.READ.fullLabelRes))
                }
            }
        }
    }

    fun onSetReadState(readState: ReadState) {
        val current = state.value.order ?: return
        if (current.readState == readState) return
        viewModelScope.launch {
            orderRepository.setReadState(orderId, readState).handle(UiText.Resource(readState.fullLabelRes))
        }
    }

    fun onDelete() {
        viewModelScope.launch {
            orderRepository
                .deleteOrder(orderId)
                .onSuccess {
                    eventChannel.send(
                        OrderDetailEvent.ShowMessage(UiText.Resource(Res.string.order_detail_deleted)),
                    )
                    eventChannel.send(OrderDetailEvent.Deleted)
                }.onFailure { error ->
                    eventChannel.send(OrderDetailEvent.ShowMessage(error.toUiText()))
                }
        }
    }

    /** "Marked <status>" — the status name is a nested resource so translators control both. */
    private suspend fun markedStatus(status: OrderStatus): UiText =
        UiText.Resource(
            id = Res.string.order_detail_marked_status,
            args = arrayOf(getString(status.labelRes)),
        )

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

/** The timeline shows a dash where a date is not known yet; not translated copy. */
private const val EM_DASH = "\u2014"

private fun buildTimeline(order: Order): List<TimelineNode> {
    fun node(
        label: org.jetbrains.compose.resources.StringResource,
        value: String,
        state: TimelineNodeState,
    ): TimelineNode =
        TimelineNode(
            label = label,
            date = if (value.isBlank()) EM_DASH else fmtDate(value),
            state = state,
            isDateKnown = value.isNotBlank(),
        )
    return when (order.status) {
        OrderStatus.CANCELLED -> {
            listOf(
                node(Res.string.timeline_ordered, order.orderDate, TimelineNodeState.DONE),
                node(Res.string.timeline_cancelled, order.orderDate, TimelineNodeState.CANCEL),
            )
        }

        OrderStatus.RECEIVED -> {
            listOf(
                node(Res.string.timeline_ordered, order.orderDate, TimelineNodeState.DONE),
                node(Res.string.timeline_shipped, order.shipDate.ifBlank { order.eta }, TimelineNodeState.DONE),
                node(Res.string.timeline_received, order.receivedDate, TimelineNodeState.DONE),
            )
        }

        OrderStatus.SHIPPED -> {
            listOf(
                node(Res.string.timeline_ordered, order.orderDate, TimelineNodeState.DONE),
                node(Res.string.timeline_shipped, order.shipDate.ifBlank { order.eta }, TimelineNodeState.DONE),
                node(Res.string.timeline_arriving, order.eta, TimelineNodeState.CURRENT),
            )
        }

        OrderStatus.DELAYED -> {
            listOf(
                node(Res.string.timeline_ordered, order.orderDate, TimelineNodeState.DONE),
                node(Res.string.timeline_delayed, order.delayedTo, TimelineNodeState.DELAY),
                node(Res.string.timeline_expected, order.delayedTo, TimelineNodeState.CURRENT),
            )
        }

        OrderStatus.ORDERED -> {
            listOf(
                node(Res.string.timeline_ordered, order.orderDate, TimelineNodeState.DONE),
                node(Res.string.timeline_awaiting_shipment, order.releaseDate, TimelineNodeState.CURRENT),
                node(Res.string.timeline_delivered, "", TimelineNodeState.TODO),
            )
        }
    }
}
