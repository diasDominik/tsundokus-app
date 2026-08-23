package uk.tsundokus.features.settings.presentation.editprofile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tsundokuapp.features.settings.presentation.generated.resources.Res
import tsundokuapp.features.settings.presentation.generated.resources.edit_profile_display_name
import tsundokuapp.features.settings.presentation.generated.resources.edit_profile_save
import tsundokuapp.features.settings.presentation.generated.resources.edit_profile_updated
import uk.tsundokus.core.designsystem.spacer.VerticalSpacer
import uk.tsundokus.core.designsystem.textfields.TsundokuTextField
import uk.tsundokus.core.presentation.navigation.TopBarActions
import uk.tsundokus.core.presentation.util.ObserveAsEvents

@Composable
fun EditProfileRoot(
    navKey: NavKey,
    snackbarHostState: SnackbarHostState,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            EditProfileEvent.Saved -> {
                snackbarHostState.showSnackbar(getString(Res.string.edit_profile_updated))
                onSaved()
            }

            is EditProfileEvent.Error -> {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
            }
        }
    }

    TopBarActions(navKey) {
        TextButton(onClick = viewModel::onSave, enabled = !state.isSubmitting) {
            Text(text = stringResource(Res.string.edit_profile_save), fontWeight = FontWeight.SemiBold)
        }
    }

    AccountFieldColumn(modifier = modifier) {
        TsundokuTextField(
            state = viewModel.nameState,
            title = stringResource(Res.string.edit_profile_display_name),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Shared layout for the single-purpose account edit screens (centered, max-width column). */
@Composable
internal fun AccountFieldColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
            VerticalSpacer(8.dp)
            content()
        }
    }
}
