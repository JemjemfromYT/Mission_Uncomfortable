// App-level build file — configures the :app module specifically.
// This is where you add dependencies, enable Compose, and set SDK versions.
plugins {
    alias(libs.plugins.android.application)  // Makes this an Android app (not a library)
    alias(libs.plugins.kotlin.android)       // Enables Kotlin in this module
    alias(libs.plugins.kotlin.compose)       // Enables Jetpack Compose compiler (required for Kotlin 2.0+)
}

android {
    namespace = "com.example.missionuncomfortable"
    compileSdk = 35  // The SDK version used to compile — always use a stable release

    defaultConfig {
        applicationId = "com.example.missionuncomfortable"
        minSdk = 24          // Minimum Android version supported (Android 7.0 Nougat = 99.2% of devices)
        targetSdk = 35       // The SDK version the app is designed/tested for
        versionCode = 1      // Internal version number — increment with every release to the Play Store
        versionName = "1.0"  // User-facing version string shown in Play Store

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false  // Set to true when releasing to shrink and obfuscate code
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11  // Java 11 source compatibility
        targetCompatibility = JavaVersion.VERSION_11  // Java 11 bytecode target
    }

    kotlinOptions {
        jvmTarget = "11"  // Must match compileOptions above
    }

    buildFeatures {
        compose = true  // Enables Jetpack Compose — REQUIRED to use @Composable functions
    }
}

dependencies {

    // ── AndroidX Core ────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)      // Kotlin extensions for Android core APIs
    implementation(libs.androidx.appcompat)     // Backwards-compatible Activity/Fragment support

    // ── Jetpack Compose BOM ───────────────────────────────────────────────────
    // BOM = Bill of Materials. It pins all Compose library versions so they
    // stay compatible with each other — you don't need individual version numbers.
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // ── Core Compose UI ───────────────────────────────────────────────────────
    implementation(libs.androidx.compose.ui)                   // Core Compose UI building blocks
    implementation(libs.androidx.compose.ui.graphics)          // Graphics/drawing primitives
    implementation(libs.androidx.compose.ui.tooling.preview)   // @Preview annotation support

    // ── Material 3 ────────────────────────────────────────────────────────────
    // Provides Text, CircularProgressIndicator, and other Material components
    // used throughout DashboardScreen.kt
    implementation(libs.androidx.compose.material3)

    // ── LiveData → Compose Bridge ─────────────────────────────────────────────
    // REQUIRED: provides observeAsState() used in DashboardScreen.kt to
    // convert LiveData from DashboardViewModel into Compose State.
    // Without this the project WILL NOT compile.
    implementation(libs.androidx.compose.runtime.livedata)

    // ── ViewModel + LiveData ──────────────────────────────────────────────────
    implementation(libs.androidx.lifecycle.viewmodel.ktx)       // ViewModel base class + coroutine support
    implementation(libs.androidx.lifecycle.livedata.ktx)        // LiveData + coroutine extensions
    // Provides viewModel() composable function used in DashboardScreen.kt
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // ── Activity Compose ──────────────────────────────────────────────────────
    // Provides setContent {} in MainActivity so you can call DashboardScreen() directly
    implementation(libs.androidx.activity.compose)

    // ── Debug Tools ──────────────────────────────────────────────────────────
    // UI inspector and layout debugger — only included in debug builds
    debugImplementation(libs.androidx.compose.ui.tooling)

    // ── Testing ───────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
