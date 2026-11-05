// The Android app: an Activity, a ViewModel, and a Canvas. All game rules come
// from the :game module.
//
// Note: from AGP 9, Kotlin support is built into the Android plugin, so there
// is no separate `org.jetbrains.kotlin.android` plugin here. The Compose
// plugin is still needed because it adds the compiler step that turns
// @Composable functions into UI code.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ryanshumway.dinorunner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ryanshumway.dinorunner"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    // Matches the JVM target of :game.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":game"))

    // The BOM pins every Compose library to versions tested together, so the
    // individual Compose lines below have no version of their own.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
}
