package com.example.paceup.shared.runmatching.di

import com.example.paceup.shared.runmatching.data.SupabaseRunRepository
import com.example.paceup.shared.runmatching.data.SupabaseUserRepository
import com.example.paceup.shared.runmatching.domain.RunRepository
import com.example.paceup.shared.runmatching.domain.UserRepository
import org.koin.dsl.module

/** Koin bindings for run discovery. Requires [SupabaseClient] from supabaseModule. */
val runMatchingModule = module {
    single<RunRepository> { SupabaseRunRepository(get()) }
    single<UserRepository> { SupabaseUserRepository(get()) }
}
