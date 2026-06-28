package uk.tsundokus.features.orders.data.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import uk.tsundokus.features.orders.database.DatabaseFactory
import uk.tsundokus.features.orders.database.TsundokuDatabase
import uk.tsundokus.features.orders.database.dao.OrderDao

@Module
@Configuration
actual class PlatformOrdersDataModule {
    @Single
    fun provideDatabaseFactory(): DatabaseFactory = DatabaseFactory()

    @Single
    fun provideDatabase(factory: DatabaseFactory): TsundokuDatabase =
        factory
            .create()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Single
    fun provideOrderDao(database: TsundokuDatabase): OrderDao = database.orderDao
}
