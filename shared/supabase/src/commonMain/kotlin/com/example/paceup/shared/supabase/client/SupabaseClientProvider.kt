package com.example.paceup.shared.supabase.client

import com.example.paceup.shared.network.logger.AppLogger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

// Supabase anon key is public by design — safe to hardcode per CLAUDE.md
private const val SUPABASE_URL = "https://xlbccbaeaesgfydkajdu.supabase.co"
private const val SUPABASE_ANON_KEY =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhsYmNjYmFlYWVzZ2Z5ZGthamR1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzY1MDA0MzksImV4cCI6MjA5MjA3NjQzOX0.JO4dR5HMm-epvFblqLfLg6RC5lpk2tIVtJftt7lwyVU"

/** Factory for the shared Supabase client. Called once by the Koin module. */
object SupabaseClientProvider {

    fun create(): SupabaseClient {
        AppLogger.d("SupabaseClientProvider", "Creating Supabase client")
        return createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY,
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }.also {
            AppLogger.d("SupabaseClientProvider", "Supabase client created")
        }
    }
}
