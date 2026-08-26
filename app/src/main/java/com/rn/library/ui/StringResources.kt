package com.rn.library.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rn.library.R
import com.rn.library.data.MangaType
import com.rn.library.data.SeriesType
import com.rn.library.data.WorkStatus
import com.rn.library.data.WorkType

@Composable
fun workStatusLabel(status: WorkStatus): String = stringResource(
    when (status) {
        WorkStatus.IN_PLANS -> R.string.in_plans
        WorkStatus.ABANDONED -> R.string.abandoned
        WorkStatus.READING -> R.string.reading
        WorkStatus.WATCHING -> R.string.watching
        WorkStatus.READ -> R.string.read
        WorkStatus.WATCHED -> R.string.watched
    }
)

@Composable
fun seriesTypeLabel(seriesType: SeriesType?): String = when (seriesType) {
    SeriesType.TV_SERIES -> stringResource(R.string.tv_series)
    SeriesType.FILM -> stringResource(R.string.film)
    SeriesType.CARTOON -> stringResource(R.string.cartoon)
    SeriesType.DRAMA -> stringResource(R.string.drama)
    null -> ""
}

@Composable
fun mangaTypeLabel(mangaType: MangaType?): String = when (mangaType) {
    MangaType.MANGA -> stringResource(R.string.manga)
    MangaType.MANHWA -> stringResource(R.string.manhwa)
    MangaType.MANHUA -> stringResource(R.string.manhua)
    null -> ""
}

@Composable
fun workTypeLabel(type: WorkType): String = when (type) {
    WorkType.BOOK -> stringResource(R.string.books)
    WorkType.MANGA -> stringResource(R.string.manga)
    WorkType.ANIME -> stringResource(R.string.anime)
    WorkType.SERIES -> stringResource(R.string.tv_series)
}

@StringRes
fun workStatusLabelRes(status: WorkStatus): Int = when (status) {
    WorkStatus.IN_PLANS -> R.string.in_plans
    WorkStatus.ABANDONED -> R.string.abandoned
    WorkStatus.READING -> R.string.reading
    WorkStatus.WATCHING -> R.string.watching
    WorkStatus.READ -> R.string.read
    WorkStatus.WATCHED -> R.string.watched
}
