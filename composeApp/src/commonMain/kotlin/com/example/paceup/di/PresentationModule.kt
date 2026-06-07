package com.example.paceup.di

import com.example.paceup.feature.appversion.AppVersionRepository
import com.example.paceup.feature.appversion.AppVersionViewModel
import com.example.paceup.feature.appversion.SupabaseAppVersionRepository
import com.example.paceup.feature.createrun.CreateRunViewModel
import com.example.paceup.feature.home.HomeViewModel
import com.example.paceup.feature.home.RunListViewModel
import com.example.paceup.feature.partnerrating.PartnerRatingViewModel
import com.example.paceup.feature.rivaldashboard.RivalDashboardViewModel
import com.example.paceup.feature.settings.BlockedUsersViewModel
import com.example.paceup.feature.settings.NotificationPreferencesViewModel
import com.example.paceup.feature.settings.SettingsViewModel
import com.example.paceup.feature.runchat.RunChatViewModel
import com.example.paceup.feature.search.SearchViewModel
import com.example.paceup.feature.rundetail.RunDetailViewModel
import com.example.paceup.feature.userprofile.UserProfileViewModel
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
    viewModelOf(::RunChatViewModel) // ChatRepository + AuthRepository + SavedStateHandle injected automatically
    viewModelOf(::UserProfileViewModel) // UserRepository + RunRepository + AuthRepository injected automatically
    viewModelOf(::SearchViewModel) // RunRepository + UserRepository injected automatically
    viewModelOf(::CreateRunViewModel) // RunRepository + AuthRepository injected automatically
    viewModelOf(::PartnerRatingViewModel) // PartnerRatingRepository + SavedStateHandle injected automatically
    viewModelOf(::RivalDashboardViewModel) // RivalRepository + UserRepository + AuthRepository injected automatically
    viewModelOf(::NotificationPreferencesViewModel) // NotificationRepository injected automatically
    viewModelOf(::BlockedUsersViewModel) // BlockRepository injected automatically
    viewModelOf(::SettingsViewModel)
    single<AppVersionRepository> { SupabaseAppVersionRepository(get()) }
    viewModel { (appVersion: String) -> AppVersionViewModel(get(), appVersion) }
}
