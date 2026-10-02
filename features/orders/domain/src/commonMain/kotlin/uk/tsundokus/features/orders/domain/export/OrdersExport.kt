package uk.tsundokus.features.orders.domain.export

/** The file formats orders can be exported to. */
enum class ExportFormat(
    val extension: String,
    val mimeType: String,
) {
    /** One row per order, for spreadsheets. */
    CSV("csv", "text/csv"),

    /** Every field with its type, versioned, for backups and moving to another app. */
    JSON("json", "application/json"),
}

/** An export ready to save: [baseName] is the file name without the format's extension. */
class ExportedFile(
    val baseName: String,
    val format: ExportFormat,
    val bytes: ByteArray,
) {
    val fileName: String get() = "$baseName.${format.extension}"
}

/** Writes every order on this device out as a file. */
interface OrdersExporter {
    suspend fun export(format: ExportFormat): ExportedFile
}
