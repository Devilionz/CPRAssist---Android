package com.cprassist.data.models

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Categories of events that can be logged during cardiac arrest management
 */
enum class EventCategory {
    AIRWAY_ACCESS,
    RHYTHM,
    INTERVENTION,
    OUTCOME,
    SYSTEM
}

/**
 * Specific event types with their display labels and categories
 */
enum class EventType(val displayName: String, val category: EventCategory) {
    // Airway/Access
    IGEL("iGel", EventCategory.AIRWAY_ACCESS),
    TUBE("Tube", EventCategory.AIRWAY_ACCESS),
    CANNULA("Cannula", EventCategory.AIRWAY_ACCESS),
    IO("IO", EventCategory.AIRWAY_ACCESS),

    // Rhythms
    VF("VF", EventCategory.RHYTHM),
    VT("VT", EventCategory.RHYTHM),
    ASYSTOLE("Asystole", EventCategory.RHYTHM),
    PEA("PEA", EventCategory.RHYTHM),

    // Interventions
    SHOCK_GIVEN("Shock Given", EventCategory.INTERVENTION),
    ADRENALINE("Adrenaline", EventCategory.INTERVENTION),
    AMIODARONE("Amiodarone", EventCategory.INTERVENTION),
    NARCAN("Narcan", EventCategory.INTERVENTION),
    FLUIDS("Fluids", EventCategory.INTERVENTION),
    TXA("TXA", EventCategory.INTERVENTION),
    ATROPINE("Atropine", EventCategory.INTERVENTION),

    // Outcomes
    ROSC("ROSC", EventCategory.OUTCOME),
    RE_ARREST("Re-Arrest", EventCategory.OUTCOME),

    // System events
    ARREST_STARTED("Cardiac Arrest Confirmed", EventCategory.SYSTEM),
    ARREST_ENDED("Event Ended", EventCategory.SYSTEM),
    CYCLE_COMPLETED("Cycle Completed", EventCategory.SYSTEM),
    PAUSED("Paused", EventCategory.SYSTEM),
    RESUMED("Resumed", EventCategory.SYSTEM)
}

/**
 * Phase of arrest for event tracking
 */
enum class ArrestPhase {
    ARREST,
    ROSC,
    RE_ARREST
}

/**
 * A timestamped event recorded during an arrest
 */
data class TimestampedEvent(
    val id: String = UUID.randomUUID().toString(),
    val type: EventType,
    val timestamp: Long = System.currentTimeMillis(),
    val elapsedTimeMs: Long, // Time since arrest started
    val cycleNumber: Int,
    val arrestPhase: ArrestPhase = ArrestPhase.ARREST,
    val arrestEpisode: Int = 1, // Which arrest episode (increments on re-arrest)
    val notes: String? = null
) {
    /**
     * Format elapsed time as MM:SS
     */
    fun formatElapsedTime(): String {
        val totalSeconds = elapsedTimeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    /**
     * Format absolute timestamp
     */
    fun formatTimestamp(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
        return sdf.format(Date(timestamp))
    }

    /**
     * Format for display in event log
     */
    fun toDisplayString(): String {
        return "${formatElapsedTime()} - ${type.displayName}"
    }

    /**
     * Format for export
     */
    fun toExportString(): String {
        val phasePrefix = when (arrestPhase) {
            ArrestPhase.ARREST -> if (arrestEpisode > 1) "[RE-ARREST #$arrestEpisode] " else ""
            ArrestPhase.ROSC -> "[ROSC] "
            ArrestPhase.RE_ARREST -> "[RE-ARREST #$arrestEpisode] "
        }
        return "${formatTimestamp()} | ${formatElapsedTime()} | Cycle $cycleNumber | $phasePrefix${type.displayName}${notes?.let { " ($it)" } ?: ""}"
    }
}

/**
 * Complete arrest session data
 */
data class ArrestSession(
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long = System.currentTimeMillis(),
    var endTime: Long? = null,
    val events: MutableList<TimestampedEvent> = mutableListOf(),
    var totalROSCTime: Long = 0L, // Total time in ROSC state
    var arrestCount: Int = 1 // Number of arrest episodes (re-arrests)
) {
    /**
     * Total active arrest duration (excluding ROSC periods)
     */
    fun getTotalArrestDuration(): Long {
        val end = endTime ?: System.currentTimeMillis()
        return (end - startTime) - totalROSCTime
    }

    /**
     * Generate complete event summary for export
     */
    fun generateSummary(): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        sb.appendLine("═══════════════════════════════════════════")
        sb.appendLine("         CARDIAC ARREST EVENT SUMMARY       ")
        sb.appendLine("═══════════════════════════════════════════")
        sb.appendLine()
        sb.appendLine("Session ID: $id")
        sb.appendLine("Start Time: ${dateFormat.format(Date(startTime))}")
        endTime?.let { sb.appendLine("End Time: ${dateFormat.format(Date(it))}") }
        sb.appendLine()
        sb.appendLine("───────────────────────────────────────────")
        sb.appendLine("                 STATISTICS                 ")
        sb.appendLine("───────────────────────────────────────────")
        sb.appendLine("Total Arrest Duration: ${formatDuration(getTotalArrestDuration())}")
        sb.appendLine("Number of Arrest Episodes: $arrestCount")
        if (totalROSCTime > 0) {
            sb.appendLine("Total ROSC Time: ${formatDuration(totalROSCTime)}")
        }
        sb.appendLine("Total Events Logged: ${events.size}")
        sb.appendLine()

        // Count by category
        sb.appendLine("───────────────────────────────────────────")
        sb.appendLine("              EVENT COUNTS                  ")
        sb.appendLine("───────────────────────────────────────────")

        val shockCount = events.count { it.type == EventType.SHOCK_GIVEN }
        val adrenalineCount = events.count { it.type == EventType.ADRENALINE }
        val amiodaroneCount = events.count { it.type == EventType.AMIODARONE }

        sb.appendLine("Shocks Delivered: $shockCount")
        sb.appendLine("Adrenaline Doses: $adrenalineCount")
        sb.appendLine("Amiodarone Doses: $amiodaroneCount")
        sb.appendLine()

        // Full timeline
        sb.appendLine("───────────────────────────────────────────")
        sb.appendLine("              EVENT TIMELINE                ")
        sb.appendLine("───────────────────────────────────────────")
        sb.appendLine("Time     | Elapsed | Cycle | Event")
        sb.appendLine("───────────────────────────────────────────")

        events.forEach { event ->
            sb.appendLine(event.toExportString())
        }

        sb.appendLine()
        sb.appendLine("═══════════════════════════════════════════")
        sb.appendLine("Generated by CPR Assist")
        sb.appendLine("This log is for documentation purposes only.")
        sb.appendLine("═══════════════════════════════════════════")

        return sb.toString()
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
