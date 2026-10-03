package uk.tsundokus.features.orders.presentation.di

import android.content.Context
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import uk.tsundokus.features.orders.presentation.widgets.GlanceWidgetPublisher
import uk.tsundokus.features.orders.presentation.widgets.WidgetPublisher

@Module
@Configuration
actual class PlatformWidgetsModule {
    @Single
    fun provideWidgetPublisher(context: Context): WidgetPublisher = GlanceWidgetPublisher(context)
}
