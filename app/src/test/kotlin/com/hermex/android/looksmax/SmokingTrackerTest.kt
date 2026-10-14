package com.hermex.android.looksmax

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SmokingTrackerTest {

    @Before
    fun setUp() {
        SmokingTracker.clearAll()
    }

    @Test
    fun testLogCigaretteIncrementsTotal() {
        val initialToday = SmokingTracker.getStats().totalToday
        SmokingTracker.logCigarette()
        val afterOne = SmokingTracker.getStats().totalToday
        assertEquals(initialToday + 1, afterOne)

        SmokingTracker.removeLastCigarette()
        assertEquals(initialToday, SmokingTracker.getStats().totalToday)
    }

    @Test
    fun testBuildEvaSmokingContext_stressDetection() {
        SmokingTracker.logCigarette()
        SmokingTracker.logCigarette()
        SmokingTracker.logCigarette()

        val context = SmokingTracker.buildEvaSmokingContext()
        assertTrue("Context should contain mention of 3+ cigarettes or stress", context.contains("стресс") || context.contains("3"))
    }
}
