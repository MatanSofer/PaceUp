package com.example.paceup.shared.pacezone

/**
 * Runner pace zones based on average pace over the last 90 days (spec §4.1).
 * Boundaries are in seconds per kilometre.
 *
 * @param minSecPerKm Inclusive lower bound (null = no lower bound).
 * @param maxSecPerKm Exclusive upper bound (null = no upper bound).
 * @param displayRange Human-readable pace range, e.g. "4:30–5:00 /km".
 */
enum class PaceZone(
    val minSecPerKm: Int?,
    val maxSecPerKm: Int?,
    val displayRange: String
) {
    /** Elite — under 4:30 /km */
    A(minSecPerKm = null, maxSecPerKm = 270, displayRange = "< 4:30 /km"),

    /** Advanced — 4:30–5:00 /km */
    B(minSecPerKm = 270, maxSecPerKm = 300, displayRange = "4:30–5:00 /km"),

    /** Intermediate — 5:00–5:30 /km */
    C(minSecPerKm = 300, maxSecPerKm = 330, displayRange = "5:00–5:30 /km"),

    /** Recreational — 5:30–6:30 /km */
    D(minSecPerKm = 330, maxSecPerKm = 390, displayRange = "5:30–6:30 /km"),

    /** Casual — above 6:30 /km */
    E(minSecPerKm = 390, maxSecPerKm = null, displayRange = "> 6:30 /km");

    companion object {
        /** Returns the zone for the given [paceSecPerKm]. */
        fun fromPace(paceSecPerKm: Int): PaceZone = when {
            paceSecPerKm < 270 -> A
            paceSecPerKm < 300 -> B
            paceSecPerKm < 330 -> C
            paceSecPerKm < 390 -> D
            else -> E
        }
    }
}
