package uk.tsundokus.features.orders.domain.validation

object OrderValidator {
    fun isTitleValid(title: String): Boolean = title.isNotBlank()
}
