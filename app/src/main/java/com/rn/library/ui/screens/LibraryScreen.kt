package com.rn.library.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.rn.library.data.*
import com.rn.library.data.toCoverImageData
import com.rn.library.ui.*
import com.rn.library.ui.components.ActivityStatsConfirmDialog
import com.rn.library.ui.components.BottomNavigationBar
import com.rn.library.ui.components.NavigationItem
import com.rn.library.ui.components.SearchBar
import com.rn.library.ui.components.SunIcon
import com.rn.library.ui.components.WorkItem
import com.rn.library.ui.components.WorkItemCard
import com.rn.library.ui.components.WorkItemGridCard
import com.rn.library.ui.theme.IconTextColor
import com.rn.library.ui.theme.MainBackgroundColor
import com.rn.library.ui.theme.ThemePalette
import com.rn.library.ui.theme.TitleColorBetween
import kotlinx.coroutines.launch
import java.io.File

// Порядок сортировки списка произведений
enum class SortOrder { TITLE_ASC, TITLE_DESC, DATE_MODIFIED_DESC, DATE_MODIFIED_ASC }

private data class PendingWorkSave(
    val work: Work,
    val previous: Work?,
    val onAfterSave: (Work) -> Unit
)

// Extension function to convert Work to WorkItem (with localized labels and status)
fun Work.toWorkItem(
    strings: com.rn.library.ui.Strings,
    imageUrlOverride: String? = null
): WorkItem {
    fun plural(count: Int, one: String, few: String, many: String): String {
        val mod10 = count % 10
        val mod100 = count % 100
        val form = if (mod10 == 1 && mod100 != 11) one
        else if (mod10 in 2..4 && mod100 !in 12..14) few
        else many
        return "$count $form"
    }

    fun formatDouble(value: Double): String {
        // Remove trailing zeros and decimal point if not needed
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            value.toString().trimEnd('0').trimEnd('.')
        }
    }

    fun formatDoubleWithPlural(value: Double, one: String, few: String, many: String): String {
        val formatted = formatDouble(value)
        // For pluralization, use the integer part
        val intPart = value.toInt()
        val mod10 = intPart % 10
        val mod100 = intPart % 100
        val form = if (mod10 == 1 && mod100 != 11) one
        else if (mod10 in 2..4 && mod100 !in 12..14) few
        else many
        return "$formatted $form"
    }

    val metaParts = mutableListOf<String>()
    val metaLine1: String?
    val metaLine2: String?
    when (type) {
        WorkType.BOOK -> {
            val volumesText = chapters?.let { formatDoubleWithPlural(it, "том", "тома", "томов") }
            val chaptersText = bookChapters?.let { formatDoubleWithPlural(it, "глава", "главы", "глав") }
            metaLine1 = volumesText
            metaLine2 = chaptersText
            volumesText?.let { metaParts.add(it) }
            chaptersText?.let { metaParts.add(it) }
        }
        WorkType.MANGA -> {
            val volumesText = volumes?.let { formatDoubleWithPlural(it, "том", "тома", "томов") }
            val chaptersText = chapters?.let { formatDoubleWithPlural(it, "глава", "главы", "глав") }
            metaLine1 = volumesText
            metaLine2 = chaptersText
            volumesText?.let { metaParts.add(it) }
            chaptersText?.let { metaParts.add(it) }
        }
        WorkType.ANIME -> {
            val episodesText = episodes?.let { formatDoubleWithPlural(it, "серия", "серии", "серий") }
            metaLine1 = episodesText
            metaLine2 = null
            episodesText?.let { metaParts.add(it) }
        }
        WorkType.SERIES -> {
            val seasonsText = seasons?.let { plural(it, "сезон", "сезона", "сезонов") }
            val episodesText = episodes?.let { formatDoubleWithPlural(it, "серия", "серии", "серий") }
            metaLine1 = seasonsText
            metaLine2 = episodesText
            seasonsText?.let { metaParts.add(it) }
            episodesText?.let { metaParts.add(it) }
        }
    }

    val statusLabel = when (status) {
        WorkStatus.IN_PLANS -> strings.inPlans
        WorkStatus.ABANDONED -> strings.abandoned
        WorkStatus.READING -> strings.reading
        WorkStatus.WATCHING -> strings.watching
        WorkStatus.READ -> strings.read
        WorkStatus.WATCHED -> strings.watched
    }

    return WorkItem(
        id = id,
        title = title,
        imageUrl = imageUrlOverride ?: displayCoverPath(),
        meta = metaParts.joinToString(" • "),
        metaLine1 = metaLine1,
        metaLine2 = metaLine2,
        description = description,
        status = status,
        type = type,
        statusLabel = statusLabel
    )
}

