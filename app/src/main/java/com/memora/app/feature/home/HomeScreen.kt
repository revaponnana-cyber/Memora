package com.memora.app.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.memora.app.R
import com.memora.app.domain.model.ItemSummary

object HomeTestTags {
    const val SENTENCE_INPUT = "sentence_input"
    const val SENTENCE_SEND = "sentence_send"
    const val ITEM_LIST = "item_list"
    const val ITEM_ROW = "item_row"
}

/**
 * Home screen — the main screen of Memora.
 *
 * Top: one sentence input ("I put my car keys in my drawer in bedroom").
 * Submitting records the item; no "Add" button is required.
 * Below: the items list. Tapping an item opens its location history.
 * Ambiguity (several same-named objects) and uncertainty (guesses) are
 * resolved with explicit dialogs — never by guesswork.
 */
@Composable
fun HomeScreen(
    onOpenItem: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val event by viewModel.event.collectAsStateWithLifecycle()
    val ambiguity by viewModel.ambiguity.collectAsStateWithLifecycle()
    val uncertainty by viewModel.uncertainty.collectAsStateWithLifecycle()

    var sentence by rememberSaveable { mutableStateOf("") }

    val submit = {
        val text = sentence
        if (text.isNotBlank()) {
            viewModel.onSentenceSubmit(text)
            sentence = ""
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.home_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = sentence,
            onValueChange = { sentence = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(HomeTestTags.SENTENCE_INPUT),
            label = { Text(stringResource(R.string.home_sentence_label)) },
            placeholder = { Text(stringResource(R.string.home_sentence_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { submit() }),
            trailingIcon = {
                IconButton(
                    onClick = { submit() },
                    modifier = Modifier.testTag(HomeTestTags.SENTENCE_SEND),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = stringResource(R.string.home_send),
                    )
                }
            },
        )

        event?.let { current ->
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val text = when (current) {
                    is HomeEvent.Saved -> current.message
                    is HomeEvent.Info -> current.message
                    is HomeEvent.Answer -> current.text
                    // HomeEvent is sealed with exactly the three cases above.
                    else -> current.toString()
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("feedback_text"),
                )
                if (current is HomeEvent.Saved && current.undo != null) {
                    TextButton(onClick = { viewModel.onUndo(current.undo) }) {
                        Text(stringResource(R.string.home_undo))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.home_my_items),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.home_items_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(HomeTestTags.ITEM_LIST),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items = items, key = { it.id }) { item ->
                    ItemRow(item = item, onClick = { onOpenItem(item.id) })
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }

    ambiguity?.let { request ->
        AlertDialog(
            onDismissRequest = { viewModel.onAmbiguityDismissed() },
            title = { Text(stringResource(R.string.ambiguity_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.ambiguity_body))
                    Spacer(modifier = Modifier.height(8.dp))
                    request.candidates.forEach { candidate ->
                        TextButton(
                            onClick = { viewModel.onCandidateChosen(candidate.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            val where = candidate.currentPathDisplay ?: "—"
                            Text("${candidate.name} — $where")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onCandidateChosen(null) }) {
                    Text(stringResource(R.string.ambiguity_add_new))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAmbiguityDismissed() }) {
                    Text(stringResource(R.string.ambiguity_cancel))
                }
            },
        )
    }

    uncertainty?.let { request ->
        AlertDialog(
            onDismissRequest = { viewModel.onUncertaintyAnswer(null) },
            title = { Text(stringResource(R.string.uncertainty_title)) },
            text = {
                Text(stringResource(R.string.uncertainty_body))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = request.statement.raw,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { viewModel.onUncertaintyAnswer(false) }) {
                        Text(stringResource(R.string.uncertainty_record_uncertain))
                    }
                    TextButton(onClick = { viewModel.onUncertaintyAnswer(true) }) {
                        Text(stringResource(R.string.uncertainty_record_confirmed))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onUncertaintyAnswer(null) }) {
                    Text(stringResource(R.string.uncertainty_cancel))
                }
            },
        )
    }
}

@Composable
private fun ItemRow(
    item: ItemSummary,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(HomeTestTags.ITEM_ROW),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = item.currentPath?.display()
                    ?: "—",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
