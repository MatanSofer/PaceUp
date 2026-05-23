package com.example.paceup.feature.home

/** Presentation-layer coordinate pair. Kept in composeApp to avoid polluting shared domain. */
data class LatLng(val lat: Double, val lng: Double)

/** Default map center used when the user's location is unknown. */
val DefaultMapCenter = LatLng(lat = 32.0726, lng = 34.7925) // Tel Aviv
