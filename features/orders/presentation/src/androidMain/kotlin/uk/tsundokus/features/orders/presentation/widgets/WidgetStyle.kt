package uk.tsundokus.features.orders.presentation.widgets

import android.os.Build
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey

enum class WidgetBackground {
    /** The launcher's widget card. */
    SOLID,

    /** The card, with the wallpaper showing through. */
    TRANSLUCENT,

    /** No card: the text sits on the wallpaper. */
    NONE,
}

/**
 * Text colour without a background. With a card the theme picks text that reads on it; on the bare
 * wallpaper only the user can tell whether light or dark text reads better.
 */
enum class WidgetTextTone { AUTOMATIC, LIGHT, DARK }

enum class WidgetPalette {
    /** Material You: colours taken from the wallpaper (Android 12 and later). */
    WALLPAPER,

    /** The app's own colours. */
    TSUNDOKU,
}

/**
 * How one placed widget looks, chosen in its settings screen and kept in that widget's Glance state,
 * so two widgets of the same kind can differ. What it shows comes from the snapshot either way.
 */
data class WidgetStyle(
    val background: WidgetBackground = WidgetBackground.SOLID,
    val textTone: WidgetTextTone = WidgetTextTone.AUTOMATIC,
    val palette: WidgetPalette = WidgetPalette.WALLPAPER,
    val showHeading: Boolean = true,
    /** Next arrivals: include orders still waiting for their release, not just parcels on the way. */
    val showReleases: Boolean = true,
    /** Your pile: the line with how many are being read and how long the oldest has waited. */
    val showDetail: Boolean = true,
) {
    /** Wallpaper colours exist from Android 12; before that the app's own stand in. */
    val usesWallpaperColors: Boolean
        get() = palette == WidgetPalette.WALLPAPER && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun writeTo(preferences: MutablePreferences) {
        preferences[BACKGROUND] = background.name
        preferences[TEXT_TONE] = textTone.name
        preferences[PALETTE] = palette.name
        preferences[SHOW_HEADING] = showHeading
        preferences[SHOW_RELEASES] = showReleases
        preferences[SHOW_DETAIL] = showDetail
    }

    /** This style as widget state, to render a preview of it before it is saved. */
    fun toPreferences(): Preferences = mutablePreferencesOf().also(::writeTo)

    companion object {
        private val BACKGROUND = stringPreferencesKey("style.background")
        private val TEXT_TONE = stringPreferencesKey("style.textTone")
        private val PALETTE = stringPreferencesKey("style.palette")
        private val SHOW_HEADING = booleanPreferencesKey("style.showHeading")
        private val SHOW_RELEASES = booleanPreferencesKey("style.showReleases")
        private val SHOW_DETAIL = booleanPreferencesKey("style.showDetail")

        /** The style kept in [preferences]; the defaults for a widget never configured. */
        fun from(preferences: Preferences): WidgetStyle {
            val defaults = WidgetStyle()
            return WidgetStyle(
                background = enumOr(preferences[BACKGROUND], defaults.background),
                textTone = enumOr(preferences[TEXT_TONE], defaults.textTone),
                palette = enumOr(preferences[PALETTE], defaults.palette),
                showHeading = preferences[SHOW_HEADING] ?: defaults.showHeading,
                showReleases = preferences[SHOW_RELEASES] ?: defaults.showReleases,
                showDetail = preferences[SHOW_DETAIL] ?: defaults.showDetail,
            )
        }

        private inline fun <reified E : Enum<E>> enumOr(
            name: String?,
            default: E,
        ): E = enumValues<E>().firstOrNull { it.name == name } ?: default
    }
}
