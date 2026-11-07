package com.memora.app.feature.more

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.memora.app.R
import com.memora.app.core.navigation.MemoraDestination
import com.memora.app.core.ui.theme.MemoraTheme

private data class MoreSection(
    @StringRes val title: Int,
    @StringRes val summary: Int,
    val route: String,
)

private val MoreSections = listOf(
    MoreSection(R.string.more_row_objects, R.string.more_row_objects_summary, MemoraDestination.Objects.route),
    MoreSection(R.string.more_row_locations, R.string.more_row_locations_summary, MemoraDestination.Locations.route),
    MoreSection(R.string.more_row_history, R.string.more_row_history_summary, MemoraDestination.History.route),
    MoreSection(R.string.more_row_products, R.string.more_row_products_summary, MemoraDestination.Products.route),
    MoreSection(R.string.more_row_reminders, R.string.more_row_reminders_summary, MemoraDestination.Reminders.route),
    MoreSection(R.string.more_row_backup, R.string.more_row_backup_summary, MemoraDestination.Backup.route),
    MoreSection(R.string.more_row_settings, R.string.more_row_settings_summary, MemoraDestination.Settings.route),
)

/**
 * More screen: navigation hub for app sections plus an honest development
 * status note. Each row navigates to a real (currently placeholder) screen —
 * no dead rows.
 */
@Composable
fun MoreScreen(
    onOpenSection: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.more_sections_heading),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        items(MoreSections) { section ->
            SectionRow(
                title = stringResource(section.title),
                summary = stringResource(section.summary),
                onClick = { onOpenSection(section.route) },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.more_status_heading),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Text(
                    text = stringResource(R.string.more_status_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionRow(
    title: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MoreScreenPreview() {
    MemoraTheme {
        MoreScreen(onOpenSection = {})
    }
}
