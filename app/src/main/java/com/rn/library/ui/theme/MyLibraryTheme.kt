package com.rn.library.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

data class AppColors(
    val panelColor: Color,
    val iconTextColor: Color,
    val mainBackgroundColor: Color,
    val searchBarColor: Color,
    val titleColorBetween: Color,
    val tabCircleColor: Color,
    val selectedTabColor: Color,
    val workTitleColor: Color,
    val bottomPanelLabelColor: Color,
    val bottomPanelIconColor: Color,
    val statsActivityColor: Color,
    /** Фон круга выбранной вкладки без Material You */
    val staticTabBackgroundColor: Color,
    /** Иконки и подписи выбранной вкладки без Material You */
    val staticTabContentColor: Color,
    /** Heatmap и столбцы «Активность по периодам» без Material You */
    val staticChartAccentColor: Color
)

private val LocalAppColors = staticCompositionLocalOf {
    AppColors(
        panelColor = Color(0xFF111111),
        iconTextColor = Color(0xFFEAEAEA),
        mainBackgroundColor = Color(0xFF1A1A1A),
        searchBarColor = Color(0xFF2A2A2A),
        titleColorBetween = Color(0xFFEAEAEA),
        tabCircleColor = Color(0xFF2C2C2C),
        selectedTabColor = Color(0xFF6750A4),
        workTitleColor = Color(0xFFEAEAEA),
        bottomPanelLabelColor = Color(0xFFBDBDBD),
        bottomPanelIconColor = Color(0xFFBDBDBD),
        statsActivityColor = Color(0xFF7C4DFF),
        staticTabBackgroundColor = Color(0xFF494456),
        staticTabContentColor = Color(0xFFE8DEF7),
        staticChartAccentColor = Color(0xFFE8DEF7)
    )
}

@Composable
fun MyLibraryTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    palette: ThemePalette = ThemePalette.DEFAULT,
    useCustomAccent: Boolean = false,
    customAccentArgb: Int = 0,
    useCustomStatsColor: Boolean = false,
    customStatsArgb: Int = 0,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val accent = if (useCustomAccent) Color(customAccentArgb) else when (palette) {
        ThemePalette.DEFAULT -> Color(0xFF6750A4)
        ThemePalette.OCEAN -> Color(0xFF1E88E5)
        ThemePalette.SUNSET -> Color(0xFFFF7043)
    }

    val stats = if (useCustomStatsColor) Color(customStatsArgb) else Color(0xFF7C4DFF)

    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            primary = accent,
            secondary = accent,
            tertiary = stats
        )
        else -> lightColorScheme(
            primary = accent,
            secondary = accent,
            tertiary = stats
        )
    }

    // При Material You используем tertiary из динамической схемы для статистики
    val finalStatsColor = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        scheme.tertiary
    } else {
        stats
    }

    val appColors = if (darkTheme) {
        AppColors(
            panelColor = Color(0xFF1B1B1B),
            iconTextColor = Color(0xFFEDEDED),
            mainBackgroundColor = Color(0xFF1B1B1B),
            searchBarColor = Color(0xFF2A2A2A),
            titleColorBetween = Color(0xFFEDEDED),
            tabCircleColor = Color(0xFF2C2C2C),
            selectedTabColor = accent,
            workTitleColor = Color(0xFFEDEDED),
            bottomPanelLabelColor = Color(0xFFBDBDBD),
            bottomPanelIconColor = Color(0xFFBDBDBD),
            statsActivityColor = finalStatsColor,
            staticTabBackgroundColor = Color(0xFF494456),
            staticTabContentColor = Color(0xFFE8DEF7),
            staticChartAccentColor = Color(0xFFE8DEF7)
        )
    } else {
        AppColors(
            panelColor = Color(0xFFFFFFFF),
            iconTextColor = Color(0xFF202020),
            mainBackgroundColor = Color(0xFFFFFFFF),
            searchBarColor = Color(0xFFF0F0F0),
            titleColorBetween = Color(0xFF202020),
            tabCircleColor = Color(0xFFE6E6E6),
            selectedTabColor = accent,
            workTitleColor = Color(0xFF202020),
            bottomPanelLabelColor = Color(0xFF5F6368),
            bottomPanelIconColor = Color(0xFF5F6368),
            statsActivityColor = finalStatsColor,
            staticTabBackgroundColor = Color(0xFFFFDBD7),
            staticTabContentColor = Color(0xFFF04E4F),
            staticChartAccentColor = Color(0xFFF04E4F)
        )
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = MaterialTheme.typography,
            content = content
        )
    }
}

@Composable fun PanelColor(): Color = LocalAppColors.current.panelColor
@Composable fun IconTextColor(): Color = LocalAppColors.current.iconTextColor
@Composable fun MainBackgroundColor(): Color = LocalAppColors.current.mainBackgroundColor
@Composable fun SearchBarColor(): Color = LocalAppColors.current.searchBarColor
@Composable fun TitleColorBetween(): Color = LocalAppColors.current.titleColorBetween
@Composable fun TabCircleColor(): Color = LocalAppColors.current.tabCircleColor
@Composable fun SelectedTabColor(): Color = LocalAppColors.current.selectedTabColor
@Composable fun WorkTitleColor(): Color = LocalAppColors.current.workTitleColor
@Composable fun BottomPanelLabelColor(): Color = LocalAppColors.current.bottomPanelLabelColor
@Composable fun BottomPanelIconColor(): Color = LocalAppColors.current.bottomPanelIconColor
@Composable fun StatsActivityColor(): Color = LocalAppColors.current.statsActivityColor
@Composable fun StaticTabBackgroundColor(): Color = LocalAppColors.current.staticTabBackgroundColor
@Composable fun StaticTabContentColor(): Color = LocalAppColors.current.staticTabContentColor
@Composable fun StaticChartAccentColor(): Color = LocalAppColors.current.staticChartAccentColor

