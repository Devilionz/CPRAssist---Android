package com.cprassist.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension to create DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cpr_assist_settings")

/**
 * Settings data class containing all user preferences
 */
data class AppSettings(
    // Metronome settings
    val defaultBpm: Int = 110,
    val metronomeVolume: Int = 100,
    val useMetronomeSound: Boolean = true,
    val useMetronomeVibration: Boolean = true,

    // TTS settings
    val ttsEnabled: Boolean = true,
    val ttsSpeechRate: Float = 0.9f,
    val ttsVolume: Int = 100,

    // Alert settings
    val cycleAlertEnabled: Boolean = true,
    val cycleAlertVolume: Int = 100,
    val cycleIntervalMinutes: Int = 2,

    // Display settings
    val keepScreenOn: Boolean = true,
    val useDarkTheme: Boolean = true,
    val showCompressionCount: Boolean = true,

    // Professional mode settings
    val autoStartMetronome: Boolean = true,
    val confirmEventLogging: Boolean = false,

    // Onboarding
    val hasCompletedOnboarding: Boolean = false,
    val hasAcceptedDisclaimer: Boolean = false
)

/**
 * DataStore manager for persisting user settings
 */
class SettingsDataStore(private val context: Context) {

    private object PreferencesKeys {
        // Metronome
        val DEFAULT_BPM = intPreferencesKey("default_bpm")
        val METRONOME_VOLUME = intPreferencesKey("metronome_volume")
        val USE_METRONOME_SOUND = booleanPreferencesKey("use_metronome_sound")
        val USE_METRONOME_VIBRATION = booleanPreferencesKey("use_metronome_vibration")

        // TTS
        val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val TTS_SPEECH_RATE = floatPreferencesKey("tts_speech_rate")
        val TTS_VOLUME = intPreferencesKey("tts_volume")

        // Alerts
        val CYCLE_ALERT_ENABLED = booleanPreferencesKey("cycle_alert_enabled")
        val CYCLE_ALERT_VOLUME = intPreferencesKey("cycle_alert_volume")
        val CYCLE_INTERVAL_MINUTES = intPreferencesKey("cycle_interval_minutes")

        // Display
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val USE_DARK_THEME = booleanPreferencesKey("use_dark_theme")
        val SHOW_COMPRESSION_COUNT = booleanPreferencesKey("show_compression_count")

        // Professional mode
        val AUTO_START_METRONOME = booleanPreferencesKey("auto_start_metronome")
        val CONFIRM_EVENT_LOGGING = booleanPreferencesKey("confirm_event_logging")

        // Onboarding
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val HAS_ACCEPTED_DISCLAIMER = booleanPreferencesKey("has_accepted_disclaimer")
    }

    /**
     * Flow of current settings
     */
    val settings: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            defaultBpm = preferences[PreferencesKeys.DEFAULT_BPM] ?: 110,
            metronomeVolume = preferences[PreferencesKeys.METRONOME_VOLUME] ?: 100,
            useMetronomeSound = preferences[PreferencesKeys.USE_METRONOME_SOUND] ?: true,
            useMetronomeVibration = preferences[PreferencesKeys.USE_METRONOME_VIBRATION] ?: true,

            ttsEnabled = preferences[PreferencesKeys.TTS_ENABLED] ?: true,
            ttsSpeechRate = preferences[PreferencesKeys.TTS_SPEECH_RATE] ?: 0.9f,
            ttsVolume = preferences[PreferencesKeys.TTS_VOLUME] ?: 100,

            cycleAlertEnabled = preferences[PreferencesKeys.CYCLE_ALERT_ENABLED] ?: true,
            cycleAlertVolume = preferences[PreferencesKeys.CYCLE_ALERT_VOLUME] ?: 100,
            cycleIntervalMinutes = preferences[PreferencesKeys.CYCLE_INTERVAL_MINUTES] ?: 2,

            keepScreenOn = preferences[PreferencesKeys.KEEP_SCREEN_ON] ?: true,
            useDarkTheme = preferences[PreferencesKeys.USE_DARK_THEME] ?: true,
            showCompressionCount = preferences[PreferencesKeys.SHOW_COMPRESSION_COUNT] ?: true,

            autoStartMetronome = preferences[PreferencesKeys.AUTO_START_METRONOME] ?: true,
            confirmEventLogging = preferences[PreferencesKeys.CONFIRM_EVENT_LOGGING] ?: false,

            hasCompletedOnboarding = preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false,
            hasAcceptedDisclaimer = preferences[PreferencesKeys.HAS_ACCEPTED_DISCLAIMER] ?: false
        )
    }

    // Metronome settings
    suspend fun setDefaultBpm(bpm: Int) {
        context.dataStore.edit { it[PreferencesKeys.DEFAULT_BPM] = bpm.coerceIn(60, 180) }
    }

    suspend fun setMetronomeVolume(volume: Int) {
        context.dataStore.edit { it[PreferencesKeys.METRONOME_VOLUME] = volume.coerceIn(0, 100) }
    }

    suspend fun setUseMetronomeSound(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.USE_METRONOME_SOUND] = enabled }
    }

    suspend fun setUseMetronomeVibration(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.USE_METRONOME_VIBRATION] = enabled }
    }

    // TTS settings
    suspend fun setTtsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.TTS_ENABLED] = enabled }
    }

    suspend fun setTtsSpeechRate(rate: Float) {
        context.dataStore.edit { it[PreferencesKeys.TTS_SPEECH_RATE] = rate.coerceIn(0.5f, 2.0f) }
    }

    suspend fun setTtsVolume(volume: Int) {
        context.dataStore.edit { it[PreferencesKeys.TTS_VOLUME] = volume.coerceIn(0, 100) }
    }

    // Alert settings
    suspend fun setCycleAlertEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.CYCLE_ALERT_ENABLED] = enabled }
    }

    suspend fun setCycleAlertVolume(volume: Int) {
        context.dataStore.edit { it[PreferencesKeys.CYCLE_ALERT_VOLUME] = volume.coerceIn(0, 100) }
    }

    suspend fun setCycleIntervalMinutes(minutes: Int) {
        context.dataStore.edit { it[PreferencesKeys.CYCLE_INTERVAL_MINUTES] = minutes.coerceIn(1, 5) }
    }

    // Display settings
    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.KEEP_SCREEN_ON] = enabled }
    }

    suspend fun setUseDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.USE_DARK_THEME] = enabled }
    }

    suspend fun setShowCompressionCount(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHOW_COMPRESSION_COUNT] = enabled }
    }

    // Professional mode settings
    suspend fun setAutoStartMetronome(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_START_METRONOME] = enabled }
    }

    suspend fun setConfirmEventLogging(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.CONFIRM_EVENT_LOGGING] = enabled }
    }

    // Onboarding
    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = completed }
    }

    suspend fun setHasAcceptedDisclaimer(accepted: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.HAS_ACCEPTED_DISCLAIMER] = accepted }
    }

    /**
     * Reset all settings to defaults
     */
    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
