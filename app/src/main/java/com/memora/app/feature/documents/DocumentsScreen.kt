package com.memora.app.feature.documents

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.memora.app.R
import com.memora.app.core.common.PlaceholderScreen
import com.memora.app.core.ui.theme.MemoraTheme

/**
 * Documents tab.
 *
 * Phase 1 placeholder: document registration, scanning and organisation
 * arrive in Phase 5. See [PlaceholderScreen] for the honesty policy.
 */
@Composable
fun DocumentsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.documents_title),
        description = stringResource(R.string.documents_pending),
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun DocumentsScreenPreview() {
    MemoraTheme {
        DocumentsScreen()
    }
}
