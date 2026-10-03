package uk.tsundokus.features.orders.presentation.widgets

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlin.test.Test
import kotlin.test.assertEquals

class WidgetStyleTest {
    @Test
    fun `a widget never configured gets the defaults`() {
        assertEquals(WidgetStyle(), WidgetStyle.from(emptyPreferences()))
    }

    @Test
    fun `a style reads back as it was saved`() {
        val style =
            WidgetStyle(
                background = WidgetBackground.NONE,
                textTone = WidgetTextTone.LIGHT,
                palette = WidgetPalette.TSUNDOKU,
                showHeading = false,
                showReleases = false,
                showDetail = false,
            )

        assertEquals(style, WidgetStyle.from(style.toPreferences()))
    }

    @Test
    fun `a value a newer version wrote falls back to the default`() {
        val preferences = mutablePreferencesOf(stringPreferencesKey("style.background") to "GLASS")

        assertEquals(WidgetBackground.SOLID, WidgetStyle.from(preferences).background)
    }
}
