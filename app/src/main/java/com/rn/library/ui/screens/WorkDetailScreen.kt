package com.rn.library.ui.screens

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.rn.library.data.*
import com.rn.library.data.readProgressUnits
import com.rn.library.data.watchedProgressUnits
import com.rn.library.ui.LocalStrings
import com.rn.library.util.LinkOpenHelper
import com.rn.library.ui.theme.*
import java.io.File
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.window.Dialog
import com.rn.library.ui.AppSettings
import com.rn.library.ui.components.CoverCarousel
import com.rn.library.ui.components.filterUnitDecimalInput
import com.rn.library.ui.components.formatEditableUnitNumber
import com.rn.library.ui.components.parseUnitDecimalInput
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

// Модель данных для редактирования информации
private data class EditInfoItem(
    val key: String,
    val label: String,
    val value: String?,
    val type: EditInfoType,
    val originalValue: Any?
)

private enum class EditInfoType {
    STRING,
    INT,
    DATE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkDetailScreen(
    work: Work,
    coverPaths: List<String> = work.allCoverPaths(),
    sessionCoverPath: String? = null,
    onSessionCoverPathChange: (String) -> Unit = {},
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSave: (Work) -> Unit,
    onCoverClick: () -> Unit = {},
    currentTheme: AppTheme = AppTheme.DARK,
    onScrollStateChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val mainBackgroundColor = MainBackgroundColor()
    val titleColorBetween = TitleColorBetween()
    val iconTextColor = IconTextColor()
    val searchBarColor = SearchBarColor()
    val fieldTextColor = if (currentTheme == AppTheme.DARK) Color.White else Color.Black

    val statusLabel = when (work.status) {
        WorkStatus.READ -> strings.read
        WorkStatus.READING -> strings.reading
        WorkStatus.WATCHING -> strings.watching
        WorkStatus.WATCHED -> strings.watched
        WorkStatus.IN_PLANS -> strings.inPlans
        WorkStatus.ABANDONED -> strings.abandoned
    }

    // Цвета для статусов (те же, что в ProfileScreen)
    val statusColor = when (work.status) {
        WorkStatus.IN_PLANS -> Color(0xFF8E6687)      // rgb(142, 102, 147)
        WorkStatus.ABANDONED -> Color(0xFFFF5F5A)     // rgb(255, 95, 90)
        WorkStatus.READING -> Color(0xFF7179A4)       // rgb(113, 121, 164)
        WorkStatus.WATCHING -> Color(0xFF7179A4)      // rgb(113, 121, 164)
        WorkStatus.READ -> Color(0xFF79C77C)          // rgb(121, 199, 124)
        WorkStatus.WATCHED -> Color(0xFF79C77C)        // rgb(121, 199, 124)
    }

    // Лейблы для SeriesType и MangaType
    val seriesTypeLabel = when (work.seriesType) {
        SeriesType.TV_SERIES -> strings.tvSeries
        SeriesType.FILM -> strings.film
        SeriesType.CARTOON -> strings.cartoon
        SeriesType.DRAMA -> strings.drama
        null -> null
    }

    val mangaTypeLabel = when (work.mangaType) {
        MangaType.MANGA -> strings.manga
        MangaType.MANHWA -> strings.manhwa
        MangaType.MANHUA -> strings.manhua
        null -> null
    }

    val scrollState = rememberScrollState()
    val context = LocalContext.current
    // Используем LocalClipboardManager: он помечен deprecated, но стабильно работает в текущей версии Compose
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    var currentCoverPath by remember(work.id, coverPaths, sessionCoverPath) {
        mutableStateOf(
            sessionCoverPath?.takeIf { it in coverPaths }
                ?: coverPaths.firstOrNull()
        )
    }

    LaunchedEffect(sessionCoverPath, coverPaths) {
        val resolved = sessionCoverPath?.takeIf { it in coverPaths } ?: coverPaths.firstOrNull()
        if (resolved != null && resolved != currentCoverPath) {
            currentCoverPath = resolved
        }
    }

    val otherTitles = remember(work.otherTitle) {
        work.otherTitle
            ?.split(";", "\n")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
    }
    var otherTitlesExpanded by remember { mutableStateOf(false) }
    var statusMenuExpanded by remember { mutableStateOf(false) }
    var seriesTypeMenuExpanded by remember { mutableStateOf(false) }
    var mangaTypeMenuExpanded by remember { mutableStateOf(false) }
    
    // Pending changes to apply asynchronously
    var pendingStatusChange by remember { mutableStateOf<WorkStatus?>(null) }
    var pendingSeriesTypeChange by remember { mutableStateOf<SeriesType?>(null) }
    var pendingMangaTypeChange by remember { mutableStateOf<MangaType?>(null) }

    // Состояния для редактирования информации
    var showEditInfoDialog by remember { mutableStateOf(false) }
    var editInfoParts by remember { mutableStateOf<List<EditInfoItem>>(emptyList()) }
    var temporaryWork by remember { mutableStateOf(work) }

    // Синхронизируем temporaryWork с work при изменении
    LaunchedEffect(work) {
        temporaryWork = work
    }
    
    // Handle pending status changes asynchronously
    LaunchedEffect(pendingStatusChange) {
        pendingStatusChange?.let { newStatus ->
            if (work.status != newStatus) {
                val updatedWork = work.copy(status = newStatus)
                onSave(updatedWork)
            }
            pendingStatusChange = null
        }
    }
    
    // Handle pending series type changes asynchronously
    LaunchedEffect(pendingSeriesTypeChange) {
        pendingSeriesTypeChange?.let { newSeriesType ->
            if (work.seriesType != newSeriesType) {
                val updatedWork = work.copy(seriesType = newSeriesType)
                onSave(updatedWork)
            }
            pendingSeriesTypeChange = null
        }
    }
    
    // Handle pending manga type changes asynchronously
    LaunchedEffect(pendingMangaTypeChange) {
        pendingMangaTypeChange?.let { newMangaType ->
            if (work.mangaType != newMangaType) {
                val updatedWork = work.copy(mangaType = newMangaType)
                onSave(updatedWork)
            }
            pendingMangaTypeChange = null
        }
    }

    // Определяем доступные статусы в зависимости от типа произведения
    val availableStatuses = when (work.type) {
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
        else -> emptyList()
    }

    // Определяем доступные типы сериалов
    val availableSeriesTypes = listOf(
        null to "",
        SeriesType.TV_SERIES to strings.tvSeries,
        SeriesType.FILM to strings.film,
        SeriesType.CARTOON to strings.cartoon,
        SeriesType.DRAMA to strings.drama
    )

    // Определяем доступные типы манги
    val availableMangaTypes = listOf(
        null to "",
        MangaType.MANGA to strings.manga,
        MangaType.MANHWA to strings.manhwa,
        MangaType.MANHUA to strings.manhua
    )

    // Функция подготовки данных для редактирования
    fun prepareEditInfo(work: Work, strings: com.rn.library.ui.Strings): List<EditInfoItem> {
        val items = mutableListOf<EditInfoItem>()

        if (work.type == WorkType.SERIES) {
            items.add(
                EditInfoItem(
                    key = "seasons",
                    label = strings.seasonsView,
                    value = work.seasons?.toString().orEmpty(),
                    type = EditInfoType.INT,
                    originalValue = work.seasons
                )
            )
        }

        if (work.type == WorkType.ANIME || work.type == WorkType.SERIES) {
            items.add(
                EditInfoItem(
                    key = "episodes",
                    label = strings.seriesView,
                    value = work.episodes?.let { formatDoubleForDisplay(it) }.orEmpty(),
                    type = EditInfoType.INT,
                    originalValue = work.episodes
                )
            )
        }

        if (work.type == WorkType.BOOK) {
            items.add(
                EditInfoItem(
                    key = "chapters",
                    label = strings.volumesView,
                    value = work.chapters?.let { formatDoubleForDisplay(it) }.orEmpty(),
                    type = EditInfoType.INT,
                    originalValue = work.chapters
                )
            )
            items.add(
                EditInfoItem(
                    key = "bookChapters",
                    label = strings.chaptersView,
                    value = work.bookChapters?.let { formatDoubleForDisplay(it) }.orEmpty(),
                    type = EditInfoType.INT,
                    originalValue = work.bookChapters
                )
            )
        }

        if (work.type == WorkType.MANGA) {
            items.add(
                EditInfoItem(
                    key = "volumes",
                    label = strings.volumesView,
                    value = work.volumes?.let { formatDoubleForDisplay(it) }.orEmpty(),
                    type = EditInfoType.INT,
                    originalValue = work.volumes
                )
            )
            items.add(
                EditInfoItem(
                    key = "chapters",
                    label = strings.chaptersView,
                    value = work.chapters?.let { formatDoubleForDisplay(it) }.orEmpty(),
                    type = EditInfoType.INT,
                    originalValue = work.chapters
                )
            )
        }

        val progressLabel = when (work.type) {
            WorkType.BOOK, WorkType.MANGA -> strings.read
            WorkType.ANIME, WorkType.SERIES -> strings.watched
        }
        items.add(
            EditInfoItem(
                key = "progress",
                label = progressLabel,
                value = work.progress?.let { formatDoubleForDisplay(it) }.orEmpty(),
                type = EditInfoType.INT,
                originalValue = work.progress
            )
        )

        items.add(
            EditInfoItem(
                key = "year",
                label = strings.year,
                value = (work.yearPeriod ?: work.year?.toString()).orEmpty(),
                type = EditInfoType.STRING,
                originalValue = work.yearPeriod ?: work.year
            )
        )

        
        items.add(
            EditInfoItem(
                key = "dateRead",
                label = when (work.type) {
                    WorkType.BOOK, WorkType.MANGA -> strings.dateReadForBooks
                    WorkType.ANIME, WorkType.SERIES -> strings.dateWatched
                    else -> strings.dateRead
                },
                // Как в AddWorkScreen: храним только цифры DDMMYYYY, чтобы применить визуальную маску
                value = work.dateRead?.split("-")?.let { parts ->
                    if (parts.size == 3) parts[2] + parts[1] + parts[0] else work.dateRead.filter { c -> c.isDigit() }
                }.orEmpty(),
                type = EditInfoType.DATE,
                originalValue = work.dateRead
            )
        )
        items.add(
            EditInfoItem(
                key = "rereadDates",
                label = when (work.type) {
                    WorkType.BOOK, WorkType.MANGA -> strings.dateReread
                    WorkType.ANIME, WorkType.SERIES -> strings.dateRewatch
                },
                // Храним только цифры DDMMYYYY (показываем с точками через DateVisualTransformation)
                value = work.rereadDates.firstOrNull()?.split("-")?.let { parts ->
                    if (parts.size == 3) parts[2] + parts[1] + parts[0] else null
                }.orEmpty(),
                type = EditInfoType.DATE,
                originalValue = work.rereadDates
            )
        )

        items.add(
            EditInfoItem(
                key = "note",
                label = strings.noteLabel,
                value = work.note.orEmpty(),
                type = EditInfoType.STRING,
                originalValue = work.note
            )
        )

        return items
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Фон рисуется на уровне `LibraryScreen` (обложка + градиент + фон темы),
            // поэтому здесь оставляем прозрачным, чтобы не перекрывать его.
            .background(Color.Transparent)
    ) {
        // Main scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            // Немного уменьшаем вертикальные отступы между блоками,
            // чтобы сократить расстояние между AssistChip и информационным полем.
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Область обложки
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(330.dp)
            ) {
                CoverCarousel(
                    coverPaths = coverPaths,
                    currentPath = currentCoverPath,
                    onCurrentPathChange = { path ->
                        currentCoverPath = path
                        onSessionCoverPathChange(path)
                    },
                    modifier = Modifier
                        .width(215.dp)
                        .height(340.dp)
                        .align(Alignment.Center),
                    cardShape = RoundedCornerShape(20.dp),
                    cardContainerColor = searchBarColor,
                    onCoverClick = onCoverClick
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = work.title,
                    color = titleColorBetween,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                // Other titles: show first one, tap to open bottom popup with all
                otherTitles.firstOrNull()?.let { firstOtherTitle ->
                    Text(
                        text = firstOtherTitle,
                        color = iconTextColor.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { otherTitlesExpanded = true }
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Статус с цветом (только текст и рамка) - теперь кликабельный
                Box {
                    AssistChip(
                        onClick = { statusMenuExpanded = true },
                        enabled = true,
                        label = { Text(statusLabel) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.Transparent,
                            labelColor = statusColor,
                            disabledContainerColor = Color.Transparent,
                            disabledLabelColor = statusColor
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            borderColor = statusColor,
                            borderWidth = 2.dp,
                            enabled = true
                        )
                    )

                    DropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false },
                        modifier = Modifier.background(mainBackgroundColor)
                    ) {
                        availableStatuses.forEach { (status, label) ->
                            val isSelected = work.status == status
                            val itemStatusColor = when (status) {
                                WorkStatus.IN_PLANS -> Color(0xFF8E6687)
                                WorkStatus.ABANDONED -> Color(0xFFFF5F5A)
                                WorkStatus.READING -> Color(0xFF7179A4)
                                WorkStatus.WATCHING -> Color(0xFF7179A4)
                                WorkStatus.READ -> Color(0xFF79C77C)
                                WorkStatus.WATCHED -> Color(0xFF79C77C)
                            }

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        color = if (isSelected) itemStatusColor else titleColorBetween,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    statusMenuExpanded = false
                                    if (!isSelected) {
                                        pendingStatusChange = status
                                    }
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = if (isSelected) itemStatusColor else titleColorBetween
                                )
                            )
                        }
                    }
                }
                // SeriesType для сериалов - теперь кликабельный
                if (work.type == WorkType.SERIES) {
                    Box {
                        AssistChip(
                            onClick = { seriesTypeMenuExpanded = true },
                            enabled = true,
                            label = {
                                Text(
                                    text = seriesTypeLabel ?: strings.tvSeriesType,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color.Transparent,
                                labelColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = Color.Transparent,
                                disabledLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            border = AssistChipDefaults.assistChipBorder(
                                borderColor = MaterialTheme.colorScheme.primary,
                                borderWidth = 2.dp,
                                enabled = true
                            )
                        )

                        DropdownMenu(
                            expanded = seriesTypeMenuExpanded,
                            onDismissRequest = { seriesTypeMenuExpanded = false },
                            modifier = Modifier.background(mainBackgroundColor)
                        ) {
                            availableSeriesTypes.forEach { (type, label) ->
                                if (label.isEmpty()) return@forEach // Пропускаем пустой вариант
                                val isSelected = work.seriesType == type

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = label,
                                            color = if (isSelected) iconTextColor else titleColorBetween,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        seriesTypeMenuExpanded = false
                                        if (!isSelected) {
                                            pendingSeriesTypeChange = type
                                        }
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = if (isSelected) iconTextColor else titleColorBetween
                                    )
                                )
                            }
                        }
                    }
                }
                // MangaType для манги - теперь кликабельный
                if (work.type == WorkType.MANGA) {
                    Box {
                        AssistChip(
                            onClick = { mangaTypeMenuExpanded = true },
                            enabled = true,
                            label = {
                                Text(
                                    text = mangaTypeLabel ?: strings.mangaType,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color.Transparent,
                                labelColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = Color.Transparent,
                                disabledLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            border = AssistChipDefaults.assistChipBorder(
                                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                borderWidth = 2.dp,
                                enabled = true
                            )
                        )

                        DropdownMenu(
                            expanded = mangaTypeMenuExpanded,
                            onDismissRequest = { mangaTypeMenuExpanded = false },
                            modifier = Modifier.background(mainBackgroundColor)
                        ) {
                            availableMangaTypes.forEach { (type, label) ->
                                if (label.isEmpty()) return@forEach // Пропускаем пустой вариант
                                val isSelected = work.mangaType == type

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = label,
                                            color = if (isSelected) iconTextColor else titleColorBetween,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        mangaTypeMenuExpanded = false
                                        if (!isSelected) {
                                            pendingMangaTypeChange = type
                                        }
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = if (isSelected) iconTextColor else titleColorBetween
                                    )
                                )
                            }
                        }
                    }
                }
            }

