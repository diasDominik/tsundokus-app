package uk.tsundokus.features.authentication.presentation.emailverification

data class EmailVerificationState(
    val isVerifying: Boolean = true,
    val isVerified: Boolean = false,
)
