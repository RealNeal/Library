package com.rn.library.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rn.library.data.WorkRepository
import com.rn.library.ui.AppSettings
import com.rn.library.ui.Language
import com.rn.library.ui.LocalStrings
import com.rn.library.ui.components.HsvColorPicker
import com.rn.library.ui.theme.MainBackgroundColor
import com.rn.library.ui.theme.TitleColorBetween
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    currentLanguage: Language,
    onLanguageChange: (Language) -> Unit,
    currentTheme: AppTheme,
    dynamicColorsEnabled: Boolean,
    onDynamicColorsEnabledChange: (Boolean) -> Unit,
    useCustomAccent: Boolean,
    onUseCustomAccentChange: (Boolean) -> Unit,
    customAccentArgb: Int,
    onCustomAccentArgbChange: (Int) -> Unit,
    useCustomStatsColor: Boolean,
    onUseCustomStatsColorChange: (Boolean) -> Unit,
    customStatsArgb: Int,
    onCustomStatsArgbChange: (Int) -> Unit,
    booksTabEnabled: Boolean,
    onBooksTabEnabledChange: (Boolean) -> Unit,
    animeTabEnabled: Boolean,
    onAnimeTabEnabledChange: (Boolean) -> Unit,
    mangaTabEnabled: Boolean,
    onMangaTabEnabledChange: (Boolean) -> Unit,
    tvSeriesTabEnabled: Boolean,
    onTvSeriesTabEnabledChange: (Boolean) -> Unit,
    isGridView: Boolean,
    onGridViewChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val repository = remember { WorkRepository(context) }
    val scope = rememberCoroutineScope()
    val mainBackgroundColor = MainBackgroundColor()
    val titleColorBetween = TitleColorBetween()
    val dynamicScheme = MaterialTheme.colorScheme
    val settingsSectionCardColor =
        if (dynamicColorsEnabled) dynamicScheme.secondaryContainer else Color(0xFF5A5568)
    val settingsCardTextColor =
        if (dynamicColorsEnabled) {
            if (currentTheme == AppTheme.LIGHT) dynamicScheme.onSurface else dynamicScheme.onSurface
        } else {
            Color.White
        }
    val settingsInsetSurfaceColor =
        if (dynamicColorsEnabled) dynamicScheme.surfaceContainerHighest else Color(0xFF494458)

    var incrementStepText by remember {
        mutableStateOf(AppSettings.getIncrementStep(context).toString())
    }
    val scrollState = rememberScrollState()
    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(mainBackgroundColor)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = strings.cancel,
                tint = titleColorBetween
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = strings.settings,
                color = titleColorBetween,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch {
                            val (success, total) = withContext(Dispatchers.IO) {
                                repository.recompressAllCovers(context)
                            }
                            val message = if (total > 0) {
                                "Оптимизировано обложек: $success из $total"
                            } else {
                                "Обложки не найдены"
                            }
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = settingsSectionCardColor)
            ) {
                Text(
                    text = strings.optimizeCovers,
                    color = settingsCardTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = settingsSectionCardColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.tabs,
                        color = settingsCardTextColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    tabSwitchRow(strings.tabBooks, booksTabEnabled, onBooksTabEnabledChange, currentTheme, dynamicColorsEnabled, settingsCardTextColor)
                    tabSwitchRow(strings.tabAnime, animeTabEnabled, onAnimeTabEnabledChange, currentTheme, dynamicColorsEnabled, settingsCardTextColor)
                    tabSwitchRow(strings.tabManga, mangaTabEnabled, onMangaTabEnabledChange, currentTheme, dynamicColorsEnabled, settingsCardTextColor)
                    tabSwitchRow(strings.tabSeries, tvSeriesTabEnabled, onTvSeriesTabEnabledChange, currentTheme, dynamicColorsEnabled, settingsCardTextColor)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = settingsSectionCardColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.colorSettingsSection,
                        color = settingsCardTextColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.dynamicColorMaterialYou,
                            color = settingsCardTextColor,
                            fontSize = 16.sp
                        )
                        Switch(
                            checked = dynamicColorsEnabled,
                            onCheckedChange = onDynamicColorsEnabledChange,
                            colors = switchColors(currentTheme, dynamicColorsEnabled)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.useCustomAccent,
                            color = settingsCardTextColor,
                            fontSize = 16.sp
                        )
                        Switch(
                            checked = useCustomAccent,
                            onCheckedChange = onUseCustomAccentChange,
                            colors = switchColors(currentTheme, dynamicColorsEnabled)
                        )
                    }
                    if (useCustomAccent) {
                        HsvColorPicker(
                            colorArgb = customAccentArgb,
                            onColorArgbChange = onCustomAccentArgbChange,
                            labelHue = strings.colorPickerHue,
                            labelSaturation = strings.colorPickerSaturation,
                            labelValue = strings.colorPickerValue,
                            hexInputLabel = strings.colorHexCode
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.useCustomStatsColor,
                            color = settingsCardTextColor,
                            fontSize = 16.sp
                        )
                        Switch(
                            checked = useCustomStatsColor,
                            onCheckedChange = onUseCustomStatsColorChange,
                            colors = switchColors(currentTheme, dynamicColorsEnabled)
                        )
                    }
                    if (useCustomStatsColor) {
                        HsvColorPicker(
                            colorArgb = customStatsArgb,
                            onColorArgbChange = onCustomStatsArgbChange,
                            labelHue = strings.colorPickerHue,
                            labelSaturation = strings.colorPickerSaturation,
                            labelValue = strings.colorPickerValue,
                            hexInputLabel = strings.colorHexCode
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = settingsSectionCardColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.language,
                        color = settingsCardTextColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    languageSwitchRow(strings.english, currentLanguage == Language.ENGLISH, currentTheme, dynamicColorsEnabled, settingsCardTextColor) {
                        onLanguageChange(Language.ENGLISH)
                    }
                    languageSwitchRow(strings.russian, currentLanguage == Language.RUSSIAN, currentTheme, dynamicColorsEnabled, settingsCardTextColor) {
                        onLanguageChange(Language.RUSSIAN)
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = settingsSectionCardColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.incrementStepLabel,
                            color = settingsCardTextColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(70.dp))
                        OutlinedTextField(
                            value = incrementStepText,
                            onValueChange = { newValue: String ->
                                incrementStepText = newValue.filter(Char::isDigit)
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                textAlign = TextAlign.Center,
                                color = settingsCardTextColor,
                                fontSize = 16.sp
                            ),
                            modifier = Modifier
                                .width(90.dp)
                                .heightIn(max = 48.dp)
                                .onFocusChanged { state ->
                                    if (!state.isFocused) {
                                        val step = incrementStepText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                        incrementStepText = step.toString()
                                        AppSettings.setIncrementStep(context, step)
                                    }
                                },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = settingsInsetSurfaceColor,
                                unfocusedContainerColor = settingsInsetSurfaceColor,
                                disabledContainerColor = settingsInsetSurfaceColor,
                                focusedIndicatorColor = settingsCardTextColor.copy(alpha = 0.7f),
                                unfocusedIndicatorColor = settingsCardTextColor.copy(alpha = 0.4f),
                                focusedTextColor = settingsCardTextColor,
                                unfocusedTextColor = settingsCardTextColor,
                                focusedLabelColor = settingsCardTextColor.copy(alpha = 0.8f),
                                unfocusedLabelColor = settingsCardTextColor.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = settingsSectionCardColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.gridViewMode,
                            color = settingsCardTextColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = isGridView,
                            onCheckedChange = onGridViewChange,
                            colors = switchColors(currentTheme, dynamicColorsEnabled)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun tabSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    currentTheme: AppTheme,
    dynamicColorsEnabled: Boolean,
    textColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = textColor, fontSize = 16.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = switchColors(currentTheme, dynamicColorsEnabled)
        )
    }
}

