package com.cprassist.data.models

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ArrestSession
 */
class ArrestSessionTest {

    @Test
    fun `new session has unique ID`() {
        val session1 = ArrestSession()
        val session2 = ArrestSession()

        assertNotEquals(session1.id, session2.id)
    }

    @Test
    fun `new session has start time set`() {
        val before = System.currentTimeMillis()
        val session = ArrestSession()
        val after = System.currentTimeMillis()

        assertTrue(session.startTime in before..after)
    }

    @Test
    fun `new session has no end time`() {
        val session = ArrestSession()

        assertNull(session.endTime)
    }

    @Test
    fun `new session has empty events list`() {
        val session = ArrestSession()

        assertTrue(session.events.isEmpty())
    }

    @Test
    fun `new session has arrest count of 1`() {
        val session = ArrestSession()

        assertEquals(1, session.arrestCount)
    }

    @Test
    fun `getTotalArrestDuration returns time since start when active`() {
        val startTime = System.currentTimeMillis() - 10_000 // 10 seconds ago
        val session = ArrestSession(startTime = startTime)

        val duration = session.getTotalArrestDuration()

        assertTrue(duration >= 10_000)
        assertTrue(duration < 11_000) // Should be close to 10 seconds
    }

    @Test
    fun `getTotalArrestDuration subtracts ROSC time`() {
        val startTime = System.currentTimeMillis() - 10_000
        val session = ArrestSession(
            startTime = startTime,
            totalROSCTime = 5_000 // 5 seconds in ROSC
        )

        val duration = session.getTotalArrestDuration()

        assertTrue(duration >= 5_000)
        assertTrue(duration < 6_000) // 10 seconds - 5 seconds ROSC
    }

    @Test
    fun `getTotalArrestDuration uses endTime when set`() {
        val startTime = System.currentTimeMillis() - 60_000
        val endTime = System.currentTimeMillis() - 30_000
        val session = ArrestSession(
            startTime = startTime,
            endTime = endTime
        )

        val duration = session.getTotalArrestDuration()

        assertEquals(30_000, duration)
    }

    @Test
    fun `generateSummary includes session ID`() {
        val session = ArrestSession()

        val summary = session.generateSummary()

        assertTrue(summary.contains(session.id))
    }

    @Test
    fun `generateSummary includes statistics`() {
        val session = ArrestSession()
        session.events.add(TimestampedEvent(
            type = EventType.SHOCK_GIVEN,
            elapsedTimeMs = 60_000,
            cycleNumber = 1
        ))
        session.events.add(TimestampedEvent(
            type = EventType.SHOCK_GIVEN,
            elapsedTimeMs = 120_000,
            cycleNumber = 1
        ))
        session.events.add(TimestampedEvent(
            type = EventType.ADRENALINE,
            elapsedTimeMs = 180_000,
            cycleNumber = 2
        ))

        val summary = session.generateSummary()

        assertTrue(summary.contains("Shocks Delivered: 2"))
        assertTrue(summary.contains("Adrenaline Doses: 1"))
    }

    @Test
    fun `generateSummary includes event timeline`() {
        val session = ArrestSession()
        session.events.add(TimestampedEvent(
            type = EventType.VF,
            elapsedTimeMs = 0,
            cycleNumber = 1
        ))
        session.events.add(TimestampedEvent(
            type = EventType.SHOCK_GIVEN,
            elapsedTimeMs = 30_000,
            cycleNumber = 1
        ))

        val summary = session.generateSummary()

        assertTrue(summary.contains("EVENT TIMELINE"))
        assertTrue(summary.contains("VF"))
        assertTrue(summary.contains("Shock Given"))
    }

    @Test
    fun `generateSummary includes arrest count for re-arrests`() {
        val session = ArrestSession(arrestCount = 2)

        val summary = session.generateSummary()

        assertTrue(summary.contains("Number of Arrest Episodes: 2"))
    }

    @Test
    fun `generateSummary includes ROSC time when present`() {
        val session = ArrestSession(totalROSCTime = 120_000) // 2 minutes

        val summary = session.generateSummary()

        assertTrue(summary.contains("Total ROSC Time:"))
    }

    @Test
    fun `events can be added to session`() {
        val session = ArrestSession()

        session.events.add(TimestampedEvent(
            type = EventType.ADRENALINE,
            elapsedTimeMs = 60_000,
            cycleNumber = 1
        ))

        assertEquals(1, session.events.size)
    }
}
