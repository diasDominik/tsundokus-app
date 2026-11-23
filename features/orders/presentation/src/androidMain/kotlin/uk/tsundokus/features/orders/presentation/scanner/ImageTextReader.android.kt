package uk.tsundokus.features.orders.presentation.scanner

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
internal actual fun rememberImageTextReader(): ImageTextReader? {
    val context = LocalContext.current.applicationContext
    return remember(context) { MlKitImageTextReader(context).takeIf { context.hasPlayServices() } }
}

// The model comes with Play services; without them (a de-Googled phone) there is nothing to read with.
private fun Context.hasPlayServices(): Boolean =
    GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS

/**
 * ML Kit's Latin text recognition, run on the device by Google Play services. The Latin model reads
 * the digits on Japanese pages too, which is all an ISBN needs.
 */
private class MlKitImageTextReader(
    private val context: Context,
) : ImageTextReader {
    override suspend fun read(image: PlatformFile): String {
        val uri =
            when (val file = image.androidFile) {
                is AndroidFile.UriWrapper -> file.uri
                is AndroidFile.FileWrapper -> Uri.fromFile(file.file)
            }
        // Decodes the image and applies its EXIF rotation, so a sideways photo reads upright.
        val input = withContext(Dispatchers.IO) { InputImage.fromFilePath(context, uri) }
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            recognizer.process(input).await().text
        } finally {
            recognizer.close()
        }
    }
}

internal suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }
