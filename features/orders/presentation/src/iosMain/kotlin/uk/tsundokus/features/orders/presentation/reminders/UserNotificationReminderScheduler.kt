package uk.tsundokus.features.orders.presentation.reminders

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import uk.tsundokus.features.orders.presentation.components.parseIsoDate
import kotlin.coroutines.resume

/**
 * Local notifications with a calendar trigger: the date and time go to iOS as they are, not as an
 * instant, so a reminder follows the device into another time zone and through daylight saving —
 * 9:00 stays 9:00 wherever the phone is. Ours are told apart from anything else pending by [PREFIX].
 */
class UserNotificationReminderScheduler : ReminderScheduler {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun replaceAll(
        reminders: List<Reminder>,
        hour: Int,
        minute: Int,
    ) {
        center.removePendingNotificationRequestsWithIdentifiers(pendingReminderIds())
        reminders.forEach { reminder ->
            val date = parseIsoDate(reminder.date) ?: return@forEach
            val content =
                UNMutableNotificationContent().apply {
                    setTitle(reminder.title)
                    setBody(reminder.body)
                    setSound(UNNotificationSound.defaultSound)
                    setUserInfo(mapOf(DEEP_LINK_KEY to reminder.deepLink))
                }
            val components =
                NSDateComponents().apply {
                    setYear(date.year.toLong())
                    setMonth(date.month.toLong())
                    setDay(date.day.toLong())
                    setHour(hour.toLong())
                    setMinute(minute.toLong())
                }
            val request =
                UNNotificationRequest.requestWithIdentifier(
                    PREFIX + reminder.id,
                    content,
                    UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = false),
                )
            add(request)
        }
    }

    // Waits for iOS to take the request, so a replaceAll right after this one (reminders switched
    // off straight after on) finds it pending and removes it.
    private suspend fun add(request: UNNotificationRequest) =
        suspendCancellableCoroutine { continuation ->
            center.addNotificationRequest(request) { continuation.resume(Unit) }
        }

    private suspend fun pendingReminderIds(): List<String> =
        suspendCancellableCoroutine { continuation ->
            center.getPendingNotificationRequestsWithCompletionHandler { requests ->
                val ids =
                    requests
                        .orEmpty()
                        .filterIsInstance<UNNotificationRequest>()
                        .map { it.identifier }
                        .filter { it.startsWith(PREFIX) }
                continuation.resume(ids)
            }
        }

    companion object {
        private const val PREFIX = "reminder."

        /** Where the order link travels in the notification's userInfo; iOSApp.swift reads it on tap. */
        const val DEEP_LINK_KEY = "deepLink"
    }
}
