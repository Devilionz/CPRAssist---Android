package com.cprassist.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cprassist.data.models.EventType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Entity representing a saved arrest session
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey
    val id: String,
    val startTime: Long,
    val endTime: Long?,
    val totalArrestDurationMs: Long,
    val totalROSCTimeMs: Long,
    val arrestCount: Int,
    val eventCount: Int,
    val shockCount: Int,
    val adrenalineCount: Int,
    val amiodaroneCount: Int,
    val hadROSC: Boolean,
    val summaryText: String
) {
    /**
     * Format start time for display
     */
    fun formatStartTime(): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)
        return sdf.format(Date(startTime))
    }

    /**
     * Format duration for display
     */
    fun formatDuration(): String {
        val totalSeconds = totalArrestDurationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Get outcome description
     */
    fun getOutcome(): String {
        return when {
            hadROSC -> "ROSC Achieved"
            else -> "No ROSC"
        }
    }
}

/**
 * Entity representing an event within a session
 */
@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class EventEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val eventType: EventType,
    val timestamp: Long,
    val elapsedTimeMs: Long,
    val cycleNumber: Int,
    val notes: String?
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
}

/**
 * Session with all its events
 */
data class SessionWithEvents(
    val session: SessionEntity,
    val events: List<EventEntity>
)
