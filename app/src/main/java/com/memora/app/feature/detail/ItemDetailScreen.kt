package com.memora.app.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.memora.app.R
import com.memora.app.domain.model.ItemWithPlaces
import com.memora.app.domain.model.PastPlace
import java.text.DateFormat
import java.util.Date

object DetailTestTags {
    const val CURRENT_LOCATION = "current_location"
    const val PAST_PLACES = "past_places"
    const val MOVE_BUTTON = "move_button"
    const val MOVE_INPUT = "move_input"
}

/**
 * Item detail: the current location highlighted on top, past addresses
 * underneath (deduplicated — repeating addresses are shown once).
 */
@Composable
fun ItemDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ItemDetailViewModel,
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var moveDialogOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                )
            }
            Text(
                text = detail?.item?.name ?: stringResource(R.string.detail_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (detail == null) {
            Text(
                text = stringResource(R.string.detail_missing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        ItemDetailContent(
            detail = detail!!,
            onMoveClick = { moveDialogOpen = true },
        )

        message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }

    if (moveDialogOpen) {
        MoveDialog(
            onConfirm = { placeText ->
                moveDialogOpen = false
                viewModel.onMove(placeText)
            },
            onDismiss = { moveDialogOpen = false },
        )
    }
}

@Composable
private fun ItemDetailContent(
    detail: ItemWithPlaces,
    onMoveClick: () -> Unit,
) {
    // Current location — highlighted on top.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DetailTestTags.CURRENT_LOCATION),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(
                    text = stringResource(R.string.detail_current_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = detail.item.currentPath?.display()
                    ?: stringResource(R.string.detail_unknown),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            detail.currentSince?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.detail_since, formatDate(it)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    TextButton(onClick = onMoveClick, modifier = Modifier.testTag(DetailTestTags.MOVE_BUTTON)) {
        Text(stringResource(R.string.detail_move))
    }
    Spacer(modifier = Modifier.height(12.dp))

    // Past addresses underneath — each distinct place once.
    Text(
        text = stringResource(R.string.detail_past_title),
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(modifier = Modifier.height(8.dp))

    if (detail.pastPlaces.isEmpty()) {
        Text(
            text = stringResource(R.string.detail_no_past),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Column(
            modifier = Modifier.testTag(DetailTestTags.PAST_PLACES),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            detail.pastPlaces.forEach { past ->
                PastPlaceRow(past)
            }
        }
    }
}

@Composable
private fun PastPlaceRow(past: PastPlace) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = past.path.display(),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = formatDate(past.lastSeenAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MoveDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var placeText by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_move_title)) },
        text = {
            OutlinedTextField(
                value = placeText,
                onValueChange = { placeText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(DetailTestTags.MOVE_INPUT),
                label = { Text(stringResource(R.string.detail_move_hint)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(placeText) },
                enabled = placeText.isNotBlank(),
            ) {
                Text(stringResource(R.string.detail_move_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ambiguity_cancel))
            }
        },
    )
}

private fun formatDate(timestamp: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))
