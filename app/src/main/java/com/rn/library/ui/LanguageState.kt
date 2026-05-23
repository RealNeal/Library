package com.rn.library.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class LanguageState(context: Context) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var currentLanguage: Language by mutableStateOf(
        runCatching { Language.valueOf(prefs.getString("language", Language.ENGLISH.name) ?: Language.ENGLISH.name) }
            .getOrDefault(Language.ENGLISH)
    )
        private set

    val strings: Strings
        get() = when (currentLanguage) {
            Language.ENGLISH -> LocalizedStrings.english
            Language.RUSSIAN -> LocalizedStrings.russian
        }

    fun setLanguage(language: Language) {
        currentLanguage = language
        prefs.edit().putString("language", language.name).apply()
    }
}

