package com.rn.library.ui.screens

/** YYYY-MM-DD → ДД.ММ.ГГГГ */
fun formatStorageDateToDisplay(date: String): String {
    val parts = date.split("-")
    return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else date
}

/** Список дат хранения → текст для многострочного поля (по одной дате в строке). */
fun formatRereadDatesForDisplay(dates: List<String>): String =
    dates.joinToString("\n") { formatStorageDateToDisplay(it) }

/** Одна строка (ДД.ММ.ГГГГ или DDMMYYYY) → YYYY-MM-DD или null. */
fun parseDateLineToStorage(line: String): String? {
    val digits = line.filter { it.isDigit() }
    if (digits.length < 8) return null
    val day = digits.substring(0, 2)
    val month = digits.substring(2, 4)
    val year = digits.substring(4, 8)
    return "$year-$month-$day"
}

/** Многострочный ввод → список дат в формате YYYY-MM-DD. */
fun parseRereadDatesForSave(input: String): List<String> =
    input.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { parseDateLineToStorage(it) }
        .toList()

fun filterRereadDatesInput(input: String): String =
    input.filter { it.isDigit() || it == '.' || it == '\n' }
