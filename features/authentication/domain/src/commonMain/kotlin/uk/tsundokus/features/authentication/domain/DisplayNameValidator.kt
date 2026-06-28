package uk.tsundokus.features.authentication.domain

object DisplayNameValidator {
    fun validate(name: String): Boolean = name.isNotBlank()
}
