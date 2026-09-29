package uk.tsundokus.features.orders.presentation.scanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import org.jetbrains.compose.resources.stringResource
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerView
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.scanner_permission_allow
import tsundokuapp.features.orders.presentation.generated.resources.scanner_permission_denied
import tsundokuapp.features.orders.presentation.generated.resources.scanner_permission_open_settings
import tsundokuapp.features.orders.presentation.generated.resources.scanner_permission_rationale
import uk.tsundokus.features.orders.domain.models.Isbn

actual fun isCameraScanningSupported(): Boolean = true

@Composable
internal actual fun PlatformIsbnCamera(
    accept: (isbn: String) -> Boolean,
    onIsbn: (isbn: String) -> Unit,
    onError: (message: String) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasCameraPermission()) }
    var denied by remember { mutableStateOf(false) }
    // Saved so turning the phone does not ask a second time.
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            granted = result
            denied = !result
        }

    // Asked for straight away: the user opened the scanner, so the camera is what they want.
    LaunchedEffect(Unit) {
        if (!granted && !asked) {
            asked = true
            launcher.launch(Manifest.permission.CAMERA)
        }
    }
    // Coming back from the app's settings page with the camera allowed there.
    LifecycleResumeEffect(Unit) {
        granted = context.hasCameraPermission()
        onPauseOrDispose {}
    }

    when {
        granted -> {
            KScanIsbnView(accept = accept, onIsbn = onIsbn, onError = onError, modifier = modifier)
        }

        // After a refusal Android shows its prompt at most once more; past that, only the app's
        // settings page can turn the camera on.
        denied && context.findActivity()?.canAskForCameraAgain() == false -> {
            ScannerMessage(
                text = stringResource(Res.string.scanner_permission_denied),
                actionLabel = stringResource(Res.string.scanner_permission_open_settings),
                onAction = { context.openAppSettings() },
            )
        }

        else -> {
            ScannerMessage(
                text = stringResource(Res.string.scanner_permission_rationale),
                actionLabel = stringResource(Res.string.scanner_permission_allow),
                onAction = { launcher.launch(Manifest.permission.CAMERA) },
            )
        }
    }
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun Activity.canAskForCameraAgain(): Boolean =
    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

/** The KScan preview, narrowed to barcodes that are ISBNs. */
@Composable
private fun KScanIsbnView(
    accept: (isbn: String) -> Boolean,
    onIsbn: (isbn: String) -> Unit,
    onError: (message: String) -> Unit,
    modifier: Modifier,
) {
    ScannerView(
        // A book's ISBN barcode is always EAN-13; asking for nothing else keeps the decoder quick.
        codeTypes = listOf(BarcodeFormat.FORMAT_EAN_13),
        modifier = modifier,
        // Rejecting here keeps the camera running, so a non-ISBN code in view is simply passed over.
        filter = { barcode -> Isbn.normalize(barcode.data)?.let(accept) == true },
        result = { result ->
            when (result) {
                is BarcodeResult.OnSuccess -> Isbn.normalize(result.barcode.data)?.let(onIsbn)
                is BarcodeResult.OnFailed -> onError(result.exception.message.orEmpty())
            }
        },
    )
}
