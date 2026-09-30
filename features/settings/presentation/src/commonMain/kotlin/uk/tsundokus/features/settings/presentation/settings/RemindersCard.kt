package uk.tsundokus.features.settings.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tsundokuapp.features.settings.presentation.generated.resources.Res
import tsundokuapp.features.settings.presentation.generated.resources.settings_reminders
import tsundokuapp.features.settings.presentation.generated.resources.settings_reminders_blocked
import tsundokuapp.features.settings.presentation.generated.resources.settings_reminders_delayed
import tsundokuapp.features.settings.presentation.generated.resources.settings_reminders_delayed_caption
import tsundokuapp.features.settings.presentation.generated.resources.settings_reminders_enabled
import tsundokuapp.features.settings.presentation.generated.resources.settings_reminders_open_settings
import tsundokuapp.features.settings.presentation.generated.resources.settings_reminders_overdue
import tsundokuapp.features.settings.presentation.generated.resources.settings_reminders_overdue_caption
import tsundokuapp.features.settings.presentation.generated.resources.settings_reminders_time
import tsundokuapp.features.settings.presentation.generated.resources.settings_reminders_time_cancel
import tsundokuapp.features.settings.presentation.generated.resources.settings_reminders_time_confirm
import uk.tsundokus.core.domain.preferences.ReminderSettings
import uk.tsundokus.core.presentation.notifications.rememberNotificationPermissionRequester

/**
 * Reminders for overdue and delayed orders. Off until switched on here, which is when notification
 * permission is asked for — never before, and never without the user reaching for it.
 */
@Composable
internal fun RemindersCard(
    reminders: ReminderSettings,
    notificationsBlocked: Boolean,
    onAction: (SettingsAction) -> Unit,
) {
    val permission = rememberNotificationPermissionRequester()
    PreferenceCard(title = stringResource(Res.string.settings_reminders)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SwitchRow(
                title = stringResource(Res.string.settings_reminders_enabled),
                checked = reminders.enabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        permission.request { granted ->
                            onAction(
                                if (granted) {
                                    SettingsAction.SetRemindersEnabled(
                                        true,
                                    )
                                } else {
                                    SettingsAction.NotificationsBlocked
                                },
                            )
                        }
                    } else {
                        onAction(SettingsAction.SetRemindersEnabled(false))
                    }
                },
            )
            if (notificationsBlocked) {
                Text(
                    text = stringResource(Res.string.settings_reminders_blocked),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = permission::openSystemSettings) {
                    Text(stringResource(Res.string.settings_reminders_open_settings))
                }
            }
            if (reminders.enabled) {
                SwitchRow(
                    title = stringResource(Res.string.settings_reminders_overdue),
                    caption = stringResource(Res.string.settings_reminders_overdue_caption),
                    checked = reminders.overdue,
                    onCheckedChange = { onAction(SettingsAction.SetOverdueReminders(it)) },
                )
                SwitchRow(
                    title = stringResource(Res.string.settings_reminders_delayed),
                    caption = stringResource(Res.string.settings_reminders_delayed_caption),
                    checked = reminders.delayedDateReached,
                    onCheckedChange = { onAction(SettingsAction.SetDelayedDateReminders(it)) },
                )
                ReminderTimeRow(
                    hour = reminders.hour,
                    minute = reminders.minute,
                    onTimeSelected = { hour, minute -> onAction(SettingsAction.SetReminderTime(hour, minute)) },
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    caption: String? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            caption?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeRow(
    hour: Int,
    minute: Int,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable { picking = true }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.settings_reminders_time),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    if (picking) {
        // The picker follows the device's 12/24-hour setting on its own.
        val pickerState = rememberTimePickerState(initialHour = hour, initialMinute = minute)
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        picking = false
                        onTimeSelected(pickerState.hour, pickerState.minute)
                    },
                ) { Text(stringResource(Res.string.settings_reminders_time_confirm)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { picking = false },
                ) { Text(stringResource(Res.string.settings_reminders_time_cancel)) }
            },
            text = { TimePicker(state = pickerState) },
        )
    }
}
