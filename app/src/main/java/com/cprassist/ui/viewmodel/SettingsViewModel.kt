package com.cprassist.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cprassist.data.preferences.AppSettings
import com.cprassist.data.preferences.SettingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for Settings screen
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application.applicationContext)

    val settings: StateFlow<AppSettings> = settingsDataStore.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    // Metronome settings
    fun setDefaultBpm(bpm: Int) {
        viewModelScope.launch {
            settingsDataStore.setDefaultBpm(bpm)
        }
    }

    fun setMetronomeVolume(volume: Int) {
        viewModelScope.launch {
            settingsDataStore.setMetronomeVolume(volume)
        }
    }

    fun setUseMetronomeSound(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setUseMetronomeSound(enabled)
        }
    }

    fun setUseMetronomeVibration(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setUseMetronomeVibration(enabled)
        }
    }

    // TTS settings
    fun setTtsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setTtsEnabled(enabled)
        }
    }

    fun setTtsSpeechRate(rate: Float) {
        viewModelScope.launch {
            settingsDataStore.setTtsSpeechRate(rate)
        }
    }

    fun setTtsVolume(volume: Int) {
        viewModelScope.launch {
            settingsDataStore.setTtsVolume(volume)
        }
    }

    // Alert settings
    fun setCycleAlertEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setCycleAlertEnabled(enabled)
        }
    }

    fun setCycleAlertVolume(volume: Int) {
        viewModelScope.launch {
            settingsDataStore.setCycleAlertVolume(volume)
        }
    }

    fun setCycleIntervalMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsDataStore.setCycleIntervalMinutes(minutes)
        }
    }

    // Display settings
    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setKeepScreenOn(enabled)
        }
    }

    fun setUseDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setUseDarkTheme(enabled)
        }
    }

    fun setShowCompressionCount(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setShowCompressionCount(enabled)
        }
    }

    // Professional mode settings
    fun setAutoStartMetronome(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setAutoStartMetronome(enabled)
        }
    }

    fun setConfirmEventLogging(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setConfirmEventLogging(enabled)
        }
    }

    // Onboarding
    fun completeOnboarding() {
        viewModelScope.launch {
            settingsDataStore.setHasCompletedOnboarding(true)
        }
    }

    fun acceptDisclaimer() {
        viewModelScope.launch {
            settingsDataStore.setHasAcceptedDisclaimer(true)
        }
    }

    // Reset
    fun resetToDefaults() {
        viewModelScope.launch {
            settingsDataStore.resetToDefaults()
        }
    }
}
