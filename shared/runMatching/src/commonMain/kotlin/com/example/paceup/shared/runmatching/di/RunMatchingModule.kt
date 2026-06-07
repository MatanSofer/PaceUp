package com.example.paceup.shared.runmatching.di

import com.example.paceup.shared.runmatching.data.SupabaseBlockRepository
import com.example.paceup.shared.runmatching.data.SupabaseChatRepository
import com.example.paceup.shared.runmatching.data.SupabasePartnerRatingRepository
import com.example.paceup.shared.runmatching.data.SupabaseRunRepository
import com.example.paceup.shared.runmatching.data.SupabaseUserRepository
import com.example.paceup.shared.runmatching.domain.BlockRepository
import com.example.paceup.shared.runmatching.domain.ChatRepository
import com.example.paceup.shared.runmatching.domain.PartnerRatingRepository
import com.example.paceup.shared.runmatching.domain.RunRepository
import com.example.paceup.shared.runmatching.domain.UserRepository
import org.koin.dsl.module

/** Koin bindings for run discovery, group chat, partner ratings, and user blocking. Requires [SupabaseClient] from supabaseModule. */
val runMatchingModule = module {
    single<RunRepository> { SupabaseRunRepository(get()) }
    single<UserRepository> { SupabaseUserRepository(get()) }
    single<ChatRepository> { SupabaseChatRepository(get()) }
    single<PartnerRatingRepository> { SupabasePartnerRatingRepository(get()) }
    single<BlockRepository> { SupabaseBlockRepository(get()) }
}
