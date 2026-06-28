package uk.tsundokus.core.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import uk.tsundokus.core.presentation.util.UiText

interface TopLevelTab : LoggedIn {
    val icon: ImageVector
        @Composable get
    val selectedIcon: ImageVector
        @Composable get
    val label: UiText
}
