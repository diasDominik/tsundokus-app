package uk.tsundokus.features.orders.presentation.widgets

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.material3.ColorProviders
import uk.tsundokus.core.designsystem.theme.TsundokuDarkColorScheme
import uk.tsundokus.core.designsystem.theme.TsundokuLightColorScheme

/** How much of the card the wallpaper shows through when it is translucent. */
private const val TRANSLUCENT_ALPHA = 0.7f

/**
 * The colours [style] asks for. Without a background, a chosen text tone wins over the system's day
 * or night: light text uses the dark scheme in both, dark text the light one.
 */
@Composable
internal fun WidgetTheme(
    style: WidgetStyle,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val bare = style.background == WidgetBackground.NONE
    when {
        bare && style.textTone == WidgetTextTone.LIGHT -> {
            GlanceTheme(
                ColorProviders(schemesFor(style, context).second),
                content,
            )
        }

        bare && style.textTone == WidgetTextTone.DARK -> {
            GlanceTheme(
                ColorProviders(schemesFor(style, context).first),
                content,
            )
        }

        // The system's own dynamic colours follow a wallpaper change without a redraw.
        style.usesWallpaperColors -> {
            GlanceTheme(content = content)
        }

        else -> {
            GlanceTheme(ColorProviders(TsundokuLightColorScheme, TsundokuDarkColorScheme), content)
        }
    }
}

/** The widget's card as [style] has it: solid, see-through, or none. */
@Composable
internal fun GlanceModifier.widgetCard(style: WidgetStyle): GlanceModifier {
    val card = fillMaxSize().appWidgetBackground().cornerRadius(20.dp)
    val withBackground =
        when (style.background) {
            WidgetBackground.SOLID -> {
                card.background(GlanceTheme.colors.widgetBackground)
            }

            WidgetBackground.TRANSLUCENT -> {
                val (light, dark) = schemesFor(style, LocalContext.current)
                card.background(
                    ColorProvider(
                        day = light.surfaceContainer.copy(alpha = TRANSLUCENT_ALPHA),
                        night = dark.surfaceContainer.copy(alpha = TRANSLUCENT_ALPHA),
                    ),
                )
            }

            WidgetBackground.NONE -> {
                card
            }
        }
    return withBackground.padding(12.dp)
}

/** The light and dark schemes behind [style]'s palette. */
private fun schemesFor(
    style: WidgetStyle,
    context: Context,
): Pair<ColorScheme, ColorScheme> =
    if (style.usesWallpaperColors) {
        dynamicLightColorScheme(context) to dynamicDarkColorScheme(context)
    } else {
        TsundokuLightColorScheme to TsundokuDarkColorScheme
    }
