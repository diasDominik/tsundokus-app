package uk.tsundokus.features.orders.presentation.di

import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import uk.tsundokus.features.orders.presentation.reminders.ReminderScheduler
import uk.tsundokus.features.orders.presentation.reminders.UserNotificationReminderScheduler

@Module
@Configuration
actual class PlatformRemindersModule {
    @Single
    fun provideReminderScheduler(): ReminderScheduler = UserNotificationReminderScheduler()
}
