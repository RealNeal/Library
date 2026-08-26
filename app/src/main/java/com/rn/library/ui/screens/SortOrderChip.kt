package com.rn.library.ui.screens

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import com.rn.library.R

@Composable
fun SortOrderChip(
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    iconTextColor: Color
) {
    val label = when (sortOrder) {
        SortOrder.TITLE_ASC -> stringResource(R.string.sort_title_asc)
        SortOrder.TITLE_DESC -> stringResource(R.string.sort_title_desc)
        SortOrder.DATE_MODIFIED_DESC -> stringResource(R.string.sort_date_new)
        SortOrder.DATE_MODIFIED_ASC -> stringResource(R.string.sort_date_old)
    }

    val next = when (sortOrder) {
        SortOrder.TITLE_ASC -> SortOrder.TITLE_DESC
        SortOrder.TITLE_DESC -> SortOrder.DATE_MODIFIED_DESC
        SortOrder.DATE_MODIFIED_DESC -> SortOrder.DATE_MODIFIED_ASC
        SortOrder.DATE_MODIFIED_ASC -> SortOrder.TITLE_ASC
    }

    AssistChip(
        onClick = { onSortOrderChange(next) },
        label = { Text(label, color = MaterialTheme.colorScheme.primary) },
        leadingIcon = { Icon(Icons.Default.Sort, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.primary
        ),
        border = AssistChipDefaults.assistChipBorder(
            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            borderWidth = 2.dp,
            enabled = true
        )
    )
}
