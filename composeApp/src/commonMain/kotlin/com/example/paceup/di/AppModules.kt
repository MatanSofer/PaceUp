package com.example.paceup.di

import com.example.paceup.shared.supabase.di.supabaseModule
import org.koin.core.module.Module

/** All shared KMP Koin modules. Add new modules here as each feature is built. */
val sharedModules: List<Module> = listOf(
    supabaseModule,
)
