package com.example.paceup.shared.auth.di

import com.example.paceup.shared.auth.data.SupabaseAuthRepository
import com.example.paceup.shared.auth.domain.AuthRepository
import com.example.paceup.shared.auth.profile.ProfileRepository
import com.example.paceup.shared.auth.profile.SupabaseProfileRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin bindings for the auth layer. Requires [SupabaseClient] from supabaseModule.
 * Note: [StravaAuthRepository] is bound in the platform entry-point (PaceUpApplication /
 * iOSKoinSetup) because it needs platform-specific credentials from BuildConfig.
 */
val authModule: Module = module {
    single<AuthRepository> { SupabaseAuthRepository(get()) }
    single<ProfileRepository> { SupabaseProfileRepository(get()) }
}
