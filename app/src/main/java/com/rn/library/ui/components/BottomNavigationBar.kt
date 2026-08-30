package com.rn.library.ui.components

import androidx.annotation.StringRes
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import com.rn.library.R
import com.rn.library.ui.screens.AppTheme
import com.rn.library.ui.theme.*
import com.rn.library.util.WindowHeightClass
import com.rn.library.util.rememberWindowSizeInfo

sealed class NavigationItem(
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    object Books : NavigationItem(R.string.tab_books, Icons.AutoMirrored.Filled.MenuBook)
    object Anime : NavigationItem(R.string.tab_anime, Icons.Default.Movie)
    object Manga : NavigationItem(R.string.tab_manga, Icons.Default.AutoStories)
    object TVSeries : NavigationItem(R.string.tab_series, Icons.Default.Tv)
    object Profile : NavigationItem(R.string.profile, Icons.Default.Person)
}

internal fun saturatedAccent(base: Color, darkTheme: Boolean, selected: Boolean): Color {
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

internal fun bottomNavInnerHeight(compactHeight: Boolean) =
    if (compactHeight) 64.dp else 88.dp

@Composable
fun bottomNavigationClearance(): Dp {
    val windowInfo = rememberWindowSizeInfo()
    val compact = windowInfo.isLandscape || windowInfo.heightClass == WindowHeightClass.COMPACT
    return bottomNavInnerHeight(compact) +
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
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
    val windowInfo = rememberWindowSizeInfo()
    val isCompactHeight = windowInfo.isLandscape || windowInfo.heightClass == WindowHeightClass.COMPACT
    val barHeight = if (isCompactHeight) 56.dp else 78.dp
    val bottomPadding = if (isCompactHeight) 4.dp else 2.dp
    val panelColor = PanelColor()
    val items = buildList {
        if (booksEnabled) add(NavigationItem.Books)
        if (animeEnabled) add(NavigationItem.Anime)
        if (mangaEnabled) add(NavigationItem.Manga)
        if (tvSeriesEnabled) add(NavigationItem.TVSeries)
        add(NavigationItem.Profile)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(panelColor)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(barHeight)
            .padding(bottom = bottomPadding),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            NavigationButton(
                item = item,
                label = stringResource(item.labelRes),
                isSelected = item == selectedItem,
                onClick = { onItemSelected(item) },
                currentTheme = currentTheme,
                dynamicColorsEnabled = dynamicColorsEnabled
            )
        }
    }
}

@Composable
private fun NavigationButton(
    item: NavigationItem,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    currentTheme: AppTheme,
    dynamicColorsEnabled: Boolean
) {
    val iconColor = BottomPanelIconColor()
    val labelColor = BottomPanelLabelColor()
    val activeScheme = MaterialTheme.colorScheme
    val primaryAccent = activeScheme.primary
    val selectedAccentTint = saturatedAccent(primaryAccent, currentTheme == AppTheme.DARK, selected = true)
    val staticTabContent = StaticTabContentColor()
    val selectedContentColor = if (dynamicColorsEnabled) selectedAccentTint else staticTabContent
    val selectedBackgroundColor = if (dynamicColorsEnabled) {
        activeScheme.secondaryContainer
    } else {
        primaryAccent.copy(alpha = 0.25f)
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "navScale"
    )
    val animatedIconColor by animateColorAsState(
        targetValue = if (isSelected) selectedContentColor else iconColor,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "navIconColor"
    )
    val animatedLabelColor by animateColorAsState(
        targetValue = if (isSelected) selectedContentColor else labelColor,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "navLabelColor"
    )
    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (isSelected) selectedBackgroundColor else Color.Transparent,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "navBgColor"
    )

    Column(
        modifier = Modifier
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(animatedBackgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = label,
                tint = animatedIconColor,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = animatedLabelColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}
