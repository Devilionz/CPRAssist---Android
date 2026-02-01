package com.cprassist.data.models

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for TimestampedEvent and related models
 */
class TimestampedEventTest {

    @Test
    fun `formatElapsedTime formats correctly for seconds`() {
        val event = TimestampedEvent(
            type = EventType.ADRENALINE,
            elapsedTimeMs = 45_000, // 45 seconds
            cycleNumber = 1
        )

        assertEquals("00:45", event.formatElapsedTime())
    }

    @Test
    fun `formatElapsedTime formats correctly for minutes and seconds`() {
        val event = TimestampedEvent(
            type = EventType.SHOCK_GIVEN,
            elapsedTimeMs = 185_000, // 3:05
            cycleNumber = 2
        )

        assertEquals("03:05", event.formatElapsedTime())
    }

    @Test
    fun `formatElapsedTime handles zero`() {
        val event = TimestampedEvent(
            type = EventType.ARREST_STARTED,
            elapsedTimeMs = 0,
            cycleNumber = 1
        )

        assertEquals("00:00", event.formatElapsedTime())
    }

    @Test
    fun `formatElapsedTime handles long durations`() {
        val event = TimestampedEvent(
            type = EventType.ROSC,
            elapsedTimeMs = 3_661_000, // 61:01
            cycleNumber = 30
        )

        assertEquals("61:01", event.formatElapsedTime())
    }

    @Test
    fun `toDisplayString includes time and event name`() {
        val event = TimestampedEvent(
            type = EventType.AMIODARONE,
            elapsedTimeMs = 120_000, // 2:00
            cycleNumber = 1
        )

        val display = event.toDisplayString()

        assertTrue(display.contains("02:00"))
        assertTrue(display.contains("Amiodarone"))
    }

    @Test
    fun `toExportString includes all fields`() {
        val event = TimestampedEvent(
            type = EventType.VF,
            elapsedTimeMs = 60_000,
            cycleNumber = 1,
            notes = "Initial rhythm"
        )

        val export = event.toExportString()

        assertTrue(export.contains("01:00"))
        assertTrue(export.contains("Cycle 1"))
        assertTrue(export.contains("VF"))
        assertTrue(export.contains("Initial rhythm"))
    }

    @Test
    fun `toExportString works without notes`() {
        val event = TimestampedEvent(
            type = EventType.ADRENALINE,
            elapsedTimeMs = 60_000,
            cycleNumber = 2
        )

        val export = event.toExportString()

        assertTrue(export.contains("Adrenaline"))
        assertFalse(export.contains("null"))
    }

    @Test
    fun `event types have correct categories`() {
        assertEquals(EventCategory.AIRWAY_ACCESS, EventType.IGEL.category)
        assertEquals(EventCategory.AIRWAY_ACCESS, EventType.TUBE.category)
        assertEquals(EventCategory.AIRWAY_ACCESS, EventType.IO.category)

        assertEquals(EventCategory.RHYTHM, EventType.VF.category)
        assertEquals(EventCategory.RHYTHM, EventType.VT.category)
        assertEquals(EventCategory.RHYTHM, EventType.ASYSTOLE.category)
        assertEquals(EventCategory.RHYTHM, EventType.PEA.category)

        assertEquals(EventCategory.INTERVENTION, EventType.SHOCK_GIVEN.category)
        assertEquals(EventCategory.INTERVENTION, EventType.ADRENALINE.category)
        assertEquals(EventCategory.INTERVENTION, EventType.AMIODARONE.category)

        assertEquals(EventCategory.OUTCOME, EventType.ROSC.category)
        assertEquals(EventCategory.OUTCOME, EventType.RE_ARREST.category)

        assertEquals(EventCategory.SYSTEM, EventType.ARREST_STARTED.category)
    }

    @Test
    fun `event types have correct display names`() {
        assertEquals("iGel", EventType.IGEL.displayName)
        assertEquals("VF", EventType.VF.displayName)
        assertEquals("Shock Given", EventType.SHOCK_GIVEN.displayName)
        assertEquals("Adrenaline", EventType.ADRENALINE.displayName)
        assertEquals("ROSC", EventType.ROSC.displayName)
    }

    @Test
    fun `event has unique ID`() {
        val event1 = TimestampedEvent(
            type = EventType.ADRENALINE,
            elapsedTimeMs = 0,
            cycleNumber = 1
        )
        val event2 = TimestampedEvent(
            type = EventType.ADRENALINE,
            elapsedTimeMs = 0,
            cycleNumber = 1
        )

        assertNotEquals(event1.id, event2.id)
    }
}
