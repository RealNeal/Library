package com.rn.library.ui.screens

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import com.rn.library.ui.LocalStrings

@Composable
fun SortOrderChip(
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    iconTextColor: Color
) {
    val strings = LocalStrings.current
    val label = when (sortOrder) {
        SortOrder.TITLE_ASC -> strings.sortTitleAsc
        SortOrder.TITLE_DESC -> strings.sortTitleDesc
        SortOrder.DATE_MODIFIED_DESC -> strings.sortDateNew
        SortOrder.DATE_MODIFIED_ASC -> strings.sortDateOld
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

