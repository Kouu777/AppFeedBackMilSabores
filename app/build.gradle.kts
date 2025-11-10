plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.proyectomilsabores"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.proyectomilsabores"
        minSdk = 24
        targetSdk = 36
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
}

dependencies {
    // dep proyecto

    // Cámara
    implementation ("androidx.camera:camera-camera2:1.3.0")
    implementation ("androidx.camera:camera-lifecycle:1.3.0")
    implementation ("androidx.camera:camera-view:1.3.0")

    // Google Auth
    implementation ("com.google.android.gms:play-services-auth:20.7.0")

    // Google Cloud APIs
    implementation ("com.google.cloud:google-cloud-language:2.28.0")
    implementation ("com.google.cloud:google-cloud-storage:2.28.0")

    // QR Scanning
    implementation ("com.google.mlkit:barcode-scanning:17.2.0")

    // Networking
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Image Loading
    implementation ("io.coil-kt:coil:2.5.0")

    // Permisos
    implementation ("com.google.accompanist:accompanist-permissions:0.32.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Otras dependencias que ya tienes...
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")



    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
