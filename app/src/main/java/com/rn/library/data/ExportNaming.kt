package com.rn.library.data

/**
 * `WorkRepository.kt` ожидает эту функцию как top-level в пакете `com.rn.library.data`.
 * Не меняем сам `WorkRepository.kt`, просто добавляем недостающий хелпер.
 */
fun uniqueExportBase(title: String, id: String, usedExportNames: MutableSet<String>): String {
    val base = title.trim().ifBlank { "work" }
        .replace(Regex("""[\\/:*?"<>|]"""), "_")
        .replace(Regex("""\s+"""), " ")
        .trim()
        .take(60)
        .ifBlank { "work" }

    var candidate = base
    var i = 2
    while (!usedExportNames.add(candidate)) {
        candidate = "$base ($i)"
        i++
        if (i > 9999) {
            candidate = "$base-$id"
            usedExportNames.add(candidate)
            break
        }
    }
    return candidate
}

/** Имя файла обложки при экспорте: первая — `base.ext`, остальные — `base_2.ext`, `base_3.ext`, … */
fun exportCoverBaseName(exportBase: String, index: Int): String =
    if (index == 0) exportBase else "${exportBase}_${index + 1}"

/** Индекс обложки (0-based) по имени файла без расширения относительно базы экспорта. */
fun parseExportCoverIndex(exportBase: String, fileBase: String): Int? {
    if (fileBase.equals(exportBase, ignoreCase = true)) return 0
    val escapedBase = Regex.escape(exportBase)
    val suffixPattern = Regex("^${escapedBase}_(\\d+)$", RegexOption.IGNORE_CASE)
    val suffixMatch = suffixPattern.matchEntire(fileBase) ?: return null
    val number = suffixMatch.groupValues[1].toIntOrNull() ?: return null
    return if (number >= 2) number - 1 else null
}

/** Все известные имена папок обложек по типу произведения (EN + RU). */
fun knownCoverFolderNames(type: WorkType): List<String> = when (type) {
    WorkType.BOOK -> listOf("Book covers", "Обложки книг")
    WorkType.ANIME -> listOf("Anime covers", "Обложки аниме")
    WorkType.MANGA -> listOf("Manga covers", "Обложки манги")
    WorkType.SERIES -> listOf("Series covers", "Обложки сериалов")
}

fun isInKnownCoverFolder(relativePath: String, type: WorkType): Boolean {
    if (relativePath.isBlank()) return true
    return knownCoverFolderNames(type).any { folder ->
        relativePath.equals(folder, ignoreCase = true) ||
            relativePath.startsWith("$folder/", ignoreCase = true)
    }
}

