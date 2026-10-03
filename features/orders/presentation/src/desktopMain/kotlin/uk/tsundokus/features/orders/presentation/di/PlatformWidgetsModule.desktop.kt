package uk.tsundokus.features.orders.presentation.di

import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import uk.tsundokus.features.orders.presentation.widgets.NoWidgetPublisher
import uk.tsundokus.features.orders.presentation.widgets.WidgetPublisher

/** No home-screen widgets here. */
@Module
@Configuration
actual class PlatformWidgetsModule {
    @Single
    fun provideWidgetPublisher(): WidgetPublisher = NoWidgetPublisher
}
