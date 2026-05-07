package com.example.paceup.shared.pacezone

/**
 * Calculated pace zone result for a runner.
 *
 * @param zone Assigned pace zone A–E.
 * @param avgPaceSecondsPerKm Time-weighted average pace in sec/km across qualifying runs.
 * @param weeklyMileageAvgKm Average weekly distance in km over the 90-day window.
 */
data class PaceZoneResult(
    val zone: PaceZone,
    val avgPaceSecondsPerKm: Int,
    val weeklyMileageAvgKm: Float
)

/**
 * Calculates pace zone from a list of run activities.
 *
 * Input contract (enforced by the caller):
 * - Only runs with distance ≥ 3 km (spec §4.1)
 * - Only runs within the last 90 days (spec §4.1)
 *
 * The calculator is a pure function with no I/O — all filtering happens upstream.
 */
object PaceZoneCalculator {

    private const val WEEKS_IN_WINDOW = 90.0 / 7.0 // 90-day rolling window

    /**
     * Calculates the runner's pace zone from qualifying activities.
     *
     * @param activities Each item is (avgPaceSecondsPerKm, distanceKm) for one run.
     * @return [PaceZoneResult] or null if [activities] is empty (user is unverified).
     */
    fun calculate(activities: List<Pair<Int, Float>>): PaceZoneResult? {
        if (activities.isEmpty()) return null

        val totalDistanceKm = activities.sumOf { (_, dist) -> dist.toDouble() }.toFloat()
        if (totalDistanceKm <= 0f) return null

        // Time-weighted average: totalTime / totalDistance (correct way to average pace)
        val totalTimeSeconds = activities.sumOf { (pace, dist) -> pace.toDouble() * dist }
        val avgPaceSecPerKm = (totalTimeSeconds / totalDistanceKm).toInt()

        val weeklyMileageAvgKm = (totalDistanceKm / WEEKS_IN_WINDOW).toFloat()

        return PaceZoneResult(
            zone = PaceZone.fromPace(avgPaceSecPerKm),
            avgPaceSecondsPerKm = avgPaceSecPerKm,
            weeklyMileageAvgKm = weeklyMileageAvgKm
        )
    }

    /** Formats [paceSecondsPerKm] as "M:SS /km", e.g. 305 → "5:05 /km". */
    fun formatPace(paceSecondsPerKm: Int): String {
        val minutes = paceSecondsPerKm / 60
        val seconds = paceSecondsPerKm % 60
        return "$minutes:${seconds.toString().padStart(2, '0')} /km"
    }
}
