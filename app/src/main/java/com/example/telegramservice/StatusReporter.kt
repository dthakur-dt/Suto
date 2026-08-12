package com.example.telegramservice

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.util.Base64

/**
 * GitHub ko Cloud ke taur par use karna
 * Android App -> GitHub Repo (Suto) -> Telegram WebApp Console
 */

class StatusReporter(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // GitHub Config - Testing ke liye testing token use kar rahe hain
    // Production me naya token banao aur local.properties me rakho
    private val GITHUB_USER = "dthakur-dt"
    private val GITHUB_REPO = "Suto"
    private val GITHUB_BRANCH = "main"

    // GitHub PAT - BuildConfig se aayega (local.properties me daalo)
    // github.pat=ghp_... (testing ke liye purana use kar sakte ho, baad me badalna)
    private fun getGitHubPat(): String {
        return try {
            BuildConfig.GITHUB_PAT
        } catch (e: Exception) {
            ""
        }
    }

    fun getDeviceId(): String {
        // Unique device ID
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return "DEVICE_${androidId.take(8).uppercase()}"
    }

    fun getDeviceName(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    fun getBatteryLevel(): Int {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            -1
        }
    }

    fun createStatusJson(customOutput: String): JSONObject {
        val now = Date()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        return JSONObject().apply {
            put("deviceId", getDeviceId())
            put("name", getDeviceName())
            put("model", Build.MODEL)
            put("manufacturer", Build.MANUFACTURER)
            put("androidVersion", Build.VERSION.RELEASE)
            put("sdkInt", Build.VERSION.SDK_INT)
            put("appVersion", BuildConfig.VERSION_NAME)
            put("lastSeen", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(now))
            put("lastSeenReadable", sdf.format(now))
            put("battery", getBatteryLevel())
            put("status", "online")
            put("output", customOutput.ifEmpty { 
                """
                ✅ App Running
                📱 Device: ${getDeviceName()}
                🔋 Battery: ${getBatteryLevel()}%
                🤖 Android: ${Build.VERSION.RELEASE}
                ⏰ Time: ${sdf.format(now)}
                📍 Status: Active and sending to GitHub Cloud
                ☁️ Cloud: github.com/$GITHUB_USER/$GITHUB_REPO
                🤖 Bot: @T1311bot - Telegram Admin Panel
                """.trimIndent()
            })
        }
    }

    /**
     * Status ko GitHub par bhejo - Cloud ke taur par
     * File path: data/devices/DEVICE_XXXX.json
     */
    suspend fun sendStatusToGitHub(customOutput: String = ""): Result = withContext(Dispatchers.IO) {
        val pat = getGitHubPat()
        if (pat.isEmpty() || pat.contains("YOUR_") || pat.length < 10) {
            return@withContext Result(false, "GitHub PAT set nahi hai - local.properties me github.pat daalo. Testing ke liye purana PAT use kar sakte ho.")
        }

        try {
            val deviceId = getDeviceId()
            val statusJson = createStatusJson(customOutput)
            val filePath = "data/devices/$deviceId.json"
            val content = statusJson.toString(2)
            val contentBase64 = Base64.encodeToString(content.toByteArray(), Base64.NO_WRAP)

            // Check if file exists to get SHA
            var sha: String? = null
            val getUrl = "https://api.github.com/repos/$GITHUB_USER/$GITHUB_REPO/contents/$filePath"
            val getRequest = Request.Builder()
                .url(getUrl)
                .header("Authorization", "token $pat")
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .build()

            val getResponse = client.newCall(getRequest).execute()
            if (getResponse.isSuccessful) {
                val body = getResponse.body?.string() ?: ""
                val json = JSONObject(body)
                sha = json.optString("sha", null)
            }

            // Now create/update file
            val putBodyJson = JSONObject().apply {
                put("message", "📱 Status update from $deviceId at ${Date()}")
                put("content", contentBase64)
                put("branch", GITHUB_BRANCH)
                if (sha != null) put("sha", sha)
            }

            val putRequest = Request.Builder()
                .url(getUrl)
                .header("Authorization", "token $pat")
                .header("Accept", "application/vnd.github.v3+json")
                .put(putBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val putResponse = client.newCall(putRequest).execute()
            val respBody = putResponse.body?.string() ?: ""

            if (putResponse.isSuccessful) {
                // Also update index file
                updateDevicesIndex(deviceId)
                Result(true, "☁️ GitHub Cloud pe bhej diya! Console me dekho: dthakur-dt.github.io/Suto/")
            } else {
                Result(false, "GitHub Error ${putResponse.code}: $respBody")
            }

        } catch (e: Exception) {
            Result(false, "Exception: ${e.message}")
        }
    }

    private suspend fun updateDevicesIndex(deviceId: String) {
        // Update data/devices/index.json with list of device IDs
        try {
            val pat = getGitHubPat()
            val indexPath = "data/devices/index.json"
            val getUrl = "https://api.github.com/repos/$GITHUB_USER/$GITHUB_REPO/contents/$indexPath"
            
            var existingDevices = mutableSetOf<String>()
            var sha: String? = null

            val getRequest = Request.Builder()
                .url(getUrl)
                .header("Authorization", "token $pat")
                .get()
                .build()
            val getResp = client.newCall(getRequest).execute()
            if (getResp.isSuccessful) {
                val body = getResp.body?.string() ?: ""
                val json = JSONObject(body)
                sha = json.optString("sha", null)
                val contentB64 = json.optString("content", "")
                if (contentB64.isNotEmpty()) {
                    val contentStr = String(Base64.decode(contentB64, Base64.DEFAULT))
                    val contentJson = JSONObject(contentStr)
                    val arr = contentJson.optJSONArray("devices")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            existingDevices.add(arr.getString(i))
                        }
                    }
                }
            }

            existingDevices.add(deviceId)

            val newContent = JSONObject().apply {
                put("devices", org.json.JSONArray(existingDevices.toList()))
                put("lastUpdated", Date().toString())
                put("total", existingDevices.size)
            }

            val newContentB64 = Base64.encodeToString(newContent.toString(2).toByteArray(), Base64.NO_WRAP)
            val putBody = JSONObject().apply {
                put("message", "📋 Update devices index - $deviceId")
                put("content", newContentB64)
                put("branch", GITHUB_BRANCH)
                if (sha != null) put("sha", sha)
            }

            val putRequest = Request.Builder()
                .url(getUrl)
                .header("Authorization", "token $pat")
                .put(putBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(putRequest).execute()

        } catch (e: Exception) {
            // Ignore index update error
        }
    }

    /**
     * Fallback: Telegram par bhi status bhejo (agar GitHub fail ho)
     */
    suspend fun sendStatusToTelegram(customOutput: String): TelegramBotHelper.Result {
        val helper = TelegramBotHelper()
        val deviceId = getDeviceId()
        val statusJson = createStatusJson(customOutput)
        
        val telegramMessage = """
            📱 *Device Status - GitHub Cloud*
            
            🆔 Device: $deviceId
            📱 Model: ${getDeviceName()}
            🔋 Battery: ${getBatteryLevel()}%
            ⏰ Time: ${statusJson.optString("lastSeenReadable")}
            
            📝 *Output:*
            ```
            ${customOutput.ifEmpty { statusJson.optString("output") }}
            ```
            
            ☁️ GitHub: dthakur-dt/Suto/data/devices/$deviceId.json
            💻 Console: dthakur-dt.github.io/Suto/
        """.trimIndent()

        // Use helper to send as booking message with full details
        return helper.sendBookingMessage(
            name = "STATUS_$deviceId",
            phone = "Battery:${getBatteryLevel()}%",
            service = telegramMessage
        )
    }

    data class Result(val ok: Boolean, val message: String)
}
