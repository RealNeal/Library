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