            val cleanedPrimaryLink = run {
                val raw = work.link?.takeIf { it.isNotBlank() }?.trim()
                    ?: work.link2?.takeIf { it.isNotBlank() }?.trim()
                raw?.replace("\\:", ":")?.replace("\\/", "/")
            }
            val hasPrimaryLink = !cleanedPrimaryLink.isNullOrBlank()

            fun openPrimaryLink() {
                val uri = cleanedPrimaryLink ?: return
                LinkOpenHelper.openInExternalViewer(context, uri)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasPrimaryLink) {
                    val actionLabel = when (work.type) {
                        WorkType.BOOK, WorkType.MANGA -> strings.linkActionRead
                        WorkType.ANIME, WorkType.SERIES -> strings.linkActionWatch
                    }
                    Button(
                        onClick = { openPrimaryLink() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(actionLabel)
                    }
                }
                OutlinedButton(
                    onClick = {
                        editInfoParts = prepareEditInfo(work, strings)
                        temporaryWork = work.copy()
                        showEditInfoDialog = true
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(strings.editInfoButton)
                }
            }

            // Информационное поле: прочитано/просмотрено → серии/главы → тома/сезоны → сезон аниме → год → страна → дата
            val infoParts = mutableListOf<String>()
            fun formatDouble(value: Double): String {
                return if (value % 1.0 == 0.0) {
                    value.toInt().toString()
                } else {
                    value.toString().trimEnd('0').trimEnd('.')
                }
            }

            work.progress?.let { p ->
                val formattedProgress = formatDouble(p)
                when (work.type) {
                    WorkType.ANIME, WorkType.SERIES ->
                        infoParts.add("${strings.progressWatchedEpisodes}: $formattedProgress")
                    WorkType.BOOK, WorkType.MANGA ->
                        infoParts.add("${strings.progressReadChapters}: $formattedProgress")
                }
            }
            when (work.type) {
                WorkType.SERIES -> {
                    work.episodes?.let {
                        val formatted = formatDouble(it)
                        infoParts.add("${strings.seriesView}: $formatted")
                    }
                    work.seasons?.let { infoParts.add("${strings.seasonsView}: $it") }
                }
                WorkType.ANIME -> {
                    work.episodes?.let {
                        val formatted = formatDouble(it)
                        infoParts.add("${strings.seriesView}: $formatted")
                    }
                }
                WorkType.BOOK -> {
                    work.bookChapters?.let {
                        infoParts.add("${strings.chaptersView}: ${formatDouble(it)}")
                    }
                    work.chapters?.let {
                        val formatted = formatDouble(it)
                        infoParts.add("${strings.volumesView}: $formatted")
                    }
                }
                WorkType.MANGA -> {
                    work.chapters?.let {
                        val formatted = formatDouble(it)
                        infoParts.add("${strings.chaptersView}: $formatted")
                    }
                    work.volumes?.let {
                        infoParts.add("${strings.volumesView}: ${formatDouble(it)}")
                    }
                }
            }
            if (work.type == WorkType.ANIME) {
                work.animeSeason?.let { season ->
                    val seasonLabel = when (season) {
                        AnimeSeason.SPRING -> strings.spring
                        AnimeSeason.SUMMER -> strings.summer
                        AnimeSeason.FALL -> strings.fall
                        AnimeSeason.WINTER -> strings.winter
                    }
                    infoParts.add("${strings.animeSeason}: $seasonLabel")
                }
            }
            (work.yearPeriod ?: work.year?.toString())?.let { infoParts.add("${strings.year}: $it") }
            work.country?.let { infoParts.add("${strings.country}: $it") }
            work.dateRead?.let { dateStr ->
                val dateLabel = when (work.type) {
                    WorkType.BOOK, WorkType.MANGA -> strings.dateReadForBooks
                    WorkType.ANIME, WorkType.SERIES -> strings.dateWatched
                    else -> strings.dateRead
                }
                // Конвертируем дату из YYYY-MM-DD в DD.MM.YYYY для отображения
                val formattedDate = try {
                    val parts = dateStr.split("-")
                    if (parts.size == 3) {
                        "${parts[2]}.${parts[1]}.${parts[0]}"
                    } else {
                        dateStr
                    }
                } catch (e: Exception) {
                    dateStr
                }
                infoParts.add("$dateLabel: $formattedDate")
            }
            if (work.rereadDates.isNotEmpty()) {
                val rereads = work.rereadDates.joinToString(", ") { date ->
                    val parts = date.split("-")
                    if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else date
                }
                val rereadLabel = when (work.type) {
                    WorkType.BOOK, WorkType.MANGA -> strings.dateReread
                    WorkType.ANIME, WorkType.SERIES -> strings.dateRewatch
                }
                infoParts.add("$rereadLabel: $rereads")
            }
            if (work.readingPeriods.isNotEmpty()) {
                val periods = work.readingPeriods.joinToString("; ") { period ->
                    val end = period.endDate ?: "..."
                    "${period.startDate} - $end"
                }
                infoParts.add("Периоды: $periods")
            }
            if (infoParts.isNotEmpty()) {
                Surface(
                    color = mainBackgroundColor,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        infoParts.forEach { info ->
                            Text(
                                text = info,
                                color = if (currentTheme == AppTheme.DARK) Color.White else Color.Black,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // Description block – фон и текст зависят от темы (без заголовка "Описание")
            Surface(
                modifier = Modifier.padding(top = if (infoParts.isNotEmpty()) 1.dp else 0.dp),
                color = mainBackgroundColor,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = if (work.description.isBlank()) strings.descriptionEmpty else work.description,
                        color = titleColorBetween,
                        fontSize = 17.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Justify,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Progress bar for reading / watching (hidden for Abandoned or when no totals)
            val totalUnits = when (work.type) {
                WorkType.BOOK -> work.bookChapters?.toDouble()
                WorkType.MANGA -> work.chapters
                WorkType.ANIME, WorkType.SERIES -> work.episodes
            }
            val currentProgress = when (work.type) {
                WorkType.BOOK, WorkType.MANGA -> readProgressUnits(work)
                WorkType.ANIME, WorkType.SERIES -> watchedProgressUnits(work)
            }.takeIf { it > 0.0 }

            if (totalUnits != null && totalUnits > 0 && currentProgress != null) {
                val clampedProgress = currentProgress.coerceIn(0.0, totalUnits)
                val progressFraction = clampedProgress.toFloat() / totalUnits.toFloat()
                val progressPercent = ((progressFraction * 100f).toInt()).coerceIn(0, 100)

                Spacer(modifier = Modifier.height(8.dp))

                // Custom progress bar to avoid visual split into two segments
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(iconTextColor.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction)
                            .background(statusColor)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val progressPrefix = when (work.type) {
                    WorkType.BOOK, WorkType.MANGA -> strings.read
                    WorkType.ANIME, WorkType.SERIES -> strings.watched
                }
                // Format values for display (remove trailing zeros)
                fun formatDouble(value: Double): String {
                    return if (value % 1.0 == 0.0) {
                        value.toInt().toString()
                    } else {
                        value.toString().trimEnd('0').trimEnd('.')
                    }
                }
                val formattedProgress = formatDouble(clampedProgress)
                val formattedTotal = formatDouble(totalUnits)
                val countText = "$progressPrefix $formattedProgress / $formattedTotal"

                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "$progressPercent%",
                        color = iconTextColor.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Text(
                        text = countText,
                        color = iconTextColor.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }

            @Composable
            fun LinkBlock(title: String, rawLink: String) {
                // Очищаем ссылку от возможных обратных слэшей, которые могли появиться при сохранении
                val cleanLink = rawLink.replace("\\:", ":").replace("\\/", "/")
                if (cleanLink.isBlank()) return

                Surface(
                    color = mainBackgroundColor,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) {
                            LinkOpenHelper.openInExternalViewer(context, cleanLink)
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = title,
                            color = if (currentTheme == AppTheme.DARK) Color.White else Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = cleanLink,
                            color = Color(0xFF2196F3), // Blue color
                            fontSize = 14.sp,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }
            }

            // Link fields (up to two separate links)
            work.link?.takeIf { it.isNotBlank() }?.let { link ->
                LinkBlock(strings.link1, link)
            }
            work.link2?.takeIf { it.isNotBlank() }?.let { link ->
                LinkBlock(strings.link2, link)
            }

            // Note (at the very bottom)
            work.note?.takeIf { it.isNotBlank() }?.let { note ->
                Surface(
                    color = mainBackgroundColor,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = strings.noteLabel,
                            color = if (currentTheme == AppTheme.DARK) Color.White else Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = note,
                            color = iconTextColor.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Bottom padding so the last row (progress text / etc.) is never hidden by bottom nav
            Spacer(modifier = Modifier.height(8.dp))

        }

        // Bottom popup with names (like sheet), animated over content
        androidx.compose.animation.AnimatedVisibility(
            visible = otherTitlesExpanded && otherTitles.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(110)) + slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(110)
            ),
            exit = fadeOut(animationSpec = tween(90)) + slideOutVertically(
                targetOffsetY = { it / 3 },
                animationSpec = tween(90)
            ),
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.BottomCenter)
        ) {
            // Контейнер для фонового клика и самого Popup
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Фоновый слой: закрывает Popup только при клике за его пределами
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) {
                            otherTitlesExpanded = false
                        }
                )

                // Сам Popup: клики внутри него НЕ закрывают окно
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 0.dp, start = 16.dp, end = 16.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) {
                            // Поглощаем клик внутри Popup, чтобы не срабатывало закрытие
                        },
                    color = mainBackgroundColor,
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Название
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = strings.title,
                                color = titleColorBetween,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(work.title))
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = strings.copyTitleContentDesc,
                                    tint = iconTextColor
                                )
                            }
                        }
                        Text(
                            text = work.title,
                            color = iconTextColor,
                            fontSize = 14.sp
                        )

                        // Разделитель
                        HorizontalDivider(color = iconTextColor.copy(alpha = 0.2f))

                        // Альтернативные названия
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = strings.alternativeTitles,
                                color = titleColorBetween,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(otherTitles.joinToString("\n")))
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = strings.copyAlternativeTitlesContentDesc,
                                    tint = iconTextColor
                                )
                            }
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            otherTitles.forEach { t ->
                                Text(
                                    text = t,
                                    color = iconTextColor,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Диалог редактирования информации
        if (showEditInfoDialog) {
            EditInfoDialog(
                work = temporaryWork,
                editInfoParts = editInfoParts,
                onDismiss = { showEditInfoDialog = false },
                onSave = { updatedWork ->
                    onSave(updatedWork)
                    showEditInfoDialog = false
                },
                currentTheme = currentTheme
            )
        }

    }
}

