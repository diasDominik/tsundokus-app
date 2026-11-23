package uk.tsundokus.features.orders.presentation.scanner

import io.github.vinceglb.filekit.PlatformFile
import platform.Foundation.NSURL

internal actual fun platformFileOf(uri: String): PlatformFile? = NSURL.URLWithString(uri)?.let(::PlatformFile)
