import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localProps.load(FileInputStream(localPropsFile))
}

fun getLocalProperty(key: String, defaultValue: String = ""): String {
    return localProps.getProperty(key) ?: System.getenv(key.uppercase().replace(".", "_")) ?: defaultValue
}

android {
    namespace = "com.example.telegramservice"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.telegramservice"
        minSdk = 24
        targetSdk = 34
        versionCode = 2 // Old wale se bada rakho taki install ho jaye
        versionName = "2.0-fixed"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Secure way - BuildConfig me daalo
        buildConfigField("String", "BOT_TOKEN", "\"${getLocalProperty("telegram.bot.token", "YOUR_NEW_TOKEN_HERE")}\"")
        buildConfigField("String", "CHAT_ID", "\"${getLocalProperty("telegram.chat.id", "YOUR_CHAT_ID")}\"")
        buildConfigField("String", "FIREBASE_URL", "\"${getLocalProperty("firebase.url", "")}\"")
        buildConfigField("String", "GITHUB_PAT", "\"${getLocalProperty("github.pat", "")}\"")
        buildConfigField("String", "GITHUB_USER", "\"${getLocalProperty("github.user", "dthakur-dt")}\"")
        buildConfigField("String", "GITHUB_REPO", "\"${getLocalProperty("github.repo", "Suto")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ""
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Networking for Telegram Bot + GitHub Cloud
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Location for Status Output
    implementation("com.google.android.gms:play-services-location:21.2.0")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
