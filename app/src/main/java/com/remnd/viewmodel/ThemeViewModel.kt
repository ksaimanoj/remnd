package com.remnd.viewmodel

import androidx.lifecycle.ViewModel
import com.remnd.data.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeRepository: ThemeRepository
) : ViewModel() {

    val themeHue: StateFlow<Float> = themeRepository.themeHue

    fun setThemeHue(hue: Float) {
        themeRepository.setThemeHue(hue)
    }
}
