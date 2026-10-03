package uk.tsundokus.features.orders.presentation.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.widget_settings_background
import tsundokuapp.features.orders.presentation.generated.resources.widget_settings_background_none
import tsundokuapp.features.orders.presentation.generated.resources.widget_settings_background_solid
import tsundokuapp.features.orders.presentation.generated.resources.widget_settings_background_translucent
import tsundokuapp.features.orders.presentation.generated.resources.widget_settings_close_cd
import tsundokuapp.features.orders.presentation.generated.resources.widget_settings_colours
import tsundokuapp.features.orders.presentation.generated.resources.widget_settings_colours_tsundoku
import tsundokuapp.features.orders.presentation.generated.resources.widget_settings_colours_wallpaper
import tsundokuapp.features.orders.presentation.generated.resources.widget_settings_detail
import tsundokuapp.features.orders.presentation.generated.resources.widget_settings_detail_caption
import tsundokuapp.features.orders.presentation.generated.resources.widget_settings_done
import tsundokuapp.features.orders.presentation.generated.resources.widget_settings_heading
import tsundokuapp.features.orders.presentation.generated.resources.widget_settings_releases
import tsundokuapp.features.orders.presentation.generated.resources.widget_settings_releases_caption
import tsundokuapp.features.orders.presentation.generated.resources.widget_settings_text
import tsundokuapp.features.orders.presentation.generated.resources.widget_settings_text_automatic
import tsundokuapp.features.orders.presentation.generated.resources.widget_settings_text_caption
import tsundokuapp.features.orders.presentation.generated.resources.widget_settings_text_dark
import tsundokuapp.features.orders.presentation.generated.resources.widget_settings_text_light
import tsundokuapp.features.orders.presentation.generated.resources.widget_settings_title
import uk.tsundokus.core.designsystem.icon.TsundokuIcons
import uk.tsundokus.core.designsystem.theme.TsundokuTheme

/** Which widget a settings screen is for, told apart by the receiver the launcher placed. */
internal enum class WidgetKind(
    private val receiver: Class<out GlanceAppWidgetReceiver>,
    /** The size to preview at when the launcher hasn't reported one yet. */
    val defaultSize: DpSize,
) {
    NEXT_ARRIVALS(NextArrivalsWidgetReceiver::class.java, DpSize(150.dp, 200.dp)),
    PILE(PileWidgetReceiver::class.java, DpSize(150.dp, 108.dp)),
    ;

    fun widget(): GlanceAppWidget =
        when (this) {
            NEXT_ARRIVALS -> NextArrivalsWidget()
            PILE -> PileWidget()
        }

    companion object {
        fun of(
            context: Context,
            appWidgetId: Int,
        ): WidgetKind? {
            val provider = AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)?.provider
            return entries.firstOrNull { it.receiver.name == provider?.className }
        }
    }
}

/**
 * A placed widget's settings, opened by the launcher: when placing it on Android 11 and earlier,
 * and from its long press on Android 12 and later. Shown over the home screen's wallpaper with a
 * live preview, so a transparent widget can be judged where it will sit.
 */
class WidgetSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val appWidgetId =
            intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )
        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        // Leaving without Done keeps the widget as it was; where this opens on placing, it isn't placed.
        setResult(RESULT_CANCELED, result)
        val kind = WidgetKind.of(this, appWidgetId) ?: return finish()
        setContent {
            TsundokuTheme {
                WidgetSettingsRoot(
                    kind = kind,
                    appWidgetId = appWidgetId,
                    onClose = ::finish,
                    onSaved = {
                        setResult(RESULT_OK, result)
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
private fun WidgetSettingsRoot(
    kind: WidgetKind,
    appWidgetId: Int,
    onClose: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val glanceId = remember(appWidgetId) { GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId) }
    val previewSize = remember(appWidgetId) { placedSize(context, appWidgetId) ?: kind.defaultSize }
    var style by remember { mutableStateOf<WidgetStyle?>(null) }
    var preview by remember { mutableStateOf<RemoteViews?>(null) }

    LaunchedEffect(glanceId) {
        style = WidgetStyle.from(getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId))
    }
    val current = style ?: return
    // The real widget, drawn with the style not yet saved.
    LaunchedEffect(current) {
        preview = kind.widget().compose(context, glanceId, size = previewSize, state = current.toPreferences())
    }

    WidgetSettingsScreen(
        kind = kind,
        style = current,
        preview = preview,
        previewSize = previewSize,
        onChange = { style = it },
        onClose = onClose,
        onDone = {
            scope.launch {
                save(context, kind, glanceId, current)
                onSaved()
            }
        },
    )
}

private suspend fun save(
    context: Context,
    kind: WidgetKind,
    glanceId: GlanceId,
    style: WidgetStyle,
) {
    updateAppWidgetState(context, glanceId) { style.writeTo(it) }
    kind.widget().update(context, glanceId)
}

/** The widget's size in portrait, as the launcher reports it; null until it has. */
private fun placedSize(
    context: Context,
    appWidgetId: Int,
): DpSize? {
    val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
    val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
    val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
    return if (width > 0 && height > 0) DpSize(width.dp, height.dp) else null
}

@Composable
private fun WidgetSettingsScreen(
    kind: WidgetKind,
    style: WidgetStyle,
    preview: RemoteViews?,
    previewSize: DpSize,
    onChange: (WidgetStyle) -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // The window shows the home screen's wallpaper here, behind the preview.
        Box(modifier = Modifier.weight(1f).fillMaxWidth().statusBarsPadding()) {
            IconButton(
                onClick = onClose,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape),
            ) {
                Icon(TsundokuIcons.Close, contentDescription = stringResource(Res.string.widget_settings_close_cd))
            }
            preview?.let {
                WidgetPreview(it, Modifier.align(Alignment.Center).size(previewSize.width, previewSize.height))
            }
        }
        Surface(shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), tonalElevation = 2.dp) {
            Column(
                modifier =
                    Modifier
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(Res.string.widget_settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                StyleSettings(kind = kind, style = style, onChange = onChange)
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.widget_settings_done))
                }
            }
        }
    }
}

