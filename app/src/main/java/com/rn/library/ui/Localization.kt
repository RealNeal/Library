package com.rn.library.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

private class LocalizedContextWrapper(
    base: Context,
    language: Language
) : ContextWrapper(base) {
    private val localizedContext = base.createConfigurationContext(
        Configuration(base.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(language.languageTag))
        }
    )

    override fun getResources(): Resources = localizedContext.resources
    override fun getTheme() = localizedContext.theme
}

@Composable
fun rememberLanguageState(): LanguageState {
    val context = LocalContext.current
    return remember(context) { LanguageState(context) }
}

/** Applies an app language immediately, without recreating the Activity. */
@Composable
fun ProvideAppLanguage(language: Language, content: @Composable () -> Unit) {
    val baseContext = LocalContext.current
    val localizedContext = remember(baseContext, language) {
        LocalizedContextWrapper(baseContext, language)
    }
    CompositionLocalProvider(LocalContext provides localizedContext, content = content)
}
