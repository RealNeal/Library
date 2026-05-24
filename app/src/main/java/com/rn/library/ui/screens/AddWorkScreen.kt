package com.rn.library.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.rn.library.data.*
import com.rn.library.ui.LocalStrings
import com.rn.library.ui.components.ScrollIsolatedMultilineField
import com.rn.library.ui.components.UnitProgressEditor
import com.rn.library.ui.components.buildDefaultUnits
import com.rn.library.ui.theme.MainBackgroundColor
import com.rn.library.ui.theme.SearchBarColor
import com.rn.library.ui.theme.TitleColorBetween
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

// Функция для форматирования чисел без десятичных знаков, если они целые
fun formatNumberForDisplay(value: Double?): String {
    return if (value == null) {
        ""
    } else {
        if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            value.toString()
        }
    }
}

// Преобразование альтернативных названий из формата сохранения в многострочный для отображения
fun formatAlternativeTitlesForDisplay(input: String?): String {
    return if (input.isNullOrEmpty()) {
        ""
    } else {
        input.split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }
}

@Composable
fun AddWorkScreen(
    onBack: () -> Unit,
    onSave: (Work) -> Unit,
    work: Work? = null,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val bg = MainBackgroundColor()
    val fieldBg = SearchBarColor()
    val text = TitleColorBetween()
    val fieldColors = TextFieldDefaults.colors(
        focusedContainerColor = fieldBg,
        unfocusedContainerColor = fieldBg,
        disabledContainerColor = fieldBg,
        errorContainerColor = fieldBg,
        focusedTextColor = text,
        unfocusedTextColor = text,
        disabledTextColor = text,
        focusedLabelColor = text,
        unfocusedLabelColor = text,
        focusedPlaceholderColor = text.copy(alpha = 0.5f),
        unfocusedPlaceholderColor = text.copy(alpha = 0.5f),
        cursorColor = text
    )
    val context = LocalContext.current
    val repository = remember { WorkRepository(context) }
    val stableWorkId = remember(work?.id) { work?.id ?: UUID.randomUUID().toString() }

    // Основные поля
    var title by remember(work?.title) { mutableStateOf(work?.title ?: "") }
    var alternativeTitles by remember(work?.otherTitle) { mutableStateOf(formatAlternativeTitlesForDisplay(work?.otherTitle) ?: "") }
    var description by remember(work?.description) { mutableStateOf(work?.description ?: "") }
    var type by remember(work?.type) { mutableStateOf(work?.type ?: WorkType.BOOK) }
    var status by remember(work?.status) { mutableStateOf(work?.status ?: WorkStatus.IN_PLANS) }

    // Поля для томов/сезонов и глав/серий
    var volumes by remember(work?.volumes) { mutableStateOf(formatNumberForDisplay(work?.volumes)) }
    var seasons by remember(work?.seasons) { mutableStateOf(work?.seasons?.toString() ?: "") }
    var chapters by remember(work?.chapters) { mutableStateOf(formatNumberForDisplay(work?.chapters)) }
    var bookChapters by remember(work?.bookChapters) { mutableStateOf(formatNumberForDisplay(work?.bookChapters)) }
    var episodes by remember(work?.episodes) { mutableStateOf(formatNumberForDisplay(work?.episodes)) }
    var readChapters by remember(work?.progress) { mutableStateOf(formatNumberForDisplay(work?.progress)) }
    var useUnitProgress by remember(work?.unitProgress) {
        mutableStateOf(work?.unitProgress?.isNotEmpty() == true)
    }
    var unitProgressList by remember(work?.unitProgress) {
        mutableStateOf(work?.unitProgress ?: emptyList())
    }

    // Обложки (до 10) в порядке добавления; selectedCoverIndex — главная для списка библиотеки
    var coverPaths by remember(work?.id, work?.coverPath, work?.coverPaths) {
        mutableStateOf(work?.allCoverPaths()?.take(10) ?: emptyList())
    }
    var selectedCoverIndex by remember(work?.id, work?.coverPath, work?.coverPaths) {
        val paths = work?.allCoverPaths().orEmpty()
        val primaryIndex = work?.coverPath?.let { paths.indexOf(it) }?.takeIf { it >= 0 } ?: 0
        mutableStateOf(primaryIndex)
    }

    // Дополнительные поля
    var country by remember(work?.country) { mutableStateOf(work?.country ?: "") }
    var animeSeason by remember(work?.animeSeason) { mutableStateOf(work?.animeSeason ?: AnimeSeason.SPRING) }
    var mangaType by remember(work?.mangaType) { mutableStateOf(work?.mangaType ?: MangaType.MANGA) }
    var seriesType by remember(work?.seriesType) { mutableStateOf(work?.seriesType ?: SeriesType.TV_SERIES) }
    var year by remember(work?.year, work?.yearPeriod) {
        mutableStateOf(work?.yearPeriod ?: work?.year?.toString().orEmpty())
    }

    // Инициализация дат в формате ДД.ММ.ГГГГ для отображения
    var dateRead by remember(work?.dateRead) {
        mutableStateOf(
            work?.dateRead?.let { date ->
                val parts = date.split("-")
                if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else date
            } ?: ""
        )
    }
    // Храним только цифры DDMMYYYY (маска ставит точки визуально)
    var rereadDate by remember(work?.rereadDates) {
        mutableStateOf(
            work?.rereadDates
                ?.firstOrNull()
                ?.split("-")
                ?.let { parts -> if (parts.size == 3) parts[2] + parts[1] + parts[0] else "" }
                .orEmpty()
        )
    }
    var link1 by remember(work?.link) { mutableStateOf(work?.link ?: "") }
    var link2 by remember(work?.link2) { mutableStateOf(work?.link2 ?: "") }
    var note by remember(work?.note) { mutableStateOf(work?.note ?: "") }

    // Состояния для выпадающих списков
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }
    var seasonDropdownExpanded by remember { mutableStateOf(false) }
    var mangaTypeDropdownExpanded by remember { mutableStateOf(false) }
    var seriesTypeDropdownExpanded by remember { mutableStateOf(false) }

    fun closeNow() {
        typeDropdownExpanded = false
        statusDropdownExpanded = false
        seasonDropdownExpanded = false
        mangaTypeDropdownExpanded = false
        seriesTypeDropdownExpanded = false
        onBack()
    }

    // Обработка системной кнопки "Назад"
    BackHandler { closeNow() }

    // Launcher для выбора обложки
    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            val persisted = repository.persistCoverFromPicker(pickedUri, stableWorkId, coverPaths.size)
                ?: pickedUri.toString()
            val existingIndex = coverPaths.indexOf(persisted)
            if (existingIndex >= 0) {
                selectedCoverIndex = existingIndex
            } else if (coverPaths.size < 10) {
                coverPaths = coverPaths + persisted
            }
        }
    }

    // Валидация года: YYYY или период YYYY - YYYY
    val singleYearPattern = Pattern.compile("^\\d{0,4}$")
    val yearPeriodPattern = Pattern.compile("^\\d{4}\\s?-\\s?\\d{0,4}$")

    // Визуальное преобразование для форматирования даты ДД.ММ.ГГГГ
    val dateVisualTransformation = VisualTransformation { text ->
        val digits = text.text.replace("[^0-9]".toRegex(), "")
        val formatted = StringBuilder()
        for (i in digits.indices) {
            if (i == 2 || i == 4) {
                if (formatted.length < 10) formatted.append(".")
            }
            if (formatted.length < 10) formatted.append(digits[i])
        }
        TransformedText(
            text = AnnotatedString(formatted.toString()),
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    var transformedOffset = offset
                    if (offset > 2) transformedOffset++
                    if (offset > 4) transformedOffset++
                    return transformedOffset.coerceAtMost(formatted.length)
                }

                override fun transformedToOriginal(offset: Int): Int {
                    var originalOffset = offset
                    if (offset > 2) originalOffset--
                    if (offset > 5) originalOffset--
                    return originalOffset.coerceAtMost(digits.length)
                }
            }
        )
    }

    // Форматирование даты для сохранения (только цифры)
    fun formatDateForSave(input: String): String {
        val digits = input.replace("[^0-9]".toRegex(), "")
        if (digits.length >= 8) {
            val day = digits.substring(0, 2)
            val month = digits.substring(2, 4)
            val year = digits.substring(4, 8)
            return "$year-$month-$day"
        }
        return ""
    }

    // Форматирование альтернативных названий для сохранения (преобразование переносов в точки с запятой)
    fun formatAlternativeTitlesForSave(input: String): String {
        return input.split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("; ")
    }

    // Получаем правильные опции статуса в зависимости от типа
    fun getStatusOptions(): List<Pair<WorkStatus, String>> {
        return when (type) {
            WorkType.BOOK, WorkType.MANGA -> listOf(
                WorkStatus.IN_PLANS to strings.inPlans,
                WorkStatus.READING to strings.reading,
                WorkStatus.READ to strings.read,
                WorkStatus.ABANDONED to strings.abandoned
            )
            WorkType.ANIME, WorkType.SERIES -> listOf(
                WorkStatus.IN_PLANS to strings.inPlans,
                WorkStatus.WATCHING to strings.watching,
                WorkStatus.WATCHED to strings.watched,
                WorkStatus.ABANDONED to strings.abandoned
            )
        }
    }

    // Проверяем, нужно ли показывать дату прочтения
    val showDateRead = status in listOf(WorkStatus.READ, WorkStatus.WATCHED, WorkStatus.ABANDONED)

    val formScrollState = rememberScrollState()


    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
    ) {
            // Заголовок с кнопкой назад
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(vertical = 16.dp)
            ) {
                IconButton(onClick = ::closeNow) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = text)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (work == null) strings.addWork else strings.editWork,
                    color = text,
                    style = MaterialTheme.typography.titleLarge
                )
            }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(state = formScrollState) // Скролл формы
                .padding(horizontal = 16.dp)
                .imePadding(), // <--- ДОБАВЬ ЭТУ СТРОКУ
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                // Название
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.title) },
                    placeholder = { Text(strings.titlePlaceholder) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBg
                    )
                )

                ScrollIsolatedMultilineField(
                    value = alternativeTitles,
                    onValueChange = { alternativeTitles = it },
                    label = { Text(strings.alternativeTitles) },
                    placeholder = { Text(strings.otherTitlesPlaceholder) },
                    minLines = 1,
                    maxLines = 3,
                    colors = fieldColors,
                )

                ScrollIsolatedMultilineField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(strings.description) },
                    minLines = 5,
                    maxLines = 11,
                    colors = fieldColors,
                )

                // Тип произведения
                CustomDropdown(
                    label = strings.type,
                    value = when (type) {
                        WorkType.BOOK -> "Книга"
                        WorkType.ANIME -> "Аниме"
                        WorkType.MANGA -> "Манга"
                        WorkType.SERIES -> "Сериал"
                    },
                    items = listOf("Книга", "Аниме", "Манга", "Сериал"),
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = it },
                    onItemSelected = { selected ->
                        type = when (selected) {
                            "Книга" -> WorkType.BOOK
                            "Аниме" -> WorkType.ANIME
                            "Манга" -> WorkType.MANGA
                            "Сериал" -> WorkType.SERIES
                            else -> WorkType.BOOK
                        }
                        typeDropdownExpanded = false
                        // Сбрасываем статус если он не соответствует типу
                        val validStatuses = getStatusOptions().map { it.first }
                        if (status !in validStatuses) {
                            status = WorkStatus.IN_PLANS
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Подтип для манги/сериала
                AnimatedVisibility(
                    visible = type == WorkType.MANGA,
                    enter = expandVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) +
                        fadeIn(animationSpec = tween(250, easing = FastOutSlowInEasing)),
                    exit = shrinkVertically(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                        fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
                ) {
                    CustomDropdown(
                        label = strings.mangaType,
                        value = when (mangaType) {
                            MangaType.MANGA -> "Манга"
                            MangaType.MANHWA -> "Манхва"
                            MangaType.MANHUA -> "Маньхуа"
                        },
                        items = listOf("Манга", "Манхва", "Маньхуа"),
                        expanded = mangaTypeDropdownExpanded,
                        onExpandedChange = { mangaTypeDropdownExpanded = it },
                        onItemSelected = { selected ->
                            mangaType = when (selected) {
                                "Манга" -> MangaType.MANGA
                                "Манхва" -> MangaType.MANHWA
                                "Маньхуа" -> MangaType.MANHUA
                                else -> MangaType.MANGA
                            }
                            mangaTypeDropdownExpanded = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                AnimatedVisibility(
                    visible = type == WorkType.SERIES,
                    enter = expandVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) +
                        fadeIn(animationSpec = tween(250, easing = FastOutSlowInEasing)),
                    exit = shrinkVertically(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                        fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
                ) {
                    CustomDropdown(
                        label = strings.tvSeriesType,
                        value = when (seriesType) {
                            SeriesType.TV_SERIES -> "Сериал"
                            SeriesType.FILM -> "Фильм"
                            SeriesType.CARTOON -> "Мультфильм"
                            SeriesType.DRAMA -> "Дорама"
                        },
                        items = listOf("Сериал", "Фильм", "Мультфильм", "Дорама"),
                        expanded = seriesTypeDropdownExpanded,
                        onExpandedChange = { seriesTypeDropdownExpanded = it },
                        onItemSelected = { selected ->
                            seriesType = when (selected) {
                                "Сериал" -> SeriesType.TV_SERIES
                                "Фильм" -> SeriesType.FILM
                                "Мультфильм" -> SeriesType.CARTOON
                                "Дорама" -> SeriesType.DRAMA
                                else -> SeriesType.TV_SERIES
                            }
                            seriesTypeDropdownExpanded = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Тома/Сезоны
                val volumesLabel = when (type) {
                    WorkType.BOOK, WorkType.MANGA -> strings.volumes
                    WorkType.SERIES -> strings.seasons
                    WorkType.ANIME -> "" // Для аниме тома не используются
                }
                val volumesValue = when (type) {
                    WorkType.BOOK -> chapters
                    WorkType.MANGA -> volumes
                    WorkType.SERIES -> seasons
                    WorkType.ANIME -> ""
                }

                AnimatedVisibility(
                    visible = volumesLabel.isNotEmpty(),
                    enter = expandVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) +
                        fadeIn(animationSpec = tween(250, easing = FastOutSlowInEasing)),
                    exit = shrinkVertically(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                        fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
                ) {
                    OutlinedTextField(
                        value = volumesValue,
                        onValueChange = { newValue ->
                            when (type) {
                                WorkType.BOOK -> chapters = newValue
                                WorkType.MANGA -> volumes = newValue
                                WorkType.SERIES -> seasons = newValue
                                WorkType.ANIME -> {}
                            }
                        },
                        label = { Text(volumesLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = fieldBg,
                            unfocusedContainerColor = fieldBg
                        )
                    )
                }

                // Главы/Серии
                val chaptersLabel = when (type) {
                    WorkType.BOOK -> strings.chapters
                    WorkType.MANGA -> strings.chapters
                    WorkType.ANIME, WorkType.SERIES -> strings.episodes
                }
                val chaptersValue = when (type) {
                    WorkType.BOOK -> bookChapters
                    WorkType.MANGA -> chapters
                    WorkType.ANIME, WorkType.SERIES -> episodes
                }

                OutlinedTextField(
                    value = chaptersValue,
                    onValueChange = { newValue ->
                        when (type) {
                            WorkType.BOOK -> bookChapters = newValue
                            WorkType.MANGA -> chapters = newValue
                            WorkType.ANIME, WorkType.SERIES -> episodes = newValue
                        }
                    },
                    label = { Text(chaptersLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBg
                    )
                )

                val unitCountHint = when (type) {
                    WorkType.BOOK -> chapters.toDoubleOrNull()?.toInt()
                    WorkType.MANGA -> volumes.toDoubleOrNull()?.toInt()
                    WorkType.SERIES -> seasons.toIntOrNull()
                    WorkType.ANIME -> seasons.toIntOrNull()
                }

                UnitProgressEditor(
                    enabled = useUnitProgress,
                    onEnabledChange = { enabled ->
                        useUnitProgress = enabled
                        if (!enabled) {
                            unitProgressList = emptyList()
                        } else if (unitProgressList.isEmpty() && unitCountHint != null && unitCountHint > 0) {
                            val prefix = when (type) {
                                WorkType.BOOK, WorkType.MANGA -> strings.volumeUnitPrefix
                                WorkType.SERIES, WorkType.ANIME -> strings.seasonUnitPrefix
                            }
                            unitProgressList = buildDefaultUnits(unitCountHint, prefix)
                        }
                    },
                    units = unitProgressList,
                    onUnitsChange = { unitProgressList = it },
                    workType = type,
                    unitCountHint = unitCountHint,
                    fieldBg = fieldBg,
                    labelColor = text
                )

                val progressLabel = when (type) {
                    WorkType.BOOK, WorkType.MANGA -> strings.progressReadChapters
                    WorkType.ANIME, WorkType.SERIES -> strings.progressWatchedEpisodes
                }

                OutlinedTextField(
                    value = readChapters,
                    onValueChange = { readChapters = it },
                    label = { Text(progressLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBg
                    )
                )

                // Обложка
                Column {
                    Text(
                        "${strings.cover} (${coverPaths.size}/10)",
                        color = text,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(fieldBg, RoundedCornerShape(12.dp))
                            .clickable {
                                if (coverPaths.size < 10) {
                                    coverPickerLauncher.launch("image/*")
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val selectedCover = coverPaths.getOrNull(selectedCoverIndex)
                        if (!selectedCover.isNullOrBlank()) {
                            androidx.compose.foundation.Image(
                                painter = rememberAsyncImagePainter(selectedCover.toCoverImageData()),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Text(
                                strings.cover,
                                color = text.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    if (coverPaths.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(coverPaths.size) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (index == selectedCoverIndex) text.copy(alpha = 0.25f) else fieldBg
                                        )
                                        .clickable { selectedCoverIndex = index }
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = rememberAsyncImagePainter(coverPaths[index].toCoverImageData()),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = {
                                            val newList = coverPaths.toMutableList().also { it.removeAt(index) }
                                            coverPaths = newList
                                            selectedCoverIndex = selectedCoverIndex.coerceAtMost((newList.size - 1).coerceAtLeast(0))
                                        },
                                        modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Страна
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text(strings.country) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBg
                    )
                )

                // Время года (только для аниме)
                AnimatedVisibility(
                    visible = type == WorkType.ANIME,
                    enter = expandVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) +
                        fadeIn(animationSpec = tween(250, easing = FastOutSlowInEasing)),
                    exit = shrinkVertically(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                        fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
                ) {
                    CustomDropdown(
                        label = strings.animeSeason,
                        value = when (animeSeason) {
                            AnimeSeason.SPRING -> strings.spring
                            AnimeSeason.SUMMER -> strings.summer
                            AnimeSeason.FALL -> strings.fall
                            AnimeSeason.WINTER -> strings.winter
                        },
                        items = listOf(strings.winter, strings.spring, strings.summer, strings.fall),
                        expanded = seasonDropdownExpanded,
                        onExpandedChange = { seasonDropdownExpanded = it },
                        onItemSelected = { selected ->
                            animeSeason = when (selected) {
                                strings.winter -> AnimeSeason.WINTER
                                strings.spring -> AnimeSeason.SPRING
                                strings.summer -> AnimeSeason.SUMMER
                                strings.fall -> AnimeSeason.FALL
                                else -> AnimeSeason.SPRING
                            }
                            seasonDropdownExpanded = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Год
                OutlinedTextField(
                    value = year,
                    onValueChange = { input ->
                        val normalized = input.trimStart()
                        if (
                            normalized.isEmpty() ||
                            singleYearPattern.matcher(normalized).matches() ||
                            yearPeriodPattern.matcher(normalized).matches()
                        ) {
                            year = input
                        }
                    },
                    label = { Text(strings.year) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    placeholder = { Text("ГГГГ или ГГГГ - ГГГГ") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBg
                    )
                )

                // Статус
                CustomDropdown(
                    label = strings.status,
                    value = when (status) {
                        WorkStatus.IN_PLANS -> strings.inPlans
                        WorkStatus.READING -> strings.reading
                        WorkStatus.WATCHING -> strings.watching
                        WorkStatus.READ -> strings.read
                        WorkStatus.WATCHED -> strings.watched
                        WorkStatus.ABANDONED -> strings.abandoned
                    },
                    items = getStatusOptions().map { it.second },
                    expanded = statusDropdownExpanded,
                    onExpandedChange = { statusDropdownExpanded = it },
                    onItemSelected = { selected ->
                        status = getStatusOptions().find { it.second == selected }?.first ?: WorkStatus.IN_PLANS
                        statusDropdownExpanded = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Дата прочтения (если статус прочитано/просмотрено/заброшено)
                AnimatedVisibility(
                    visible = showDateRead,
                    enter = expandVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) +
                        fadeIn(animationSpec = tween(250, easing = FastOutSlowInEasing)),
                    exit = shrinkVertically(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                        fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
                ) {
                    OutlinedTextField(
                        value = dateRead,
                        onValueChange = { input ->
                            // Сохраняем только цифры
                            val digits = input.replace("[^0-9]".toRegex(), "")
                            dateRead = digits
                        },
                        label = { Text(
                            when (type) {
                                WorkType.BOOK, WorkType.MANGA -> strings.dateReadForBooks
                                else -> strings.dateWatched
                            }
                        ) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(strings.datePlaceholder) },
                        visualTransformation = dateVisualTransformation,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = fieldBg,
                            unfocusedContainerColor = fieldBg
                        )
                    )
                }

                // Дата перепрочтения/пересмотра
                OutlinedTextField(
                    value = rereadDate,
                    onValueChange = { input ->
                        // Сохраняем только цифры
                        val digits = input.replace("[^0-9]".toRegex(), "")
                        rereadDate = digits
                    },
                    label = { Text(
                        when (type) {
                            WorkType.BOOK, WorkType.MANGA -> strings.dateReread
                            WorkType.ANIME, WorkType.SERIES -> strings.dateRewatch
                        }
                    ) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(strings.datePlaceholder) },
                    visualTransformation = dateVisualTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBg
                    )
                )

                // Ссылки
                OutlinedTextField(
                    value = link1,
                    onValueChange = { link1 = it },
                    label = { Text(strings.link1) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBg
                    )
                )

                OutlinedTextField(
                    value = link2,
                    onValueChange = { link2 = it },
                    label = { Text(strings.link2) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBg
                    )
                )

                // Заметка
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(strings.noteLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBg
                    )
                )

                Button(
                    onClick = {
                        val id = stableWorkId
                        val rereadSaved = formatDateForSave(rereadDate)
                            .takeIf { it.isNotEmpty() }
                            ?.let { listOf(it) }
                            ?: emptyList()

                        val workToSave = Work(
                            // Год может быть либо одиночным, либо периодом.
                            // Если введён период, сохраняем в yearPeriod, а year оставляем null.
                            // Если введён одиночный год, сохраняем в year.
                            // Это сохраняет обратную совместимость со старыми данными.
                            id = id,
                            title = title.trim(),
                            description = description,
                            type = type,
                            status = status,
                            otherTitle = formatAlternativeTitlesForSave(alternativeTitles).ifEmpty { null },
                            coverPath = coverPaths.getOrNull(selectedCoverIndex),
                            coverPaths = coverPaths,
                            volumes = if (type == WorkType.MANGA) volumes.toDoubleOrNull() else null,
                            seasons = seasons.toIntOrNull(),
                            chapters = when (type) {
                                WorkType.BOOK, WorkType.MANGA -> chapters.toDoubleOrNull()
                                else -> null
                            },
                            bookChapters = bookChapters.toDoubleOrNull(),
                            episodes = episodes.toDoubleOrNull(),
                            progress = when {
                                useUnitProgress && unitProgressList.isNotEmpty() ->
                                    unitProgressList.sumOf { it.completed }
                                readChapters.isBlank() -> null
                                else -> readChapters.toDoubleOrNull()
                            },
                            activeUnitIndex = work?.activeUnitIndex,
                            country = country.ifEmpty { null },
                            animeSeason = animeSeason,
                            mangaType = if (type == WorkType.MANGA) mangaType else null,
                            seriesType = if (type == WorkType.SERIES) seriesType else null,
                            year = year.trim().takeIf { singleYearPattern.matcher(it).matches() }?.toIntOrNull(),
                            yearPeriod = year.trim()
                                .takeIf { yearPeriodPattern.matcher(it).matches() }
                                ?.replace(Regex("\\s*-\\s*"), " - "),
                            dateRead = formatDateForSave(dateRead).ifEmpty { null },
                            rereadDates = rereadSaved,
                            link = link1.ifEmpty { null },
                            link2 = link2.ifEmpty { null },
                            note = note.ifEmpty { null },
                            updatedAt = System.currentTimeMillis(),
                            // Сохраняем существующие поля
                            abandonedProgress = work?.abandonedProgress,
                            readingPeriods = work?.readingPeriods ?: emptyList(),
                            unitProgress = if (useUnitProgress) unitProgressList else emptyList()
                        )
                        onSave(workToSave)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    enabled = title.isNotBlank()
                ) {
                    Text(strings.save)
                }

                // Чтобы кнопка и последние поля не “прилипали” к нижней панели вкладок.
                Spacer(modifier = Modifier.height(24.dp))
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDropdown(
    label: String,
    value: String,
    items: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val fieldBg = SearchBarColor()
    val text = TitleColorBetween()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = text,
                    modifier = Modifier.rotate(
                        animateFloatAsState(
                            targetValue = if (expanded) 180f else 0f,
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ).value
                    )
                )
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = fieldBg,
                unfocusedContainerColor = fieldBg
            )
        )

        // Анимированное выпадающее меню
        AnimatedVisibility(
            visible = expanded,
            enter = slideInVertically(
                initialOffsetY = { -20 },
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(250, easing = FastOutSlowInEasing)),
            exit = slideOutVertically(
                targetOffsetY = { -20 },
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
        ) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.background(fieldBg),
                offset = DpOffset((-2).dp, (-8).dp)
            ) {
                items.forEachIndexed { index, item ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(
                            initialOffsetY = { (index + 1) * 10 },
                            animationSpec = tween(200, delayMillis = index * 30, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(200, delayMillis = index * 30, easing = FastOutSlowInEasing)),
                        exit = slideOutVertically(
                            targetOffsetY = { -10 },
                            animationSpec = tween(150, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = item,
                                    color = text
                                )
                            },
                            onClick = {
                                onItemSelected(item)
                            },
                            modifier = Modifier.background(fieldBg)
                        )
                    }
                }
            }
        }
    }
}