package com.example.paceup.platform

import platform.Foundation.NSUserDefaults

private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

// iOS: verified on Mac before PR
actual class OnboardingPrefs : OnboardingPrefsSource {
    actual override fun isCompleted(): Boolean =
        NSUserDefaults.standardUserDefaults.boolForKey(KEY_ONBOARDING_COMPLETED)

    actual override fun markCompleted() {
        NSUserDefaults.standardUserDefaults.setBool(true, forKey = KEY_ONBOARDING_COMPLETED)
    }
}
