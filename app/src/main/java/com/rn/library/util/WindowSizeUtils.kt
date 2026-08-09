package com.rn.library.util

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthClass { COMPACT, MEDIUM, EXPANDED }
enum class WindowHeightClass { COMPACT, MEDIUM, EXPANDED }

data class WindowSizeInfo(
    val widthClass: WindowWidthClass,
    val heightClass: WindowHeightClass,
    val screenWidthDp: Dp,
    val screenHeightDp: Dp,
    val isLandscape: Boolean
)

@Composable
fun rememberWindowSizeInfo(): WindowSizeInfo {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val widthClass = when {
        screenWidth < 600.dp -> WindowWidthClass.COMPACT
        screenWidth < 840.dp -> WindowWidthClass.MEDIUM
        else -> WindowWidthClass.EXPANDED
    }

    val heightClass = when {
        screenHeight < 480.dp -> WindowHeightClass.COMPACT
        screenHeight < 900.dp -> WindowHeightClass.MEDIUM
        else -> WindowHeightClass.EXPANDED
    }

    return WindowSizeInfo(
        widthClass = widthClass,
        heightClass = heightClass,
        screenWidthDp = screenWidth,
        screenHeightDp = screenHeight,
        isLandscape = isLandscape
    )
}