@Composable
fun LibraryScreen(
    currentTheme: AppTheme = AppTheme.DARK,
    onThemeChange: (AppTheme) -> Unit = {},
    dynamicColorsEnabled: Boolean = false,
    onDynamicColorsEnabledChange: (Boolean) -> Unit = {},
    themePalette: ThemePalette = ThemePalette.DEFAULT,
    onThemePaletteChange: (ThemePalette) -> Unit = {},
    useCustomAccent: Boolean = false,
    onUseCustomAccentChange: (Boolean) -> Unit = {},
    customAccentArgb: Int = 0xFF6750A4.toInt(),
    onCustomAccentArgbChange: (Int) -> Unit = {},
    useCustomStatsColor: Boolean = false,
    onUseCustomStatsColorChange: (Boolean) -> Unit = {},
    customStatsArgb: Int = 0xFF7C4DFF.toInt(),
    onCustomStatsArgbChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val languageState = rememberLanguageState()
    val currentLanguage = languageState.currentLanguage // Observe language changes
    val strings = remember(currentLanguage) { languageState.strings }
    val context = LocalContext.current
    val density = LocalDensity.current
    val repository = remember { WorkRepository(context) }

    // Preferences for tabs и сортировки
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val coverPrefs = remember { context.getSharedPreferences("cover_prefs", Context.MODE_PRIVATE) }

    // Сортировка списка
    var sortOrder by remember {
        mutableStateOf(
            SortOrder.valueOf(
                prefs.getString("sort_order", SortOrder.TITLE_ASC.name) ?: SortOrder.TITLE_ASC.name
            )
        )
    }

    // Per-tab visibility switches (except Profile, который всегда показан)
    var booksTabEnabled by remember {
        mutableStateOf(prefs.getBoolean("tab_books_enabled", true))
    }
    var animeTabEnabled by remember {
        mutableStateOf(prefs.getBoolean("tab_anime_enabled", true))
    }
    var mangaTabEnabled by remember {
        mutableStateOf(prefs.getBoolean("tab_manga_enabled", true))
    }
    var tvSeriesTabEnabled by remember {
        mutableStateOf(prefs.getBoolean("tab_tv_enabled", true))
    }

    // Режим отображения произведений: список (false) или блоки (true)
    var isGridView by remember {
        mutableStateOf(prefs.getBoolean("view_grid_mode", false))
    }

    fun getDefaultTab(
        booksEnabled: Boolean,
        animeEnabled: Boolean,
        mangaEnabled: Boolean,
        tvEnabled: Boolean
    ): NavigationItem {
        // Выбираем первую доступную вкладку с произведениями
        return when {
            booksEnabled -> NavigationItem.Books
            animeEnabled -> NavigationItem.Anime
            mangaEnabled -> NavigationItem.Manga
            tvEnabled -> NavigationItem.TVSeries
            else -> NavigationItem.Profile
        }
    }

    var selectedItem by remember {
        mutableStateOf<NavigationItem>(
            getDefaultTab(
                booksEnabled = booksTabEnabled,
                animeEnabled = animeTabEnabled,
                mangaEnabled = mangaTabEnabled,
                tvEnabled = tvSeriesTabEnabled
            )
        )
    }

    var searchQuery by remember { mutableStateOf<String>("") }
    var statusFilter by remember { mutableStateOf<WorkStatus?>(null) }
    var showAddWorkScreen by remember { mutableStateOf<Boolean>(false) }
    var works by remember { mutableStateOf<List<Work>>(emptyList()) }
    var sessionCoverByWorkId by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedWork by remember { mutableStateOf<Work?>(null) }
    var editingWork by remember { mutableStateOf<Work?>(null) }
    var workToDelete by remember { mutableStateOf<Work?>(null) }
    var expandedCoverWork by remember { mutableStateOf<Work?>(null) }
    var pendingWorkSave by remember { mutableStateOf<PendingWorkSave?>(null) }
    var showActivityStatsConfirm by remember { mutableStateOf(false) }
    /** Счётчик повторного выбора вкладки «Профиль» (закрытие Настроек и др. оверлеев). */
    var profileReselectSignal by remember { mutableStateOf(0) }

    fun commitWorkSave(work: Work, previous: Work?, recordActivity: Boolean, onAfterSave: (Work) -> Unit) {
        repository.saveWork(work, recordActivity = recordActivity)
        works = repository.getAllWorks()
        onAfterSave(work)
    }

    fun requestSaveWork(work: Work, previous: Work?, onAfterSave: (Work) -> Unit) {
        val resolvedPrevious = previous ?: repository.getWorkById(work.id)
        if (repository.shouldConfirmLargeActivityDelta(resolvedPrevious, work)) {
            pendingWorkSave = PendingWorkSave(work, resolvedPrevious, onAfterSave)
            showActivityStatsConfirm = true
        } else {
            commitWorkSave(work, resolvedPrevious, recordActivity = true, onAfterSave)
        }
    }

    // Search inside details-mode (icon-only search bar)
    var detailSearchExpanded by remember { mutableStateOf(false) }
    var detailSearchQuery by remember { mutableStateOf("") }

    // Как и для грида: новое состояние при смене вкладки/фильтра/сортировки/поиска/режима —
    // список начинается с первого элемента (прокрутка не «залипает» на старой позиции после сортировки).
    val listState = remember(statusFilter, selectedItem, isGridView, sortOrder, searchQuery) {
        LazyListState()
    }
    val gridState = remember(statusFilter, selectedItem, isGridView, sortOrder, searchQuery) {
        LazyGridState()
    }
    var isHeaderVisible by remember { mutableStateOf(true) }

    // Back press when detail-search is open: close search instead of leaving work
    BackHandler(enabled = selectedWork != null && detailSearchExpanded) {
        detailSearchExpanded = false
        detailSearchQuery = ""
    }

    // Back для увеличенной обложки
    BackHandler(enabled = expandedCoverWork != null) {
        expandedCoverWork = null
    }

    // Общий back: закрываем экран добавления или просмотра произведения.
    // Не должен срабатывать, пока открыт detail-search (detailSearchExpanded = true) или увеличенная обложка,
    // чтобы системная кнопка «Назад» сперва закрывала поиск или обложку.
    BackHandler(enabled = showAddWorkScreen || (selectedWork != null && !detailSearchExpanded && expandedCoverWork == null)) {
        when {
            showAddWorkScreen -> {
                // Если редактировали существующее произведение, возвращаемся к экрану просмотра
                if (editingWork != null) {
                    selectedWork = editingWork
                }
                showAddWorkScreen = false
            }
            selectedWork != null -> selectedWork = null
        }
    }

    // Подгружаем каталог при старте и при каждой смене вкладки (после импорта с Профиля список не «залипает»).
    LaunchedEffect(selectedItem) {
        works = repository.getAllWorks()
    }

    // При заходе в приложение — случайная обложка; при >2 не повторяем предыдущую.
    // При добавлении нового произведения — выбираем только для него, не трогая остальные.
    LaunchedEffect(works) {
        val selectedMap = sessionCoverByWorkId.toMutableMap()
        val isAppSessionStart = selectedMap.isEmpty()
        var changed = false
        works.forEach { work ->
            if (!isAppSessionStart && work.id in selectedMap) return@forEach
            val candidates = work.allCoverPaths()
            val chosen = pickRandomCoverAvoidingLast(
                candidates,
                coverPrefs.getString("last_cover_${work.id}", null)
            ) ?: return@forEach
            selectedMap[work.id] = chosen
            coverPrefs.edit { putString("last_cover_${work.id}", chosen) }
            changed = true
        }
        if (changed) sessionCoverByWorkId = selectedMap
    }

    // Reset detail search when leaving work details и возвращаем хедер (строку поиска) при входе в детали
    LaunchedEffect(selectedWork) {
        if (selectedWork == null) {
            detailSearchExpanded = false
            detailSearchQuery = ""
        } else {
            // При открытии экрана просмотра произведения гарантированно показываем хедер,
            // даже если он был скрыт прокруткой.
            isHeaderVisible = true
        }
    }

    // Filter works by selected tab
    val filteredWorks = remember(works, selectedItem, searchQuery, statusFilter, sortOrder) {
        val typeFilter = when (selectedItem) {
            NavigationItem.Books -> WorkType.BOOK
            NavigationItem.Anime -> WorkType.ANIME
            NavigationItem.Manga -> WorkType.MANGA
            NavigationItem.TVSeries -> WorkType.SERIES
            else -> null
        }

        var filtered = if (typeFilter != null) {
            works.filter { it.type == typeFilter }
        } else {
            emptyList()
        }

        // Apply search filter
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.otherTitle?.contains(searchQuery, ignoreCase = true) == true
            }
        }

        // Apply status filter
        statusFilter?.let { sf ->
            filtered = filtered.filter { it.status == sf }
        }

        when (sortOrder) {
            SortOrder.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            SortOrder.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            // Используем updatedAt (epoch millis) как дату изменения.
            SortOrder.DATE_MODIFIED_DESC -> filtered.sortedByDescending { it.updatedAt ?: 0L }
            SortOrder.DATE_MODIFIED_ASC -> filtered.sortedBy { it.updatedAt ?: 0L }
        }
    }

    // Track scroll to hide/show header (only when not viewing work details and not in Profile).
    LaunchedEffect(
        listState.firstVisibleItemScrollOffset,
        listState.firstVisibleItemIndex,
        gridState.firstVisibleItemScrollOffset,
        gridState.firstVisibleItemIndex,
        gridState.layoutInfo.totalItemsCount,
        listState.layoutInfo.totalItemsCount,
        filteredWorks.size,
        selectedWork,
        selectedItem,
        isGridView
    ) {
        if (selectedWork == null && selectedItem != NavigationItem.Profile) {
            if (filteredWorks.size <= 4) {
                if (!isHeaderVisible) isHeaderVisible = true
                return@LaunchedEffect
            }

            val canScroll = if (isGridView) {
                gridState.canScrollForward || gridState.canScrollBackward
            } else {
                listState.canScrollForward || listState.canScrollBackward
            }

            val index: Int
            val offset: Int
            if (isGridView) {
                index = gridState.firstVisibleItemIndex
                offset = gridState.firstVisibleItemScrollOffset
            } else {
                index = listState.firstVisibleItemIndex
                offset = listState.firstVisibleItemScrollOffset
            }
            val shouldHide = index > 0 || offset > 50

            when {
                !canScroll -> {
                    if (!isHeaderVisible) isHeaderVisible = true
                }
                shouldHide -> {
                    if (isHeaderVisible) isHeaderVisible = false
                }
                else -> {
                    if (!isHeaderVisible) isHeaderVisible = true
                }
            }
        }
    }

    val mainBackgroundColor = MainBackgroundColor()
    // Colors used across header + detail-search list
    val iconTextColor = IconTextColor()
    val titleColorBetween = TitleColorBetween()

    CompositionLocalProvider(LocalStrings provides strings) {
        val bottomBarHeight = 130.dp
        Box(modifier = modifier.fillMaxSize()) {
            // Фон-обложка начинается с самого верха экрана (включая область с кнопками)
            if (selectedWork != null && !detailSearchExpanded) {
                val coverPath = selectedWork?.let { w ->
                    sessionCoverByWorkId[w.id] ?: w.displayCoverPath()
                }
                if (!coverPath.isNullOrBlank()) {
                    key(coverPath) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                // Высота: Spacer (40dp) + высота строки поиска (~56dp) + область обложки (343dp)
                                .height(439.dp)
                                .align(Alignment.TopStart)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    ImageRequest.Builder(context)
                                        .data(coverPath.toCoverImageData())
                                        .build()
                                ),
                                contentDescription = null,
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.TopCenter,
                                alpha = 0.35f
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                mainBackgroundColor
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (selectedWork != null && !detailSearchExpanded) {
                            Modifier.background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        mainBackgroundColor
                                    ),
                                    startY = 0f,
                                    endY = with(density) { 439.dp.toPx() }
                                )
                            )
                        } else {
                            Modifier.background(mainBackgroundColor)
                        }
                    )
            ) {
                // Spacer to push search bar from top
                Spacer(modifier = Modifier.height(40.dp))

                // Search bar with theme toggle and add/edit button - сворачивается при прокрутке
                // Управление видимостью через isHeaderVisible, который обновляется:
                // - Для списков произведений: через LaunchedEffect с listState
                // - Для Profile: через onScrollStateChange из ProfileScreen
                // - Для WorkDetail: через onScrollStateChange из WorkDetailScreen
                androidx.compose.animation.AnimatedVisibility(
                    visible = isHeaderVisible,
                    enter = androidx.compose.animation.fadeIn(animationSpec = tween(90)) +
                            androidx.compose.animation.slideInVertically(animationSpec = tween(280)),
                    exit = androidx.compose.animation.fadeOut(animationSpec = tween(90)) +
                            androidx.compose.animation.slideOutVertically(animationSpec = tween(110)) { -it }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 30.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val iconTextColor = IconTextColor()
                            val titleColorBetween = TitleColorBetween()

                            // Back button (only when viewing work details)
                            if (selectedWork != null) {
                                IconButton(onClick = { selectedWork = null }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = strings.cancel,
                                        tint = titleColorBetween
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            // Search bar (icon-only when viewing work details, full when not)
                            SearchBar(
                                modifier = Modifier.weight(1f),
                                currentTheme = currentTheme,
                                iconOnly = selectedWork != null,
                                // ВАЖНО: в списковом режиме всегда прокидываем внешний state,
                                // иначе при сворачивании/разворачивании хедера (AnimatedVisibility)
                                // SearchBar пересоздаётся и внутренний internalQuery сбрасывается.
                                query = if (selectedWork != null) detailSearchQuery else searchQuery,
                                onSearchQueryChange = { q ->
                                    if (selectedWork != null) detailSearchQuery = q else searchQuery = q
                                },
                                expanded = if (selectedWork != null) detailSearchExpanded else null,
                                onExpandedChange = { detailSearchExpanded = it }
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Hide other icons when detail-search expanded (search should take full width)
                            if (!(selectedWork != null && detailSearchExpanded)) {
                                SunIcon(
                                    onClick = {
                                        onThemeChange(if (currentTheme == AppTheme.DARK) AppTheme.LIGHT else AppTheme.DARK)
                                    },
                                    color = iconTextColor,
                                    iconSize = 20.dp
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                if (selectedWork != null) {
                                    IconButton(
                                        onClick = {
                                            editingWork = selectedWork
                                            selectedWork = null
                                            showAddWorkScreen = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = strings.editWork,
                                            tint = if (currentTheme == AppTheme.DARK) Color.White else Color.Black
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { workToDelete = selectedWork }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = strings.deleteWork,
                                            // same color as edit icon
                                            tint = if (currentTheme == AppTheme.DARK) Color.White else Color.Black
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = {
                                            editingWork = null // <-- ДОБАВИТЬ
                                            showAddWorkScreen = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = strings.addWork,
                                            tint = if (currentTheme == AppTheme.DARK) iconTextColor.copy(alpha = 0.9f) else Color.Black
                                        )
                                    }
                                }
                            }
                        }

                        // Status filter chips (under search bar)
                        if (selectedWork == null && selectedItem != NavigationItem.Profile) {
                            Spacer(modifier = Modifier.height(10.dp))

                            val statusItems = when (selectedItem) {
                                NavigationItem.Anime, NavigationItem.TVSeries -> listOf(
                                    WorkStatus.IN_PLANS to strings.inPlans,
                                    WorkStatus.WATCHING to strings.watching,
                                    WorkStatus.WATCHED to strings.watched,
                                    WorkStatus.ABANDONED to strings.abandoned
                                )
                                NavigationItem.Books, NavigationItem.Manga -> listOf(
                                    WorkStatus.IN_PLANS to strings.inPlans,
                                    WorkStatus.READING to strings.reading,
                                    WorkStatus.READ to strings.read,
                                    WorkStatus.ABANDONED to strings.abandoned
                                )
                                else -> emptyList()
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                statusItems.forEach { (st, label) ->
                                    val isSelected = statusFilter == st
                                    val stColor = when (st) {
                                        WorkStatus.IN_PLANS -> Color(0xFF8E6687)
                                        WorkStatus.ABANDONED -> Color(0xFFFF5F5A)
                                        WorkStatus.READING, WorkStatus.WATCHING -> Color(0xFF7179A4)
                                        WorkStatus.READ, WorkStatus.WATCHED -> Color(0xFF79C77C)
                                    }

                                    AssistChip(
                                        onClick = {
                                            statusFilter = if (isSelected) null else st
                                        },
                                        label = {
                                            Text(
                                                text = label,
                                                color = stColor,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = Color.Transparent,
                                            labelColor = stColor,
                                            disabledContainerColor = Color.Transparent,
                                            disabledLabelColor = stColor
                                        ),
                                        border = AssistChipDefaults.assistChipBorder(
                                            borderColor = stColor,
                                            borderWidth = 2.dp,
                                            enabled = true
                                        )
                                    )
                                }

                                // Чип сортировки сразу после статуса «Заброшено»
                                SortOrderChip(
                                    sortOrder = sortOrder,
                                    onSortOrderChange = { newOrder ->
                                        sortOrder = newOrder
                                        prefs.edit { putString("sort_order", newOrder.name) }
                                    },
                                    iconTextColor = iconTextColor
                                )
                            }
                        }
                    }
                }

                // Main content area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Delete confirmation dialog
                    workToDelete?.let { w ->
                        val scheme = androidx.compose.material3.MaterialTheme.colorScheme
                        AlertDialog(
                            onDismissRequest = { workToDelete = null },
                            title = { Text(strings.deleteWork) },
                            text = { Text(strings.deleteWorkConfirm) },
                            containerColor = scheme.surface,
                            titleContentColor = scheme.onSurface,
                            textContentColor = scheme.onSurfaceVariant,
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        if (repository.deleteWork(w.id)) {
                                            works = repository.getAllWorks()
                                            if (selectedWork?.id == w.id) selectedWork = null
                                            workToDelete = null
                                        }
                                    },
                                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                        containerColor = scheme.primary,
                                        contentColor = scheme.onPrimary
                                    )
                                ) { Text(strings.delete) }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { workToDelete = null },
                                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                        contentColor = scheme.primary
                                    )
                                ) { Text(strings.cancel) }
                            }
                        )
                    }

                    if (!showAddWorkScreen) {
                        when (selectedItem) {
                            NavigationItem.Profile -> {
                                ProfileScreen(
                                    currentLanguage = languageState.currentLanguage,
                                    onLanguageChange = { languageState.setLanguage(it) },
                                    currentTheme = currentTheme,
                                    onThemeChange = onThemeChange,
                                    dynamicColorsEnabled = dynamicColorsEnabled,
                                    onDynamicColorsEnabledChange = onDynamicColorsEnabledChange,
                                    useCustomAccent = useCustomAccent,
                                    onUseCustomAccentChange = onUseCustomAccentChange,
                                    customAccentArgb = customAccentArgb,
                                    onCustomAccentArgbChange = onCustomAccentArgbChange,
                                    useCustomStatsColor = useCustomStatsColor,
                                    onUseCustomStatsColorChange = onUseCustomStatsColorChange,
                                    customStatsArgb = customStatsArgb,
                                    onCustomStatsArgbChange = onCustomStatsArgbChange,
                                    booksTabEnabled = booksTabEnabled,
                                    onBooksTabEnabledChange = { enabled ->
                                        booksTabEnabled = enabled
                                        prefs.edit { putBoolean("tab_books_enabled", enabled) }
                                        if (!enabled && selectedItem == NavigationItem.Books) {
                                            selectedItem = getDefaultTab(
                                                booksEnabled = false,
                                                animeEnabled = animeTabEnabled,
                                                mangaEnabled = mangaTabEnabled,
                                                tvEnabled = tvSeriesTabEnabled
                                            )
                                        }
                                    },
                                    animeTabEnabled = animeTabEnabled,
                                    onAnimeTabEnabledChange = { enabled ->
                                        animeTabEnabled = enabled
                                        prefs.edit { putBoolean("tab_anime_enabled", enabled) }
                                        if (!enabled && selectedItem == NavigationItem.Anime) {
                                            selectedItem = getDefaultTab(
                                                booksEnabled = booksTabEnabled,
                                                animeEnabled = false,
                                                mangaEnabled = mangaTabEnabled,
                                                tvEnabled = tvSeriesTabEnabled
                                            )
                                        }
                                    },
                                    mangaTabEnabled = mangaTabEnabled,
                                    onMangaTabEnabledChange = { enabled ->
                                        mangaTabEnabled = enabled
                                        prefs.edit { putBoolean("tab_manga_enabled", enabled) }
                                        if (!enabled && selectedItem == NavigationItem.Manga) {
                                            selectedItem = getDefaultTab(
                                                booksEnabled = booksTabEnabled,
                                                animeEnabled = animeTabEnabled,
                                                mangaEnabled = false,
                                                tvEnabled = tvSeriesTabEnabled
                                            )
                                        }
                                    },
                                    tvSeriesTabEnabled = tvSeriesTabEnabled,
                                    onTvSeriesTabEnabledChange = { enabled ->
                                        tvSeriesTabEnabled = enabled
                                        prefs.edit { putBoolean("tab_tv_enabled", enabled) }
                                        if (!enabled && selectedItem == NavigationItem.TVSeries) {
                                            selectedItem = getDefaultTab(
                                                booksEnabled = booksTabEnabled,
                                                animeEnabled = animeTabEnabled,
                                                mangaEnabled = mangaTabEnabled,
                                                tvEnabled = false
                                            )
                                        }
                                    },
                                    isGridView = isGridView,
                                    onGridViewChange = { asGrid ->
                                        isGridView = asGrid
                                        prefs.edit { putBoolean("view_grid_mode", asGrid) }
                                    },
                                    onWorksUpdated = {
                                        // Обновляем список произведений при добавлении через ProfileScreen
                                        works = repository.getAllWorks()
                                    },
                                    onAddWorkRequested = {
                                        editingWork = null
                                        showAddWorkScreen = true
                                    },
                                    profileReselectSignal = profileReselectSignal,
                                    onScrollStateChange = { shouldHide -> isHeaderVisible = !shouldHide },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            else -> {
                                // Список показываем только когда детальный экран не открыт
                                if (selectedWork == null) {
                                    if (isGridView) {
                                        LazyVerticalGrid(
                                            state = gridState,
                                            columns = GridCells.Fixed(2),
                                            modifier = Modifier.fillMaxSize(),
                                            userScrollEnabled = !showAddWorkScreen,
                                            // Чуть уменьшаем внешние отступы и расстояние между карточками,
                                            // чтобы обложки в блочном режиме казались шире, а ряды ближе друг к другу.
                                            contentPadding = PaddingValues(
                                                start = 8.dp,
                                                end = 8.dp,
                                                top = 2.dp,
                                                bottom = 8.dp
                                            ),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4 .dp)
                                        ) {
                                            items(filteredWorks, key = { it.id }) { work ->
                                                WorkItemGridCard(
                                                    workItem = work.toWorkItem(
                                                        strings,
                                                        imageUrlOverride = sessionCoverByWorkId[work.id]
                                                    ),
                                                    dynamicColorsEnabled = dynamicColorsEnabled,
                                                    onClick = { selectedWork = work }
                                                )
                                            }
                                        }
                                    } else {
                                        LazyColumn(
                                            state = listState,
                                            modifier = Modifier.fillMaxSize(),
                                            userScrollEnabled = !showAddWorkScreen,
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(filteredWorks, key = { it.id }) { work ->
                                                WorkItemCard(
                                                    workItem = work.toWorkItem(
                                                        strings,
                                                        imageUrlOverride = sessionCoverByWorkId[work.id]
                                                    ),
                                                    onClick = { selectedWork = work },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Work Detail Screen (поверх списка, но внутри контентного Box, чтобы фон-обложка был виден)
                        androidx.compose.animation.AnimatedVisibility(
                            visible = selectedWork != null,
                            enter = fadeIn(animationSpec = tween(85)) +
                                    slideInVertically(
                                        initialOffsetY = { it / 4 },
                                        animationSpec = tween(85)
                                    ),
                            exit = fadeOut(animationSpec = tween(55)) +
                                    slideOutVertically(
                                        targetOffsetY = { it / 2 },
                                        animationSpec = tween(55)
                                    )
                        ) {
                            selectedWork?.let { work ->
                                if (detailSearchExpanded) {
                                    val filtered = works
                                        .filter {
                                            if (detailSearchQuery.isBlank()) true
                                            else it.title.contains(detailSearchQuery, ignoreCase = true) ||
                                                    it.otherTitle?.contains(detailSearchQuery, ignoreCase = true) == true
                                        }
                                        .sortedBy { it.title.lowercase() }

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(filtered, key = { it.id }) { item ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedWork = item
                                                        detailSearchExpanded = false
                                                        detailSearchQuery = ""
                                                    }
                                                    .padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Image(
                                                    painter = rememberAsyncImagePainter(
                                                        (sessionCoverByWorkId[item.id] ?: item.displayCoverPath())?.toCoverImageData()
                                                    ),
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(54.dp)
                                                        .clip(RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.title,
                                                        color = titleColorBetween,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Text(
                                                        text = when (item.type) {
                                                            WorkType.BOOK -> strings.books
                                                            WorkType.MANGA -> strings.manga
                                                            WorkType.ANIME -> strings.anime
                                                            WorkType.SERIES -> strings.tvSeries
                                                        },
                                                        color = iconTextColor.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    WorkDetailScreen(
                                        work = work,
                                        coverPaths = work.allCoverPaths(),
                                        sessionCoverPath = sessionCoverByWorkId[work.id],
                                        onSessionCoverPathChange = { path ->
                                            sessionCoverByWorkId = sessionCoverByWorkId + (work.id to path)
                                            coverPrefs.edit { putString("last_cover_${work.id}", path) }
                                        },
                                        onBack = { selectedWork = null },
                                        onEdit = {
                                            editingWork = work
                                            selectedWork = null
                                            showAddWorkScreen = true
                                        },
                                        onDelete = { workToDelete = work },
                                        onSave = { updatedWork ->
                                            requestSaveWork(updatedWork, selectedWork) { saved ->
                                                selectedWork = saved
                                                sessionCoverByWorkId[updatedWork.id]?.let { current ->
                                                    if (updatedWork.allCoverPaths().none { it == current }) {
                                                        val next = pickRandomCoverAvoidingLast(
                                                            updatedWork.allCoverPaths(),
                                                            coverPrefs.getString("last_cover_${updatedWork.id}", null)
                                                        )
                                                        if (next != null) {
                                                            sessionCoverByWorkId = sessionCoverByWorkId + (updatedWork.id to next)
                                                            coverPrefs.edit { putString("last_cover_${updatedWork.id}", next) }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        onCoverClick = { expandedCoverWork = work },
                                        currentTheme = currentTheme,
                                        onScrollStateChange = null
                                    )
                                }
                            }
                        }
                    }

                }
                // Bottom navigation bar
                BottomNavigationBar(
                    selectedItem = selectedItem,
                    onItemSelected = { item ->
                        if (item == NavigationItem.Profile && selectedItem == NavigationItem.Profile) {
                            profileReselectSignal++
                        } else {
                            selectedItem = item
                            isHeaderVisible = true
                            statusFilter = null
                            // При смене вкладки закрываем детальный экран, поиск внутри него и увеличенную обложку
                            selectedWork = null
                            detailSearchExpanded = false
                            detailSearchQuery = ""
                            expandedCoverWork = null
                            // Также закрываем форму добавления/редактирования
                            showAddWorkScreen = false
                        }
                    },
                    currentTheme = currentTheme,
                    dynamicColorsEnabled = dynamicColorsEnabled,
                    booksEnabled = booksTabEnabled,
                    animeEnabled = animeTabEnabled,
                    mangaEnabled = mangaTabEnabled,
                    tvSeriesEnabled = tvSeriesTabEnabled,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Add/Edit Work Screen — отдельный слой; контент вкладок под ним не рисуется (см. showAddWorkScreen выше)
            androidx.compose.animation.AnimatedVisibility(
                visible = showAddWorkScreen,
                enter = fadeIn(animationSpec = tween(140, easing = FastOutSlowInEasing)) + slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = tween(180, easing = FastOutSlowInEasing)
                ),
                exit = fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing)) + slideOutVertically(
                    targetOffsetY = { it / 3 },
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                ),

                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(mainBackgroundColor)
                ) {
                    AddWorkScreen(
                        onBack = {
                            // Кнопка "Назад" в форме добавления/редактирования
                            if (editingWork != null) {
                                // При редактировании возвращаемся в просмотр произведения
                                selectedWork = editingWork
                            }
                            showAddWorkScreen = false
                        },
                        onSave = { work ->
                            requestSaveWork(work, editingWork) { saved ->
                                showAddWorkScreen = false
                                // После сохранения:
                                // - если это было редактирование — остаёмся в экране просмотра обновлённого произведения
                                // - если это новое произведение — остаёмся на вкладке со списком
                                selectedWork = if (editingWork != null) saved else selectedWork
                            }
                        },
                        work = editingWork // Pass work for editing
                    )
                }
            }

            // Полноэкранное увеличенное изображение обложки
            expandedCoverWork?.let { work ->
                val coverPath = sessionCoverByWorkId[work.id] ?: work.displayCoverPath()
                if (coverPath != null && coverPath.isNotBlank()) {
                    val coverImageUri = when {
                        coverPath.startsWith("/") -> Uri.fromFile(File(coverPath))
                        else -> coverPath.toUri()
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.95f))
                            .clickable { expandedCoverWork = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(context)
                                    .data(coverImageUri)
                                    .build()
                            ),
                            contentDescription = strings.cover,
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .fillMaxHeight(0.85f),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            if (showActivityStatsConfirm && pendingWorkSave != null) {
                val pending = pendingWorkSave!!
                ActivityStatsConfirmDialog(
                    onConfirm = {
                        commitWorkSave(pending.work, pending.previous, recordActivity = true, pending.onAfterSave)
                        pendingWorkSave = null
                        showActivityStatsConfirm = false
                    },
                    onDecline = {
                        commitWorkSave(pending.work, pending.previous, recordActivity = false, pending.onAfterSave)
                        pendingWorkSave = null
                        showActivityStatsConfirm = false
                    },
                    onDismiss = {
                        pendingWorkSave = null
                        showActivityStatsConfirm = false
                    }
                )
            }
        }
    }
}