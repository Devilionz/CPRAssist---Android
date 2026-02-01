package com.cprassist.data.repository

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.FileProvider
import com.cprassist.data.database.EventEntity
import com.cprassist.data.database.SessionDatabase
import com.cprassist.data.database.SessionEntity
import com.cprassist.data.models.ArrestPhase
import com.cprassist.data.models.ArrestSession
import com.cprassist.data.models.EventType
import com.cprassist.data.models.TimestampedEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Application-scoped repository for managing arrest session data and event logging.
 * Thread-safe and persists across ViewModel instances within a session.
 *
 * CRITICAL: This is a singleton to ensure events are not lost when navigating between screens.
 */
object EventRepository {

    private var currentSession: ArrestSession? = null

    // Thread-safe list for events
    private val eventList = CopyOnWriteArrayList<TimestampedEvent>()

    private val _events = MutableStateFlow<List<TimestampedEvent>>(emptyList())
    val events: StateFlow<List<TimestampedEvent>> = _events.asStateFlow()

    private val _lastEvent = MutableStateFlow<TimestampedEvent?>(null)
    val lastEvent: StateFlow<TimestampedEvent?> = _lastEvent.asStateFlow()

    // Use SystemClock.elapsedRealtime() for reliable timing that survives deep sleep
    private var sessionStartElapsedRealtime: Long = 0L
    private var currentCycle: Int = 1
    private var currentArrestPhase: ArrestPhase = ArrestPhase.ARREST
    private var currentArrestEpisode: Int = 1

    // Track ROSC periods for accurate arrest duration
    private var roscStartTime: Long = 0L
    private var totalRoscDuration: Long = 0L

    /**
     * Start a new arrest session
     */
    @Synchronized
    fun startNewSession(): ArrestSession {
        // Clear previous session data
        eventList.clear()
        _events.value = emptyList()
        _lastEvent.value = null

        currentSession = ArrestSession()
        sessionStartElapsedRealtime = SystemClock.elapsedRealtime()
        currentCycle = 1
        currentArrestPhase = ArrestPhase.ARREST
        currentArrestEpisode = 1
        totalRoscDuration = 0L
        roscStartTime = 0L

        // Log the arrest start
        logEvent(EventType.ARREST_STARTED)

        return currentSession!!
    }

    /**
     * Get the current session
     */
    fun getCurrentSession(): ArrestSession? = currentSession

    /**
     * Check if there is an active session
     */
    fun hasActiveSession(): Boolean = currentSession != null && currentSession?.endTime == null

    /**
     * Log an event with the current timestamp.
     * Thread-safe and survives lifecycle events.
     */
    @Synchronized
    fun logEvent(
        eventType: EventType,
        notes: String? = null
    ): TimestampedEvent {
        val elapsedTime = if (sessionStartElapsedRealtime > 0) {
            SystemClock.elapsedRealtime() - sessionStartElapsedRealtime
        } else {
            0L
        }

        val event = TimestampedEvent(
            type = eventType,
            elapsedTimeMs = elapsedTime,
            cycleNumber = currentCycle,
            arrestPhase = currentArrestPhase,
            arrestEpisode = currentArrestEpisode,
            notes = notes
        )

        // Add to thread-safe list
        eventList.add(event)

        // Update session's event list
        currentSession?.events?.add(event)

        // Publish to StateFlow (creates new list reference to trigger emission)
        _events.value = eventList.toList()
        _lastEvent.value = event

        return event
    }

    /**
     * Update the current cycle number
     */
    @Synchronized
    fun updateCycle(cycle: Int) {
        currentCycle = cycle
    }

    /**
     * Log a cycle completion
     */
    @Synchronized
    fun logCycleComplete(): TimestampedEvent {
        val event = logEvent(EventType.CYCLE_COMPLETED, "Cycle $currentCycle completed")
        currentCycle++
        return event
    }

    /**
     * Mark ROSC - changes phase and tracks ROSC start time
     */
    @Synchronized
    fun markROSC(): TimestampedEvent {
        currentArrestPhase = ArrestPhase.ROSC
        roscStartTime = SystemClock.elapsedRealtime()
        return logEvent(EventType.ROSC)
    }

    /**
     * Mark re-arrest - ends ROSC period and starts new arrest episode
     */
    @Synchronized
    fun markReArrest(): TimestampedEvent {
        // Calculate ROSC duration and add to total
        if (roscStartTime > 0) {
            totalRoscDuration += SystemClock.elapsedRealtime() - roscStartTime
            currentSession?.totalROSCTime = totalRoscDuration
        }
        roscStartTime = 0L

        currentArrestPhase = ArrestPhase.RE_ARREST
        currentArrestEpisode++
        currentSession?.arrestCount = currentArrestEpisode

        val event = logEvent(EventType.RE_ARREST, "Re-arrest episode $currentArrestEpisode")

        // Switch phase back to ARREST for subsequent events
        currentArrestPhase = ArrestPhase.ARREST

        return event
    }

    /**
     * End the current session
     */
    @Synchronized
    fun endSession() {
        // If in ROSC, finalize ROSC duration
        if (currentArrestPhase == ArrestPhase.ROSC && roscStartTime > 0) {
            totalRoscDuration += SystemClock.elapsedRealtime() - roscStartTime
            currentSession?.totalROSCTime = totalRoscDuration
        }

        logEvent(EventType.ARREST_ENDED)
        currentSession?.endTime = System.currentTimeMillis()
    }

    /**
     * Log pause event
     */
    @Synchronized
    fun logPause(): TimestampedEvent {
        return logEvent(EventType.PAUSED)
    }

