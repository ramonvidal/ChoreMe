package com.choreme.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * The three short steps to get instant alerts with the free ntfy app, with a button that opens its page on Google Play.
 * Shared by the Map tab and the Settings tab, which both offer ntfy.
 */
@Composable
fun NtfyHelpDialog(onInstall: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ntfy_help_title)) },
        text = {
            Column(Modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.ntfy_help_intro), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.ntfy_help_step1), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.ntfy_help_step2), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.ntfy_help_step3), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.ntfy_help_stuck), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onInstall) { Text(stringResource(R.string.ntfy_help_open)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ntfy_help_later)) } },
    )
}
