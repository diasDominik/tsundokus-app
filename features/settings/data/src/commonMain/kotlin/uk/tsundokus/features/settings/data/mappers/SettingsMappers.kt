package uk.tsundokus.features.settings.data.mappers

import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.domain.preferences.ThemeMode
import uk.tsundokus.features.settings.data.dto.SettingsDto
import uk.tsundokus.features.settings.domain.models.AppSettings

fun SettingsDto.toDomain(): AppSettings =
    AppSettings(
        theme = theme.toThemeMode(),
        currency = AppCurrency.fromName(currency),
    )

/** Server transmits the enum name (LIGHT/DARK/SYSTEM); fall back to SYSTEM on anything unexpected. */
private fun String.toThemeMode(): ThemeMode =
    runCatching {
        ThemeMode.valueOf(this)
    }.getOrDefault(ThemeMode.SYSTEM)
