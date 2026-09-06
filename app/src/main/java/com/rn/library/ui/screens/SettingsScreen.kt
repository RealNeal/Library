package com.rn.library.ui.screens

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rn.library.R
import com.rn.library.data.WorkRepository
import com.rn.library.ui.AppSettings
import com.rn.library.ui.Language
import com.rn.library.ui.components.HsvColorPicker
import com.rn.library.ui.components.bottomNavigationClearance
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
    var checkingUpdates by remember { mutableStateOf(false) }
    var availableRelease by remember { mutableStateOf<com.rn.library.update.GitHubRelease?>(null) }
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
                contentDescription = stringResource(R.string.cancel),
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
                text = stringResource(R.string.settings),
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
                    text = stringResource(R.string.optimize_covers),
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
                        text = stringResource(R.string.tabs),
                        color = settingsCardTextColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    tabSwitchRow(stringResource(R.string.tab_books), booksTabEnabled, onBooksTabEnabledChange, currentTheme, dynamicColorsEnabled, settingsCardTextColor)
                    tabSwitchRow(stringResource(R.string.tab_anime), animeTabEnabled, onAnimeTabEnabledChange, currentTheme, dynamicColorsEnabled, settingsCardTextColor)
                    tabSwitchRow(stringResource(R.string.tab_manga), mangaTabEnabled, onMangaTabEnabledChange, currentTheme, dynamicColorsEnabled, settingsCardTextColor)
                    tabSwitchRow(stringResource(R.string.tab_series), tvSeriesTabEnabled, onTvSeriesTabEnabledChange, currentTheme, dynamicColorsEnabled, settingsCardTextColor)
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
                        text = stringResource(R.string.color_settings_section),
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
                            text = stringResource(R.string.dynamic_color_material_you),
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
                            text = stringResource(R.string.use_custom_accent),
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
                            labelHue = stringResource(R.string.color_picker_hue),
                            labelSaturation = stringResource(R.string.color_picker_saturation),
                            labelValue = stringResource(R.string.color_picker_value),
                            hexInputLabel = stringResource(R.string.color_hex_code)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.use_custom_stats_color),
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
                            labelHue = stringResource(R.string.color_picker_hue),
                            labelSaturation = stringResource(R.string.color_picker_saturation),
                            labelValue = stringResource(R.string.color_picker_value),
                            hexInputLabel = stringResource(R.string.color_hex_code)
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
                        .padding(16.dp)
                ) {
                    var languageMenuExpanded by remember { mutableStateOf(false) }
                    val languages = listOf(
                        Language.ENGLISH to stringResource(R.string.english),
                        Language.RUSSIAN to stringResource(R.string.russian),
                        Language.GERMAN to stringResource(R.string.german),
                        Language.FRENCH to stringResource(R.string.french),
                        Language.SPANISH to stringResource(R.string.spanish),
                        Language.PORTUGUESE to stringResource(R.string.portuguese),
                        Language.CHINESE to stringResource(R.string.chinese),
                        Language.JAPANESE to stringResource(R.string.japanese),
                        Language.KOREAN to stringResource(R.string.korean)
                    )
                    val selectedLanguageLabel = languages.firstOrNull { it.first == currentLanguage }?.second
                        ?: stringResource(R.string.english)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { languageMenuExpanded = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.language),
                            color = settingsCardTextColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedLanguageLabel,
                                    color = settingsCardTextColor,
                                    fontSize = 18.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = settingsCardTextColor
                                )
                            }
                            DropdownMenu(
                                expanded = languageMenuExpanded,
                                onDismissRequest = { languageMenuExpanded = false }
                            ) {
                                languages.forEach { (lang, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            onLanguageChange(lang)
                                            languageMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = settingsSectionCardColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.increment_step_label),
                        color = settingsCardTextColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
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
                                .width(72.dp)
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
                                unfocusedTextColor = settingsCardTextColor
                            )
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = settingsSectionCardColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.grid_view_mode),
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

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !checkingUpdates) {
                        checkingUpdates = true
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                com.rn.library.update.GitHubUpdateChecker.check(context)
                            }
                            checkingUpdates = false
                            when (result) {
                                is com.rn.library.update.UpdateCheckResult.UpToDate -> {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.update_up_to_date, result.currentVersion),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                is com.rn.library.update.UpdateCheckResult.Available -> {
                                    availableRelease = result.release
                                }
                                is com.rn.library.update.UpdateCheckResult.Error -> {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.update_error),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = settingsSectionCardColor)
            ) {
                Text(
                    text = if (checkingUpdates) {
                        stringResource(R.string.update_checking)
                    } else {
                        stringResource(R.string.check_updates)
                    },
                    color = settingsCardTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            Spacer(modifier = Modifier.height(if (isLandscape) 16.dp else bottomNavigationClearance()))
        }

        availableRelease?.let { release ->
            AlertDialog(
                onDismissRequest = { availableRelease = null },
                title = { Text(stringResource(R.string.update_available_title), color = titleColorBetween) },
                text = {
                    Text(
                        stringResource(
                            R.string.update_available_message,
                            com.rn.library.update.GitHubUpdateChecker.currentVersionName(context),
                            release.tag
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val targetUrl = release.htmlUrl.ifBlank {
                                "https://github.com/${com.rn.library.update.GitHubUpdateChecker.OWNER}/${com.rn.library.update.GitHubUpdateChecker.REPO}/releases/latest"
                            }
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(targetUrl)).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                            }
                            availableRelease = null
                        }
                    ) {
                        Text(stringResource(R.string.update_download))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { availableRelease = null }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
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
