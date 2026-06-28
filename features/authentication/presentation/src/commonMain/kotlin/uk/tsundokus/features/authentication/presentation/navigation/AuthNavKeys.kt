package uk.tsundokus.features.authentication.presentation.navigation

import kotlinx.serialization.Serializable
import uk.tsundokus.core.presentation.navigation.LoggableNavKey

@Serializable
data object SignIn : LoggableNavKey()

@Serializable
data object SignUp : LoggableNavKey()

@Serializable
data class RegisterSuccess(val email: String) : LoggableNavKey()

@Serializable
data object ForgotPassword : LoggableNavKey()

@Serializable
data class ResetPassword(val token: String) : LoggableNavKey()

@Serializable
data class EmailVerification(val token: String) : LoggableNavKey()
