package uk.tsundokus.core.designsystem.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.theme.TsundokuTheme

/**
 * Resolved visual configuration for a [TsundokuButton].
 * Produced by [TsundokuButtonStyle.resolve].
 */
@Immutable
data class TsundokuButtonConfig(
    val colors: ButtonColors,
    val border: BorderStroke?,
)

/**
 * Available visual styles for [TsundokuButton].
 *
 * Each entry knows how to [resolve] itself into concrete [ButtonColors] and
 * an optional [BorderStroke] so the composable stays free of branching logic.
 */
enum class TsundokuButtonStyle {
    Primary {
        @Composable
        override fun resolve(enabled: Boolean) =
            TsundokuButtonConfig(
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    ),
                border = if (!enabled) disabledBorder() else null,
            )
    },

    DestructivePrimary {
        @Composable
        override fun resolve(enabled: Boolean) =
            TsundokuButtonConfig(
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    ),
                border = if (!enabled) disabledBorder() else null,
            )
    },

    Secondary {
        @Composable
        override fun resolve(enabled: Boolean) =
            TsundokuButtonConfig(
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    ),
                border =
                    BorderStroke(
                        width = 1.dp,
                        color =
                            if (enabled) {
                                MaterialTheme.colorScheme.outline
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                    ),
            )
    },

    DestructiveSecondary {
        @Composable
        override fun resolve(enabled: Boolean) =
            TsundokuButtonConfig(
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.error,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    ),
                border =
                    BorderStroke(
                        width = 1.dp,
                        color =
                            if (enabled) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                    ),
            )
    },

    Text {
        @Composable
        override fun resolve(enabled: Boolean) =
            TsundokuButtonConfig(
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    ),
                border = null,
            )
    },
    ;

    /** Returns the [TsundokuButtonConfig] for this style given the current theme. */
    @Composable
    abstract fun resolve(enabled: Boolean): TsundokuButtonConfig
}

/** Thin border used on disabled filled buttons so they don't vanish. */
@Composable
private fun disabledBorder() =
    BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )

/**
 * A unified Tsundoku button whose appearance is driven by [style].
 *
 * @param text         Label shown inside the button.
 * @param onClick      Called when the button is clicked.
 * @param style        Visual style — see [TsundokuButtonStyle].
 * @param enabled      Whether the button is interactive.
 * @param isLoading    When `true` a spinner replaces the label (button stays same size).
 * @param leadingIcon  Optional composable rendered before the label.
 */
@Composable
fun TsundokuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TsundokuButtonStyle = TsundokuButtonStyle.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    val config = style.resolve(enabled)

    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !isLoading,
        shape = MaterialTheme.shapes.small,
        colors = config.colors,
        border = config.border,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 6.dp),
        ) {
            CircularProgressIndicator(
                modifier =
                    Modifier
                        .size(18.dp)
                        .alpha(if (isLoading) 1f else 0f),
                strokeWidth = 1.5.dp,
                color = config.colors.contentColor,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(if (isLoading) 0f else 1f),
            ) {
                leadingIcon?.invoke()
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@PreviewThemes
@Composable
private fun PreviewTsundokuButtons() {
    TsundokuTheme {
        Surface {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                TsundokuButton(text = "Primary", onClick = {}, style = TsundokuButtonStyle.Primary)
                TsundokuButton(text = "Destructive", onClick = {}, style = TsundokuButtonStyle.DestructivePrimary)
                TsundokuButton(text = "Secondary", onClick = {}, style = TsundokuButtonStyle.Secondary)
                TsundokuButton(
                    text = "Destructive Secondary",
                    onClick = {},
                    style = TsundokuButtonStyle.DestructiveSecondary,
                )
                TsundokuButton(text = "Text", onClick = {}, style = TsundokuButtonStyle.Text)
            }
        }
    }
}

@PreviewThemes
@Composable
private fun PreviewTsundokuButtonsDisabled() {
    TsundokuTheme {
        Surface {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                TsundokuButton(
                    text = "Disabled Primary",
                    onClick = {},
                    style = TsundokuButtonStyle.Primary,
                    enabled = false,
                )
                TsundokuButton(
                    text = "Disabled Secondary",
                    onClick = {},
                    style = TsundokuButtonStyle.Secondary,
                    enabled = false,
                )
                TsundokuButton(
                    text = "Loading…",
                    onClick = {},
                    style = TsundokuButtonStyle.Primary,
                    isLoading = true,
                )
            }
        }
    }
}
