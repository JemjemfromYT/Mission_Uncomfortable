// Top-level build file — configuration that applies to ALL modules in the project.
// Individual module settings (like app dependencies) go in app/build.gradle.kts instead.
plugins {
    alias(libs.plugins.android.application) apply false  // Android app plugin — applied in :app module
    alias(libs.plugins.kotlin.android) apply false       // Kotlin Android plugin — applied in :app module
    alias(libs.plugins.kotlin.compose) apply false       // Kotlin Compose compiler plugin — applied in :app module
}
