package com.rn.library.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.rn.library.ui.screens.AppTheme

/** Цвет карточек-кнопок на вкладке «Профиль» (Добавить, Экспорт и т.д.). */
@Composable
fun ProfileSectionCardColor(dynamicColorsEnabled: Boolean): Color =
    if (dynamicColorsEnabled) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color(0xFF5A5568)
    }

/** Цвет заливки Heatmap / столбцов «Активность» — совпадает с кнопками профиля. */
@Composable
fun ProfileChartAccentColor(dynamicColorsEnabled: Boolean): Color =
    ProfileSectionCardColor(dynamicColorsEnabled)
