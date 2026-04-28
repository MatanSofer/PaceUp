package com.example.paceup.shared.auth.domain

/** Lightweight auth-level user returned after sign-in or sign-up. */
data class AuthUser(
    val id: String,
    val email: String?
)
