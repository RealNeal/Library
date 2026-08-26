package com.rn.library.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rn.library.R

@Composable
fun ActivityStatsConfirmDialog(
    onConfirm: () -> Unit,
    onDecline: () -> Unit,
    onDismiss: () -> Unit = onDecline
) {
    val scheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.activity_stats_confirm_title)) },
        text = { Text(stringResource(R.string.activity_stats_confirm_message)) },
        containerColor = scheme.surface,
        titleContentColor = scheme.onSurface,
        textContentColor = scheme.onSurfaceVariant,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text(stringResource(R.string.no))
            }
        }
    )
}
