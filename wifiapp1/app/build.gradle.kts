plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.kapt") // Changed to direct ID to avoid version conflict
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0" // Fixed plugin ID and version
}

android {
    namespace = "com.example.wifiapp1"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.wifiapp1"
        minSdk = 29
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
//    implementation(libs.androidx.material)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.common.jvm)
    implementation(libs.androidx.room.runtime.android)

    //sau
//    implementation(libs.androidx.room.runtime)
//    kapt("androidx.room:room-compiler:2.6.1")
//    implementation(libs.androidx.room.ktx)



    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.room.ktx) // Added for coroutine support
    kapt(libs.androidx.room.compiler) // Added for Room annotation processing
    implementation(libs.kotlinx.serialization.json) // Added for JSON serialization
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.google.material) // Added for Material 3 XML themes


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

