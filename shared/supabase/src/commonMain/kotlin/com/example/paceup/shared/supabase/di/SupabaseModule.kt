package com.example.paceup.shared.supabase.di

import com.example.paceup.shared.supabase.client.SupabaseClientProvider
import org.koin.dsl.module

/** Koin module that provides the shared [io.github.jan.supabase.SupabaseClient] singleton. */
val supabaseModule = module {
    single { SupabaseClientProvider.create() }
}
