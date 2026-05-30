package com.rn.library.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import android.os.Environment
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipFile

data class BookImportResult(
    val work: Work? = null,
    val errorMessage: String? = null
)

class WorkRepository(private val context: Context) {
    private val activityDeltaLog = ActivityDeltaLog(context)

    private data class TreeDocument(
        val uri: Uri,
        val name: String,
        val relativePath: String
    )
    private val worksDirectory: File
        get() {
            // Use external files directory so it's visible in Android/data
            val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
            val dir = File(baseDir, "works")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    fun getAllWorks(): List<Work> {
        val works = mutableListOf<Work>()
        val files = worksDirectory.listFiles { _, name -> name.endsWith(".md") }
        files?.forEach { file ->
            try {
                val work = parseMarkdownFile(file)
                work?.let { parsed ->
                    val withCovers = persistWorkCovers(parsed)
                    if (withCovers != parsed) {
                        saveWork(withCovers, preserveUpdatedAt = true, recordActivity = false)
                    }
                    works.add(withCovers)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return works
    }

    /** Копирует обложку из пикера в каталог приложения и возвращает абсолютный путь к файлу. */
    fun persistCoverFromPicker(uri: Uri, workId: String, index: Int): String? =
        saveCoverToStorage(uri, workId, index)

    /**
     * Заменяет временные content:// URI на файлы в каталоге covers/.
     * Уже сохранённые абсолютные пути не трогает.
     */
    fun persistWorkCovers(work: Work): Work {
        val orderedPaths = work.allCoverPaths()
        if (orderedPaths.isEmpty()) return work

        val persistedPaths = orderedPaths.mapIndexed { index, path ->
            persistSingleCoverPath(path, work.id, index)
        }

        val primaryIndex = work.coverPath
            ?.let { orderedPaths.indexOf(it).takeIf { idx -> idx >= 0 } }
            ?: 0
        val primary = persistedPaths.getOrElse(primaryIndex) { persistedPaths.first() }

        val updated = work.copy(coverPath = primary, coverPaths = persistedPaths)
        return if (updated.coverPath == work.coverPath && updated.coverPaths == work.coverPaths) work else updated
    }

    private fun persistSingleCoverPath(path: String, workId: String, index: Int): String {
        if (path.startsWith("/")) {
            val file = File(path)
            if (file.exists() && file.isFile) return path
        }
        if (needsPersisting(path)) {
            val uri = Uri.parse(path)
            saveCoverToStorage(uri, workId, index)?.let { return it }
        }
        return path
    }

    private fun needsPersisting(path: String): Boolean {
        if (path.startsWith("content://") || path.startsWith("file://")) return true
        if (path.startsWith("/")) {
            val file = File(path)
            return !file.exists()
        }
        return true
    }

    private fun saveCoverToStorage(uri: Uri, workId: String, index: Int): String? {
        return try {
            val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
            val coversDir = File(baseDir, "covers").apply { mkdirs() }
            val outputFile = File(coversDir, "${workId}_$index.jpg")
            val compressed = compressImage(uri, context)
            if (compressed != null) {
                FileOutputStream(outputFile).use { it.write(compressed) }
                outputFile.absolutePath
            } else {
                copyCoverFromDocument(context, uri, "${workId}_$index")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** События прироста прогресса по дням (для heatmap / графиков активности). */
    fun loadActivityEvents(): List<ActivityDeltaEvent> = activityDeltaLog.loadEvents()

    fun getWorkById(workId: String): Work? {
        val file = File(worksDirectory, "$workId.md")
        return if (file.exists()) parseMarkdownFile(file) else null
    }

    /** Прирост единиц прогресса между сохранённым и новым состоянием произведения. */
    fun calculateProgressDelta(previous: Work?, saved: Work): Pair<Double, Double> {
        val oldRead = previous?.let { readProgressUnits(it) } ?: 0.0
        val newRead = readProgressUnits(saved)
        val readDelta = newRead - oldRead
        val oldWatch = previous?.let { watchedProgressUnits(it) } ?: 0.0
        val newWatch = watchedProgressUnits(saved)
        val watchDelta = newWatch - oldWatch
        return readDelta to watchDelta
    }

    /** Нужно ли спрашивать пользователя о записи в Heatmap / «Активность по периодам». */
    fun shouldConfirmLargeActivityDelta(previous: Work?, saved: Work): Boolean {
        val (readDelta, watchDelta) = calculateProgressDelta(previous, saved)
        if (readDelta <= 0.0 && watchDelta <= 0.0) return false
        return when (saved.type) {
            WorkType.BOOK, WorkType.MANGA -> readDelta > LARGE_ACTIVITY_DELTA_THRESHOLD
            WorkType.ANIME, WorkType.SERIES -> watchDelta > LARGE_ACTIVITY_DELTA_THRESHOLD
        }
    }

    fun importActivityStatisticsFromUri(context: Context, uri: Uri): Int {
        return try {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return 0
            importActivityStatisticsFromText(text)
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    fun importActivityStatisticsFromFile(file: File): Int = activityDeltaLog.importFromFile(file)

    private fun importActivityStatisticsFromText(text: String): Int {
        val events = ActivityStatisticsFormat.parseImportText(text)
        if (text.contains("format=${ActivityStatisticsFormat.FORMAT_VERSION}") ||
            text.contains("[Heatmap]")
        ) {
            activityDeltaLog.replaceAllEvents(events)
        } else {
            activityDeltaLog.mergeEvents(events)
        }
        return events.size
    }

    fun saveWork(
        work: Work,
        preserveUpdatedAt: Boolean = false,
        recordActivity: Boolean = true
    ): Boolean {
        return try {
            val workWithPersistedCovers = persistWorkCovers(work)
            val filename = "${workWithPersistedCovers.id}.md"
            val file = File(worksDirectory, filename)
            val previous = if (file.exists()) parseMarkdownFile(file) else null
            val now = System.currentTimeMillis()
            val workWithTimestamp = if (preserveUpdatedAt && workWithPersistedCovers.updatedAt != null) {
                workWithPersistedCovers
            } else {
                workWithPersistedCovers.copy(updatedAt = now)
            }
            val content = workToMarkdown(workWithTimestamp)
            file.writeText(content)
            if (recordActivity) {
                recordProgressDelta(previous, workWithTimestamp)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun recordProgressDelta(previous: Work?, saved: Work) {
        val (readDelta, watchDelta) = calculateProgressDelta(previous, saved)
        if (readDelta <= 0.0 && watchDelta <= 0.0) return
        val today = LocalDate.now(ZoneId.systemDefault())
        activityDeltaLog.appendEvent(
            ActivityDeltaEvent(
                date = today,
                workId = saved.id,
                readDelta = readDelta,
                watchDelta = watchDelta
            )
        )
    }

    fun deleteWork(workId: String): Boolean {
        return try {
            val file = File(worksDirectory, "$workId.md")
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun workToMarkdown(work: Work): String {
        val sb = StringBuilder()
        sb.appendLine("---")
        sb.appendLine("id: ${work.id}")
        sb.appendLine("title: ${escapeMarkdown(work.title)}")
        sb.appendLine("type: ${work.type.name}")
        sb.appendLine("status: ${work.status.name}")
        work.coverPath?.let {
            sb.appendLine("cover: $it")
        }
        if (work.coverPaths.isNotEmpty()) {
            sb.appendLine("covers: ${encodeList(work.coverPaths)}")
        }
        work.chapters?.let {
            sb.appendLine("chapters: ${formatDoubleForDisplay(it)}")
        }
        work.volumes?.let {
            sb.appendLine("volumes: ${formatDoubleForDisplay(it)}")
        }
        work.bookChapters?.let {
            sb.appendLine("bookChapters: ${formatDoubleForDisplay(it)}")
        }
        work.episodes?.let {
            sb.appendLine("episodes: ${formatDoubleForDisplay(it)}")
        }
        work.seasons?.let {
            sb.appendLine("seasons: $it")
        }
        work.dateRead?.let {
            sb.appendLine("dateRead: $it")
        }
        if (work.rereadDates.isNotEmpty()) {
            sb.appendLine("rereadDates: ${encodeList(work.rereadDates)}")
        }
        if (work.readingPeriods.isNotEmpty()) {
            sb.appendLine("readingPeriods: ${encodePeriods(work.readingPeriods)}")
        }
        if (work.unitProgress.isNotEmpty()) {
            sb.appendLine("unitProgress: ${encodeUnitProgress(work.unitProgress)}")
        }
        work.activeUnitIndex?.let {
            sb.appendLine("activeUnitIndex: $it")
        }
        work.year?.let {
            sb.appendLine("year: $it")
        }
        work.yearPeriod?.takeIf { it.isNotBlank() }?.let {
            sb.appendLine("yearPeriod: ${escapeMarkdown(it)}")
        }
        work.country?.let {
            sb.appendLine("country: ${escapeMarkdown(it)}")
        }
        work.seriesType?.let {
            sb.appendLine("seriesType: ${it.name}")
        }
        work.mangaType?.let {
            sb.appendLine("mangaType: ${it.name}")
        }
        work.animeSeason?.let {
            sb.appendLine("animeSeason: ${it.name}")
        }
        work.abandonedProgress?.let {
            sb.appendLine("abandonedProgress: $it")
        }
        work.progress?.let {
            sb.appendLine("progress: ${formatDoubleForDisplay(it)}")
        }
        work.otherTitle?.let {
            sb.appendLine("otherTitle: ${escapeMarkdown(it)}")
        }
        work.note?.let {
            sb.appendLine("note: ${escapeMarkdown(it)}")
        }
        work.link?.let {
            // Не применяем escapeMarkdown к ссылкам, чтобы избежать проблем с обратными слэшами
            sb.appendLine("link: $it")
        }
        work.link2?.let {
            // Вторая ссылка хранится отдельно
            sb.appendLine("link2: $it")
        }
        work.updatedAt?.let {
            sb.appendLine("updatedAt: $it")
        }
        sb.appendLine("---")
        sb.appendLine()
        sb.appendLine(work.description)
        return sb.toString()
    }

    private fun parseMarkdownFile(file: File): Work? {
        return try {
            parseMarkdownContent(file.readText())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseMarkdownContent(content: String): Work? {
        return try {
            val frontMatterRegex = Regex("---\\s*\\n([\\s\\S]*?)\\n---")
            val match = frontMatterRegex.find(content)
            if (match == null) return null

            val frontMatter = match.groupValues[1]
            val properties = frontMatter.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .associate { line ->
                    val colonIndex = line.indexOf(':')
                    if (colonIndex > 0) {
                        val key = line.substring(0, colonIndex).trim()
                        val value = unescapeMarkdown(line.substring(colonIndex + 1).trim())
                        key to value
                    } else {
                        "" to ""
                    }
                }

            val id = properties["id"] ?: return null
            val title = properties["title"] ?: ""
            val type = WorkType.valueOf(properties["type"] ?: "BOOK")
            val status = WorkStatus.valueOf(properties["status"] ?: "IN_PLANS")
            val coverPath = properties["cover"]
            val coverPaths = decodeList(properties["covers"])
            val chapters = properties["chapters"]?.toDoubleOrNull()
            val volumes = properties["volumes"]?.toDoubleOrNull()
            val bookChapters = properties["bookChapters"]?.toDoubleOrNull()
            val episodes = properties["episodes"]?.toDoubleOrNull()
            val seasons = properties["seasons"]?.toIntOrNull()
            val year = properties["year"]?.toIntOrNull()
            val yearPeriod = properties["yearPeriod"]
            val country = properties["country"]
            val seriesType = properties["seriesType"]?.let { SeriesType.valueOf(it) }
            val mangaType = properties["mangaType"]?.let { MangaType.valueOf(it) }
            val animeSeason = properties["animeSeason"]?.let { AnimeSeason.valueOf(it) }
            val abandonedProgress = properties["abandonedProgress"]?.toIntOrNull()
            val progress = properties["progress"]?.toDoubleOrNull()
            val otherTitle = properties["otherTitle"]
            val dateRead = properties["dateRead"]
            val rereadDates = decodeList(properties["rereadDates"])
            val readingPeriods = decodePeriods(properties["readingPeriods"])
            val unitProgress = decodeUnitProgress(properties["unitProgress"])
            val activeUnitIndex = properties["activeUnitIndex"]?.toIntOrNull()
            val note = properties["note"]
            val link = properties["link"]
            val link2 = properties["link2"]
            val updatedAt = properties["updatedAt"]?.toLongOrNull()

            val bodyStart = match.range.last + 1
            val description = content.substring(bodyStart).trim()

            Work(
                id = id,
                title = title,
                description = description,
                type = type,
                coverPath = coverPath,
                coverPaths = coverPaths,
                chapters = chapters,
                volumes = volumes,
                bookChapters = bookChapters,
                episodes = episodes,
                seasons = seasons,
                year = year,
                yearPeriod = yearPeriod,
                country = country,
                status = status,
                seriesType = seriesType,
                mangaType = mangaType,
                animeSeason = animeSeason,
                abandonedProgress = abandonedProgress,
                progress = progress,
                otherTitle = otherTitle,
                dateRead = dateRead,
                rereadDates = rereadDates,
                readingPeriods = readingPeriods,
                unitProgress = unitProgress,
                activeUnitIndex = activeUnitIndex,
                note = note,
                link = link,
                link2 = link2,
                updatedAt = updatedAt
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Импорт одного файла бэкапа приложения (.md с YAML‑шапкой), как после экспорта.
     */
    fun importMarkdownBackup(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val text = input.bufferedReader().readText()
                val work = parseMarkdownContent(text) ?: return false
                saveWork(work, preserveUpdatedAt = true, recordActivity = false)
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importExportedBackupsFromTree(
        context: Context,
        treeUri: Uri
    ): Triple<Int, Int, List<String>> {
        val errors = mutableListOf<String>()
        return try {
            val allDocs = collectTreeDocuments(context, treeUri)
            val markdownDocs = allDocs.filter { it.name.endsWith(".md", ignoreCase = true) }
            val imageDocs = allDocs.filter { isImageName(it.name) }
            val imageByBase = imageDocs.associateBy { it.name.substringBeforeLast('.').lowercase(Locale.getDefault()) }
            var imported = 0
            allDocs
                .filter { isActivityStatisticsDocument(context, it) }
                .distinctBy { it.uri.toString() }
                .forEach { statsDoc ->
                    importActivityStatisticsFromUri(context, statsDoc.uri)
                }

            markdownDocs.forEach { doc ->
                try {
                    val text = context.contentResolver.openInputStream(doc.uri)?.bufferedReader()?.use { it.readText() }
                    if (text.isNullOrBlank()) {
                        errors += "${doc.name} (пустой файл)"
                        return@forEach
                    }
                    val parsed = parseMarkdownContent(text)
                    if (parsed == null) {
                        errors += "${doc.name} (некорректный markdown)"
                        return@forEach
                    }
                    val baseFromMdName = doc.name.substringBeforeLast('.').lowercase(Locale.getDefault())
                    val coverDoc = imageByBase[baseFromMdName]
                    val importedCoverPath = coverDoc?.let { copyCoverFromDocument(context, it.uri, parsed.id) }
                    val workToSave = parsed.copy(coverPath = importedCoverPath ?: parsed.coverPath)
                    if (saveWork(workToSave, preserveUpdatedAt = true, recordActivity = false)) {
                        imported++
                    } else {
                        errors += "${doc.name} (не удалось сохранить)"
                    }
                } catch (e: Exception) {
                    errors += "${doc.name} (${e.message ?: "ошибка"})"
                }
            }
            Triple(imported, markdownDocs.size, errors)
        } catch (e: Exception) {
            Triple(0, 0, listOf(e.message ?: "Ошибка чтения папки"))
        }
    }

    /**
     * Импорт книги FB2 / EPUB / PDF: копирует файл в хранилище приложения и создаёт произведение типа «Книга».
     */
    fun importBookFromUri(context: Context, uri: Uri): Work? {
        return importBookFromUriDetailed(context, uri).work
    }

    fun importBookFromUriDetailed(context: Context, uri: Uri): BookImportResult {
        return try {
            val cr = context.contentResolver
            val displayName = queryDisplayName(context, uri) ?: "book.bin"
            val ext = displayName.substringAfterLast('.', "").lowercase(Locale.getDefault())
            val destDir = File(context.filesDir, "imported_books").apply { mkdirs() }
            val destFile = File(destDir, "${UUID.randomUUID()}_$displayName")
            cr.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            } ?: return BookImportResult(errorMessage = "Не удалось открыть файл: $displayName")
            if (!destFile.exists() || destFile.length() <= 0L) {
                return BookImportResult(errorMessage = "Файл пустой или не был скопирован: $displayName")
            }

            val preview = when (ext) {
                "fb2" -> BookFormatImporter.fromFb2(destFile)
                "epub" -> BookFormatImporter.fromEpub(destFile)
                "pdf" -> BookFormatImporter.fromPdf(destFile)
                "zip" -> previewFromZipArchive(destFile)
                else -> {
                    val magic = ByteArray(4)
                    destFile.inputStream().use { it.read(magic) }
                    val isZip = magic.size >= 2 &&
                            magic[0] == 0x50.toByte() &&
                            magic[1] == 0x4B.toByte()
                    when {
                        isZip -> previewFromZipArchive(destFile) ?: BookFormatImporter.fromEpub(destFile)
                        else -> BookFormatImporter.fromFb2(destFile) ?: BookFormatImporter.fromPdf(destFile)
                    }
                }
            }

            val fallbackTitle = displayName.substringBeforeLast('.', displayName)
            val title = preview?.title?.takeIf { it.isNotBlank() } ?: fallbackTitle
            val description = preview?.description?.takeIf { it.isNotBlank() } ?: ""

            val work = Work(
                id = generateId(),
                title = title,
                description = description,
                type = WorkType.BOOK,
                status = WorkStatus.IN_PLANS,
                link = destFile.absolutePath
            )
            if (saveWork(work, recordActivity = false)) {
                BookImportResult(work = work)
            } else {
                BookImportResult(errorMessage = "Не удалось сохранить произведение: $displayName")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            BookImportResult(errorMessage = e.message ?: "Ошибка импорта")
        }
    }

    private fun previewFromZipArchive(zipFile: File): BookImportPreview? {
        return try {
            ZipFile(zipFile).use { zip ->
                // EPUB в ZIP чаще всего содержит META-INF/container.xml
                if (zip.getEntry("META-INF/container.xml") != null) {
                    return BookFormatImporter.fromEpub(zipFile)
                }
                // Ищем упакованный FB2, если это fb2.zip
                var fb2Entry: java.util.zip.ZipEntry? = null
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (!entry.isDirectory && entry.name.endsWith(".fb2", ignoreCase = true)) {
                        fb2Entry = entry
                        break
                    }
                }
                if (fb2Entry == null) return null
                val fb2Temp = File(context.cacheDir, "tmp_fb2_${UUID.randomUUID()}.fb2")
                zip.getInputStream(fb2Entry).use { input ->
                    FileOutputStream(fb2Temp).use { output -> input.copyTo(output) }
                }
                try {
                    BookFormatImporter.fromFb2(fb2Temp)
                } finally {
                    fb2Temp.delete()
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Импортирует ранее экспортированные .md из Downloads/MyLibrary и его подкаталогов.
     * Возвращает Pair(успешно импортировано, найдено .md файлов).
     */
    fun importExportedBackupsFromDownloads(
        context: Context,
        booksFolder: String = "Книги",
        animeFolder: String = "Аниме",
        mangaFolder: String = "Манга",
        seriesFolder: String = "Сериалы"
    ): Pair<Int, Int> {
        return try {
            val externalRoot = Environment.getExternalStorageDirectory()
            val candidateDownloadsDirs = listOfNotNull(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                File(externalRoot, "Download"),
                File(externalRoot, "Downloads"),
                File("/storage/emulated/0/Download"),
                File("/storage/emulated/0/Downloads"),
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                context.getExternalFilesDir(null)?.let { File(it, "Download") }
            ).distinctBy { it.absolutePath }

            val directExportDirs = candidateDownloadsDirs
                .map { File(it, "MyLibrary") }
                .filter { it.exists() && it.isDirectory }

            // Ищем папку экспорта не только в Downloads, но и по основным корням памяти устройства.
            val scanRoots = listOf(
                File("/storage/emulated/0"),
                externalRoot,
                File("/sdcard"),
                File("/storage")
            ).distinctBy { it.absolutePath }
                .filter { it.exists() && it.isDirectory }

            val discoveredExportDirs = mutableListOf<File>()
            scanRoots.forEach { root ->
                try {
                    root.walkTopDown()
                        .maxDepth(6)
                        .forEach { file ->
                            if (file.isDirectory && file.name.equals("MyLibrary", ignoreCase = true)) {
                                discoveredExportDirs += file
                            }
                        }
                } catch (_: Exception) {
                    // skip inaccessible folders
                }
            }

            val exportDirs = (directExportDirs + discoveredExportDirs)
                .distinctBy { it.absolutePath }

            if (exportDirs.isEmpty()) {
                return 0 to 0
            }

            val candidateDirs = exportDirs
                .flatMap { exportDir ->
                    listOf(
                        exportDir,
                        File(exportDir, booksFolder),
                        File(exportDir, animeFolder),
                        File(exportDir, mangaFolder),
                        File(exportDir, seriesFolder),
                        File(exportDir, "Books"),
                        File(exportDir, "Anime"),
                        File(exportDir, "Manga"),
                        File(exportDir, "TV Series"),
                        File(exportDir, "Книги"),
                        File(exportDir, "Аниме"),
                        File(exportDir, "Манга"),
                        File(exportDir, "Сериалы")
                    )
                }
                .distinctBy { it.absolutePath }
                .filter { it.exists() && it.isDirectory }

            val mdFiles = candidateDirs
                .flatMap { dir ->
                    dir.listFiles { file -> file.isFile && file.name.endsWith(".md", ignoreCase = true) }
                        ?.toList()
                        .orEmpty()
                }
                .distinctBy { it.absolutePath }

            var imported = 0
            mdFiles.forEach { file ->
                try {
                    val work = parseMarkdownFile(file)
                    if (work != null && saveWork(work, preserveUpdatedAt = true, recordActivity = false)) {
                        imported++
                    }
                } catch (_: Exception) {
                    // ignore bad file and continue
                }
            }
            exportDirs.forEach { exportDir ->
                val statsFile = File(exportDir, ActivityDeltaLog.EXPORT_FILENAME)
                if (statsFile.exists()) {
                    importActivityStatisticsFromFile(statsFile)
                }
            }
            imported to mdFiles.size
        } catch (e: Exception) {
            e.printStackTrace()
            0 to 0
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) {
                    return cursor.getString(idx)
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    fun getFileDisplayName(context: Context, uri: Uri): String? {
        return queryDisplayName(context, uri)
    }

    private fun escapeMarkdown(text: String): String {
        return text.replace("\n", "\\n").replace(":", "\\:")
    }

    private fun unescapeMarkdown(text: String): String {
        return text.replace("\\:", ":").replace("\\n", "\n").replace("\\\\", "\\")
    }

    private fun encodeList(values: List<String>): String {
        return values
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("||") {
                it.replace("|", "\\|").replace(":", "\\:")
            }
    }

    private fun decodeList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split("||")
            .map { it.replace("\\|", "|").trim() }
            .filter { it.isNotEmpty() }
    }

    private fun encodePeriods(periods: List<ReadingPeriod>): String {
        return periods.joinToString("||") { period ->
            val end = period.endDate ?: ""
            "${period.startDate}..$end"
        }
    }

    private fun decodePeriods(raw: String?): List<ReadingPeriod> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split("||").mapNotNull { item ->
            val start = item.substringBefore("..").trim()
            if (start.isBlank()) return@mapNotNull null
            val end = item.substringAfter("..", "").trim().ifBlank { null }
            ReadingPeriod(startDate = start, endDate = end)
        }
    }

    private fun encodeUnitProgress(items: List<UnitProgress>): String {
        return items.joinToString("||") { item ->
            val totalRaw = item.total?.toString().orEmpty()
            "${item.unitName.replace("|", "\\|")}::${item.completed}/$totalRaw"
        }
    }

    private fun decodeUnitProgress(raw: String?): List<UnitProgress> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split("||").mapNotNull { row ->
            val name = row.substringBefore("::").replace("\\|", "|").trim()
            val values = row.substringAfter("::", "")
            if (name.isBlank() || values.isBlank()) return@mapNotNull null
            val completed = values.substringBefore("/").toDoubleOrNull() ?: return@mapNotNull null
            val total = values.substringAfter("/", "").toDoubleOrNull()
            UnitProgress(unitName = name, completed = completed, total = total)
        }
    }

    private fun collectTreeDocuments(context: Context, treeUri: Uri): List<TreeDocument> {
        val result = mutableListOf<TreeDocument>()
        val rootDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )

        fun walk(parentUri: Uri, relativePath: String) {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                parentUri,
                DocumentsContract.getDocumentId(parentUri)
            )
            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    if (idIdx < 0 || nameIdx < 0 || mimeIdx < 0) continue
                    val docId = cursor.getString(idIdx) ?: continue
                    val name = cursor.getString(nameIdx) ?: continue
                    val mime = cursor.getString(mimeIdx) ?: continue
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    if (DocumentsContract.Document.MIME_TYPE_DIR == mime) {
                        val nextPath = if (relativePath.isBlank()) name else "$relativePath/$name"
                        walk(docUri, nextPath)
                    } else {
                        result += TreeDocument(
                            uri = docUri,
                            name = name,
                            relativePath = relativePath
                        )
                    }
                }
            }
        }

        walk(rootDocumentUri, "")
        return result
    }

    private fun isImageName(fileName: String): Boolean {
        val lower = fileName.lowercase(Locale.getDefault())
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")
    }

    private fun isActivityStatisticsDocument(context: Context, doc: TreeDocument): Boolean {
        if (doc.name.equals(ActivityDeltaLog.EXPORT_FILENAME, ignoreCase = true)) return true
        if (!doc.name.endsWith(".txt", ignoreCase = true)) return false
        return looksLikeActivityStatisticsExport(context, doc.uri)
    }

    private fun looksLikeActivityStatisticsExport(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val head = input.bufferedReader().readText().take(4096)
                head.contains("format=${ActivityStatisticsFormat.FORMAT_VERSION}") ||
                        head.contains("[Heatmap]")
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun copyCoverFromDocument(context: Context, uri: Uri, workId: String): String? {
        return try {
            val name = queryDisplayName(context, uri) ?: "cover.jpg"
            val ext = name.substringAfterLast('.', "jpg")
            val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
            val coversDir = File(baseDir, "covers").apply { mkdirs() }
            val imageFile = File(coversDir, "${workId}_imported.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(imageFile).use { output -> input.copyTo(output) }
            } ?: return null
            imageFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun exportWorksToDownloads(
        booksFolder: String = "Книги",
        animeFolder: String = "Аниме",
        mangaFolder: String = "Манга",
        seriesFolder: String = "Сериалы",
        bookCoversFolder: String = "Обложки книг",
        animeCoversFolder: String = "Обложки аниме",
        mangaCoversFolder: String = "Обложки манги",
        seriesCoversFolder: String = "Обложки сериалов"
    ): String? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val exportDir = File(downloadsDir, "MyLibrary")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            // Create subdirectories for each work type
            val booksDir = File(exportDir, booksFolder)
            val animeDir = File(exportDir, animeFolder)
            val mangaDir = File(exportDir, mangaFolder)
            val seriesDir = File(exportDir, seriesFolder)
            val coversDir = File(exportDir, bookCoversFolder)
            val animeCoversDir = File(exportDir, animeCoversFolder)
            val mangaCoversDir = File(exportDir, mangaCoversFolder)
            val seriesCoversDir = File(exportDir, seriesCoversFolder)

            listOf(booksDir, animeDir, mangaDir, seriesDir, coversDir, animeCoversDir, mangaCoversDir, seriesCoversDir).forEach { dir ->
                if (!dir.exists()) {
                    dir.mkdirs()
                }
            }
            listOf(coversDir, animeCoversDir, mangaCoversDir, seriesCoversDir).forEach { coverDir ->
                val noMediaFile = File(coverDir, ".nomedia")
                if (!noMediaFile.exists()) {
                    noMediaFile.writeText("")
                }
            }

            val usedExportNames = mutableSetOf<String>()
            val sourceFiles = worksDirectory.listFiles { _, name -> name.endsWith(".md") }
            sourceFiles?.forEach { sourceFile ->
                val work = parseMarkdownFile(sourceFile)
                if (work != null) {
                    val targetDir = when (work.type) {
                        WorkType.BOOK -> booksDir
                        WorkType.ANIME -> animeDir
                        WorkType.MANGA -> mangaDir
                        WorkType.SERIES -> seriesDir
                    }

                    val base = uniqueExportBase(work.title, work.id, usedExportNames)
                    val destFile = File(targetDir, "$base.md")
                    FileInputStream(sourceFile).use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    work.coverPath?.let { coverPath ->
                        val coverFile = File(coverPath)
                        if (coverFile.exists()) {
                            val coverTargetDir = when (work.type) {
                                WorkType.BOOK -> coversDir
                                WorkType.ANIME -> animeCoversDir
                                WorkType.MANGA -> mangaCoversDir
                                WorkType.SERIES -> seriesCoversDir
                            }
                            val coverExt = coverFile.extension.ifBlank { "jpg" }
                            val coverDestFile = File(coverTargetDir, "$base.$coverExt")
                            FileInputStream(coverFile).use { input ->
                                FileOutputStream(coverDestFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }
            }

            val statsFile = File(exportDir, ActivityDeltaLog.EXPORT_FILENAME)
            activityDeltaLog.exportToFile(statsFile)

            exportDir.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getWorksDirectoryPath(): String {
        return worksDirectory.absolutePath
    }

    /**
     * Пересжимает все существующие обложки для уменьшения размера
     * @return Pair<Int, Int> - (успешно пересжато, всего обработано)
     */
    fun recompressAllCovers(context: Context): Pair<Int, Int> {
        var successCount = 0
        var totalCount = 0

        try {
            val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
            val coversDir = File(baseDir, "covers")

            if (!coversDir.exists() || !coversDir.isDirectory) {
                return Pair(0, 0)
            }

            val coverFiles = coversDir.listFiles { _, name ->
                name.endsWith(".jpg", ignoreCase = true) ||
                        name.endsWith(".jpeg", ignoreCase = true) ||
                        name.endsWith(".png", ignoreCase = true)
            }

            coverFiles?.forEach { coverFile ->
                totalCount++
                try {
                    val compressedImage = compressImageFile(coverFile, context)
                    if (compressedImage != null) {
                        // Сохраняем сжатое изображение обратно в тот же файл
                        FileOutputStream(coverFile).use { outputStream ->
                            outputStream.write(compressedImage)
                        }
                        successCount++
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Pair(successCount, totalCount)
    }

    /**
     * Сжимает изображение из файла
     */
    private fun compressImageFile(
        imageFile: File,
        context: Context,
        maxWidth: Int = 1024,
        maxHeight: Int = 1024,
        quality: Int = 85
    ): ByteArray? {
        return try {
            if (!imageFile.exists()) return null

            // Загружаем изображение с уменьшенным разрешением для экономии памяти
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(imageFile.absolutePath, options)

            // Вычисляем коэффициент масштабирования
            val scale = minOf(
                maxWidth.toFloat() / options.outWidth,
                maxHeight.toFloat() / options.outHeight
            ).coerceAtMost(1f)

            // Загружаем изображение с нужным масштабом
            val scaledOptions = BitmapFactory.Options().apply {
                inSampleSize = if (scale < 1f) {
                    var sampleSize = (1f / scale).toInt()
                    // Округляем до ближайшей степени двойки (требование Android)
                    var rounded = 1
                    while (rounded * 2 <= sampleSize) {
                        rounded *= 2
                    }
                    rounded
                } else {
                    1
                }
            }

            var bitmap = BitmapFactory.decodeFile(imageFile.absolutePath, scaledOptions)
            if (bitmap == null) return null

            // Дополнительное масштабирование, если нужно
            if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
                val width = bitmap.width
                val height = bitmap.height
                val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
                val newWidth = (width * ratio).toInt()
                val newHeight = (height * ratio).toInt()

                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            }

            // Сжимаем в JPEG
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            bitmap.recycle()

            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Сжимает и масштабирует изображение для уменьшения размера файла
     */
    private fun compressImage(
        uri: Uri,
        context: Context,
        maxWidth: Int = 1024,
        maxHeight: Int = 1024,
        quality: Int = 85
    ): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            // Загружаем изображение с уменьшенным разрешением для экономии памяти
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // Вычисляем коэффициент масштабирования
            val scale = minOf(
                maxWidth.toFloat() / options.outWidth,
                maxHeight.toFloat() / options.outHeight
            ).coerceAtMost(1f)

            // Загружаем изображение с нужным масштабом
            val scaledOptions = BitmapFactory.Options().apply {
                inSampleSize = if (scale < 1f) {
                    var sampleSize = (1f / scale).toInt()
                    // Округляем до ближайшей степени двойки (требование Android)
                    var rounded = 1
                    while (rounded * 2 <= sampleSize) {
                        rounded *= 2
                    }
                    rounded
                } else {
                    1
                }
            }

            val inputStream2 = context.contentResolver.openInputStream(uri) ?: return null
            var bitmap = BitmapFactory.decodeStream(inputStream2, null, scaledOptions)
            inputStream2.close()

            if (bitmap == null) return null

            // Дополнительное масштабирование, если нужно
            if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
                val width = bitmap.width
                val height = bitmap.height
                val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
                val newWidth = (width * ratio).toInt()
                val newHeight = (height * ratio).toInt()

                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            }

            // Сжимаем в JPEG
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            bitmap.recycle()

            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        const val LARGE_ACTIVITY_DELTA_THRESHOLD = 50

        fun generateId(): String = UUID.randomUUID().toString()
    }
}