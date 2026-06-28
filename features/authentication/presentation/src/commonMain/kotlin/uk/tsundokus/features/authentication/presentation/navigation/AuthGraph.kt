package uk.tsundokus.features.authentication.presentation.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import uk.tsundokus.features.authentication.presentation.emailverification.EmailVerificationScreenRoot
import uk.tsundokus.features.authentication.presentation.forgotpassword.ForgotPasswordScreenRoot
import uk.tsundokus.features.authentication.presentation.login.LoginRoot
import uk.tsundokus.features.authentication.presentation.register.RegisterRoot
import uk.tsundokus.features.authentication.presentation.registersuccess.RegisterSuccessScreenRoot
import uk.tsundokus.features.authentication.presentation.resetpassword.ResetPasswordScreenRoot

val authSerializersModule =
    SerializersModule {
        polymorphic(NavKey::class) {
            subclass(SignIn::class)
            subclass(SignUp::class)
            subclass(RegisterSuccess::class)
            subclass(ForgotPassword::class)
            subclass(ResetPassword::class)
            subclass(EmailVerification::class)
        }
    }

fun EntryProviderScope<NavKey>.authGraph(
    backStack: NavBackStack<NavKey>,
    onAuthSuccess: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    entry<SignIn> {
        LoginRoot(
            backStack = backStack,
            snackbarHostState = snackbarHostState,
            onLoginSuccess = onAuthSuccess,
        )
    }

    entry<SignUp> {
        RegisterRoot(
            backStack = backStack,
            snackbarHostState = snackbarHostState,
            // Registration does not log the user in — route to the "verify your email" screen.
            onRegisterSuccess = { email ->
                backStack.add(RegisterSuccess(email))
                backStack.remove(SignUp)
            },
        )
    }

    entry<RegisterSuccess> {
        RegisterSuccessScreenRoot(
            email = it.email,
            onBackToSignIn = {
                backStack.clear()
                backStack.add(SignIn)
            },
        )
    }

    entry<ForgotPassword> {
        ForgotPasswordScreenRoot()
    }

    entry<ResetPassword> {
        ResetPasswordScreenRoot(token = it.token)
    }

    entry<EmailVerification> {
        EmailVerificationScreenRoot(
            token = it.token,
            onContinue = {
                // Verified — send the user to sign in (mirrors the reference flow). A logged-in
                // session, if any, stays valid; signing in is a no-op refresh from there.
                backStack.clear()
                backStack.add(SignIn)
            },
        )
    }
}
