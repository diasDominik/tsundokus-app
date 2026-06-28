package uk.tsundokus.features.orders.presentation.navigation
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import uk.tsundokus.core.designsystem.icon.TsundokuIcons
import uk.tsundokus.core.presentation.navigation.FabAction
import uk.tsundokus.core.presentation.navigation.LoggableNavKey
import uk.tsundokus.core.presentation.navigation.LoggedIn
import uk.tsundokus.core.presentation.navigation.ScreenWithFab
import uk.tsundokus.core.presentation.navigation.ScreenWithTopBar
import uk.tsundokus.core.presentation.navigation.TopBarAction
import uk.tsundokus.core.presentation.navigation.TopLevelTab
import uk.tsundokus.core.presentation.util.UiText

@Serializable
data object Orders : LoggableNavKey(), TopLevelTab, ScreenWithFab {
    override val icon: ImageVector
        @Composable
        get() = TsundokuIcons.ShoppingCart
    override val selectedIcon: ImageVector
        @Composable
        get() = TsundokuIcons.ShoppingCart
    override val label: UiText = UiText.DynamicString("Orders")

    override val fabAction: FabAction = FabAction.AddOrder
}

@Serializable
data object ReadingList : LoggableNavKey(), TopLevelTab {
    override val icon: ImageVector
        @Composable
        get() = TsundokuIcons.List
    override val selectedIcon: ImageVector
        @Composable
        get() = TsundokuIcons.List
    override val label: UiText = UiText.DynamicString("Reading")
}

@Serializable
data class OrderDetail(val orderId: String) : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.DynamicString("")
    override val topBarAction: TopBarAction get() = TopBarAction.Back
}

@Serializable
data object AddOrder : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.DynamicString("Add order")
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

@Serializable
data class EditOrder(val orderId: String) : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.DynamicString("Edit order")
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}

@Serializable
data class ReportDelay(val orderId: String) : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.DynamicString("Report delay")
    override val topBarAction: TopBarAction get() = TopBarAction.Close
}
