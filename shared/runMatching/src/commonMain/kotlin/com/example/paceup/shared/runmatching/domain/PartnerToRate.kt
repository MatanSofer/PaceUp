package com.example.paceup.shared.runmatching.domain

/** A run partner available for rating after a shared attended run. */
data class PartnerToRate(
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val paceZone: String?,
)
