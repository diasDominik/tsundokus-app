package uk.tsundokus.features.orders.presentation.scanner

import io.github.vinceglb.filekit.PlatformFile

// Nothing is shared into the app here, and images are not read for text.
internal actual fun platformFileOf(uri: String): PlatformFile? = null
