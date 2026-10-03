package uk.tsundokus.features.orders.presentation.widgets

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import androidx.core.content.pm.PackageInfoCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import uk.tsundokus.features.orders.domain.dates.isoPlusDays

/**
 * What the widget picker shows: made-up orders, so the picker looks the same signed in or out and
 * never shows anyone's own titles. Dated from [today] so "today" and "tomorrow" read right.
 */
internal fun previewSnapshot(today: String): WidgetSnapshot =
    WidgetSnapshot(
        signedIn = true,
        arrivals =
            listOf(
                WidgetArrival("preview-1", "Frieren", "13", today, "SHIPPED"),
                WidgetArrival("preview-2", "Dandadan", "18", isoPlusDays(today, 1) ?: today, "ORDERED"),
                WidgetArrival("preview-3", "Berserk", "42", isoPlusDays(today, 6) ?: today, "DELAYED"),
            ),
        unread = 12,
        reading = 2,
        oldestUnreadSince = isoPlusDays(today, -30),
    )

/**
 * Hands the launcher both widgets' picker previews, drawn from their providePreview (Android 15 and
 * later; earlier the picker shows the app icon). Once per app version: the system rate-limits these
 * calls, and a preview only changes when the widget's code does.
 */
internal object WidgetPreviews {
    private const val PREFS = "widget_previews"
    private const val PUBLISHED_VERSION = "published_version"

    suspend fun publishOnce(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val version =
            PackageInfoCompat.getLongVersionCode(
                context.packageManager.getPackageInfo(context.packageName, 0),
            )
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getLong(PUBLISHED_VERSION, -1) == version) return
        val manager = GlanceAppWidgetManager(context)
        val published =
            listOf(NextArrivalsWidgetReceiver::class, PileWidgetReceiver::class)
                .map { manager.setWidgetPreviews(it) }
                .all { it == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS }
        // Rate-limited or refused: tried again next time the app runs.
        if (published) prefs.edit { putLong(PUBLISHED_VERSION, version) }
    }
}
