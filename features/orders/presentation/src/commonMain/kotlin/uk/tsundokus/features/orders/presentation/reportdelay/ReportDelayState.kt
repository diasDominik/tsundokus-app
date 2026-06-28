package uk.tsundokus.features.orders.presentation.reportdelay

data class ReportDelayState(
    val date: String = "",
    val isSaving: Boolean = false,
) {
    val canSave: Boolean
        get() = date.isNotBlank() && !isSaving
}
