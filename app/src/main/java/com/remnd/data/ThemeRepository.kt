package com.remnd.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("remnd_theme", Context.MODE_PRIVATE)

    private val _themeHue = MutableStateFlow(prefs.getFloat("theme_hue", 264f))
    val themeHue: StateFlow<Float> = _themeHue.asStateFlow()

    fun setThemeHue(hue: Float) {
        _themeHue.value = hue
        prefs.edit().putFloat("theme_hue", hue).apply()
    }
}
