package uk.tsundokus.features.settings.presentation

import uk.tsundokus.core.data.AppBuildInfo

/**
 * What the About screen and the settings footer print for the app's identity.
 *
 * The name is a brand and is never translated. The version is a placeholder fed into a translatable
 * sentence rather than baked into one, so a translation cannot reorder or lose it.
 * The version is the one the build was made from, i.e. its git tag.
 */
internal object AppInfo {
    const val NAME = "Tsundoku"
    val VERSION: String = AppBuildInfo.version
}
