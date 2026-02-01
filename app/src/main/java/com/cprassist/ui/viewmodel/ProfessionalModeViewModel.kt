package com.cprassist.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cprassist.data.models.ArrestState
import com.cprassist.data.models.EventType
import com.cprassist.data.models.MetronomeConfig
import com.cprassist.data.models.ProfessionalModeUIState
import com.cprassist.data.repository.EventRepository
import com.cprassist.services.ArrestTimerManager
import com.cprassist.services.ArrestTimerState
import com.cprassist.services.CycleAlertManager
import com.cprassist.services.MetronomeService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel for Professional Mode - Cardiac Arrest Timer.
 * Manages arrest state, event logging, timers, and metronome.
 *
 * Uses singleton EventRepository to ensure events persist across navigation.
 */
class ProfessionalModeViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // Timer managers
    private val timerManager = ArrestTimerManager()
    private val cycleAlertManager = CycleAlertManager(context)

    // Metronome service connection
    private var metronomeService: MetronomeService? = null
    private var isServiceBound = false

    // Timer update job for direct polling
    private var timerUpdateJob: Job? = null

    // UI State
    private val _uiState = MutableStateFlow(ProfessionalModeUIState())
    val uiState: StateFlow<ProfessionalModeUIState> = _uiState.asStateFlow()

    // Arrest state machine
    private val _arrestState = MutableStateFlow<ArrestState>(ArrestState.Idle)
    val arrestState: StateFlow<ArrestState> = _arrestState.asStateFlow()

    // Events from singleton repository
    val events = EventRepository.events
    val lastEvent = EventRepository.lastEvent

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
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            metronomeService = null
            isServiceBound = false
        }
    }

    init {
        bindMetronomeService()
        observeTimers()
        observeCycleAlerts()
        observeEvents()
    }

    private fun bindMetronomeService() {
        val intent = MetronomeService.createIntent(context)
        context.startForegroundService(intent)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun observeTimers() {
        // Start direct polling for timer updates - more reliable than StateFlow
        startTimerPolling()

        // Observe cycle timer
        viewModelScope.launch {
            cycleAlertManager.elapsedInCycleMs.collect { elapsed ->
                val seconds = (elapsed / 1000) % 60
                val minutes = (elapsed / 1000) / 60
                val formatted = String.format("%d:%02d", minutes, seconds)
                _uiState.update { it.copy(formattedCycleTime = formatted) }
            }
        }
    }

    /**
     * Start polling timer values directly - bypasses StateFlow for reliability
     */
    private fun startTimerPolling() {
        timerUpdateJob?.cancel()
        timerUpdateJob = viewModelScope.launch {
            while (isActive) {
                // Read current timer values directly
                val totalTime = timerManager.formattedTotalTime.value
                val episodeTime = timerManager.formattedEpisodeTime.value
                val roscTime = timerManager.formattedRoscTime.value

                // Update UI state
                _uiState.update {
                    it.copy(
                        formattedElapsedTime = totalTime,
                        formattedEpisodeTime = episodeTime,
                        formattedRoscTime = roscTime
                    )
                }

                delay(100) // Poll every 100ms
            }
        }
    }

    private fun observeCycleAlerts() {
        viewModelScope.launch {
            cycleAlertManager.cycleAlertTriggered.collect { cycle ->
                EventRepository.logCycleComplete()
                updateEventCount()
            }
        }

        viewModelScope.launch {
            cycleAlertManager.currentCycle.collect { cycle ->
                EventRepository.updateCycle(cycle)
                // Update UI state with current cycle
                val currentState = _arrestState.value
                if (currentState is ArrestState.Active) {
                    _arrestState.value = currentState.copy(currentCycle = cycle)
                    _uiState.update { it.copy(arrestState = _arrestState.value) }
                }
            }
        }
    }

    private fun observeEvents() {
        viewModelScope.launch {
            EventRepository.events.collect { eventList ->
                _uiState.update { it.copy(eventCount = eventList.size) }
            }
        }

        viewModelScope.launch {
            EventRepository.lastEvent.collect { event ->
                _uiState.update { it.copy(lastEvent = event) }
            }
        }
    }

    /**
     * Confirm cardiac arrest - starts all timers and metronome
     */
    fun confirmCardiacArrest() {
        val session = EventRepository.startNewSession()

        _arrestState.value = ArrestState.Active(session = session)
        _uiState.update {
            it.copy(
                arrestState = _arrestState.value,
                eventCount = EventRepository.events.value.size
            )
        }

        // Start timers
        timerManager.start()
        cycleAlertManager.start()

        // Start metronome
        metronomeService?.start(_uiState.value.metronomeConfig)
    }

    /**
     * Log an event (button press) - immediately creates timestamped entry
     */
    fun logEvent(eventType: EventType) {
        // Handle ROSC specially - it changes state machine
        if (eventType == EventType.ROSC) {
            handleROSC()
            return
        }

        // Log to singleton repository - this is immediate and thread-safe
        EventRepository.logEvent(eventType)
        updateEventCount()
    }

    /**
     * Handle ROSC - stops arrest timers, starts ROSC tracking
     */
    private fun handleROSC() {
        val currentState = _arrestState.value

        // Get session from current state
        val session = when (currentState) {
            is ArrestState.Active -> currentState.session
            is ArrestState.ROSC -> currentState.session
            else -> EventRepository.getCurrentSession()
        }

        if (session == null) return

        // Mark ROSC in repository (logs the event once)
        EventRepository.markROSC()
        updateEventCount()

        // Stop arrest timer, start ROSC timer
        timerManager.markROSC()

        // Pause cycle alerts during ROSC
        cycleAlertManager.pause()

        // Stop metronome
        metronomeService?.stop()

        // Update state
        _arrestState.value = ArrestState.ROSC(
            session = session,
            previousArrestDuration = timerManager.getTotalArrestDuration()
        )
        _uiState.update { it.copy(arrestState = _arrestState.value) }
    }

    /**
     * Handle re-arrest - resumes arrest timing from previous duration
     */
    fun handleReArrest() {
        val currentState = _arrestState.value

        // Get session from current state (works for ROSC or Active)
        val session = when (currentState) {
            is ArrestState.ROSC -> currentState.session
            is ArrestState.Active -> currentState.session
            else -> EventRepository.getCurrentSession()
        }

        if (session == null) return

        // Log re-arrest to repository
        EventRepository.markReArrest()

        // Resume arrest timer from where it was
        timerManager.markReArrest()

        // Reset and start cycle alerts for new arrest phase
        cycleAlertManager.resetForNewArrest()
        cycleAlertManager.start()

        // Restart metronome
        metronomeService?.start(_uiState.value.metronomeConfig)

        // Update state to Active
        _arrestState.value = ArrestState.Active(
            session = session,
            currentCycle = cycleAlertManager.currentCycle.value
        )

        // Force UI update with current timer values
        _uiState.update {
            it.copy(
                arrestState = _arrestState.value,
                formattedElapsedTime = timerManager.formattedTotalTime.value,
                formattedEpisodeTime = "00:00",  // Episode resets on re-arrest
                formattedCycleTime = "0:00",
                currentEpisode = EventRepository.getCurrentEpisode()
            )
        }
        updateEventCount()
    }

    /**
     * Pause - for transport or other interruptions
     */
    fun pause() {
        timerManager.pause()
        cycleAlertManager.pause()
        metronomeService?.stop()
        EventRepository.logPause()

        val currentState = _arrestState.value
        if (currentState is ArrestState.Active) {
            _arrestState.value = currentState.copy(isPaused = true)
            _uiState.update { it.copy(arrestState = _arrestState.value) }
        }
    }

    /**
     * Resume from pause
     */
    fun resume() {
        timerManager.resume()
        cycleAlertManager.resume()
        metronomeService?.start(_uiState.value.metronomeConfig)
        EventRepository.logResume()

        val currentState = _arrestState.value
        if (currentState is ArrestState.Active) {
            _arrestState.value = currentState.copy(isPaused = false)
            _uiState.update { it.copy(arrestState = _arrestState.value) }
        }
    }

    /**
     * End the arrest event
     */
    fun endEvent() {
        timerManager.stop()
        cycleAlertManager.stop()
        metronomeService?.stop()
        EventRepository.endSession()

        val session = EventRepository.getCurrentSession()
        if (session != null) {
            _arrestState.value = ArrestState.Completed(session)
            _uiState.update { it.copy(arrestState = _arrestState.value) }
        }
    }

    /**
     * Update metronome BPM
     */
    fun updateMetronomeBPM(bpm: Int) {
        val newConfig = _uiState.value.metronomeConfig.copy(bpm = bpm)
        _uiState.update { it.copy(metronomeConfig = newConfig) }
        metronomeService?.updateConfig(newConfig)
    }

    /**
     * Toggle metronome sound
     */
    fun toggleMetronomeSound(enabled: Boolean) {
        val newConfig = _uiState.value.metronomeConfig.copy(useSound = enabled)
        _uiState.update { it.copy(metronomeConfig = newConfig) }
        metronomeService?.updateConfig(newConfig)
    }

    /**
     * Toggle metronome vibration
     */
    fun toggleMetronomeVibration(enabled: Boolean) {
        val newConfig = _uiState.value.metronomeConfig.copy(useVibration = enabled)
        _uiState.update { it.copy(metronomeConfig = newConfig) }
        metronomeService?.updateConfig(newConfig)
    }

    /**
     * Get the event summary text
     */
    fun getEventSummary(): String {
        return EventRepository.generateTextSummary()
    }

    /**
     * Get JSON export
     */
    fun getJsonExport(): String {
        return EventRepository.generateJsonExport()
    }

    /**
     * Export to file and get share intent
     */
    fun exportAndShare(format: EventRepository.ExportFormat): Intent {
        val file = EventRepository.exportToFile(context, format)
        return EventRepository.createShareIntent(context, file)
    }

    /**
     * Reset for a new event
     */
    fun reset() {
        timerManager.stop()
        cycleAlertManager.stop()
        metronomeService?.stop()

        // Save session to database before clearing (keeps last 10)
        if (EventRepository.hasActiveSession()) {
            EventRepository.endSession()
            EventRepository.saveSessionToDatabase(context)
        }

        EventRepository.clear()

        _arrestState.value = ArrestState.Idle
        _uiState.value = ProfessionalModeUIState()
    }

    private fun updateEventCount() {
        _uiState.update { it.copy(eventCount = EventRepository.events.value.size) }
    }

    override fun onCleared() {
        super.onCleared()

        timerUpdateJob?.cancel()

        if (isServiceBound) {
            context.unbindService(serviceConnection)
            isServiceBound = false
        }

        timerManager.release()
        cycleAlertManager.release()
    }
}
