package uk.tsundokus.features.orders.presentation.export

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.CancellationException
import uk.tsundokus.features.orders.domain.export.ExportedFile

/**
 * The system "save to" dialog. FileKit launches it through an activity result registry, handed over
 * here rather than in MainActivity so the app module needn't know about FileKit.
 */
@Composable
actual fun rememberExportSaver(): ExportSaver {
    val registry = LocalActivityResultRegistryOwner.current?.activityResultRegistry
    return remember(registry) {
        ExportSaver { file ->
            registry?.let { FileKit.init(it) } ?: return@ExportSaver ExportSaveResult.Failed
            saveWithDialog(file)
        }
    }
}

// The same on Android, iOS and desktop; the web has no save dialog and so has its own.
internal suspend fun saveWithDialog(file: ExportedFile): ExportSaveResult {
    val target =
        FileKit.openFileSaver(suggestedName = file.baseName, extension = file.format.extension)
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
