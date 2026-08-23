package uk.tsundokus.features.settings.presentation.settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tsundokuapp.features.settings.presentation.generated.resources.Res
import tsundokuapp.features.settings.presentation.generated.resources.settings_about
import tsundokuapp.features.settings.presentation.generated.resources.settings_about_caption
import tsundokuapp.features.settings.presentation.generated.resources.settings_account_fallback
import tsundokuapp.features.settings.presentation.generated.resources.settings_appearance
import tsundokuapp.features.settings.presentation.generated.resources.settings_change_email
import tsundokuapp.features.settings.presentation.generated.resources.settings_change_email_caption
import tsundokuapp.features.settings.presentation.generated.resources.settings_change_password
import tsundokuapp.features.settings.presentation.generated.resources.settings_change_password_caption
import tsundokuapp.features.settings.presentation.generated.resources.settings_currency
import tsundokuapp.features.settings.presentation.generated.resources.settings_delete_account
import tsundokuapp.features.settings.presentation.generated.resources.settings_delete_account_caption
import tsundokuapp.features.settings.presentation.generated.resources.settings_edit
import tsundokuapp.features.settings.presentation.generated.resources.settings_footer
import tsundokuapp.features.settings.presentation.generated.resources.settings_oss_licenses
import tsundokuapp.features.settings.presentation.generated.resources.settings_oss_licenses_caption
import tsundokuapp.features.settings.presentation.generated.resources.settings_privacy_policy
import tsundokuapp.features.settings.presentation.generated.resources.settings_privacy_policy_caption
import tsundokuapp.features.settings.presentation.generated.resources.settings_section_about
import tsundokuapp.features.settings.presentation.generated.resources.settings_section_account
import tsundokuapp.features.settings.presentation.generated.resources.settings_section_preferences
import tsundokuapp.features.settings.presentation.generated.resources.settings_sign_out
import tsundokuapp.features.settings.presentation.generated.resources.settings_theme_dark
import tsundokuapp.features.settings.presentation.generated.resources.settings_theme_light
import tsundokuapp.features.settings.presentation.generated.resources.settings_theme_system
import uk.tsundokus.core.designsystem.icon.TsundokuIcons
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.spacer.HorizontalSpacer
import uk.tsundokus.core.designsystem.spacer.VerticalSpacer
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.core.domain.legal.LegalUrls
import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.domain.preferences.ThemeMode
import uk.tsundokus.core.presentation.util.ObserveAsEvents
import uk.tsundokus.features.settings.presentation.AppInfo

@Composable
fun SettingsRoot(
    onSignedOut: () -> Unit,
    accountName: String,
    accountEmail: String,
    onEditProfile: () -> Unit,
    onChangeEmail: () -> Unit,
    onChangePassword: () -> Unit,
    onDeleteAccount: () -> Unit,
    onAbout: () -> Unit,
    onLicenses: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            SettingsEvent.SignedOut -> onSignedOut()
            is SettingsEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message.asStringAsync())
        }
    }

    SettingsScreen(
        // Prefer the values supplied by the host (kept fresh from the session) and fall back to
        // whatever the ViewModel derived from SessionStorage.
        state =
            state.copy(
                accountName = accountName.ifBlank { state.accountName },
                accountEmail = accountEmail.ifBlank { state.accountEmail },
            ),
        onAction = viewModel::onAction,
        onEditProfile = onEditProfile,
        onChangeEmail = onChangeEmail,
        onChangePassword = onChangePassword,
        onDeleteAccount = onDeleteAccount,
        onAbout = onAbout,
        onLicenses = onLicenses,
        modifier = modifier,
    )
}

@Composable
internal fun SettingsScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    onEditProfile: () -> Unit,
    onChangeEmail: () -> Unit,
    onChangePassword: () -> Unit,
    onDeleteAccount: () -> Unit,
    onAbout: () -> Unit,
    onLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier =
                Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VerticalSpacer(8.dp)
            AccountHeaderCard(
                name = state.accountName,
                email = state.accountEmail,
                onEdit = onEditProfile,
            )

            SectionLabel(stringResource(Res.string.settings_section_account))
            SettingsRow(
                icon = TsundokuIcons.Email,
                title = stringResource(Res.string.settings_change_email),
                subtitle =
                    state.accountEmail.ifBlank { stringResource(Res.string.settings_change_email_caption) },
                onClick = onChangeEmail,
            )
            SettingsRow(
                icon = TsundokuIcons.Lock,
                title = stringResource(Res.string.settings_change_password),
                subtitle = stringResource(Res.string.settings_change_password_caption),
                onClick = onChangePassword,
            )

            SectionLabel(stringResource(Res.string.settings_section_preferences))
            AppearanceCard(theme = state.theme, onThemeSelected = { onAction(SettingsAction.ChangeTheme(it)) })
            CurrencyCard(
                currency = state.currency,
                onCurrencySelected = { onAction(SettingsAction.ChangeCurrency(it)) },
            )

            SectionLabel(stringResource(Res.string.settings_section_about))
            SettingsRow(
                icon = TsundokuIcons.Info,
                title = stringResource(Res.string.settings_about),
                subtitle = stringResource(Res.string.settings_about_caption),
                onClick = onAbout,
            )
            PrivacyPolicyRow()
            SettingsRow(
                icon = TsundokuIcons.Info,
                title = stringResource(Res.string.settings_oss_licenses),
                subtitle = stringResource(Res.string.settings_oss_licenses_caption),
                onClick = onLicenses,
            )
            SettingsRow(
                icon = TsundokuIcons.Delete,
                title = stringResource(Res.string.settings_delete_account),
                subtitle = stringResource(Res.string.settings_delete_account_caption),
                onClick = onDeleteAccount,
                destructive = true,
            )

            VerticalSpacer(4.dp)
            SignOutButton(onClick = { onAction(SettingsAction.SignOut) })
            VerticalSpacer(8.dp)
            Text(
                text = stringResource(Res.string.settings_footer, AppInfo.NAME, AppInfo.VERSION),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            VerticalSpacer(16.dp)
        }
    }
}

