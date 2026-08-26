package com.rn.library.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat

class LanguageState(context: Context) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var currentLanguage: Language by mutableStateOf(loadLanguage())
        private set

    init {
        applyLanguage(currentLanguage)
    }

    fun setLanguage(language: Language) {
        if (currentLanguage == language) return
        currentLanguage = language
        prefs.edit().putString("language", language.name).apply()
        applyLanguage(language)
    }

    private fun loadLanguage(): Language =
        runCatching {
            Language.valueOf(prefs.getString("language", Language.ENGLISH.name) ?: Language.ENGLISH.name)
        }.getOrDefault(Language.ENGLISH)

    private fun applyLanguage(language: Language) {
        AppCompatDelegate.setApplicationLocales(
            when (language) {
                Language.ENGLISH -> LocaleListCompat.forLanguageTags("en")
                Language.RUSSIAN -> LocaleListCompat.forLanguageTags("ru")
            }
        )
    }
}
