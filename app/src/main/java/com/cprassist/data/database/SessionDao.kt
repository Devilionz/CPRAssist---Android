package com.cprassist.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for session history
 */
@Dao
interface SessionDao {

    /**
     * Insert a new session
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    /**
     * Insert multiple events
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    /**
     * Insert a session with all its events
     */
    @Transaction
    suspend fun insertSessionWithEvents(session: SessionEntity, events: List<EventEntity>) {
        insertSession(session)
        insertEvents(events)
    }

    /**
     * Get all sessions ordered by start time (newest first)
     */
    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    /**
     * Get a specific session by ID
     */
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): SessionEntity?

    /**
     * Get all events for a session
     */
    @Query("SELECT * FROM events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getEventsForSession(sessionId: String): List<EventEntity>

    /**
     * Get session with events
     */
    @Transaction
    suspend fun getSessionWithEvents(sessionId: String): SessionWithEvents? {
        val session = getSessionById(sessionId) ?: return null
        val events = getEventsForSession(sessionId)
        return SessionWithEvents(session, events)
    }

    /**
     * Delete a session (events are cascaded)
     */
    @Delete
    suspend fun deleteSession(session: SessionEntity)

    /**
     * Delete a session by ID
     */
    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

    /**
     * Delete all sessions
     */
    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()

    /**
     * Get session count
     */
    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun getSessionCount(): Int

    /**
     * Delete oldest sessions keeping only the most recent N sessions
     */
    @Query("DELETE FROM sessions WHERE id NOT IN (SELECT id FROM sessions ORDER BY startTime DESC LIMIT :keepCount)")
    suspend fun deleteOldSessions(keepCount: Int = 10)

    /**
     * Insert session and enforce limit of 10 sessions
     */
    @Transaction
    suspend fun insertSessionAndEnforceLimit(session: SessionEntity, events: List<EventEntity>, maxSessions: Int = 10) {
        insertSessionWithEvents(session, events)
        deleteOldSessions(maxSessions)
    }

    /**
     * Get sessions with ROSC
     */
    @Query("SELECT * FROM sessions WHERE hadROSC = 1 ORDER BY startTime DESC")
    fun getSessionsWithROSC(): Flow<List<SessionEntity>>

    /**
     * Search sessions by date range
     */
    @Query("SELECT * FROM sessions WHERE startTime BETWEEN :startMs AND :endMs ORDER BY startTime DESC")
    fun getSessionsInRange(startMs: Long, endMs: Long): Flow<List<SessionEntity>>

    /**
     * Get statistics
     */
    @Query("""
        SELECT
            COUNT(*) as totalSessions,
            SUM(CASE WHEN hadROSC = 1 THEN 1 ELSE 0 END) as roscCount,
            AVG(totalArrestDurationMs) as avgDuration,
            SUM(shockCount) as totalShocks,
            SUM(adrenalineCount) as totalAdrenaline
        FROM sessions
    """)
    suspend fun getStatistics(): SessionStatistics
}

/**
 * Aggregate statistics from sessions
 */
data class SessionStatistics(
    val totalSessions: Int,
    val roscCount: Int,
    val avgDuration: Long?,
    val totalShocks: Int,
    val totalAdrenaline: Int
)
