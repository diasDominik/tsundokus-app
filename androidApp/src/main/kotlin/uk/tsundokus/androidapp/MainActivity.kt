package uk.tsundokus.androidapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.IntentCompat
import uk.tsundokus.composeapp.App
import uk.tsundokus.composeapp.deeplink.DeepLinkHandler
import uk.tsundokus.composeapp.share.ShareHandler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIntent(intent)
        // Recreated (turned, say) with the same intent: the share was already turned into an order.
        if (savedInstanceState == null) handleShare(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        handleShare(intent)
    }

    private fun handleIntent(intent: Intent) {
        val uri = intent.data?.toString() ?: return
        DeepLinkHandler.onDeepLink(uri)
    }

    /** A screenshot or text shared from another app, to start a new order with the ISBN in it. */
    private fun handleShare(intent: Intent) {
        if (intent.action != Intent.ACTION_SEND) return
        val type = intent.type ?: return
        val shared =
            when {
                type.startsWith("image/") -> {
                    IntentCompat
                        .getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                        ?.let { ShareHandler.Shared.Image(it.toString()) }
                }

                type == "text/plain" -> {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let(ShareHandler.Shared::Text)
                }

                else -> {
                    null
                }
            }
        shared?.let(ShareHandler::onShared)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
