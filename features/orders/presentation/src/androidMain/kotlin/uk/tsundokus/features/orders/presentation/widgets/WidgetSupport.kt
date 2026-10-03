package uk.tsundokus.features.orders.presentation.widgets

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceTheme
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import org.jetbrains.compose.resources.getString
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.widget_today
import tsundokuapp.features.orders.presentation.generated.resources.widget_tomorrow
import uk.tsundokus.features.orders.domain.dates.parseIsoDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun WidgetHeading(text: String) {
    Text(
        text = text,
        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = GlanceTheme.colors.primary),
        maxLines = 1,
    )
}

/** Opens the app, on [link] when given — it is routed like any deep link. */
internal fun openAppIntent(
    context: Context,
    link: String? = null,
): Intent {
    val launch =
        context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(Intent.ACTION_MAIN).setPackage(context.packageName)
    link?.let { launch.data = Uri.parse(it) }
    return launch
}

/** "today", "tomorrow", or the date the way the device's language writes a short day: "Thu 9 Oct". */
internal suspend fun widgetDay(
    date: String,
    today: String,
    locale: Locale = Locale.getDefault(),
): String =
    when (relativeDay(date, today)) {
        RelativeDay.TODAY -> getString(Res.string.widget_today)
        RelativeDay.TOMORROW -> getString(Res.string.widget_tomorrow)
        RelativeDay.OTHER -> shortDate(date, locale)
    }

private fun shortDate(
    date: String,
    locale: Locale,
): String {
    val parts = parseIsoDate(date) ?: return date
    val pattern = DateFormat.getBestDateTimePattern(locale, "EEEdMMM")
    return LocalDate.of(parts.year, parts.month, parts.day).format(DateTimeFormatter.ofPattern(pattern, locale))
}
