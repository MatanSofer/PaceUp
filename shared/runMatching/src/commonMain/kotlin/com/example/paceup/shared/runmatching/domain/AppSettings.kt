package com.example.paceup.shared.runmatching.domain

/** User-configurable app preferences (spec §5.2 App). */
data class AppSettings(
    val language: String = "en",          // "en" | "he"
    val units: String = "km",             // "km" | "miles"
    val mapStyle: String = "standard",    // "standard" | "satellite"
)
