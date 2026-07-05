// App-level build file — configures the :app module.
// AGP 9+ applies Kotlin automatically for .kt files, so we do NOT add kotlin.android here.
// We only add the Compose compiler plugin on top of what AGP already sets up.
plugins {
    alias(libs.plugins.android.application)  // Android app plugin — handles Kotlin automatically in AGP 9
    alias(libs.plugins.kotlin.compose)       // Compose compiler plugin — required for @Composable functions
}

android {
    namespace = "com.example.missionuncomfortable"
    compileSdk = 35  // Stable SDK — changed from the AGP 9 preview syntax to standard integer

    defaultConfig {
        applicationId = "com.example.missionuncomfortable"
        minSdk = 24          // Android 7.0 — covers 99%+ of devices
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true  // Enables Jetpack Compose — REQUIRED to use @Composable functions
    }
}

dependencies {

    // ── Jetpack Compose BOM ───────────────────────────────────────────────────
    // BOM pins all Compose library versions so they stay compatible with each other.
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // ── AndroidX Core ────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // ── Core Compose UI ───────────────────────────────────────────────────────
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // ── Material 3 ────────────────────────────────────────────────────────────
    implementation(libs.androidx.compose.material3)

    // ── LiveData → Compose Bridge ─────────────────────────────────────────────
    // REQUIRED: provides observeAsState() used in DashboardScreen.kt
    implementation(libs.androidx.compose.runtime.livedata)

    // ── ViewModel + LiveData ──────────────────────────────────────────────────
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)  // provides viewModel() composable

    // ── Activity Compose ──────────────────────────────────────────────────────
    implementation(libs.androidx.activity.compose)  // provides setContent {} in MainActivity

    // ── Debug only ────────────────────────────────────────────────────────────
    debugImplementation(libs.androidx.compose.ui.tooling)

    // ── Testing ───────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
