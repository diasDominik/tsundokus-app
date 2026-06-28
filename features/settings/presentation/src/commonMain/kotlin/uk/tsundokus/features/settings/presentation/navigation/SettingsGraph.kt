package uk.tsundokus.features.settings.presentation.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import uk.tsundokus.features.settings.presentation.about.AboutRoot
import uk.tsundokus.features.settings.presentation.changeemail.ChangeEmailRoot
import uk.tsundokus.features.settings.presentation.changepassword.ChangePasswordRoot
import uk.tsundokus.features.settings.presentation.deleteaccount.DeleteAccountRoot
import uk.tsundokus.features.settings.presentation.editprofile.EditProfileRoot
import uk.tsundokus.features.settings.presentation.licenses.LicensesRoot
import uk.tsundokus.features.settings.presentation.settings.SettingsRoot

val settingsSerializersModule =
    SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Settings::class)
            subclass(EditProfile::class)
            subclass(ChangeEmail::class)
            subclass(ChangePassword::class)
            subclass(DeleteAccount::class)
            subclass(About::class)
            subclass(Licenses::class)
        }
    }

fun EntryProviderScope<NavKey>.settingsGraph(
    backStack: NavBackStack<NavKey>,
    onSignedOut: () -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    accountName: String,
    accountEmail: String,
) {
    entry<Settings> {
        SettingsRoot(
            onSignedOut = onSignedOut,
            accountName = accountName,
            accountEmail = accountEmail,
            onEditProfile = { backStack.add(EditProfile) },
            onChangeEmail = { backStack.add(ChangeEmail) },
            onChangePassword = { backStack.add(ChangePassword) },
            onDeleteAccount = { backStack.add(DeleteAccount) },
            onAbout = { backStack.add(About) },
            onLicenses = { backStack.add(Licenses) },
            snackbarHostState = snackbarHostState,
        )
    }

    entry<EditProfile> { route ->
        EditProfileRoot(
            navKey = route,
            snackbarHostState = snackbarHostState,
            onSaved = onBack,
        )
    }

    entry<ChangeEmail> { route ->
        ChangeEmailRoot(
            navKey = route,
            snackbarHostState = snackbarHostState,
            onSaved = onBack,
        )
    }

    entry<ChangePassword> { route ->
        ChangePasswordRoot(
            navKey = route,
            snackbarHostState = snackbarHostState,
            onSaved = onBack,
        )
    }

    entry<DeleteAccount> {
        DeleteAccountRoot(
            snackbarHostState = snackbarHostState,
            onDeleted = onSignedOut,
        )
    }

    entry<About> {
        AboutRoot()
    }

    entry<Licenses> {
        LicensesRoot()
    }
}
