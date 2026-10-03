package uk.tsundokus.features.orders.presentation.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.download

/**
 * A browser download: there is no save dialog to ask where, and no way to tell whether the user then
 * kept the file, so starting the download counts as saved.
 */
@Composable
actual fun rememberExportSaver(): ExportSaver =
    remember {
        ExportSaver { file ->
            FileKit.download(bytes = file.bytes, fileName = file.fileName)
            ExportSaveResult.Saved(file.fileName)
        }
    }
