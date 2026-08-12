import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Load local properties safely outside android block
val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    try {
        FileInputStream(localPropsFile).use { localProps.load(it) }
    } catch (e: Exception) {
        println("Could not load local.properties: ${e.message}")
    }
}

fun getLocalProperty(key: String, defaultValue: String = ""): String {
    return localProps.getProperty(key) ?: System.getenv(key.uppercase().replace(".", "_")) ?: defaultValue
}

// Read values once, before android block, to avoid configuration mutation
val botTokenValue = getLocalProperty("telegram.bot.token", "YOUR_NEW_TOKEN_HERE")
val chatIdValue = getLocalProperty("telegram.chat.id", "YOUR_CHAT_ID")
val firebaseUrlValue = getLocalProperty("firebase.url", "")
val githubPatValue = getLocalProperty("github.pat", "")
val githubUserValue = getLocalProperty("github.user", "dthakur-dt")
val githubRepoValue = getLocalProperty("github.repo", "Suto")

android {
    namespace = "com.example.telegramservice"
    compileSdk = 34

    // Fix for dataBindingMergeDependencyArtifactsDebug failure - put buildFeatures at top
    buildFeatures {
        viewBinding = true
        buildConfig = true
        // Explicitly disable dataBinding to avoid conflict
        dataBinding = false
        aidl = false
        renderScript = false
        resValues = false
        shaders = false
    }

    defaultConfig {
        applicationId = "com.example.telegramservice"
        minSdk = 24
        targetSdk = 34
        versionCode = 4
        versionName = "4.0-full-features-fixed"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Secure - BuildConfig fields, read before configuration
        buildConfigField("String", "BOT_TOKEN", "\"${botTokenValue}\"")
        buildConfigField("String", "CHAT_ID", "\"${chatIdValue}\"")
        buildConfigField("String", "FIREBASE_URL", "\"${firebaseUrlValue}\"")
        buildConfigField("String", "GITHUB_PAT", "\"${githubPatValue}\"")
        buildConfigField("String", "GITHUB_USER", "\"${githubUserValue}\"")
        buildConfigField("String", "GITHUB_REPO", "\"${githubRepoValue}\"")
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
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Location - use stable version
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // Lifecycle - use versions compatible with AGP 8.2
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.7.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
