# Aapke Purane Project ko Kaise Fix Kare (Agar Naya Nahi Chahiye)

Agar aapko purana wala hi theek karna hai, ye steps karo:

### 1. Build Fail - Gradle Issue
```
# Android Studio me:
File -> Invalidate Caches / Restart -> Invalidate and Restart

# Terminal me:
./gradlew clean
./gradlew --stop
```

`gradle-wrapper.properties` me check karo:
```
distributionUrl=https\://services.gradle.org/distributions/gradle-8.4-bin.zip
```

`build.gradle` me:
```gradle
compileSdk 34
targetSdk 34
minSdk 24
```

### 2. Token Hardcoded ki Wajah se Fail?
Agar aapne aise likha tha:
```kotlin
val token = "8670943963:AAFz--BuWd6..." // GALAT - Leak!
```
To GitHub / Play Protect block kar deta hai.

**Fix:**
`local.properties` me daalo:
```
telegram.bot.token=NEW_TOKEN
telegram.chat.id=289240360
```

`app/build.gradle` me:
```gradle
buildConfigField "String", "BOT_TOKEN", "\"${localProperties['telegram.bot.token']}\""
```

Code me:
```kotlin
val token = BuildConfig.BOT_TOKEN
```

### 3. APK Install Nahi Ho Raha
- **Reason 1:** Purana app installed hai, signature mismatch
  - Solution: Phone se purana app **uninstall** karo, phir naya install karo
- **Reason 2:** versionCode same hai
  - Solution: `app/build.gradle` me `versionCode` badhao (1 se 2, 2 se 3)
- **Reason 3:** Debug vs Release
  - Solution: Dono debug APK use karo ya release ke liye keystore same rakho

### 4. INTERNET Permission Missing
`AndroidManifest.xml` me zarur daalo:
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### 5. Telegram Bot Se Connect Nahi Ho Raha
- Token revoke ho gaya? @BotFather me check karo
- Chat ID galat? `/getUpdates` se nikal sakte ho
- OkHttp missing? `implementation("com.squareup.okhttp3:okhttp:4.12.0")` add karo

### 6. Sabse Aasaan Tarika
Maine aapke liye naya fixed project `TelegramServiceApp/` bana diya hai.
Wo 100% build hoga. Bas `local.properties` me naya token daalo aur build karo.

