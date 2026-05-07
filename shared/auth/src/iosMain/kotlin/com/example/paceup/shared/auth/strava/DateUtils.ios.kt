package com.example.paceup.shared.auth.strava

import platform.Foundation.NSDate

actual fun ninetyDaysAgoEpoch(): Long =
    NSDate().timeIntervalSince1970.toLong() - 90L * 24 * 3600
