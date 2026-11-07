package com.memora.app.feature.add

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.memora.app.R
import com.memora.app.domain.model.AddMode

object AddTestTags {
    const val NAME_INPUT = "add_name_input"
    const val PLACE_INPUT = "add_place_input"
    const val SAVE_BUTTON = "add_save_button"
}

/**
 * Manual add screen. Items can be registered here with a name and (nested)
 * place ("drawer in bedroom"); the faster path is the sentence input on Home.
 * Documents are a later phase and say so honestly.
 */
@Composable
fun AddScreen(
    modifier: Modifier = Modifier,
    viewModel: AddViewModel = viewModel(),
    onSaved: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.onSavedHandled()
            onSaved()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.add_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(12.dp))

        TabRow(selectedTabIndex = state.mode.ordinal) {
            AddMode.entries.forEach { mode ->
                Tab(
                    selected = state.mode == mode,
                    onClick = { viewModel.onModeSelected(mode) },
                    text = {
                        Text(
                            stringResource(
                                when (mode) {
                                    AddMode.ITEM -> R.string.add_mode_item
                                    AddMode.DOCUMENT -> R.string.add_mode_document
                                },
                            ),
                        )
                    },
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        when (state.mode) {
            AddMode.ITEM -> ItemForm(state = state, viewModel = viewModel)
            AddMode.DOCUMENT -> Text(
                text = stringResource(R.string.add_pending_document),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        state.message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ItemForm(
    state: AddUiState,
    viewModel: AddViewModel,
) {
    Column {
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(AddTestTags.NAME_INPUT),
            label = { Text(stringResource(R.string.add_name_label)) },
            placeholder = { Text(stringResource(R.string.add_name_hint)) },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.add_description_label)) },
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.place,
            onValueChange = viewModel::onPlaceChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(AddTestTags.PLACE_INPUT),
            label = { Text(stringResource(R.string.add_place_label)) },
            placeholder = { Text(stringResource(R.string.add_place_hint)) },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Button(
                onClick = viewModel::onSave,
                enabled = state.canSave,
                modifier = Modifier.testTag(AddTestTags.SAVE_BUTTON),
            ) {
                Text(stringResource(R.string.add_save))
            }
        }
    }
}
