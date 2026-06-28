package uk.tsundokus.features.settings.presentation.changeemail

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
import org.koin.compose.viewmodel.koinViewModel
import uk.tsundokus.core.designsystem.spacer.VerticalSpacer
import uk.tsundokus.core.designsystem.textfields.TsundokuTextField
import uk.tsundokus.core.presentation.navigation.TopBarActions
import uk.tsundokus.core.presentation.util.ObserveAsEvents
import uk.tsundokus.features.settings.presentation.editprofile.AccountFieldColumn

@Composable
fun ChangeEmailRoot(
    navKey: NavKey,
    snackbarHostState: SnackbarHostState,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChangeEmailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ChangeEmailEvent.Saved -> {
                snackbarHostState.showSnackbar("Email updated")
                onSaved()
            }

            is ChangeEmailEvent.Error -> {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
            }
        }
    }

    TopBarActions(navKey) {
        TextButton(onClick = viewModel::onSave, enabled = !state.isSubmitting) {
            Text(text = "Save", fontWeight = FontWeight.SemiBold)
        }
    }

    AccountFieldColumn(modifier = modifier) {
        TsundokuTextField(
            state = viewModel.newEmailState,
            title = "New email",
            singleLine = true,
            keyboardType = KeyboardType.Email,
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpacer(16.dp)
        TsundokuTextField(
            state = viewModel.currentPasswordState,
            title = "Current password",
            singleLine = true,
            keyboardType = KeyboardType.Password,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