@Composable
private fun StyleSettings(
    kind: WidgetKind,
    style: WidgetStyle,
    onChange: (WidgetStyle) -> Unit,
) {
    ChoiceRow(
        label = Res.string.widget_settings_background,
        choices =
            listOf(
                WidgetBackground.SOLID to Res.string.widget_settings_background_solid,
                WidgetBackground.TRANSLUCENT to Res.string.widget_settings_background_translucent,
                WidgetBackground.NONE to Res.string.widget_settings_background_none,
            ),
        selected = style.background,
        onSelect = { onChange(style.copy(background = it)) },
    )
    if (style.background == WidgetBackground.NONE) {
        ChoiceRow(
            label = Res.string.widget_settings_text,
            caption = Res.string.widget_settings_text_caption,
            choices =
                listOf(
                    WidgetTextTone.AUTOMATIC to Res.string.widget_settings_text_automatic,
                    WidgetTextTone.LIGHT to Res.string.widget_settings_text_light,
                    WidgetTextTone.DARK to Res.string.widget_settings_text_dark,
                ),
            selected = style.textTone,
            onSelect = { onChange(style.copy(textTone = it)) },
        )
    }
    // Wallpaper colours exist from Android 12; before that there is nothing to choose.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ChoiceRow(
            label = Res.string.widget_settings_colours,
            choices =
                listOf(
                    WidgetPalette.WALLPAPER to Res.string.widget_settings_colours_wallpaper,
                    WidgetPalette.TSUNDOKU to Res.string.widget_settings_colours_tsundoku,
                ),
            selected = style.palette,
            onSelect = { onChange(style.copy(palette = it)) },
        )
    }
    SwitchRow(
        label = Res.string.widget_settings_heading,
        checked = style.showHeading,
        onCheckedChange = { onChange(style.copy(showHeading = it)) },
    )
    when (kind) {
        WidgetKind.NEXT_ARRIVALS -> {
            SwitchRow(
                label = Res.string.widget_settings_releases,
                caption = Res.string.widget_settings_releases_caption,
                checked = style.showReleases,
                onCheckedChange = { onChange(style.copy(showReleases = it)) },
            )
        }

        WidgetKind.PILE -> {
            SwitchRow(
                label = Res.string.widget_settings_detail,
                caption = Res.string.widget_settings_detail_caption,
                checked = style.showDetail,
                onCheckedChange = { onChange(style.copy(showDetail = it)) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChoiceRow(
    label: StringResource,
    choices: List<Pair<T, StringResource>>,
    selected: T,
    onSelect: (T) -> Unit,
    caption: StringResource? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = stringResource(label), style = MaterialTheme.typography.titleSmall)
        caption?.let {
            Text(
                text = stringResource(it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            choices.forEachIndexed { index, (value, name) ->
                SegmentedButton(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = choices.size),
                ) {
                    Text(stringResource(name), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    label: StringResource,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    caption: StringResource? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(label), style = MaterialTheme.typography.titleSmall)
            caption?.let {
                Text(
                    text = stringResource(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** The widget as the launcher would draw it. Taps are swallowed: in a preview they shouldn't open the app. */
@Composable
private fun WidgetPreview(
    remoteViews: RemoteViews,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            object : FrameLayout(context) {
                override fun onInterceptTouchEvent(event: MotionEvent): Boolean = true
            }
        },
        update = { frame ->
            frame.removeAllViews()
            frame.addView(remoteViews.apply(frame.context, frame))
        },
        modifier = modifier,
    )
}
