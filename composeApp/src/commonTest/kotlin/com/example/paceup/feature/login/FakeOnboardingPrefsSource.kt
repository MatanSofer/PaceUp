package com.example.paceup.feature.login

import com.example.paceup.platform.OnboardingPrefsSource

class FakeOnboardingPrefsSource(private var completed: Boolean = false) : OnboardingPrefsSource {
    override fun isCompleted(): Boolean = completed
    override fun markCompleted() { completed = true }
}
