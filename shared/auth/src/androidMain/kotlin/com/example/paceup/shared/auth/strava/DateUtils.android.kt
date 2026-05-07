package com.example.paceup.shared.auth.strava

actual fun ninetyDaysAgoEpoch(): Long =
    System.currentTimeMillis() / 1000L - 90L * 24 * 3600
