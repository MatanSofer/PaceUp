package com.example.paceup.shared.auth.di

import com.example.paceup.shared.auth.data.SupabaseAuthRepository
import com.example.paceup.shared.auth.domain.AuthRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/** Koin bindings for the auth layer. Requires [SupabaseClient] from supabaseModule. */
val authModule: Module = module {
    single<AuthRepository> { SupabaseAuthRepository(get()) }
}
