package uk.tsundokus.features.orders.presentation.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.export_failed
import tsundokuapp.features.orders.presentation.generated.resources.export_saved
import uk.tsundokus.core.presentation.util.UiText
import uk.tsundokus.features.orders.domain.export.ExportFormat
import uk.tsundokus.features.orders.domain.export.OrdersExporter
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.order.OrderRepository
import kotlin.time.Duration.Companion.seconds

/**
 * Builds the export and hands it to the screen, which owns the save dialog; the screen reports back
 * how the dialog ended. Reads the cached orders only, so it works offline.
 */
@KoinViewModel
class ExportViewModel(
    orderRepository: OrderRepository,
    private val exporter: OrdersExporter,
) : ViewModel() {
    private val eventChannel = Channel<ExportEvent>()
    val events = eventChannel.receiveAsFlow()

    private val format = MutableStateFlow(ExportFormat.CSV)
    private val isSaving = MutableStateFlow(false)

    val state: StateFlow<ExportState> =
        combine(
            orderRepository.getOrders().onStart { emit(emptyList()) },
            format,
            isSaving,
        ) { orders, format, saving ->
            ExportState(
                isLoading = false,
                format = format,
                orderCount = orders.size,
                cancelledCount = orders.count { it.status == OrderStatus.CANCELLED },
                isSaving = saving,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = ExportState(),
        )

    fun onAction(action: ExportAction) {
        when (action) {
            is ExportAction.OnFormatSelected -> format.value = action.format
            ExportAction.OnSave -> save()
            is ExportAction.OnSaveFinished -> onSaveFinished(action.result)
        }
    }

    private fun save() {
        if (isSaving.value) return
        isSaving.value = true
        viewModelScope.launch {
            eventChannel.send(ExportEvent.SaveFile(exporter.export(format.value)))
        }
    }

    private fun onSaveFinished(result: ExportSaveResult) {
        isSaving.value = false
        val message =
            when (result) {
                is ExportSaveResult.Saved -> UiText.Resource(Res.string.export_saved, arrayOf(result.fileName))

                ExportSaveResult.Failed -> UiText.Resource(Res.string.export_failed)

                // Closing the dialog was the user's choice; nothing to tell them.
                ExportSaveResult.Cancelled -> return
            }
        viewModelScope.launch { eventChannel.send(ExportEvent.ShowMessage(message)) }
    }
}