/**
 * Преобразует дату из формата YYYY-MM-DD в ДД.ММ.ГГГГ
 * Если дата уже в формате ДД.ММ.ГГГГ, возвращает её без изменений
 */
private fun convertDateFormat(date: String): String {
    return try {
        // Проверяем, является ли дата в формате YYYY-MM-DD
        if (date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            val parts = date.split("-")
            if (parts.size == 3) {
                "${parts[2]}.${parts[1]}.${parts[0]}"
            } else {
                date
            }
        } else {
            // Если уже в формате ДД.ММ.ГГГГ или другом, возвращаем как есть
            date
        }
    } catch (e: Exception) {
        date
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    iconTextColor: Color,
    titleColorBetween: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = iconTextColor.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = titleColorBetween,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditInfoDialog(
    work: Work,
    editInfoParts: List<EditInfoItem>,
    onDismiss: () -> Unit,
    onSave: (Work) -> Unit,
    currentTheme: AppTheme
) {
    val mainBackgroundColor = MainBackgroundColor()
    val titleColorBetween = TitleColorBetween()
    val iconTextColor = IconTextColor()
    val searchBarColor = SearchBarColor()
    val strings = LocalStrings.current

    // Цвет для текста полей и лейблов: белый в тёмной теме, чёрный в светлой
    val fieldTextColor = if (currentTheme == AppTheme.DARK) Color.White else Color.Black

    var tempWork by remember { mutableStateOf(work) }
    var tempValues by remember { mutableStateOf(editInfoParts.toMutableList()) }
    var unitProgressEdits by remember(work.unitProgress) {
        mutableStateOf(work.unitProgress.toMutableList())
    }
    var selectedUnitIndex by remember(work.unitProgress, work.activeUnitIndex) {
        val savedIndex = work.activeUnitIndex
        val fallback = work.unitProgress.indexOfLast { it.completed > 0.0 }.takeIf { it >= 0 } ?: 0
        val initial = savedIndex?.coerceIn(work.unitProgress.indices) ?: fallback
        mutableIntStateOf(initial)
    }
    var syncApplyUnitNumber by remember {
        mutableStateOf<() -> UnitProgressEditState>({
            UnitProgressEditState(unitProgressEdits, selectedUnitIndex)
        })
    }
    val context = LocalContext.current
    // Шаг инкремента из настроек (минимум 1)
    val incrementStep = remember { AppSettings.getIncrementStep(context).coerceAtLeast(1) }
    val unitPrefixLabel = when (tempWork.type) {
        WorkType.BOOK, WorkType.MANGA -> strings.volumeUnitPrefix
        WorkType.SERIES, WorkType.ANIME -> strings.seasonUnitPrefix
        else -> ""
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = mainBackgroundColor
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Поля для редактирования
                tempValues.forEachIndexed { index, item ->
                    if (item.key == "progress" && unitProgressEdits.isNotEmpty()) {
                        EditUnitProgressFieldRow(
                            unitPrefixLabel = unitPrefixLabel,
                            progressLabel = item.label,
                            unitProgressList = unitProgressEdits,
                            selectedUnitIndex = selectedUnitIndex.coerceIn(unitProgressEdits.indices),
                            onSelectedUnitIndexChange = { selectedUnitIndex = it },
                            onUnitProgressListChange = { unitProgressEdits = it.toMutableList() },
                            maxUnitNumber = editDialogMaxUnitNumber(tempWork, tempValues),
                            incrementStep = incrementStep,
                            onRegisterSyncApplyUnitNumber = { syncApplyUnitNumber = it },
                            searchBarColor = searchBarColor,
                            fieldTextColor = fieldTextColor,
                            iconTextColor = iconTextColor,
                        )
                        return@forEachIndexed
                    }

                    var textValue by remember(item.value) {
                        mutableStateOf(item.value ?: "")
                    }

                    if (item.type == EditInfoType.DATE) {
                        // Как в AddWorkScreen: только цифры (DDMMYYYY) + визуальная маска ДД.ММ.ГГГГ
                        OutlinedTextField(
                            value = textValue,
                            onValueChange = { newValue ->
                                val digitsOnly = newValue.filter { it.isDigit() }.take(8)
                                textValue = digitsOnly
                                tempValues[index] = item.copy(value = digitsOnly)
                            },
                            label = { Text(item.label) },
                            placeholder = { Text(strings.datePlaceholder) },
                            visualTransformation = DateVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = searchBarColor,
                                unfocusedContainerColor = searchBarColor,
                                focusedIndicatorColor = iconTextColor.copy(alpha = 0.6f),
                                unfocusedIndicatorColor = iconTextColor.copy(alpha = 0.3f),
                                focusedTextColor = fieldTextColor,
                                unfocusedTextColor = fieldTextColor,
                                focusedLabelColor = fieldTextColor,
                                unfocusedLabelColor = fieldTextColor
                            )
                        )
                    } else {
                        val isIncrementField = item.type == EditInfoType.INT &&
                                item.key in setOf(
                            "volumes",
                            "chapters",
                            "bookChapters",
                            "episodes",
                            "seasons",
                            "progress"
                        )

                        if (isIncrementField) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = textValue,
                                    onValueChange = { newValue ->
                                        // Allow digits and one decimal point
                                        val filtered = newValue.filter { it.isDigit() || it == '.' }
                                        // Ensure only one decimal point
                                        val parts = filtered.split('.')
                                        val validValue = if (parts.size > 2) {
                                            parts[0] + "." + parts.drop(1).joinToString("")
                                        } else {
                                            filtered
                                        }
                                        textValue = validValue
                                        tempValues[index] = item.copy(value = validValue)
                                    },
                                    label = { Text(item.label) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = searchBarColor,
                                        unfocusedContainerColor = searchBarColor,
                                        focusedIndicatorColor = iconTextColor.copy(alpha = 0.6f),
                                        unfocusedIndicatorColor = iconTextColor.copy(alpha = 0.3f),
                                        focusedTextColor = fieldTextColor,
                                        unfocusedTextColor = fieldTextColor,
                                        focusedLabelColor = fieldTextColor,
                                        unfocusedLabelColor = fieldTextColor
                                    )
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = {
                                        val current = textValue.toDoubleOrNull() ?: 0.0
                                        val newValue = (current + incrementStep).coerceAtLeast(0.0)
                                        // Format: remove trailing zeros if whole number
                                        val asString = if (newValue % 1.0 == 0.0) {
                                            newValue.toInt().toString()
                                        } else {
                                            newValue.toString().trimEnd('0').trimEnd('.')
                                        }
                                        textValue = asString
                                        tempValues[index] = item.copy(value = asString)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text(
                                        text = "▲",
                                        color = fieldTextColor,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = textValue,
                                onValueChange = { newValue ->
                                    // Валидация в зависимости от типа
                                    val filteredValue = when {
                                        item.key == "year" -> {
                                            val normalized = newValue.trimStart()
                                            val singleYearPattern = Regex("^\\d{0,4}$")
                                            val yearPeriodPattern = Regex("^\\d{4}\\s?-\\s?\\d{0,4}$")
                                            if (
                                                normalized.isEmpty() ||
                                                singleYearPattern.matches(normalized) ||
                                                yearPeriodPattern.matches(normalized)
                                            ) normalized else textValue
                                        }
                                        item.type == EditInfoType.INT -> newValue.filter { it.isDigit() }
                                        else -> newValue
                                    }
                                    textValue = filteredValue
                                    tempValues[index] = item.copy(value = filteredValue)
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = item.type != EditInfoType.STRING || item.key == "year",
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = searchBarColor,
                                    unfocusedContainerColor = searchBarColor,
                                    focusedIndicatorColor = iconTextColor.copy(alpha = 0.6f),
                                    unfocusedIndicatorColor = iconTextColor.copy(alpha = 0.3f),
                                    focusedTextColor = fieldTextColor,
                                    unfocusedTextColor = fieldTextColor,
                                    focusedLabelColor = fieldTextColor,
                                    unfocusedLabelColor = fieldTextColor
                                )
                            )
                        }
                    }
                }

                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text(strings.cancel, color = iconTextColor)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val unitEditState = if (unitProgressEdits.isNotEmpty()) {
                                syncApplyUnitNumber().also {
                                    unitProgressEdits = it.units.toMutableList()
                                    selectedUnitIndex = it.activeIndex
                                }
                            } else {
                                UnitProgressEditState(unitProgressEdits, selectedUnitIndex)
                            }
                            val unitProgressToSave = unitEditState.units
                            val activeUnitIndexToSave = unitEditState.activeIndex

                            // Обновляем временный объект Work
                            var updatedWork = tempWork

                            tempValues.forEach { item ->
                                updatedWork = when (item.key) {
                                    "seasons" -> updatedWork.copy(seasons = item.value?.toIntOrNull())
                                    "episodes" -> updatedWork.copy(episodes = item.value?.toDoubleOrNull())
                                    "chapters" -> {
                                        if (tempWork.type == WorkType.BOOK) {
                                            updatedWork.copy(chapters = item.value?.toDoubleOrNull())
                                        } else {
                                            updatedWork.copy(chapters = item.value?.toDoubleOrNull())
                                        }
                                    }
                                    "volumes" -> updatedWork.copy(volumes = item.value?.toDoubleOrNull())
                                    "bookChapters" -> updatedWork.copy(bookChapters = item.value?.toDoubleOrNull())
                                    "year" -> {
                                        val raw = item.value?.trim().orEmpty()
                                        val singleYearPattern = Regex("^\\d{1,4}$")
                                        val yearPeriodPattern = Regex("^\\d{4}\\s*-\\s*\\d{1,4}$")
                                        when {
                                            raw.isEmpty() -> updatedWork.copy(year = null, yearPeriod = null)
                                            singleYearPattern.matches(raw) ->
                                                updatedWork.copy(year = raw.toIntOrNull(), yearPeriod = null)
                                            yearPeriodPattern.matches(raw) ->
                                                updatedWork.copy(
                                                    year = null,
                                                    yearPeriod = raw.replace(Regex("\\s*-\\s*"), " - ")
                                                )
                                            else -> updatedWork
                                        }
                                    }
                                    "progress" -> {
                                        if (unitProgressToSave.isNotEmpty()) {
                                            updatedWork.copy(
                                                unitProgress = unitProgressToSave,
                                                progress = unitProgressToSave.sumOf { it.completed },
                                                activeUnitIndex = activeUnitIndexToSave,
                                            )
                                        } else {
                                            updatedWork.copy(
                                                progress = item.value?.toDoubleOrNull(),
                                                activeUnitIndex = null,
                                            )
                                        }
                                    }
                                    "country" -> updatedWork.copy(country = item.value?.takeIf { it.isNotBlank() })
                                    "dateRead" -> {
                                        val digits = item.value
                                            ?.filter { it.isDigit() }
                                            .orEmpty()
                                        val newDate = if (digits.length == 8) {
                                            // DDMMYYYY -> YYYY-MM-DD
                                            val day = digits.substring(0, 2)
                                            val month = digits.substring(2, 4)
                                            val year = digits.substring(4, 8)
                                            "$year-$month-$day"
                                        } else {
                                            null
                                        }
                                        updatedWork.copy(dateRead = newDate)
                                    }
                                    "rereadDates" -> {
                                        val digits = item.value
                                            ?.filter { it.isDigit() }
                                            .orEmpty()
                                        val date = if (digits.length == 8) {
                                            val day = digits.substring(0, 2)
                                            val month = digits.substring(2, 4)
                                            val year = digits.substring(4, 8)
                                            "$year-$month-$day"
                                        } else null
                                        updatedWork.copy(rereadDates = date?.let { listOf(it) } ?: emptyList())
                                    }
                                    "note" -> updatedWork.copy(note = item.value?.takeIf { it.isNotBlank() })
                                    else -> updatedWork
                                }
                            }

                            onSave(updatedWork)
                        }
                    ) {
                        Text(strings.save)
                    }
                }
            }
        }
    }
}