@Composable
private fun AccountHeaderCard(
    name: String,
    email: String,
    onEdit: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsAvatar(name = name)
            HorizontalSpacer(16.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name.ifBlank { stringResource(Res.string.settings_account_fallback) },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (email.isNotBlank()) {
                    VerticalSpacer(2.dp)
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TextButton(onClick = onEdit) {
                Text(text = stringResource(Res.string.settings_edit), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SettingsAvatar(
    name: String,
    modifier: Modifier = Modifier,
) {
    val palette =
        listOf(
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer,
            MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary,
        )
    val initials = name.toInitials()
    val (container, content) = palette[initials.map { it.code }.sum().mod(palette.size)]
    Box(
        modifier = modifier.size(56.dp).background(container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = content,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
}

/**
 * The policy is a document hosted outside the app, so this row leaves it — which the chevron alone
 * cannot say. The subtitle names the destination, so the row still reads like its siblings without
 * misleading about where it goes.
 */
@Composable
private fun PrivacyPolicyRow() {
    val uriHandler = LocalUriHandler.current
    SettingsRow(
        icon = TsundokuIcons.Shield,
        title = stringResource(Res.string.settings_privacy_policy),
        subtitle = stringResource(Res.string.settings_privacy_policy_caption),
        onClick = { uriHandler.openUri(LegalUrls.PRIVACY_POLICY) },
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
) {
    val accent = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp),
            )
            HorizontalSpacer(12.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color =
                        if (destructive) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            HorizontalSpacer(8.dp)
            Icon(
                imageVector = TsundokuIcons.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun AppearanceCard(
    theme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
) {
    val options = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
    PreferenceCard(title = stringResource(Res.string.settings_appearance)) {
        SegmentedControl(
            options =
                listOf(
                    stringResource(Res.string.settings_theme_system),
                    stringResource(Res.string.settings_theme_light),
                    stringResource(Res.string.settings_theme_dark),
                ),
            selectedIndex = options.indexOf(theme).coerceAtLeast(0),
            onSelect = { index -> onThemeSelected(options[index]) },
        )
    }
}

@Composable
private fun CurrencyCard(
    currency: AppCurrency,
    onCurrencySelected: (AppCurrency) -> Unit,
) {
    val currencies = AppCurrency.entries
    PreferenceCard(title = stringResource(Res.string.settings_currency)) {
        SegmentedControl(
            options = currencies.map { it.symbol },
            selectedIndex = currencies.indexOf(currency),
            onSelect = { index -> onCurrencySelected(currencies[index]) },
        )
    }
}

@Composable
private fun PreferenceCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            VerticalSpacer(12.dp)
            content()
        }
    }
}

@Composable
private fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val container = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                val content =
                    if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                Surface(
                    onClick = { onSelect(index) },
                    color = container,
                    contentColor = content,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SignOutButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = TsundokuIcons.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            HorizontalSpacer(8.dp)
            Text(
                text = stringResource(Res.string.settings_sign_out),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun String.toInitials(): String {
    val parts = trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].first().toString() + parts[1].first().toString()).uppercase()
    }
}

@PreviewThemes
@Composable
private fun SettingsScreenPreview() {
    TsundokuTheme {
        Surface {
            SettingsScreen(
                state =
                    SettingsState(
                        accountName = "Akira Toriyama",
                        accountEmail = "akira@example.com",
                        theme = ThemeMode.DARK,
                        currency = AppCurrency.USD,
                    ),
                onAction = {},
                onEditProfile = {},
                onChangeEmail = {},
                onChangePassword = {},
                onDeleteAccount = {},
                onAbout = {},
                onLicenses = {},
            )
        }
    }
}
