import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(project(":composeApp"))
            implementation(project(":shared:network"))
            implementation(project(":shared:database"))
            implementation(project(":shared:auth"))
            implementation(project(":shared:notifications"))
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.androidx.core.ktx)
            implementation(libs.firebase.messaging)
        }
    }
}

android {
    namespace = "com.paceup.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.paceup"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        val localProps = Properties().apply {
            val f = rootProject.file("local.properties")
            if (f.exists()) load(f.inputStream())
        }
        buildConfigField("String", "STRAVA_CLIENT_ID", "\"${localProps["strava.client.id"] ?: ""}\"")
        buildConfigField("String", "STRAVA_CLIENT_SECRET", "\"${localProps["strava.client.secret"] ?: ""}\"")
        manifestPlaceholders["MAPS_API_KEY"] = localProps["maps.api.key"] ?: ""
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
    // Firebase BOM moved here — platform() inside KMP sourceSets is removed in Kotlin 2.3 (KT-58759)
    add("androidMainImplementation", platform(libs.firebase.bom))
}
