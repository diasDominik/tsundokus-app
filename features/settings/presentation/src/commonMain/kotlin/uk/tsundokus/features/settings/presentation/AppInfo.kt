package uk.tsundokus.features.settings.presentation

/**
 * What the About screen and the settings footer print for the app's identity.
 *
 * The name is a brand and is never translated. The version is a placeholder fed into a translatable
 * sentence rather than baked into one, so a translation cannot reorder or lose it.
 *
 * NOTE: [VERSION] is hardcoded and currently disagrees with `APP_VERSION` in `gradle.properties`
 * (0.1.0), which is what actually ships as the Android versionName. Both display sites used to
 * hardcode "1.0" separately; they are at least consistent with each other now. Wiring this to
 * BuildKonfig is the real fix.
 */
internal object AppInfo {
    const val NAME = "Tsundoku"
    const val VERSION = "0.1.0"
}
