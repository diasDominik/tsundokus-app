package uk.tsundokus.features.orders.presentation.di

import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import uk.tsundokus.features.orders.presentation.widgets.AppGroupWidgetPublisher
import uk.tsundokus.features.orders.presentation.widgets.WidgetPublisher

@Module
@Configuration
actual class PlatformWidgetsModule {
    @Single
    fun provideWidgetPublisher(): WidgetPublisher = AppGroupWidgetPublisher()
}
