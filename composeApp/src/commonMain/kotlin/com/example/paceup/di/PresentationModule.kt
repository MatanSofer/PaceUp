package com.example.paceup.di

import com.example.paceup.feature.appversion.AppVersionRepository
import com.example.paceup.feature.appversion.AppVersionViewModel
import com.example.paceup.feature.appversion.SupabaseAppVersionRepository
import com.example.paceup.feature.home.HomeViewModel
import com.example.paceup.feature.home.RunListViewModel
import com.example.paceup.feature.rundetail.RunDetailViewModel
import com.example.paceup.feature.login.LoginViewModel
import com.example.paceup.feature.signup.SignUpViewModel
import com.example.paceup.feature.locationpermission.LocationPermissionViewModel
import com.example.paceup.feature.notificationpermission.NotificationPermissionViewModel
import com.example.paceup.feature.profilesetup.ProfileSetupViewModel
import com.example.paceup.feature.stravaconnect.StravaConnectViewModel
import com.example.paceup.feature.welcome.WelcomeViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Koin bindings for all screen ViewModels. Add new ViewModels here as each screen is built. */
val presentationModule: Module = module {
    viewModelOf(::WelcomeViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::SignUpViewModel)
    viewModelOf(::StravaConnectViewModel)
    viewModelOf(::LocationPermissionViewModel)
    viewModelOf(::NotificationPermissionViewModel)
    viewModelOf(::ProfileSetupViewModel)
    viewModelOf(::HomeViewModel) // RunRepository injected automatically
    viewModelOf(::RunListViewModel)
    viewModelOf(::RunDetailViewModel)
    single<AppVersionRepository> { SupabaseAppVersionRepository(get()) }
    viewModel { (appVersion: String) -> AppVersionViewModel(get(), appVersion) }
}
