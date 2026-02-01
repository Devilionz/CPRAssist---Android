package com.cprassist.wear

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * UI state for the Wear app
 */
data class WearUIState(
    val isActive: Boolean = false,
    val formattedTime: String = "00:00",
    val cycleNumber: Int = 1,
    val bpm: Int = 110,
    val elapsedMs: Long = 0
)

/**
 * ViewModel for WearOS CPR companion app
 */
class WearViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private var vibrator: Vibrator? = null

    private val _uiState = MutableStateFlow(WearUIState())
    val uiState: StateFlow<WearUIState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var metronomeJob: Job? = null
    private var cycleAlertJob: Job? = null

    private var startTime: Long = 0L

    companion object {
        private const val CYCLE_DURATION_MS = 120_000L // 2 minutes
    }

    init {
        initializeVibrator()
    }

    private fun initializeVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /**
     * Start the metronome and timer
     */
    fun startMetronome() {
        if (_uiState.value.isActive) return

        startTime = System.currentTimeMillis()
        _uiState.update { it.copy(isActive = true, cycleNumber = 1, elapsedMs = 0) }

        // Start timer
        timerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                _uiState.update {
                    it.copy(
                        elapsedMs = elapsed,
                        formattedTime = formatDuration(elapsed)
                    )
                }
                delay(100)
            }
        }

        // Start metronome vibration
        metronomeJob = viewModelScope.launch {
            val intervalMs = 60_000L / _uiState.value.bpm
            while (isActive) {
                vibrateMetronome()
                delay(intervalMs)
            }
        }

        // Start cycle alerts
        cycleAlertJob = viewModelScope.launch {
            var cycleCount = 1
            while (isActive) {
                delay(CYCLE_DURATION_MS)
                cycleCount++
                _uiState.update { it.copy(cycleNumber = cycleCount) }
                vibrateCycleAlert()
            }
        }
    }

    /**
     * Stop the metronome and timer
     */
    fun stopMetronome() {
        timerJob?.cancel()
        metronomeJob?.cancel()
        cycleAlertJob?.cancel()

        _uiState.update {
            WearUIState() // Reset to default
        }
    }

    /**
     * Vibrate for metronome beat
     */
    private fun vibrateMetronome() {
        try {
            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(
                        VibrationEffect.createOneShot(
                            30L,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(30L)
                }
            }
        } catch (e: Exception) {
            // Fail silently
        }
    }

    /**
     * Vibrate for cycle alert (more noticeable)
     */
    private fun vibrateCycleAlert() {
        try {
            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 200, 100, 200, 100, 200)
                    v.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(longArrayOf(0, 200, 100, 200, 100, 200), -1)
                }
            }
        } catch (e: Exception) {
            // Fail silently
        }
    }

    /**
     * Update BPM
     */
    fun setBpm(bpm: Int) {
        _uiState.update { it.copy(bpm = bpm.coerceIn(100, 120)) }

        // Restart metronome with new BPM if active
        if (_uiState.value.isActive) {
            metronomeJob?.cancel()
            metronomeJob = viewModelScope.launch {
                val intervalMs = 60_000L / _uiState.value.bpm
                while (isActive) {
                    vibrateMetronome()
                    delay(intervalMs)
                }
            }
        }
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        metronomeJob?.cancel()
        cycleAlertJob?.cancel()
    }
}
