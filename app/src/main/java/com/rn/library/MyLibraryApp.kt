package com.rn.library

import android.app.Application
import com.rn.library.ui.LanguageState
import com.rn.library.update.UpdateNotifier

class MyLibraryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LanguageState.applyStoredLanguage(this)
        UpdateNotifier.ensureChannel(this)
    }
}
