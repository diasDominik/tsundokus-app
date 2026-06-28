package uk.tsundokus.features.settings.presentation.licenses

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.variant.LibraryDetailMode
import tsundokuapp.features.settings.presentation.generated.resources.Res

/**
 * Renders third-party OSS libraries + their licenses. The list is generated at build time by the
 * AboutLibraries gradle plugin into composeResources/files/aboutlibraries.json. [LibraryDetailMode.Sheet]
 * is the library's built-in mode that opens a tapped library's license in a modal bottom sheet.
 */
@Composable
fun LicensesRoot(modifier: Modifier = Modifier) {
    val libraries by produceLibraries {
        Res.readBytes("files/aboutlibraries.json").decodeToString()
    }
    LibrariesContainer(
        libraries = libraries,
        modifier = modifier.fillMaxSize(),
        detailMode = LibraryDetailMode.Sheet,
    )
}
