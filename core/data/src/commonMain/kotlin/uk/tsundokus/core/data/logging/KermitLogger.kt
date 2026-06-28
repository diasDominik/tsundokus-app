package uk.tsundokus.core.data.logging

import co.touchlab.kermit.Logger
import org.koin.core.annotation.Single
import uk.tsundokus.core.domain.logging.TsundokuLogger

@Single(binds = [TsundokuLogger::class])
class KermitLogger : TsundokuLogger {
    override fun debug(
        tag: String,
        message: String,
    ) {
        Logger.d(tag = tag) { message }
    }

    override fun info(
        tag: String,
        message: String,
    ) {
        Logger.i(tag = tag) { message }
    }

    override fun warning(
        tag: String,
        message: String,
    ) {
        Logger.w(tag = tag) { message }
    }

    override fun error(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        Logger.e(tag = tag, throwable = throwable) { message }
    }
}
