package uk.tsundokus.core.data.preferences

import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import uk.tsundokus.core.domain.preferences.ReminderPreferences
import uk.tsundokus.core.domain.preferences.ReminderSettings

@Serializable
private data class StoredReminderSettings(
    val enabled: Boolean,
    val overdue: Boolean,
    val delayedDateReached: Boolean,
    val hour: Int,
    val minute: Int,
)

@Single(binds = [ReminderPreferences::class])
class KSafeReminderPreferences(
    @Named("prefs") private val prefs: KSafe,
    private val json: Json,
) : ReminderPreferences {
    override fun settings(): Flow<ReminderSettings> =
        prefs.getFlow(KEY, "").map { stored ->
            if (stored.isBlank()) return@map ReminderSettings()
            // Settings written by an app version that no longer decodes start over from the defaults.
            try {
                json.decodeFromString<StoredReminderSettings>(stored).toSettings()
            } catch (_: SerializationException) {
                ReminderSettings()
            }
        }

    override suspend fun update(settings: ReminderSettings) {
        prefs.put(KEY, json.encodeToString(settings.toStored()), KSafeWriteMode.Plain)
    }

    private companion object {
        const val KEY = "reminderSettings"
    }
}

private fun StoredReminderSettings.toSettings() =
    ReminderSettings(enabled, overdue, delayedDateReached, hour, minute)

private fun ReminderSettings.toStored() =
    StoredReminderSettings(enabled, overdue, delayedDateReached, hour, minute)
