plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.ilsecondodasinistra.catchit"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ilsecondodasinistra.catchit"
        minSdk = 26
        targetSdk = 37
        versionCode = 13
        versionName = "13.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            // Personal sideloaded build: sign with the debug key.
            signingConfig = signingConfigs.getByName("debug")
            // The phones in use are all 64-bit ARM; MapLibre's .so is 9-13 MB per ABI and is stored uncompressed in the
            // APK, so each extra ABI is that much to send and to keep on the phone. The debug build keeps all of them so any emulator runs.
            // Add "armeabi-v7a" back for a phone with a 32-bit system (entry-level Android Go).
            ndk {
                abiFilters += listOf("arm64-v8a")
            }
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.work.runtime)
    implementation(libs.haze)
    implementation(libs.haze.materials)
    implementation(libs.maplibre)
    implementation(project(":timetable-builder"))
    // Installs the baseline profile (precompiled hot start-up paths) when the app is installed.
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
