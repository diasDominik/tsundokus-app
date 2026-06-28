package uk.tsundokus.features.orders.sqlitewasmworker

import androidx.sqlite.driver.web.WebWorkerSQLiteDriver

expect fun createSQLiteWasmWorker(): WebWorkerSQLiteDriver
