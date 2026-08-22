package uk.tsundokus.features.orders.data.order

import io.ktor.client.HttpClient
import org.koin.core.annotation.Single
import uk.tsundokus.core.data.networking.delete
import uk.tsundokus.core.data.networking.get
import uk.tsundokus.core.data.networking.post
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.core.domain.util.asEmptyResult
import uk.tsundokus.core.domain.util.map
import uk.tsundokus.features.orders.data.dto.OrderDto
import uk.tsundokus.features.orders.data.dto.SyncResponseDto
import uk.tsundokus.features.orders.data.dto.request.CreateOrderRequest
import uk.tsundokus.features.orders.data.mappers.toCreateRequest
import uk.tsundokus.features.orders.data.mappers.toDomain
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.order.OrderService
import uk.tsundokus.features.orders.domain.order.OrderSync

@Single(binds = [OrderService::class])
class KtorOrderService(
    private val httpClient: HttpClient,
) : OrderService {
    override suspend fun upsertOrder(order: Order): Result<Order, DataError.Remote> =
        httpClient
            .post<CreateOrderRequest, OrderDto>(
                route = "/api/orders",
                body = order.toCreateRequest(),
            ).map { it.toDomain() }

    override suspend fun deleteOrder(id: String): EmptyResult<DataError.Remote> =
        httpClient
            .delete<Unit>(route = "/api/orders/$id")
            .asEmptyResult()

    override suspend fun syncOrders(since: Long?): Result<OrderSync, DataError.Remote> =
        httpClient
            .get<SyncResponseDto>(
                route = "/api/sync/orders",
                queryParams = since?.let { mapOf("since" to it) } ?: emptyMap(),
            ).map { it.toDomain() }
}
