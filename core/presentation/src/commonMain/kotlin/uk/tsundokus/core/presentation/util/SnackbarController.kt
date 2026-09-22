package uk.tsundokus.core.presentation.util

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Posts messages to the shell's snackbar host from a scope that outlives any single screen.
 *
 * Showing a snackbar suspends for as long as it is on screen, so a screen that awaits it before
 * navigating stays around for seconds after its work is done. Handing the message to this
 * controller instead lets the screen leave immediately: the message is displayed by the shell,
 * which is still in composition.
 */
class SnackbarController(
    private val hostState: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    fun show(message: UiText) {
        scope.launch {
            // The message from the screen we are leaving must not sit behind the one it replaced.
            hostState.currentSnackbarData?.dismiss()
            hostState.showSnackbar(message.asStringAsync())
        }
    }

    fun show(message: String) {
        show(UiText.DynamicString(message))
    }
}

/** Remembers a controller for [hostState]; call it where the host itself lives, not inside a screen. */
@Composable
fun rememberSnackbarController(hostState: SnackbarHostState): SnackbarController {
    val scope = rememberCoroutineScope()
    return remember(hostState, scope) { SnackbarController(hostState, scope) }
}
