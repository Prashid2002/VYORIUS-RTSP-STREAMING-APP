plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.vyorius"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.vyorius"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Required for Picture-in-Picture
        vectorDrawables.useSupportLibrary = true
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

    packagingOptions {
        pickFirst ("lib/arm64-v8a/libc++_shared.so")
        pickFirst ("lib/x86_64/libc++_shared.so")
        pickFirst ("lib/x86/libc++_shared.so")
        pickFirst("lib/armeabi-v7a/libc++_shared.so")
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation("org.videolan.android:libvlc-all:3.5.1")

    implementation("com.arthenica:ffmpeg-kit-full:6.0")

    implementation("androidx.core:core:1.12.0")
    implementation("androidx.annotation:annotation:1.7.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
