package com.rn.library.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rn.library.ui.LocalStrings
import com.rn.library.ui.screens.AppTheme
import com.rn.library.ui.theme.BottomPanelIconColor
import com.rn.library.ui.theme.BottomPanelLabelColor
import com.rn.library.ui.theme.PanelColor
import com.rn.library.ui.theme.StaticTabBackgroundColor
import com.rn.library.ui.theme.StaticTabContentColor

@Composable
fun AppNavigationRail(
    selectedItem: NavigationItem,
    onItemSelected: (NavigationItem) -> Unit,
    currentTheme: AppTheme = AppTheme.DARK,
    dynamicColorsEnabled: Boolean = false,
    booksEnabled: Boolean = true,
    animeEnabled: Boolean = true,
    mangaEnabled: Boolean = true,
    tvSeriesEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val panelColor = PanelColor()
    val items = buildList {
        if (booksEnabled) add(NavigationItem.Books)
        if (animeEnabled) add(NavigationItem.Anime)
        if (mangaEnabled) add(NavigationItem.Manga)
        if (tvSeriesEnabled) add(NavigationItem.TVSeries)
        add(NavigationItem.Profile)
    }

    val staticTabBackground = StaticTabBackgroundColor()
    val staticTabContent = StaticTabContentColor()
    val iconColor = BottomPanelIconColor()
    val labelColor = BottomPanelLabelColor()

    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = panelColor,
        contentColor = iconColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.forEach { item ->
                val label = item.labelKey(strings)
                val isSelected = item == selectedItem
                NavigationRailItem(
                    selected = isSelected,
                    onClick = { onItemSelected(item) },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = label,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = staticTabContent,
                        selectedTextColor = staticTabContent,
                        indicatorColor = staticTabBackground,
                        unselectedIconColor = iconColor,
                        unselectedTextColor = labelColor
                    ),
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}
