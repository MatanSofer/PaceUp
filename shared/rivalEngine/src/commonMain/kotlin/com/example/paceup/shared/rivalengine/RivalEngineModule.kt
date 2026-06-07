package com.example.paceup.shared.rivalengine

import com.example.paceup.shared.rivalengine.data.SupabaseRivalRepository
import com.example.paceup.shared.rivalengine.domain.RivalRepository
import org.koin.dsl.module

/** Koin bindings for the rival system. Requires [SupabaseClient] from supabaseModule. */
val rivalEngineModule = module {
    single<RivalRepository> { SupabaseRivalRepository(get()) }
}
