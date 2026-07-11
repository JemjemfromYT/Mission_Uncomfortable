plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

base {
    archivesName.set("Mission Uncomfortable")
}

android {
    namespace = "com.example.missionuncomfortable"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.missionuncomfortable"
        minSdk = 24
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
        compose = true
    }
}

afterEvaluate {
    tasks.named("assembleDebug") {
        doLast {
            val outDir = layout.buildDirectory.asFile.get().resolve("outputs/apk/debug")
            outDir.listFiles()
                ?.filter { it.name.endsWith("-debug.apk") }
                ?.forEach { apk ->
                    val renamed = File(outDir, "Mission Uncomfortable.apk")
                    val oldName = apk.name
                    apk.renameTo(renamed)
                    val metadata = File(outDir, "output-metadata.json")
                    if (metadata.exists()) {
                        metadata.writeText(metadata.readText().replace(oldName, renamed.name))
                    }
                }
        }
    }
    tasks.named("assembleRelease") {
        doLast {
            val outDir = layout.buildDirectory.asFile.get().resolve("outputs/apk/release")
            outDir.listFiles()
                ?.filter { it.name.endsWith("-release.apk") }
                ?.forEach { apk ->
                    val renamed = File(outDir, "Mission Uncomfortable.apk")
                    val oldName = apk.name
                    apk.renameTo(renamed)
                    val metadata = File(outDir, "output-metadata.json")
                    if (metadata.exists()) {
                        metadata.writeText(metadata.readText().replace(oldName, renamed.name))
                    }
                }
        }
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}