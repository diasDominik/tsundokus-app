package uk.tsundokus.core.presentation.currency

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.stringResource
import tsundokuapp.core.presentation.generated.resources.Res
import tsundokuapp.core.presentation.generated.resources.currency_field_cd
import tsundokuapp.core.presentation.generated.resources.currency_picker_all
import tsundokuapp.core.presentation.generated.resources.currency_picker_close
import tsundokuapp.core.presentation.generated.resources.currency_picker_no_match
import tsundokuapp.core.presentation.generated.resources.currency_picker_search
import tsundokuapp.core.presentation.generated.resources.currency_picker_suggested
import tsundokuapp.core.presentation.generated.resources.currency_picker_title
import uk.tsundokus.core.designsystem.icon.TsundokuIcons
import uk.tsundokus.core.domain.preferences.AppCurrency

/**
 * Shows the chosen currency and opens [CurrencyPickerDialog] to change it. [suggested] are offered
 * first in the picker — the ones this user actually uses.
 */
@Composable
fun CurrencyField(
    currency: AppCurrency,
    onSelect: (AppCurrency) -> Unit,
    modifier: Modifier = Modifier,
    suggested: List<AppCurrency> = emptyList(),
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val description = stringResource(Res.string.currency_field_cd, currency.displayName)
    Surface(
        onClick = { picking = true },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth().semantics { contentDescription = description },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CurrencyLine(currency = currency, modifier = Modifier.weight(1f))
            Icon(TsundokuIcons.ExpandMore, contentDescription = null)
        }
    }
    if (picking) {
        CurrencyPickerDialog(
            selected = currency,
            suggested = suggested,
            onSelect = {
                picking = false
                onSelect(it)
            },
            onDismiss = { picking = false },
        )
    }
}

/**
 * Every currency, searchable by code, name or symbol. With no search typed, [suggested] come first
 * so the usual pick is one tap away, then the full list.
 */
@Composable
fun CurrencyPickerDialog(
    selected: AppCurrency,
    suggested: List<AppCurrency>,
    onSelect: (AppCurrency) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    // 155 entries: filtering them all on each keystroke costs nothing worth caching.
    val matches = AppCurrency.all.filter { it.matches(query) }
    val pinned = (listOf(selected) + suggested).distinctBy(AppCurrency::code)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(16.dp).widthIn(max = 480.dp).fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Row(
                    modifier = Modifier.padding(start = 24.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.currency_picker_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            TsundokuIcons.Close,
                            contentDescription = stringResource(Res.string.currency_picker_close),
                        )
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(Res.string.currency_picker_search)) },
                    leadingIcon = { Icon(TsundokuIcons.Search, contentDescription = null) },
                    singleLine = true,
                    modifier =
                        Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                )
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    if (query.isBlank()) {
                        item(
                            key = "suggested",
                        ) { SectionTitle(stringResource(Res.string.currency_picker_suggested)) }
                        items(pinned, key = { "pinned_${it.code}" }) { currency ->
                            CurrencyRow(
                                currency,
                                isSelected = currency.code == selected.code,
                                onClick = { onSelect(currency) },
                            )
                        }
                        item(key = "divider") { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                        item(key = "all") { SectionTitle(stringResource(Res.string.currency_picker_all)) }
                    }
                    items(matches, key = AppCurrency::code) { currency ->
                        CurrencyRow(
                            currency,
                            isSelected = currency.code == selected.code,
                            onClick = { onSelect(currency) },
                        )
                    }
                    if (matches.isEmpty()) {
                        item(key = "none") {
                            Text(
                                text = stringResource(Res.string.currency_picker_no_match, query.trim()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@Composable
private fun CurrencyRow(
    currency: AppCurrency,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color =
            if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CurrencyLine(currency = currency, modifier = Modifier.weight(1f))
            if (isSelected) Icon(TsundokuIcons.Check, contentDescription = null)
        }
    }
}

/** "€   EUR   Euro": symbol in a fixed-width column so the codes line up down the list. */
@Composable
private fun CurrencyLine(
    currency: AppCurrency,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.widthIn(min = 40.dp)) {
            Text(
                text = currency.symbol,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
        }
        Text(text = currency.code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            text = currency.displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
