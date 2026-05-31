package com.example.paceup.shared.notifications

import io.github.jan.supabase.SupabaseClient
import org.koin.dsl.module

/** Koin bindings for push-notification infrastructure. */
val notificationsModule = module {
    single<NotificationRepository> { SupabaseNotificationRepository(get<SupabaseClient>()) }
}
