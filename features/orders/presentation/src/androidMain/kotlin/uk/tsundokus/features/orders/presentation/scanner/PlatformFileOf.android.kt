package uk.tsundokus.features.orders.presentation.scanner

import android.net.Uri
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.PlatformFile

internal actual fun platformFileOf(uri: String): PlatformFile? =
    PlatformFile(AndroidFile.UriWrapper(Uri.parse(uri)))
