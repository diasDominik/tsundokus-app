package uk.tsundokus.features.orders.presentation.di

import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import uk.tsundokus.features.orders.presentation.widgets.NoWidgetPublisher
import uk.tsundokus.features.orders.presentation.widgets.WidgetPublisher

/** Widgets on iOS come with their WidgetKit extension; until then there is nothing to publish to. */
@Module
@Configuration
actual class PlatformWidgetsModule {
    @Single
    fun provideWidgetPublisher(): WidgetPublisher = NoWidgetPublisher
}
