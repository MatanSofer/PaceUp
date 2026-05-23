package com.example.paceup.shared.auth.domain

/** Active Supabase session. Non-null means the user is logged in. */
data class AuthSession(
    val userId: String,
    val accessToken: String
)
