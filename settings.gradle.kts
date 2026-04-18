rootProject.name = "PaceUp"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")
include(":androidApp")

include(":shared:auth")
include(":shared:supabase")
include(":shared:realtime")
include(":shared:activities")
include(":shared:paceZone")
include(":shared:runMatching")
include(":shared:rivalEngine")
include(":shared:reputationEngine")
include(":shared:notifications")
include(":shared:database")
include(":shared:network")
include(":shared:deeplink")
include(":shared:i18n")