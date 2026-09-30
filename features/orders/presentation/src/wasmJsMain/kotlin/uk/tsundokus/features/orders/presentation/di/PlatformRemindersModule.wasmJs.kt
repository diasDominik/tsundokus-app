package uk.tsundokus.features.orders.presentation.di

import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import uk.tsundokus.features.orders.presentation.reminders.NoReminderScheduler
import uk.tsundokus.features.orders.presentation.reminders.ReminderScheduler

/** No notifications while the app is closed here, so nothing is scheduled; Settings hides reminders. */
@Module
@Configuration
actual class PlatformRemindersModule {
    @Single
    fun provideReminderScheduler(): ReminderScheduler = NoReminderScheduler
}
