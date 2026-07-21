package com.rn.library.data

/** Сумма прогресса по томам/сезонам, если включён поштучный учёт. */
fun Work.sumUnitProgressOrNull(): Double? =
    unitProgress.takeIf { it.isNotEmpty() }?.sumOf { it.completed }

private fun Work.chapterUnitsFromFields(): Double? = when (type) {
    WorkType.BOOK -> bookChapters ?: chapters
    WorkType.MANGA -> chapters
    else -> null
}

private fun Work.episodeUnitsFromFields(): Double? = when (type) {
    WorkType.ANIME, WorkType.SERIES -> episodes
    else -> null
}

private fun Work.unitProgressTotalUnits(): Double? =
    unitProgress.takeIf { it.isNotEmpty() }?.sumOf { unit ->
        unit.total?.takeIf { it > 0.0 } ?: unit.completed
    }

/** Полный объём глав для книги/манги при статусе «Прочитано». */
fun fullReadChapterUnits(work: Work): Double {
    val fromFields = work.chapterUnitsFromFields()
    val fromUnits = work.unitProgressTotalUnits()
    return when {
        fromUnits != null && fromFields != null -> maxOf(fromUnits, fromFields)
        fromUnits != null -> maxOf(fromUnits, work.progress ?: 0.0)
        fromFields != null -> fromFields
        else -> work.progress ?: 0.0
    }
}

/** Полный объём серий для аниме/сериала при статусе «Просмотрено». */
fun fullWatchedEpisodeUnits(work: Work): Double {
    val fromFields = work.episodeUnitsFromFields()
    val fromUnits = work.unitProgressTotalUnits()
    return when {
        fromUnits != null && fromFields != null -> maxOf(fromUnits, fromFields)
        fromUnits != null -> maxOf(fromUnits, work.progress ?: 0.0)
        fromFields != null -> fromFields
        else -> work.progress ?: 0.0
    }
}

/**
 * Единицы прогресса «чтения» для журнала активности (heatmap / периоды).
 * Всегда отражает фактически прочитанное (completed), а не полный объём при статусе «Прочитано».
 */
fun activityReadProgressUnits(work: Work): Double {
    if (work.type !in setOf(WorkType.BOOK, WorkType.MANGA)) return 0.0
    if (work.status == WorkStatus.READ) return fullReadChapterUnits(work)
    if (work.unitProgress.isNotEmpty()) return work.unitProgress.sumOf { it.completed }
    return when {
        work.progress != null -> work.progress
        work.status == WorkStatus.ABANDONED -> work.abandonedProgress?.toDouble() ?: 0.0
        else -> 0.0
    }
}

/**
 * Единицы прогресса «просмотра» для журнала активности (heatmap / периоды).
 * Всегда отражает фактически просмотренное (completed), а не полный объём при статусе «Просмотрено».
 */
fun activityWatchedProgressUnits(work: Work): Double {
    if (work.type !in setOf(WorkType.ANIME, WorkType.SERIES)) return 0.0
    if (work.status == WorkStatus.WATCHED) return fullWatchedEpisodeUnits(work)
    if (work.unitProgress.isNotEmpty()) return work.unitProgress.sumOf { it.completed }
    return when {
        work.progress != null -> work.progress
        work.status == WorkStatus.ABANDONED -> work.abandonedProgress?.toDouble() ?: 0.0
        else -> 0.0
    }
}

/** Единицы прогресса «чтения» (главы книг / манги) для статистики. */
fun readProgressUnits(work: Work): Double {
    if (work.type !in setOf(WorkType.BOOK, WorkType.MANGA)) return 0.0
    if (work.status == WorkStatus.READ) return fullReadChapterUnits(work)
    if (work.unitProgress.isNotEmpty()) return work.unitProgress.sumOf { it.completed }
    return when (work.type) {
        WorkType.BOOK -> {
            when {
                work.progress != null -> work.progress
                work.status == WorkStatus.ABANDONED -> work.abandonedProgress?.toDouble() ?: 0.0
                else -> 0.0
            }
        }
        WorkType.MANGA -> {
            when {
                work.progress != null -> work.progress
                work.status == WorkStatus.ABANDONED -> work.abandonedProgress?.toDouble() ?: 0.0
                else -> 0.0
            }
        }
        else -> 0.0
    }
}

/** Единицы прогресса «просмотра» (серии) для статистики. */
fun watchedProgressUnits(work: Work): Double {
    if (work.type !in setOf(WorkType.ANIME, WorkType.SERIES)) return 0.0
    if (work.status == WorkStatus.WATCHED) return fullWatchedEpisodeUnits(work)
    if (work.unitProgress.isNotEmpty()) return work.unitProgress.sumOf { it.completed }
    return when (work.type) {
        WorkType.ANIME, WorkType.SERIES -> {
            when {
                work.progress != null -> work.progress
                work.status == WorkStatus.ABANDONED -> work.abandonedProgress?.toDouble() ?: 0.0
                else -> 0.0
            }
        }
        else -> 0.0
    }
}
