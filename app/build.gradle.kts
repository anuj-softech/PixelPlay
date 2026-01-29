plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.rock.pixelplay"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rock.pixelplay"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        viewBinding = true
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.cardview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    ksp("com.github.bumptech.glide:compiler:4.16.0")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.0")

    implementation(files("libs/media3-decode-ffmpeg-1.9.1.aar"))
    implementation(files("libs/media3-decode-av1-1.9.1.aar"))
    implementation(files("libs/media3-decode-flac-1.9.1.aar"))
    implementation(files("libs/media3-decode-iamf-1.9.1.aar"))
    implementation(files("libs/media3-decode-opus-1.9.1.aar"))
    implementation(files("libs/media3-decode-vp9-1.9.1.aar"))
    implementation("androidx.media3:media3-exoplayer:1.9.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.9.1")
    implementation("androidx.media3:media3-ui:1.9.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.9.1")
    implementation("androidx.media3:media3-common:1.9.1")
    implementation("androidx.media3:media3-decoder:1.9.1")
    implementation("androidx.media3:media3-effect:1.9.1")
    implementation("androidx.media3:media3-common-ktx:1.9.1")
    implementation("androidx.media3:media3-transformer:1.9.1")



}