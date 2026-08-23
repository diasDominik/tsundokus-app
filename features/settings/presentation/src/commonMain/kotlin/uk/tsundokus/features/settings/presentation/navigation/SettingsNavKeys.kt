package uk.tsundokus.features.settings.presentation.navigation
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import tsundokuapp.features.settings.presentation.generated.resources.Res
import tsundokuapp.features.settings.presentation.generated.resources.nav_about
import tsundokuapp.features.settings.presentation.generated.resources.nav_change_email
import tsundokuapp.features.settings.presentation.generated.resources.nav_change_password
import tsundokuapp.features.settings.presentation.generated.resources.nav_delete_account
import tsundokuapp.features.settings.presentation.generated.resources.nav_edit_profile
import tsundokuapp.features.settings.presentation.generated.resources.nav_licenses
import tsundokuapp.features.settings.presentation.generated.resources.nav_settings
import uk.tsundokus.core.designsystem.icon.TsundokuIcons
import uk.tsundokus.core.presentation.navigation.LoggableNavKey
import uk.tsundokus.core.presentation.navigation.LoggedIn
import uk.tsundokus.core.presentation.navigation.ScreenWithTopBar
import uk.tsundokus.core.presentation.navigation.TopBarAction
import uk.tsundokus.core.presentation.navigation.TopLevelTab
import uk.tsundokus.core.presentation.util.UiText

@Serializable
data object Settings : LoggableNavKey(), TopLevelTab {
    override val icon: ImageVector
        @Composable
        get() = TsundokuIcons.Settings
    override val selectedIcon: ImageVector
        @Composable
        get() = TsundokuIcons.Settings
    override val label: UiText = UiText.Resource(Res.string.nav_settings)
}

@Serializable
data object EditProfile : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.nav_edit_profile)
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

@Serializable
data object ChangeEmail : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.nav_change_email)
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

@Serializable
data object ChangePassword : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.nav_change_password)
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

@Serializable
data object DeleteAccount : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.nav_delete_account)
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

@Serializable
data object About : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.nav_about)
    override val topBarAction: TopBarAction get() = TopBarAction.Back
}

@Serializable
data object Licenses : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.Resource(Res.string.nav_licenses)
    override val topBarAction: TopBarAction get() = TopBarAction.Back
}
