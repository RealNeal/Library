package com.rn.library.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.rn.library.ui.LocalStrings

@Composable
fun ActivityStatsConfirmDialog(
    onConfirm: () -> Unit,
    onDecline: () -> Unit,
    onDismiss: () -> Unit = onDecline
) {
    val strings = LocalStrings.current
    val scheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.activityStatsConfirmTitle) },
        text = { Text(strings.activityStatsConfirmMessage) },
        containerColor = scheme.surface,
        titleContentColor = scheme.onSurface,
        textContentColor = scheme.onSurfaceVariant,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(strings.yes)
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text(strings.no)
            }
        }
    )
}
