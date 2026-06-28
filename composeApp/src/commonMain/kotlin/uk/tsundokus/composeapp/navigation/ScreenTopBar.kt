package uk.tsundokus.composeapp.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import uk.tsundokus.core.presentation.navigation.TopBarAction
import uk.tsundokus.core.presentation.util.UiText
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import tsundokuapp.composeapp.generated.resources.Res
import tsundokuapp.composeapp.generated.resources.back
import tsundokuapp.composeapp.generated.resources.close
import tsundokuapp.composeapp.generated.resources.ic_arrow_back
import tsundokuapp.composeapp.generated.resources.ic_close

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTopBar(
    title: UiText,
    navigationAction: TopBarAction,
    onNavigationClick: () -> Unit,
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = { Text(title.asString()) },
        navigationIcon = {
            IconButton(onClick = onNavigationClick) {
                Icon(
                    imageVector = vectorResource(navigationAction.iconRes),
                    contentDescription = stringResource(navigationAction.contentDescriptionRes),
                )
            }
        },
        actions = { actions() },
    )
}

private val TopBarAction.iconRes: DrawableResource
    get() =
        when (this) {
            TopBarAction.Close -> Res.drawable.ic_close
            TopBarAction.Back -> Res.drawable.ic_arrow_back
        }

private val TopBarAction.contentDescriptionRes: StringResource
    get() =
        when (this) {
            TopBarAction.Close -> Res.string.close
            TopBarAction.Back -> Res.string.back
        }
