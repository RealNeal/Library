package com.rn.library.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

val LocalStrings: ProvidableCompositionLocal<Strings> = compositionLocalOf {
    error("No Strings provided")
}

@Composable
fun rememberLanguageState(): LanguageState {
    val context = LocalContext.current
    return remember(context) { LanguageState(context) }
}

