package uk.tsundokus.features.settings.presentation.changepassword

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tsundokuapp.features.settings.presentation.generated.resources.Res
import tsundokuapp.features.settings.presentation.generated.resources.change_password_changed
import tsundokuapp.features.settings.presentation.generated.resources.change_password_confirm
import tsundokuapp.features.settings.presentation.generated.resources.change_password_current
import tsundokuapp.features.settings.presentation.generated.resources.change_password_new
import tsundokuapp.features.settings.presentation.generated.resources.change_password_save
import uk.tsundokus.core.designsystem.spacer.VerticalSpacer
import uk.tsundokus.core.designsystem.textfields.TsundokuTextField
import uk.tsundokus.core.presentation.navigation.TopBarActions
import uk.tsundokus.core.presentation.util.ObserveAsEvents
import uk.tsundokus.features.settings.presentation.editprofile.AccountFieldColumn

@Composable
fun ChangePasswordRoot(
    navKey: NavKey,
    snackbarHostState: SnackbarHostState,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChangePasswordViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ChangePasswordEvent.Saved -> {
                snackbarHostState.showSnackbar(getString(Res.string.change_password_changed))
                onSaved()
            }

            is ChangePasswordEvent.Error -> {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
            }
        }
    }

    TopBarActions(navKey) {
        TextButton(onClick = viewModel::onSave, enabled = !state.isSubmitting) {
            Text(text = stringResource(Res.string.change_password_save), fontWeight = FontWeight.SemiBold)
        }
    }

    AccountFieldColumn(modifier = modifier) {
        TsundokuTextField(
            state = viewModel.currentPasswordState,
            title = stringResource(Res.string.change_password_current),
            singleLine = true,
            keyboardType = KeyboardType.Password,
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpacer(16.dp)
        TsundokuTextField(
            state = viewModel.newPasswordState,
            title = stringResource(Res.string.change_password_new),
            singleLine = true,
            keyboardType = KeyboardType.Password,
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpacer(16.dp)
        TsundokuTextField(
            state = viewModel.confirmPasswordState,
            title = stringResource(Res.string.change_password_confirm),
            singleLine = true,
            keyboardType = KeyboardType.Password,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
