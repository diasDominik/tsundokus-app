package uk.tsundokus.features.settings.presentation.licenses

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.variant.LibraryDetailMode

/**
 * Renders third-party OSS libraries + their licenses. [loadLibraries] reads the AboutLibraries JSON,
 * which the app module generates at build time: it depends on every module, so it is the one that
 * sees every library the app ships. [LibraryDetailMode.Sheet] is the library's built-in mode that
 * opens a tapped library's license in a modal bottom sheet.
 */
@Composable
fun LicensesRoot(
    loadLibraries: suspend () -> String,
    modifier: Modifier = Modifier,
) {
    val libraries by produceLibraries { loadLibraries() }
    LibrariesContainer(
        libraries = libraries,
        modifier = modifier.fillMaxSize(),
        detailMode = LibraryDetailMode.Sheet,
    )
}
