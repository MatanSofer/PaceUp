package com.example.paceup.di

import com.example.paceup.feature.login.LoginViewModel
import com.example.paceup.feature.signup.SignUpViewModel
import com.example.paceup.feature.welcome.WelcomeViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Koin bindings for all screen ViewModels. Add new ViewModels here as each screen is built. */
val presentationModule: Module = module {
    viewModelOf(::WelcomeViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::SignUpViewModel)
}
