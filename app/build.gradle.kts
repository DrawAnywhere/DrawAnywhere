import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.shezik.drawanywhere"
    compileSdk = 36

    lint {
        disable += "NullSafeMutableLiveData"
    }

    defaultConfig {
        applicationId = "com.shezik.drawanywhere"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    // Disable extra signing block 'Dependency metadata'
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.ui.text)
    implementation(libs.ui)
    implementation(libs.androidx.ui.unit)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.datastore.preferences.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.material)

    // Transitive dependencies
    androidTestImplementation(libs.androidx.monitor)
    androidTestImplementation(libs.junit)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.animation.core)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.foundation.layout)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.ui.geometry)
    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.savedstate)
    implementation(libs.kotlinx.coroutines.core)
}

// Disable baseline.prof for reproducibility
tasks.whenTaskAdded {
    if (name.contains("ArtProfile")) {
        enabled = false
    }
}