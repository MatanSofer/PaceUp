package com.example.paceup.platform

import platform.Foundation.NSUserDefaults

private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

// iOS: verified on Mac before PR
actual class OnboardingPrefs {
    actual fun isCompleted(): Boolean =
        NSUserDefaults.standardUserDefaults.boolForKey(KEY_ONBOARDING_COMPLETED)

    actual fun markCompleted() {
        NSUserDefaults.standardUserDefaults.setBool(true, forKey = KEY_ONBOARDING_COMPLETED)
    }
}
