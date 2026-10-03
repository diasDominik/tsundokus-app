package uk.tsundokus.features.orders.presentation.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.CancellationException
import uk.tsundokus.features.orders.domain.export.ExportedFile

/** The native save dialog. */
@Composable
actual fun rememberExportSaver(): ExportSaver = remember { ExportSaver(::saveWithDialog) }

// The same on Android, iOS and desktop; the web has no save dialog and so has its own.
internal suspend fun saveWithDialog(file: ExportedFile): ExportSaveResult {
    val target =
        FileKit.openFileSaver(suggestedName = file.baseName, defaultExtension = file.format.extension)
            ?: return ExportSaveResult.Cancelled
    return try {
        target.write(file.bytes)
        ExportSaveResult.Saved(target.name)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ExportSaveResult.Failed
    }
}