    /**
     * Log resume event
     */
    @Synchronized
    fun logResume(): TimestampedEvent {
        return logEvent(EventType.RESUMED)
    }

    /**
     * Get count of specific event type
     */
    fun getEventCount(eventType: EventType): Int {
        return eventList.count { it.type == eventType }
    }

    /**
     * Get all events of a specific category
     */
    fun getEventsByCategory(category: com.cprassist.data.models.EventCategory): List<TimestampedEvent> {
        return eventList.filter { it.type.category == category }
    }

    /**
     * Get current arrest phase
     */
    fun getCurrentPhase(): ArrestPhase = currentArrestPhase

    /**
     * Get current arrest episode number
     */
    fun getCurrentEpisode(): Int = currentArrestEpisode

    /**
     * Generate text summary for export
     */
    fun generateTextSummary(): String {
        return currentSession?.generateSummary() ?: "No active session"
    }

    /**
     * Generate JSON export
     */
    fun generateJsonExport(): String {
        val session = currentSession ?: return "{}"

        val json = JSONObject().apply {
            put("session_id", session.id)
            put("start_time", session.startTime)
            put("start_time_formatted", formatTimestamp(session.startTime))
            session.endTime?.let {
                put("end_time", it)
                put("end_time_formatted", formatTimestamp(it))
            }
            put("total_arrest_duration_ms", session.getTotalArrestDuration())
            put("arrest_count", session.arrestCount)
            put("total_rosc_time_ms", session.totalROSCTime)

            val eventsArray = JSONArray()
            eventList.forEach { event ->
                eventsArray.put(JSONObject().apply {
                    put("id", event.id)
                    put("type", event.type.name)
                    put("display_name", event.type.displayName)
                    put("category", event.type.category.name)
                    put("timestamp", event.timestamp)
                    put("timestamp_formatted", event.formatTimestamp())
                    put("elapsed_ms", event.elapsedTimeMs)
                    put("elapsed_formatted", event.formatElapsedTime())
                    put("cycle", event.cycleNumber)
                    put("arrest_phase", event.arrestPhase.name)
                    put("arrest_episode", event.arrestEpisode)
                    event.notes?.let { put("notes", it) }
                })
            }
            put("events", eventsArray)

            // Summary statistics
            put("statistics", JSONObject().apply {
                put("total_events", eventList.size)
                put("shocks", eventList.count { it.type == EventType.SHOCK_GIVEN })
                put("adrenaline_doses", eventList.count { it.type == EventType.ADRENALINE })
                put("amiodarone_doses", eventList.count { it.type == EventType.AMIODARONE })
                put("total_cycles", currentCycle)
                put("arrest_episodes", currentArrestEpisode)
            })
        }

        return json.toString(2)
    }

    /**
     * Export summary to a file and return share intent
     */
    fun exportToFile(context: Context, format: ExportFormat): File {
        val content = when (format) {
            ExportFormat.TEXT -> generateTextSummary()
            ExportFormat.JSON -> generateJsonExport()
        }

        val extension = when (format) {
            ExportFormat.TEXT -> "txt"
            ExportFormat.JSON -> "json"
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "cpr_assist_log_$timestamp.$extension"

        val file = File(context.cacheDir, fileName)
        file.writeText(content)

        return file
    }

    /**
     * Create share intent for export file
     */
    fun createShareIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = if (file.extension == "json") "application/json" else "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "CPR Assist Event Log")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Save the current session to the database (keeps only last 10 sessions)
     */
    fun saveSessionToDatabase(context: Context) {
        val session = currentSession ?: return
        if (eventList.isEmpty()) return

        // Ensure session has an end time
        if (session.endTime == null) {
            session.endTime = System.currentTimeMillis()
        }

        val sessionEntity = SessionEntity(
            id = session.id,
            startTime = session.startTime,
            endTime = session.endTime,
            totalArrestDurationMs = session.getTotalArrestDuration(),
            totalROSCTimeMs = session.totalROSCTime,
            arrestCount = session.arrestCount,
            eventCount = eventList.size,
            shockCount = eventList.count { it.type == EventType.SHOCK_GIVEN },
            adrenalineCount = eventList.count { it.type == EventType.ADRENALINE },
            amiodaroneCount = eventList.count { it.type == EventType.AMIODARONE },
            hadROSC = eventList.any { it.type == EventType.ROSC },
            summaryText = generateTextSummary()
        )

        val eventEntities = eventList.map { event ->
            EventEntity(
                id = event.id,
                sessionId = session.id,
                eventType = event.type,
                timestamp = event.timestamp,
                elapsedTimeMs = event.elapsedTimeMs,
                cycleNumber = event.cycleNumber,
                notes = event.notes
            )
        }

        // Save to database on IO thread
        CoroutineScope(Dispatchers.IO).launch {
            val database = SessionDatabase.getInstance(context)
            database.sessionDao().insertSessionAndEnforceLimit(sessionEntity, eventEntities, 10)
        }
    }

    /**
     * Clear all data - call when starting fresh
     */
    @Synchronized
    fun clear() {
        currentSession = null
        eventList.clear()
        _events.value = emptyList()
        _lastEvent.value = null
        sessionStartElapsedRealtime = 0L
        currentCycle = 1
        currentArrestPhase = ArrestPhase.ARREST
        currentArrestEpisode = 1
        totalRoscDuration = 0L
        roscStartTime = 0L
    }

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp))
    }

    enum class ExportFormat {
        TEXT,
        JSON
    }
}
