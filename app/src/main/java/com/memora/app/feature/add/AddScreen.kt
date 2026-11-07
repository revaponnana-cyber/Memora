package com.memora.app.feature.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.memora.app.R
import com.memora.app.core.ui.theme.MemoraTheme
import com.memora.app.domain.model.AddMode

/**
 * Add screen: register a new item or document.
 *
 * Phase 1 provides the mode selection (Item / Document) via MVVM; the actual
 * registration forms arrive with the database layer (Phases 2–3 for items,
 * Phase 5 for documents). The screen states this honestly — nothing is
 * silently discarded and nothing is saved yet.
 */
@Composable
fun AddScreen(
    modifier: Modifier = Modifier,
    viewModel: AddViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AddContent(
        state = state,
        onModeSelected = viewModel::onModeSelected,
        modifier = modifier,
    )
}

@Composable
private fun AddContent(
    state: AddUiState,
    onModeSelected: (AddMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.add_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeButton(
                label = stringResource(R.string.add_mode_item),
                selected = state.mode == AddMode.ITEM,
                onClick = { onModeSelected(AddMode.ITEM) },
                modifier = Modifier.weight(1f),
            )
            ModeButton(
                label = stringResource(R.string.add_mode_document),
                selected = state.mode == AddMode.DOCUMENT,
                onClick = { onModeSelected(AddMode.DOCUMENT) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(
                if (state.mode == AddMode.ITEM) {
                    R.string.add_pending_item
                } else {
                    R.string.add_pending_document
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier.fillMaxWidth()) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
            Text(label)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddContentItemPreview() {
    MemoraTheme {
        AddContent(state = AddUiState(mode = AddMode.ITEM), onModeSelected = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun AddContentDocumentPreview() {
    MemoraTheme {
        AddContent(state = AddUiState(mode = AddMode.DOCUMENT), onModeSelected = {})
    }
}
