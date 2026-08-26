package com.rn.library.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberLanguageState(): LanguageState {
    val context = LocalContext.current
    return remember(context) { LanguageState(context) }
}
