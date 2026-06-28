package uk.tsundokus.composeapp

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import uk.tsundokus.composeapp.deeplink.DeepLinkHandler
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val currentUrl = window.location.href
    DeepLinkHandler.onDeepLink(currentUrl)

    ComposeViewport {
        App()
    }
}
