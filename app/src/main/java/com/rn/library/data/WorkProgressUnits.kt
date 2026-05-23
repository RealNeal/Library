package com.rn.library.data

/** Сумма прогресса по томам/сезонам, если включён поштучный учёт. */
fun Work.sumUnitProgressOrNull(): Double? =
    unitProgress.takeIf { it.isNotEmpty() }?.sumOf { it.completed }

/** Единицы прогресса «чтения» (главы книг / манги) для статистики. */
fun readProgressUnits(work: Work): Double {
    if (work.unitProgress.isNotEmpty()) {
        if (work.type !in setOf(WorkType.BOOK, WorkType.MANGA)) return 0.0
        return work.progress ?: work.unitProgress.sumOf { it.completed }
    }
    return when (work.type) {
        WorkType.BOOK -> {
            when {
                work.progress != null -> work.progress
                work.status == WorkStatus.READ -> work.bookChapters ?: work.chapters ?: 0.0
                work.status == WorkStatus.ABANDONED -> work.abandonedProgress?.toDouble() ?: 0.0
                else -> 0.0
            }
        }
        WorkType.MANGA -> {
            when {
                work.progress != null -> work.progress
                work.status == WorkStatus.READ -> work.chapters ?: 0.0
                work.status == WorkStatus.ABANDONED -> work.abandonedProgress?.toDouble() ?: 0.0
                else -> 0.0
            }
        }
        else -> 0.0
    }
}

/** Единицы прогресса «просмотра» (серии) для статистики. */
fun watchedProgressUnits(work: Work): Double {
    if (work.unitProgress.isNotEmpty()) {
        if (work.type !in setOf(WorkType.ANIME, WorkType.SERIES)) return 0.0
        return work.progress ?: work.unitProgress.sumOf { it.completed }
    }
    return when (work.type) {
        WorkType.ANIME, WorkType.SERIES -> {
            when {
                work.progress != null -> work.progress
                work.status == WorkStatus.WATCHED -> work.episodes ?: 0.0
                work.status == WorkStatus.ABANDONED -> work.abandonedProgress?.toDouble() ?: 0.0
                else -> 0.0
            }
        }
        else -> 0.0
    }
}