@Composable
private fun languageSwitchRow(
    label: String,
    checked: Boolean,
    currentTheme: AppTheme,
    dynamicColorsEnabled: Boolean,
    textColor: Color,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = textColor, fontSize = 16.sp)
        Switch(
            checked = checked,
            onCheckedChange = { if (it) onSelect() },
            colors = switchColors(currentTheme, dynamicColorsEnabled)
        )
    }
}

@Composable
private fun switchColors(currentTheme: AppTheme, dynamicColorsEnabled: Boolean) = SwitchDefaults.colors(
    checkedThumbColor = if (dynamicColorsEnabled) MaterialTheme.colorScheme.onPrimary else Color.White,
    checkedTrackColor = when {
        dynamicColorsEnabled -> MaterialTheme.colorScheme.primary
        currentTheme == AppTheme.DARK -> Color(0xFF494458)
        else -> Color(0xFF8A84A3)
    },
    uncheckedThumbColor = if (dynamicColorsEnabled) {
        MaterialTheme.colorScheme.outline
    } else if (currentTheme == AppTheme.DARK) {
        Color(0xFF757575)
    } else {
        Color(0xFFBDBDBD)
    },
    uncheckedTrackColor = if (dynamicColorsEnabled) {
        MaterialTheme.colorScheme.surfaceVariant
    } else if (currentTheme == AppTheme.DARK) {
        Color(0xFF2A2A2A)
    } else {
        Color(0xFFE0E0E0)
    }
)
