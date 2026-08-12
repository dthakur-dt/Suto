package com.example.telegramservice

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Remote Config - Bina APK Rebuild Ke App Change!
 * 
 * Flow:
 * Telegram Admin: /set_config servicePrice ₹250
 *   -> Bot updates data/remote_config.json in GitHub
 *   -> All Android Apps fetch on next open and apply
 * 
 * Can change: appName, colors, prices, messages, feature flags, UI toggles
 * No need to rebuild APK!
 */

class RemoteConfigManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val GITHUB_USER = "dthakur-dt"
    private val GITHUB_REPO = "Suto"
    private val CONFIG_URL = "https://raw.githubusercontent.com/$GITHUB_USER/$GITHUB_REPO/main/data/remote_config.json"
    private val LOCAL_FILE = "remote_config.json"

    data class RemoteConfig(
        val appName: String,
        val primaryColor: String,
        val servicePrice: String,
        val announcement: String,
        val bookingEnabled: Boolean,
        val maintenanceMode: Boolean,
        val welcomeMessage: String,
        val rawJson: JSONObject
    )

    suspend fun fetchRemoteConfig(): RemoteConfig? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(CONFIG_URL).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                
                // Save locally for offline use
                try {
                    val file = File(context.filesDir, LOCAL_FILE)
                    file.writeText(body)
                } catch (e: Exception) {}

                return@withContext RemoteConfig(
                    appName = json.optString("appName", "Suto"),
                    primaryColor = json.optString("primaryColor", "#667eea"),
                    servicePrice = json.optString("servicePrice", "₹100"),
                    announcement = json.optString("announcement", ""),
                    bookingEnabled = json.optBoolean("bookingEnabled", true),
                    maintenanceMode = json.optBoolean("maintenanceMode", false),
                    welcomeMessage = json.optJSONObject("messages")?.optString("welcome", "Welcome!") ?: "Welcome!",
                    rawJson = json
                )
            }
        } catch (e: Exception) {
            // Try local cached config
            try {
                val file = File(context.filesDir, LOCAL_FILE)
                if (file.exists()) {
                    val body = file.readText()
                    val json = JSONObject(body)
                    return@withContext RemoteConfig(
                        appName = json.optString("appName", "Suto"),
                        primaryColor = json.optString("primaryColor", "#667eea"),
                        servicePrice = json.optString("servicePrice", "₹100"),
                        announcement = json.optString("announcement", ""),
                        bookingEnabled = json.optBoolean("bookingEnabled", true),
                        maintenanceMode = json.optBoolean("maintenanceMode", false),
                        welcomeMessage = json.optJSONObject("messages")?.optString("welcome", "Welcome!") ?: "Welcome!",
                        rawJson = json
                    )
                }
            } catch (ex: Exception) {}
        }
        return@withContext null
    }

    fun applyConfigToUI(config: RemoteConfig, onUpdate: (String, String) -> Unit) {
        // Apply to UI - can change colors, texts without rebuild
        try {
            onUpdate("appName", config.appName)
            onUpdate("servicePrice", config.servicePrice)
            onUpdate("announcement", config.announcement)
            onUpdate("primaryColor", config.primaryColor)
        } catch (e: Exception) {}
    }

    fun getLocalConfig(): JSONObject? {
        return try {
            val file = File(context.filesDir, LOCAL_FILE)
            if (file.exists()) JSONObject(file.readText()) else null
        } catch (e: Exception) {
            null
        }
    }
}
