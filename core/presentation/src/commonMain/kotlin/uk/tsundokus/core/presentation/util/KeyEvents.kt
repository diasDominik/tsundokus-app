package uk.tsundokus.core.presentation.util

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed

/**
 * The "app shortcut" modifier: Command on Apple platforms, Control everywhere else.
 *
 * Accepting either rather than branching on the host platform keeps this in `commonMain` and costs
 * nothing in practice — no shortcut in the app assigns different meanings to the two.
 */
val KeyEvent.isCommandOrControlPressed: Boolean
    get() = isMetaPressed || isCtrlPressed
