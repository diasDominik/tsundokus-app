package uk.tsundokus.features.orders.presentation.reportdelay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import uk.tsundokus.core.domain.util.onFailure
import uk.tsundokus.core.domain.util.onSuccess
import uk.tsundokus.core.presentation.util.UiText
import uk.tsundokus.core.presentation.util.toUiText
import uk.tsundokus.features.orders.domain.order.OrderRepository

@KoinViewModel
class ReportDelayViewModel(
    @InjectedParam private val orderId: String,
    private val orderRepository: OrderRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ReportDelayState())
    val state = _state.asStateFlow()

    private val eventChannel = Channel<ReportDelayEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onDateChange(value: String) {
        _state.update { it.copy(date = value) }
    }

    fun onSave() {
        val current = _state.value
        if (!current.canSave) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            orderRepository
                .reportDelay(orderId, current.date.trim())
                .onSuccess {
                    _state.update { it.copy(isSaving = false) }
                    eventChannel.send(ReportDelayEvent.Saved(UiText.DynamicString("Delay saved")))
                }.onFailure { error ->
                    _state.update { it.copy(isSaving = false) }
                    eventChannel.send(ReportDelayEvent.ShowError(error.toUiText()))
                }
        }
    }
}
