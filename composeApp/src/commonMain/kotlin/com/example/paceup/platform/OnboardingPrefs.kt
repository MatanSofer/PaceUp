package com.example.paceup.platform

/**
 * Persists whether the user has completed the full onboarding flow.
 * Used to skip onboarding on re-login for existing users.
 */
expect class OnboardingPrefs {
    fun isCompleted(): Boolean
    fun markCompleted()
}