private data class UnitProgressEditState(
    val units: List<UnitProgress>,
    val activeIndex: Int,
)

/** Максимальный номер тома/сезона из поля «Тома» / «Сезоны» в диалоге редактирования. */
private fun editDialogMaxUnitNumber(work: Work, tempValues: List<EditInfoItem>): Int? {
    val raw = when (work.type) {
        WorkType.MANGA -> tempValues.find { it.key == "volumes" }?.value?.toDoubleOrNull()
        WorkType.BOOK -> tempValues.find { it.key == "chapters" }?.value?.toDoubleOrNull()
        WorkType.SERIES -> tempValues.find { it.key == "seasons" }?.value?.toIntOrNull()?.toDouble()
        WorkType.ANIME -> work.seasons?.toDouble()
        else -> null
    }
    return raw?.toInt()?.takeIf { it > 0 }
}

@Composable
private fun EditUnitProgressFieldRow(
    unitPrefixLabel: String,
    progressLabel: String,
    unitProgressList: List<UnitProgress>,
    selectedUnitIndex: Int,
    onSelectedUnitIndexChange: (Int) -> Unit,
    onUnitProgressListChange: (List<UnitProgress>) -> Unit,
    maxUnitNumber: Int?,
    incrementStep: Int,
    onRegisterSyncApplyUnitNumber: ((() -> UnitProgressEditState) -> Unit)? = null,
    searchBarColor: Color,
    fieldTextColor: Color,
    iconTextColor: Color,
) {
    if (unitProgressList.isEmpty()) return

    val safeIndex = selectedUnitIndex.coerceIn(unitProgressList.indices)
    val currentUnit = unitProgressList[safeIndex]

    var unitNumberText by remember { mutableStateOf((safeIndex + 1).toString()) }
    var unitNumberFieldFocused by remember { mutableStateOf(false) }
    var completedText by remember(safeIndex) {
        mutableStateOf(formatEditableUnitNumber(currentUnit.completed, emptyAsZero = true))
    }

    LaunchedEffect(safeIndex, currentUnit.completed) {
        val parsed = completedText.trim().toDoubleOrNull() ?: 0.0
        if (parsed != currentUnit.completed) {
            completedText = formatEditableUnitNumber(currentUnit.completed, emptyAsZero = true)
        }
    }

    LaunchedEffect(safeIndex) {
        if (!unitNumberFieldFocused) {
            unitNumberText = (safeIndex + 1).toString()
        }
    }

    val fieldColors = TextFieldDefaults.colors(
        focusedContainerColor = searchBarColor,
        unfocusedContainerColor = searchBarColor,
        focusedIndicatorColor = iconTextColor.copy(alpha = 0.6f),
        unfocusedIndicatorColor = iconTextColor.copy(alpha = 0.3f),
        focusedTextColor = fieldTextColor,
        unfocusedTextColor = fieldTextColor,
        focusedLabelColor = fieldTextColor,
        unfocusedLabelColor = fieldTextColor,
    )

    fun formatProgressValue(value: Double): String =
        if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            value.toString().trimEnd('0').trimEnd('.')
        }

    fun coerceUnitNumber(raw: Int): Int {
        var n = raw.coerceAtLeast(1)
        if (maxUnitNumber != null) {
            n = n.coerceAtMost(maxUnitNumber)
        }
        return n
    }

    fun ensureUnitsUpTo(targetIndex: Int): List<UnitProgress> {
        val updated = unitProgressList.toMutableList()
        while (updated.size <= targetIndex) {
            updated += UnitProgress(
                unitName = "$unitPrefixLabel ${updated.size + 1}",
                completed = 0.0,
                total = null,
            )
        }
        return updated
    }

    fun selectUnitNumber(unitNumber: Int) {
        val coerced = coerceUnitNumber(unitNumber)
        val targetIndex = coerced - 1
        val updated = ensureUnitsUpTo(targetIndex)
        onUnitProgressListChange(updated)
        onSelectedUnitIndexChange(targetIndex)
        unitNumberText = coerced.toString()
    }

    fun applyUnitNumberFromText(): UnitProgressEditState {
        val digits = unitNumberText.filter { it.isDigit() }
        if (digits.isEmpty()) {
            unitNumberText = (safeIndex + 1).toString()
            return UnitProgressEditState(unitProgressList, safeIndex)
        }
        val coerced = coerceUnitNumber(digits.toIntOrNull() ?: (safeIndex + 1))
        val targetIndex = coerced - 1
        val updated = ensureUnitsUpTo(targetIndex)
        onUnitProgressListChange(updated)
        onSelectedUnitIndexChange(targetIndex)
        unitNumberText = coerced.toString()
        unitNumberFieldFocused = false
        return UnitProgressEditState(updated, targetIndex)
    }

    fun currentUnitProgressState(): UnitProgressEditState =
        UnitProgressEditState(unitProgressList, safeIndex)

    SideEffect {
        onRegisterSyncApplyUnitNumber?.invoke {
            if (unitNumberText.filter { it.isDigit() }.isNotEmpty()) {
                applyUnitNumberFromText()
            } else {
                currentUnitProgressState()
            }
        }
    }

    fun updateCompletedForIndex(index: Int, completed: Double) {
        val updated = unitProgressList.toMutableList()
        if (index !in updated.indices) return
        val unit = updated[index]
        val safeCompleted = completed.coerceAtLeast(0.0)
        val newTotal = unit.total?.takeIf { it > 0.0 }
            ?: safeCompleted.takeIf { it > 0.0 }
        updated[index] = unit.copy(completed = safeCompleted, total = newTotal)
        onUnitProgressListChange(updated)
    }

    LaunchedEffect(maxUnitNumber, safeIndex) {
        if (maxUnitNumber != null && safeIndex + 1 > maxUnitNumber) {
            selectUnitNumber(maxUnitNumber)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Номер тома / сезона (без подписи) + стрелка
        Row(
            modifier = Modifier.weight(0.38f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = unitNumberText,
                onValueChange = { newValue ->
                    unitNumberText = newValue.filter { it.isDigit() }
                    val digits = unitNumberText
                    if (digits.isNotEmpty()) {
                        val coerced = coerceUnitNumber(digits.toIntOrNull() ?: 1)
                        val targetIndex = coerced - 1
                        if (targetIndex != safeIndex) {
                            val updated = ensureUnitsUpTo(targetIndex)
                            onUnitProgressListChange(updated)
                            onSelectedUnitIndexChange(targetIndex)
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focus ->
                        if (focus.isFocused) {
                            unitNumberFieldFocused = true
                        } else if (unitNumberFieldFocused) {
                            applyUnitNumberFromText()
                        }
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { applyUnitNumberFromText() },
                ),
                colors = fieldColors,
            )
            IconButton(
                onClick = {
                    val current = if (unitNumberFieldFocused) {
                        applyUnitNumberFromText()
                        unitNumberText.toIntOrNull() ?: (safeIndex + 1)
                    } else {
                        unitNumberText.toIntOrNull() ?: (safeIndex + 1)
                    }
                    if (maxUnitNumber != null && current >= maxUnitNumber) return@IconButton
                    selectUnitNumber(current + 1)
                },
                modifier = Modifier.size(36.dp),
            ) {
                Text(
                    text = "▲",
                    color = fieldTextColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Прочитано / просмотрено + стрелка (значение для выбранного тома/сезона)
        Row(
            modifier = Modifier.weight(0.62f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = completedText,
                onValueChange = { newValue ->
                    val validValue = filterUnitDecimalInput(newValue)
                    completedText = validValue
                    updateCompletedForIndex(
                        safeIndex,
                        parseUnitDecimalInput(validValue, emptyAsZero = true) ?: 0.0,
                    )
                },
                label = { Text(progressLabel, fontSize = 13.5.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = fieldColors,
            )
            IconButton(
                onClick = {
                    val current = completedText.toDoubleOrNull() ?: 0.0
                    val newValue = (current + incrementStep).coerceAtLeast(0.0)
                    val asString = formatProgressValue(newValue)
                    completedText = asString
                    updateCompletedForIndex(safeIndex, newValue)
                },
                modifier = Modifier.size(36.dp),
            ) {
                Text(
                    text = "▲",
                    color = fieldTextColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
