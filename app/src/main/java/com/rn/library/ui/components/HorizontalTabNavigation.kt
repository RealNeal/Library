package com.rn.library.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rn.library.ui.LocalStrings
import com.rn.library.ui.theme.SearchBarColor
import com.rn.library.ui.theme.TitleColorBetween

enum class HorizontalTab {
    MY_TAB,
    LATEST,
    ONGOINGS,
    ANNOUNCEMENTS
}

@Composable
fun HorizontalTabNavigation(
    selectedTab: HorizontalTab,
    onTabSelected: (HorizontalTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val tabs = listOf(
        HorizontalTab.MY_TAB to strings.myTab,
        HorizontalTab.LATEST to strings.latest,
        HorizontalTab.ONGOINGS to strings.ongoings,
        HorizontalTab.ANNOUNCEMENTS to strings.announcements
    )

    val titleBetween = TitleColorBetween()
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SearchBarColor())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        tabs.forEach { (tab, label) ->
            val isSelected = tab == selectedTab
            Column(
                modifier = Modifier.clickable { onTabSelected(tab) }
            ) {
                Text(
                    text = label,
                    color = if (isSelected) accent else titleBetween.copy(alpha = 0.72f),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .fillMaxWidth()
                            .background(accent)
                    )
                }
            }
        }
    }
}
