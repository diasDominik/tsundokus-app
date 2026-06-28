package uk.tsundokus.features.settings.presentation.navigation
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
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
    override val label: UiText = UiText.DynamicString("Settings")
}

@Serializable
data object EditProfile : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.DynamicString("Edit profile")
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

@Serializable
data object ChangeEmail : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.DynamicString("Change email")
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

@Serializable
data object ChangePassword : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.DynamicString("Change password")
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

@Serializable
data object DeleteAccount : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.DynamicString("Delete account")
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

@Serializable
data object About : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.DynamicString("About Tsundoku")
    override val topBarAction: TopBarAction get() = TopBarAction.Back
}

@Serializable
data object Licenses : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.DynamicString("Open-source licenses")
    override val topBarAction: TopBarAction get() = TopBarAction.Back
}
