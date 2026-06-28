package uk.tsundokus.features.orders.data.order

import io.ktor.client.HttpClient
import org.koin.core.annotation.Single
import uk.tsundokus.core.data.networking.delete
import uk.tsundokus.core.data.networking.get
import uk.tsundokus.core.data.networking.patch
import uk.tsundokus.core.data.networking.post
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.core.domain.util.asEmptyResult
import uk.tsundokus.core.domain.util.map
import uk.tsundokus.features.orders.data.dto.OrderDto
import uk.tsundokus.features.orders.data.dto.request.CreateOrderRequest
import uk.tsundokus.features.orders.data.dto.request.ReportDelayRequest
import uk.tsundokus.features.orders.data.dto.request.SetReadStateRequest
import uk.tsundokus.features.orders.data.dto.request.SetStatusRequest
import uk.tsundokus.features.orders.data.dto.request.UpdateOrderRequest
import uk.tsundokus.features.orders.data.mappers.toCreateRequest
import uk.tsundokus.features.orders.data.mappers.toDomain
import uk.tsundokus.features.orders.data.mappers.toUpdateRequest
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.domain.order.OrderService

@Single(binds = [OrderService::class])
class KtorOrderService(
    private val httpClient: HttpClient,
) : OrderService {
    override suspend fun getOrders(): Result<List<Order>, DataError.Remote> =
        httpClient
            .get<List<OrderDto>>(route = "/api/orders")
            .map { orderDtos -> orderDtos.map { it.toDomain() } }

    override suspend fun createOrder(order: Order): Result<Order, DataError.Remote> =
        httpClient
            .post<CreateOrderRequest, OrderDto>(
                route = "/api/orders",
                body = order.toCreateRequest(),
            ).map { it.toDomain() }

    override suspend fun updateOrder(order: Order): Result<Order, DataError.Remote> =
        httpClient
            .patch<UpdateOrderRequest, OrderDto>(
                route = "/api/orders/${order.id}",
                body = order.toUpdateRequest(),
            ).map { it.toDomain() }

    override suspend fun deleteOrder(id: String): EmptyResult<DataError.Remote> =
        httpClient
            .delete<Unit>(route = "/api/orders/$id")
            .asEmptyResult()

    override suspend fun setStatus(
        id: String,
        status: OrderStatus,
    ): Result<Order, DataError.Remote> =
        httpClient
            .patch<SetStatusRequest, OrderDto>(
                route = "/api/orders/$id/status",
                body = SetStatusRequest(status = status.name),
            ).map { it.toDomain() }

    override suspend fun reportDelay(
        id: String,
        delayedTo: String,
    ): Result<Order, DataError.Remote> =
        httpClient
            .post<ReportDelayRequest, OrderDto>(
                route = "/api/orders/$id/report-delay",
                body = ReportDelayRequest(delayedTo = delayedTo),
            ).map { it.toDomain() }

    override suspend fun setReadState(
        id: String,
        readState: ReadState,
    ): Result<Order, DataError.Remote> =
        httpClient
            .patch<SetReadStateRequest, OrderDto>(
                route = "/api/orders/$id/read-state",
                body = SetReadStateRequest(readState = readState.name),
            ).map { it.toDomain() }
}
