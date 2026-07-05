// Root build file — applies to ALL modules. Keep this minimal.
// Do NOT add kotlin.android here — AGP 9 handles Kotlin automatically.
plugins {
    alias(libs.plugins.android.application) apply false  // Android app plugin
    alias(libs.plugins.kotlin.compose) apply false       // Compose compiler plugin
}
