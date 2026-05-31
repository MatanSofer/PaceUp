package com.example.paceup.platform

import android.content.Context

private const val PREFS_NAME = "paceup_prefs"
private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

actual class OnboardingPrefs(private val context: Context) : OnboardingPrefsSource {
    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    actual override fun isCompleted(): Boolean = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    actual override fun markCompleted() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
    }
}
