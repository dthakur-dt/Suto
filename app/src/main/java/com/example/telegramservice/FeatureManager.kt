package com.example.telegramservice

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Telegram se App Feature Control
 * 
 * Ye class Firebase ya simple HTTP se features check karti hai
 * Telegram bot isko update karta hai
 */

data class AppFeatures(
    val bookingEnabled: Boolean = true,
    val maintenanceMode: Boolean = false,
    val servicePrice: String = "₹100",
    val announcement: String = "",
    val paymentEnabled: Boolean = true,
    val lastUpdatedBy: String = "Telegram",
    val updatedAt: String = ""
)

class FeatureManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Option 1: Firebase Realtime Database URL (Aapko Firebase banana hoga - free hai)
    // Example: https://your-project-default-rtdb.firebaseio.com/app_features.json
    // Isko local.properties me dalo: firebase.url=YOUR_FIREBASE_URL
    private var firebaseUrl: String = ""

    init {
        // BuildConfig se Firebase URL lo agar set hai
        try {
            firebaseUrl = BuildConfig.FIREBASE_URL
        } catch (e: Exception) {
            firebaseUrl = ""
        }
    }

    // Cache last features
    private var lastFeatures = AppFeatures()

    /**
     * Features ko Firebase se fetch karo
     * Telegram bot isi Firebase ko update karta hai
     */
    suspend fun fetchFeatures(): AppFeatures = withContext(Dispatchers.IO) {
        // Agar Firebase URL nahi hai, to default features do
        // User ko Firebase setup guide README me milega
        if (firebaseUrl.isEmpty() || firebaseUrl.contains("YOUR_") || !firebaseUrl.startsWith("http")) {
            // Default - sab ON
            return@withContext lastFeatures
        }

        try {
            val request = Request.Builder().url(firebaseUrl).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful && body.isNotEmpty() && body != "null") {
                val json = JSONObject(body)
                lastFeatures = AppFeatures(
                    bookingEnabled = json.optBoolean("bookingEnabled", true),
                    maintenanceMode = json.optBoolean("maintenanceMode", false),
                    servicePrice = json.optString("servicePrice", "₹100"),
                    announcement = json.optString("announcement", ""),
                    paymentEnabled = json.optBoolean("paymentEnabled", true),
                    lastUpdatedBy = json.optString("lastUpdatedBy", "Telegram"),
                    updatedAt = json.optString("updatedAt", "")
                )
                return@withContext lastFeatures
            }
        } catch (e: Exception) {
            // Error pe last cache do
        }
        return@withContext lastFeatures
    }

    /**
     * Check if booking allowed hai ya nahi
     */
    fun canBook(features: AppFeatures): Pair<Boolean, String> {
        if (features.maintenanceMode) {
            return Pair(false, "🔧 App Maintenance me hai. ${features.announcement}")
        }
        if (!features.bookingEnabled) {
            return Pair(false, "🚫 Booking abhi band hai. ${features.announcement}")
        }
        return Pair(true, "")
    }
}
