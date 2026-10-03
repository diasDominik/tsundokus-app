package uk.tsundokus.features.orders.presentation.export

import androidx.compose.runtime.Stable
import uk.tsundokus.features.orders.domain.export.ExportFormat

@Stable
data class ExportState(
    val isLoading: Boolean = true,
    val format: ExportFormat = ExportFormat.CSV,
    val orderCount: Int = 0,
    val cancelledCount: Int = 0,
    /** Between pressing save and the file being written, or the dialog being dismissed. */
    val isSaving: Boolean = false,
) {
    val canSave: Boolean get() = orderCount > 0 && !isSaving
}
