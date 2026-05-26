package com.example.paceup.platform

/** Testable interface for onboarding completion state. Implemented by the platform [OnboardingPrefs]. */
interface OnboardingPrefsSource {
    fun isCompleted(): Boolean
    fun markCompleted()
}
