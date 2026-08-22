package uk.tsundokus.features.orders.data.mappers

import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.features.orders.data.dto.OrderDto
import uk.tsundokus.features.orders.data.dto.SyncResponseDto
import uk.tsundokus.features.orders.data.dto.request.CreateOrderRequest
import uk.tsundokus.features.orders.database.entities.OrderEntity
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.domain.order.OrderSync

fun OrderDto.toDomain(): Order =
    Order(
        id = id,
        title = title,
        author = author,
        publisher = publisher,
        volume = volume,
        store = store,
        price = price,
        currency = AppCurrency.fromName(currency),
        status = OrderStatus.fromName(status),
        readState = ReadState.fromName(readState),
        orderDate = orderDate.orEmpty(),
        releaseDate = releaseDate.orEmpty(),
        shipDate = shipDate.orEmpty(),
        eta = eta.orEmpty(),
        receivedDate = receivedDate.orEmpty(),
        delayedTo = delayedTo.orEmpty(),
        createdAt = createdAt,
    )

fun Order.toEntity(pendingSync: Boolean = false): OrderEntity =
    OrderEntity(
        id = id,
        title = title,
        author = author,
        publisher = publisher,
        volume = volume,
        store = store,
        price = price,
        currency = currency.name,
        status = status.name,
        readState = readState.name,
        orderDate = orderDate,
        releaseDate = releaseDate,
        shipDate = shipDate,
        eta = eta,
        receivedDate = receivedDate,
        delayedTo = delayedTo,
        createdAt = createdAt,
        pendingSync = pendingSync,
    )

fun OrderEntity.toDomain(): Order =
    Order(
        id = id,
        title = title,
        author = author,
        publisher = publisher,
        volume = volume,
        store = store,
        price = price,
        currency = AppCurrency.fromName(currency),
        status = OrderStatus.fromName(status),
        readState = ReadState.fromName(readState),
        orderDate = orderDate,
        releaseDate = releaseDate,
        shipDate = shipDate,
        eta = eta,
        receivedDate = receivedDate,
        delayedTo = delayedTo,
        createdAt = createdAt,
    )

fun Order.toCreateRequest(): CreateOrderRequest =
    CreateOrderRequest(
        id = id,
        title = title,
        author = author,
        publisher = publisher,
        volume = volume,
        store = store,
        price = price,
        currency = currency.name,
        status = status.name,
        readState = readState.name,
        orderDate = orderDate.toNullableDate(),
        releaseDate = releaseDate.toNullableDate(),
        shipDate = shipDate.toNullableDate(),
        eta = eta.toNullableDate(),
        receivedDate = receivedDate.toNullableDate(),
        delayedTo = delayedTo.toNullableDate(),
    )

fun SyncResponseDto.toDomain(): OrderSync =
    OrderSync(
        serverTime = serverTime,
        changed = changed.map { it.toDomain() },
        deletedIds = deleted,
    )

/** App stores unset dates as blank; the JSON contract transmits them as null. */
private fun String.toNullableDate(): String? = ifBlank { null }
