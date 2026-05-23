package com.rn.library.data

import android.content.Context
import androidx.core.content.edit
import java.io.File
import java.time.LocalDate

/**
 * Минимальная реализация лога активности, достаточная для компиляции и базовой работы.
 * Хранит события в SharedPreferences как строки.
 */
class ActivityDeltaLog(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun appendEvent(event: ActivityDeltaEvent) {
        val key = KEY_EVENTS
        val current = prefs.getStringSet(key, emptySet()).orEmpty().toMutableSet()
        current.add(encode(event))
        prefs.edit { putStringSet(key, current) }
    }

    fun loadEvents(): List<ActivityDeltaEvent> {
        return prefs.getStringSet(KEY_EVENTS, emptySet()).orEmpty()
            .mapNotNull { decode(it) }
            .sortedBy { it.date }
    }

    fun mergeEvents(newEvents: List<ActivityDeltaEvent>) {
        if (newEvents.isEmpty()) return
        val current = prefs.getStringSet(KEY_EVENTS, emptySet()).orEmpty().toMutableSet()
        newEvents.forEach { current.add(encode(it)) }
        prefs.edit { putStringSet(KEY_EVENTS, current) }
    }

    fun replaceAllEvents(events: List<ActivityDeltaEvent>) {
        val encoded = events.map { encode(it) }.toSet()
        prefs.edit { putStringSet(KEY_EVENTS, encoded) }
    }

    fun exportToFile(file: File) {
        val events = loadEvents()
        file.parentFile?.mkdirs()
        file.writeText(ActivityStatisticsFormat.buildExportText(events))
    }

    fun importFromFile(file: File): Int {
        if (!file.exists() || !file.isFile) return 0
        val text = file.readText()
        val events = ActivityStatisticsFormat.parseImportText(text)
        if (text.contains("format=${ActivityStatisticsFormat.FORMAT_VERSION}") ||
            text.contains("[Heatmap]")
        ) {
            replaceAllEvents(events)
        } else {
            mergeEvents(events)
        }
        return events.size
    }

    private fun encode(e: ActivityDeltaEvent): String =
        listOf(e.date.toString(), e.workId, e.readDelta.toString(), e.watchDelta.toString()).joinToString("|")

    private fun decode(raw: String): ActivityDeltaEvent? {
        val parts = raw.split('|')
        if (parts.size != 4) return null
        return runCatching {
            ActivityDeltaEvent(
                date = LocalDate.parse(parts[0]),
                workId = parts[1],
                readDelta = parts[2].toDouble(),
                watchDelta = parts[3].toDouble()
            )
        }.getOrNull()
    }

    companion object {
        const val EXPORT_FILENAME = "activity_statistics.txt"
        private const val PREFS = "activity_delta_log"
        private const val KEY_EVENTS = "events"
    }
}

