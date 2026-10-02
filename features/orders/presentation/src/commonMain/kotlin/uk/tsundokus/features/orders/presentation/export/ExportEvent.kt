package uk.tsundokus.features.orders.presentation.export

import uk.tsundokus.core.presentation.util.UiText
import uk.tsundokus.features.orders.domain.export.ExportedFile

sealed interface ExportEvent {
    /** The export is ready: the screen offers it to the platform's save dialog. */
    data class SaveFile(val file: ExportedFile) : ExportEvent

    data class ShowMessage(val message: UiText) : ExportEvent
}
