package com.rn.library.ui

import android.content.Context
import androidx.core.content.edit
import com.rn.library.ui.theme.ThemePalette

/**
 * Общие настройки приложения (SharedPreferences).
 * Сейчас используется только шаг инкремента числовых полей.
 */
object AppSettings {

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_INCREMENT_STEP = "increment_step"
    private const val KEY_DYNAMIC_COLORS_ENABLED = "dynamic_colors_enabled"
    private const val KEY_THEME_PALETTE = "theme_palette"
    private const val KEY_USE_CUSTOM_ACCENT = "use_custom_accent"
    private const val KEY_CUSTOM_ACCENT_ARGB = "custom_accent_argb"
    private const val KEY_USE_CUSTOM_STATS_COLOR = "use_custom_stats_color"
    private const val KEY_CUSTOM_STATS_ARGB = "custom_stats_argb"

    /**
     * Возвращает шаг инкремента для числовых полей.
     * Минимальное значение шага — 1.
     */
    fun getIncrementStep(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getInt(KEY_INCREMENT_STEP, 1)
        return stored.coerceAtLeast(1)
    }

    /**
     * Сохраняет шаг инкремента для числовых полей.
     * Значение меньше 1 автоматически приводится к 1.
     */
    fun setIncrementStep(context: Context, step: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val safeStep = step.coerceAtLeast(1)
        prefs.edit {
            putInt(KEY_INCREMENT_STEP, safeStep)
        }
    }

    fun isDynamicColorsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DYNAMIC_COLORS_ENABLED, false)
    }

    fun setDynamicColorsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(KEY_DYNAMIC_COLORS_ENABLED, enabled)
        }
    }

    fun getThemePalette(context: Context): ThemePalette {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_THEME_PALETTE, ThemePalette.DEFAULT.name) ?: ThemePalette.DEFAULT.name
        return ThemePalette.entries.firstOrNull { it.name == raw } ?: ThemePalette.DEFAULT
    }

    fun setThemePalette(context: Context, palette: ThemePalette) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_THEME_PALETTE, palette.name)
        }
    }

    fun isUseCustomAccent(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_USE_CUSTOM_ACCENT, false)
    }

    fun setUseCustomAccent(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_USE_CUSTOM_ACCENT, enabled)
        }
    }

    /** ARGB; по умолчанию — фиолетовый акцент Material. */
    fun getCustomAccentArgb(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_CUSTOM_ACCENT_ARGB, 0xFF6750A4.toInt())
    }

    fun setCustomAccentArgb(context: Context, argb: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putInt(KEY_CUSTOM_ACCENT_ARGB, argb)
        }
    }

    fun isUseCustomStatsColor(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_USE_CUSTOM_STATS_COLOR, false)
    }

    fun setUseCustomStatsColor(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_USE_CUSTOM_STATS_COLOR, enabled)
        }
    }

    fun getCustomStatsArgb(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_CUSTOM_STATS_ARGB, 0xFF7C4DFF.toInt())
    }

    fun setCustomStatsArgb(context: Context, argb: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putInt(KEY_CUSTOM_STATS_ARGB, argb)
        }
    }
}

