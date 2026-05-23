package com.rn.library.data

import java.time.LocalDate

data class ActivityDeltaEvent(
    val date: LocalDate,
    val workId: String,
    val readDelta: Double,
    val watchDelta: Double
)

