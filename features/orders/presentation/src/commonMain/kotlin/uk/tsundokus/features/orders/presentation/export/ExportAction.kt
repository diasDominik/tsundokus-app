package uk.tsundokus.features.orders.presentation.export

import uk.tsundokus.features.orders.domain.export.ExportFormat

sealed interface ExportAction {
    data class OnFormatSelected(val format: ExportFormat) : ExportAction

    data object OnSave : ExportAction

    /** The save dialog closed; the screen reports how, since only it can show the dialog. */
    data class OnSaveFinished(val result: ExportSaveResult) : ExportAction
}
