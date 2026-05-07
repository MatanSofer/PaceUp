package com.example.paceup.shared.pacezone

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PaceZoneCalculatorTest {

    // ── Zone assignment ──────────────────────────────────────────────────────

    @Test
    fun `zone A when avg pace below 270 sec per km`() {
        val result = PaceZoneCalculator.calculate(listOf(Pair(260, 10f)))
        assertEquals(PaceZone.A, result?.zone)
    }

    @Test
    fun `zone A boundary at exactly 269 sec per km`() {
        val result = PaceZoneCalculator.calculate(listOf(Pair(269, 5f)))
        assertEquals(PaceZone.A, result?.zone)
    }

    @Test
    fun `zone B at exactly 270 sec per km`() {
        val result = PaceZoneCalculator.calculate(listOf(Pair(270, 5f)))
        assertEquals(PaceZone.B, result?.zone)
    }

    @Test
    fun `zone B upper boundary at 299 sec per km`() {
        val result = PaceZoneCalculator.calculate(listOf(Pair(299, 5f)))
        assertEquals(PaceZone.B, result?.zone)
    }

    @Test
    fun `zone C at exactly 300 sec per km`() {
        val result = PaceZoneCalculator.calculate(listOf(Pair(300, 5f)))
        assertEquals(PaceZone.C, result?.zone)
    }

    @Test
    fun `zone C upper boundary at 329 sec per km`() {
        val result = PaceZoneCalculator.calculate(listOf(Pair(329, 5f)))
        assertEquals(PaceZone.C, result?.zone)
    }

    @Test
    fun `zone D at exactly 330 sec per km`() {
        val result = PaceZoneCalculator.calculate(listOf(Pair(330, 5f)))
        assertEquals(PaceZone.D, result?.zone)
    }

    @Test
    fun `zone D upper boundary at 389 sec per km`() {
        val result = PaceZoneCalculator.calculate(listOf(Pair(389, 5f)))
        assertEquals(PaceZone.D, result?.zone)
    }

    @Test
    fun `zone E at exactly 390 sec per km`() {
        val result = PaceZoneCalculator.calculate(listOf(Pair(390, 5f)))
        assertEquals(PaceZone.E, result?.zone)
    }

    @Test
    fun `zone E for slow pace above 390`() {
        val result = PaceZoneCalculator.calculate(listOf(Pair(480, 5f)))
        assertEquals(PaceZone.E, result?.zone)
    }

    // ── Null / empty ─────────────────────────────────────────────────────────

    @Test
    fun `returns null for empty activity list`() {
        assertNull(PaceZoneCalculator.calculate(emptyList()))
    }

    // ── Weighted average ─────────────────────────────────────────────────────

    @Test
    fun `time-weighted average favours longer runs`() {
        // One fast 3km run at 270 s/km, one slow 10km run at 390 s/km
        // totalTime = 270*3 + 390*10 = 810 + 3900 = 4710
        // totalDist = 13
        // avgPace = 4710 / 13 = 362 → Zone D
        val result = PaceZoneCalculator.calculate(
            listOf(
                Pair(270, 3f),  // fast, short
                Pair(390, 10f)  // slow, long
            )
        )
        assertEquals(PaceZone.D, result?.zone)
        assertEquals(362, result?.avgPaceSecondsPerKm)
    }

    @Test
    fun `avg pace is identical runs averaged correctly`() {
        val result = PaceZoneCalculator.calculate(
            listOf(Pair(300, 5f), Pair(300, 5f), Pair(300, 5f))
        )
        assertEquals(300, result?.avgPaceSecondsPerKm)
    }

    // ── Weekly mileage ───────────────────────────────────────────────────────

    @Test
    fun `weekly mileage avg is total distance divided by 90 over 7`() {
        // 91km total over 90 days ≈ 7 km/week
        val activities = List(13) { Pair(300, 7f) } // 13 * 7 = 91km
        val result = PaceZoneCalculator.calculate(activities)
        // 91 / (90/7) ≈ 91 / 12.857 ≈ 7.078
        assertEquals(7.077778f, result?.weeklyMileageAvgKm ?: 0f, 0.01f)
    }

    // ── formatPace helper ─────────────────────────────────────────────────────

    @Test
    fun `formatPace formats 300 as 5_00 per km`() {
        assertEquals("5:00 /km", PaceZoneCalculator.formatPace(300))
    }

    @Test
    fun `formatPace pads single-digit seconds`() {
        assertEquals("5:05 /km", PaceZoneCalculator.formatPace(305))
    }

    @Test
    fun `formatPace formats 270 as 4_30 per km`() {
        assertEquals("4:30 /km", PaceZoneCalculator.formatPace(270))
    }

    @Test
    fun `formatPace formats 390 as 6_30 per km`() {
        assertEquals("6:30 /km", PaceZoneCalculator.formatPace(390))
    }

    // ── PaceZone.fromPace ─────────────────────────────────────────────────────

    @Test
    fun `PaceZone fromPace returns correct zone for each boundary`() {
        assertEquals(PaceZone.A, PaceZone.fromPace(269))
        assertEquals(PaceZone.B, PaceZone.fromPace(270))
        assertEquals(PaceZone.B, PaceZone.fromPace(299))
        assertEquals(PaceZone.C, PaceZone.fromPace(300))
        assertEquals(PaceZone.D, PaceZone.fromPace(330))
        assertEquals(PaceZone.E, PaceZone.fromPace(390))
    }
}

private fun assertEquals(expected: Float, actual: Float, delta: Float) {
    assert(kotlin.math.abs(expected - actual) <= delta) {
        "Expected $expected but was $actual (delta=$delta)"
    }
}
