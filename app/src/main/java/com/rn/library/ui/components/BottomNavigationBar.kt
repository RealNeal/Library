package com.rn.library.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rn.library.ui.LocalStrings
import com.rn.library.ui.screens.AppTheme
import com.rn.library.ui.theme.*

sealed class NavigationItem(
    val labelKey: (com.rn.library.ui.Strings) -> String,
    val icon: ImageVector
) {
    object Books : NavigationItem({ it.tabBooks }, Icons.AutoMirrored.Filled.MenuBook)
    object Anime : NavigationItem({ it.tabAnime }, Icons.Default.Movie)
    // Manga: more thematic "book" icon
    object Manga : NavigationItem({ it.tabManga }, Icons.Default.AutoStories)
    // TV series: revert to TV icon
    object TVSeries : NavigationItem({ it.tabSeries }, Icons.Default.Tv)
    object Profile : NavigationItem({ it.profile }, Icons.Default.Person)
}

private fun saturatedAccent(base: Color, darkTheme: Boolean, selected: Boolean): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(base.toArgb(), hsv)
    val saturationBoost = if (selected) 1f else 0.85f
    hsv[1] = (hsv[1] * saturationBoost).coerceIn(0f, 1f)
    val valueMultiplier = when {
        darkTheme && selected -> 1f
        darkTheme && !selected -> 1.1f
        !darkTheme && selected -> 0.9f
        else -> 1.15f
    }
    hsv[2] = (hsv[2] * valueMultiplier).coerceIn(0f, 1f)
    return Color.hsv(hsv[0], hsv[1], hsv[2], base.alpha)
}

@Composable
fun BottomNavigationBar(
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

    // Bottom panel size is controlled here - height is 154.dp (line 56)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(panelColor)
            .padding(bottom = 30.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            NavigationButton(
                item = item,
                label = item.labelKey(strings),
                isSelected = item == selectedItem,
                onClick = { onItemSelected(item) },
                currentTheme = currentTheme,
                dynamicColorsEnabled = dynamicColorsEnabled,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun NavigationButton(
    item: NavigationItem,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    currentTheme: AppTheme = AppTheme.DARK,
    dynamicColorsEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bottomPanelLabelColor = BottomPanelLabelColor()
    val bottomPanelIconColor = BottomPanelIconColor()
    val staticTabBackground = StaticTabBackgroundColor()
    val staticTabContent = StaticTabContentColor()
    val interactionSource = remember { MutableInteractionSource() }

    val dynamicScheme = MaterialTheme.colorScheme
    val materialYouInactiveTint =
        if (currentTheme == AppTheme.LIGHT) Color(0xFF5F6368) else Color(0xFFB0BEC5)
    val materialYouSelectedTint = saturatedAccent(dynamicScheme.primary, currentTheme == AppTheme.DARK, selected = true)

    val iconTint = if (dynamicColorsEnabled) {
        if (isSelected) materialYouSelectedTint else materialYouInactiveTint
    } else {
        if (isSelected) staticTabContent else bottomPanelIconColor
    }
    val labelColor = if (dynamicColorsEnabled) {
        if (isSelected) materialYouSelectedTint else materialYouInactiveTint
    } else {
        if (isSelected) staticTabContent else bottomPanelLabelColor
    }

    val circleColor = if (dynamicColorsEnabled) {
        dynamicScheme.secondaryContainer
    } else if (isSelected) {
        staticTabBackground
    } else {
        Color.Transparent
    }

    Column(
        modifier = modifier
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = interactionSource
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp)
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(circleColor)
                )
            }
            Icon(
                imageVector = item.icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = labelColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
