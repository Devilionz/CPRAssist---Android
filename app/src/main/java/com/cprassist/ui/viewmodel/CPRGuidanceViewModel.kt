package com.cprassist.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cprassist.data.models.CPRGuidanceState
import com.cprassist.data.models.CPRGuidanceUIState
import com.cprassist.data.models.CPRPhase
import com.cprassist.data.models.MetronomeConfig
import com.cprassist.services.MetronomeService
import com.cprassist.services.TTSManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel for CPR Guidance Mode.
 * Provides step-by-step guidance for untrained bystanders.
 */
class CPRGuidanceViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // TTS Manager
    private val ttsManager = TTSManager(context)

    // Metronome service
    private var metronomeService: MetronomeService? = null
    private var isServiceBound = false

    // State
    private val _uiState = MutableStateFlow(CPRGuidanceUIState())
    val uiState: StateFlow<CPRGuidanceUIState> = _uiState.asStateFlow()

    private val _state = MutableStateFlow<CPRGuidanceState>(CPRGuidanceState.Idle)
    val state: StateFlow<CPRGuidanceState> = _state.asStateFlow()

    // Timers
    private var timerJob: Job? = null
    private var encouragementJob: Job? = null
    private var switchRescuerJob: Job? = null

    // Service connection
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MetronomeService.MetronomeBinder
            metronomeService = binder.getService()
            isServiceBound = true

            viewModelScope.launch {
                metronomeService?.isRunning?.collect { isRunning ->
                    _uiState.update { it.copy(isMetronomeRunning = isRunning) }
                }
            }

            viewModelScope.launch {
                metronomeService?.beatCount?.collect { count ->
                    _uiState.update { it.copy(compressionCount = count.toInt()) }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            metronomeService = null
            isServiceBound = false
        }
    }

    companion object {
        private const val SWITCH_RESCUER_INTERVAL_MS = 120_000L // 2 minutes
        private const val ENCOURAGEMENT_INTERVAL_MS = 30_000L // 30 seconds
        private const val INITIAL_GUIDANCE_DELAY_MS = 2000L
    }

    init {
        bindMetronomeService()
    }

    private fun bindMetronomeService() {
        val intent = MetronomeService.createIntent(context)
        context.startForegroundService(intent)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    /**
     * Start CPR guidance
     */
    fun startGuidance() {
        val startTime = System.currentTimeMillis()

        _state.value = CPRGuidanceState.Active(
            startTime = startTime,
            currentPhase = CPRPhase.CALL_EMERGENCY
        )

        _uiState.update {
            it.copy(
                state = _state.value,
                currentInstruction = CPRPhase.CALL_EMERGENCY.instruction
            )
        }

        // Start TTS guidance
        ttsManager.speakInitialGuidance()

        // Start timer
        startTimerLoop()

        // Start the guided sequence
        startGuidedSequence()
    }

    /**
     * Guide through the CPR steps with TTS
     */
    private fun startGuidedSequence() {
        viewModelScope.launch {
            // Wait for "Call 911" message
            delay(INITIAL_GUIDANCE_DELAY_MS)

            // Move to compressions
            updatePhase(CPRPhase.START_COMPRESSIONS)
            ttsManager.speakStartCompressions()

            delay(5000) // Wait for instructions

            // Start compressions with metronome
            startCompressions()
        }
    }

    /**
     * Start compressions with metronome
     */
    private fun startCompressions() {
        updatePhase(CPRPhase.COMPRESSIONS_ACTIVE)

        // Start metronome
        val config = MetronomeConfig(
            bpm = 110,
            useSound = true,
            useVibration = true
        )
        _uiState.update { it.copy(metronomeConfig = config) }
        metronomeService?.start(config)

        // Start switch rescuer timer
        startSwitchRescuerTimer()

        // Start periodic encouragement
        startEncouragementTimer()
    }

    /**
     * Update the current phase
     */
    private fun updatePhase(phase: CPRPhase) {
        val currentState = _state.value
        if (currentState is CPRGuidanceState.Active) {
            _state.value = currentState.copy(currentPhase = phase)
            _uiState.update {
                it.copy(
                    state = _state.value,
                    currentInstruction = phase.instruction
                )
            }
        }
    }

    /**
     * Start the elapsed time timer
     */
    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val currentState = _state.value
                if (currentState is CPRGuidanceState.Active) {
                    val elapsed = currentState.getElapsedTime()
                    val seconds = (elapsed / 1000) % 60
                    val minutes = (elapsed / 1000) / 60
                    val formatted = String.format("%02d:%02d", minutes, seconds)
                    _uiState.update { it.copy(formattedElapsedTime = formatted) }
                }
                delay(1000)
            }
        }
    }

    /**
     * Timer for 2-minute switch rescuer alerts
     */
    private fun startSwitchRescuerTimer() {
        switchRescuerJob?.cancel()
        switchRescuerJob = viewModelScope.launch {
            var cycleCount = 1
            while (isActive) {
                delay(SWITCH_RESCUER_INTERVAL_MS)

                // Show switch alert
                _uiState.update { it.copy(showSwitchAlert = true) }
                ttsManager.speakSwitchRescuer()

                // Hide alert after 10 seconds
                delay(10_000)
                _uiState.update { it.copy(showSwitchAlert = false) }

                cycleCount++
            }
        }
    }

    /**
     * Timer for periodic encouragement
     */
    private fun startEncouragementTimer() {
        encouragementJob?.cancel()
        encouragementJob = viewModelScope.launch {
            while (isActive) {
                delay(ENCOURAGEMENT_INTERVAL_MS)
                ttsManager.speakEncouragement()
            }
        }
    }

    /**
     * User acknowledged switch rescuer alert
     */
    fun acknowledgeSwitch() {
        _uiState.update { it.copy(showSwitchAlert = false) }
    }

    /**
     * Show AED instructions
     */
    fun showAEDInstructions() {
        updatePhase(CPRPhase.AED_INSTRUCTION)
        ttsManager.speak(TTSManager.Prompts.AED_ARRIVED, com.cprassist.services.TTSPriority.HIGH)
    }

    /**
     * Resume compressions after AED
     */
    fun resumeAfterAED() {
        updatePhase(CPRPhase.COMPRESSIONS_ACTIVE)
        ttsManager.speak(TTSManager.Prompts.CONTINUE_CPR, com.cprassist.services.TTSPriority.HIGH)
    }

    /**
     * Pause guidance
     */
    fun pause() {
        metronomeService?.stop()
        _state.value = CPRGuidanceState.Paused
        switchRescuerJob?.cancel()
        encouragementJob?.cancel()
    }

    /**
     * Resume guidance
     */
    fun resume() {
        val startTime = System.currentTimeMillis()
        _state.value = CPRGuidanceState.Active(
            startTime = startTime,
            currentPhase = CPRPhase.COMPRESSIONS_ACTIVE,
            isMetronomeRunning = true
        )
        _uiState.update {
            it.copy(
                state = _state.value,
                currentInstruction = CPRPhase.COMPRESSIONS_ACTIVE.instruction
            )
        }

        metronomeService?.start(_uiState.value.metronomeConfig)
        startSwitchRescuerTimer()
        startEncouragementTimer()
    }

    /**
     * Stop all guidance
     */
    fun stop() {
        timerJob?.cancel()
        switchRescuerJob?.cancel()
        encouragementJob?.cancel()

        metronomeService?.stop()
        ttsManager.stopSpeaking()

        _state.value = CPRGuidanceState.Idle
        _uiState.value = CPRGuidanceUIState()
    }

    /**
     * Update metronome BPM
     */
    fun updateBPM(bpm: Int) {
        val newConfig = _uiState.value.metronomeConfig.copy(bpm = bpm)
        _uiState.update { it.copy(metronomeConfig = newConfig) }
        metronomeService?.updateConfig(newConfig)
    }

    /**
     * Toggle vibration feedback
     */
    fun toggleVibration(enabled: Boolean) {
        val newConfig = _uiState.value.metronomeConfig.copy(useVibration = enabled)
        _uiState.update { it.copy(metronomeConfig = newConfig) }
        metronomeService?.updateConfig(newConfig)
    }

    /**
     * Toggle sound
     */
    fun toggleSound(enabled: Boolean) {
        val newConfig = _uiState.value.metronomeConfig.copy(useSound = enabled)
        _uiState.update { it.copy(metronomeConfig = newConfig) }
        metronomeService?.updateConfig(newConfig)
    }

    override fun onCleared() {
        super.onCleared()

        if (isServiceBound) {
            context.unbindService(serviceConnection)
            isServiceBound = false
        }

        timerJob?.cancel()
        switchRescuerJob?.cancel()
        encouragementJob?.cancel()

        ttsManager.shutdown()
    }
}
