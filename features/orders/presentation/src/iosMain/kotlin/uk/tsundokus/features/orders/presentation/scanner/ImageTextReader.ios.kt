package uk.tsundokus.features.orders.presentation.scanner

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSError
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate

@Composable
internal actual fun rememberImageTextReader(): ImageTextReader? = VisionImageTextReader

/** Apple's Vision text recognition, on the device. */
private object VisionImageTextReader : ImageTextReader {
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun read(image: PlatformFile): String =
        withContext(Dispatchers.IO) {
            val request =
                VNRecognizeTextRequest().apply {
                    recognitionLevel = VNRequestTextRecognitionLevelAccurate
                    // Correction is for words; it would only "fix" a number into something else.
                    usesLanguageCorrection = false
                    automaticallyDetectsLanguage = true
                }
            val handler = VNImageRequestHandler(uRL = image.nsUrl, options = emptyMap<Any?, Any?>())
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                if (!handler.performRequests(listOf(request), error.ptr)) {
                    error(error.value?.localizedDescription ?: "Vision could not read the image")
                }
            }
            request.results
                .orEmpty()
                .filterIsInstance<VNRecognizedTextObservation>()
                .mapNotNull { (it.topCandidates(1u).firstOrNull() as? VNRecognizedText)?.string }
                .joinToString("\n")
        }
}
