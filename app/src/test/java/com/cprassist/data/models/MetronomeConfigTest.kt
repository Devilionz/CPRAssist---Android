package com.cprassist.data.models

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for MetronomeConfig
 */
class MetronomeConfigTest {

    @Test
    fun `default BPM is 110`() {
        val config = MetronomeConfig()
        assertEquals(110, config.bpm)
    }

    @Test
    fun `intervalMs calculated correctly for 100 BPM`() {
        val config = MetronomeConfig(bpm = 100)
        assertEquals(600L, config.intervalMs) // 60000 / 100 = 600ms
    }

    @Test
    fun `intervalMs calculated correctly for 120 BPM`() {
        val config = MetronomeConfig(bpm = 120)
        assertEquals(500L, config.intervalMs) // 60000 / 120 = 500ms
    }

    @Test
    fun `intervalMs calculated correctly for 110 BPM`() {
        val config = MetronomeConfig(bpm = 110)
        assertEquals(545L, config.intervalMs) // 60000 / 110 = 545ms (truncated)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `BPM below 60 throws exception`() {
        MetronomeConfig(bpm = 59)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `BPM above 180 throws exception`() {
        MetronomeConfig(bpm = 181)
    }

    @Test
    fun `BPM at boundaries is valid`() {
        val configLow = MetronomeConfig(bpm = 60)
        val configHigh = MetronomeConfig(bpm = 180)
        assertEquals(60, configLow.bpm)
        assertEquals(180, configHigh.bpm)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `volume below 0 throws exception`() {
        MetronomeConfig(volumePercent = -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `volume above 100 throws exception`() {
        MetronomeConfig(volumePercent = 101)
    }

    @Test
    fun `volume at boundaries is valid`() {
        val configMin = MetronomeConfig(volumePercent = 0)
        val configMax = MetronomeConfig(volumePercent = 100)
        assertEquals(0, configMin.volumePercent)
        assertEquals(100, configMax.volumePercent)
    }

    @Test
    fun `copy preserves values`() {
        val original = MetronomeConfig(bpm = 115, useSound = false, useVibration = true)
        val copy = original.copy(bpm = 118)

        assertEquals(118, copy.bpm)
        assertFalse(copy.useSound)
        assertTrue(copy.useVibration)
    }

    @Test
    fun `default values are sensible for CPR`() {
        val config = MetronomeConfig()

        assertTrue(config.bpm in 100..120) // AHA guidelines
        assertTrue(config.isEnabled)
        assertTrue(config.useVibration)
        assertTrue(config.useSound)
        assertEquals(100, config.volumePercent)
    }
}
