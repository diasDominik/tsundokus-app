package uk.tsundokus.features.orders.presentation.export

import androidx.compose.runtime.Composable
import uk.tsundokus.features.orders.domain.export.ExportedFile

/** How saving an export ended. */
sealed interface ExportSaveResult {
    /** Saved as [fileName] — the user may have renamed it in the dialog. */
    data class Saved(val fileName: String) : ExportSaveResult

    data object Cancelled : ExportSaveResult

    data object Failed : ExportSaveResult
}

/** Saves an export where the user picks: the platform's save dialog, or a download on the web. */
fun interface ExportSaver {
    suspend fun save(file: ExportedFile): ExportSaveResult
}

@Composable
expect fun rememberExportSaver(): ExportSaver
