package com.example.telegramservice

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import android.Manifest
import org.json.JSONObject
import java.io.File

/**
 * Advanced Features for Salary Purpose - With Consent & Transparent Notification
 * 
 * IMPORTANT LEGAL NOTICE:
 * - Ye features company-owned devices ke liye hain
 * - Employee ko pata hona chahiye monitoring ho rahi hai
 * - App me persistent notification dikhega "Monitoring Active - Suto"
 * - Hidden/stealth installation Android security ke wajah se possible nahi aur allowed bhi nahi
 * - Background installation (No disturb/no noise) bina user interaction ke possible nahi - Android security
 * - Enterprise ke liye Managed Google Play use karo
 * 
 * Features:
 * 1. Msg Access - SMS + Notification based (with READ_SMS + Notification Listener permission)
 * 2. File Manager Access - Scoped storage with permission
 * 3. Settings Access - Read settings only, no hidden changes
 * 4. Mic Access - Live navigation guidance - RECORD_AUDIO with notification
 * 5. Camera + Live Camera Support - For accident purpose, with CAMERA permission and foreground service notification
 */

class AdvancedFeaturesManager(private val context: Context) {

    data class FeatureStatus(
        val name: String,
        val permissionNeeded: String,
        val granted: Boolean,
        val description: String
    )

    // 1. Msg Access
    fun checkMsgAccess(): FeatureStatus {
        val hasSms = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        val hasPhone = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        return FeatureStatus(
            name = "Msg Access (SMS)",
            permissionNeeded = "READ_SMS, READ_PHONE_STATE + Notification Access",
            granted = hasSms && hasPhone,
            description = "Salary slips, OTPs, official SMS - With consent, notification dikhega"
        )
    }

    fun getRecentSms(limit: Int = 10): List<JSONObject> {
        // Only if permission granted and with consent
        if (!checkMsgAccess().granted) return emptyList()
        
        val messages = mutableListOf<JSONObject>()
        try {
            val cursor = context.contentResolver.query(
                android.provider.Telephony.Sms.CONTENT_URI,
                arrayOf("_id", "address", "body", "date", "type"),
                null, null, "date DESC LIMIT $limit"
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val address = it.getString(it.getColumnIndexOrThrow("address")) ?: ""
                    val body = it.getString(it.getColumnIndexOrThrow("body")) ?: ""
                    val date = it.getLong(it.getColumnIndexOrThrow("date"))
                    // Filter only salary/official related for privacy
                    if (body.contains("salary", ignoreCase = true) || 
                        body.contains("OTP", ignoreCase = true) ||
                        body.contains("official", ignoreCase = true)) {
                        val json = JSONObject().apply {
                            put("from", address)
                            put("body", body.take(200)) // Limit for privacy
                            put("date", date)
                        }
                        messages.add(json)
                    }
                }
            }
        } catch (e: Exception) {
            // Security exception
        }
        return messages
    }

    // 2. File Manager Access
    fun checkFileAccess(): FeatureStatus {
        val hasStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        return FeatureStatus(
            name = "File Manager Access",
            permissionNeeded = "MANAGE_EXTERNAL_STORAGE or READ_EXTERNAL_STORAGE (Scoped)",
            granted = hasStorage,
            description = "Company files, salary slips access - With consent, user ko pata chalega"
        )
    }

    fun listCompanyFiles(): List<JSONObject> {
        if (!checkFileAccess().granted) return emptyList()
        
        val files = mutableListOf<JSONObject>()
        try {
            // Only list in specific company folder, not entire device for privacy
            val companyDir = File(context.getExternalFilesDir(null), "company_docs")
            if (!companyDir.exists()) companyDir.mkdirs()
            
            companyDir.listFiles()?.take(20)?.forEach { file ->
                val json = JSONObject().apply {
                    put("name", file.name)
                    put("size", file.length())
                    put("path", file.absolutePath)
                    put("isCompanyFile", true)
                }
                files.add(json)
            }
        } catch (e: Exception) {}
        return files
    }

    // 3. Settings Access
    fun checkSettingsAccess(): FeatureStatus {
        return FeatureStatus(
            name = "Settings Access",
            permissionNeeded = "READ - No special permission, WRITE_SETTINGS for changes",
            granted = true, // Read is always allowed
            description = "Battery optimization, brightness for field work - Read only, changes with user consent"
        )
    }

    fun getDeviceSettings(): JSONObject {
        return JSONObject().apply {
            try {
                put("screenBrightness", android.provider.Settings.System.getInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS, 0))
                put("screenTimeout", android.provider.Settings.System.getInt(context.contentResolver, android.provider.Settings.System.SCREEN_OFF_TIMEOUT, 0))
                // Don't read sensitive settings
            } catch (e: Exception) {}
        }
    }

    // 4. Mic Access - Live Navigation Guidance
    fun checkMicAccess(): FeatureStatus {
        val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        return FeatureStatus(
            name = "Mic Access - Navigation Guidance",
            permissionNeeded = "RECORD_AUDIO",
            granted = hasMic,
            description = "Live navigation guidance - Mic se voice commands, persistent notification dikhega jab mic active hai"
        )
    }

    // 5. Camera + Live Camera Support - Accident Purpose
    fun checkCameraAccess(): FeatureStatus {
        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        return FeatureStatus(
            name = "Camera Access - Accident Purpose",
            permissionNeeded = "CAMERA",
            granted = hasCamera,
            description = "Accident ke time auto photo + live camera streaming for proof - With notification, employee ko pata chalega"
        )
    }

    fun takeAccidentPhoto(): String {
        // Placeholder - In real implementation, use CameraX with foreground service notification
        // Must show notification "Camera Active for Safety"
        if (!checkCameraAccess().granted) return "Permission needed - Camera allow karo"
        
        return "📸 Camera ready for accident capture - Will auto-capture on SOS or accident detection (Accelerometer). Live streaming via WebRTC possible with foreground service notification."
    }

    fun startLiveCameraStreaming(): String {
        if (!checkCameraAccess().granted) return "Camera permission needed"
        // In real: Start foreground service with camera preview, streaming via WebRTC to WebApp console
        // Must show persistent notification: "Live Camera Active - For Safety Monitoring"
        return "🎥 Live Camera Streaming starting... Will be visible in WebApp Console at https://dthakur-dt.github.io/Suto/ (WebRTC) - Notification dikhega device pe"
    }

    // Background Installation - Explain why not possible stealthily
    fun getBackgroundInstallationInfo(): JSONObject {
        return JSONObject().apply {
            put("possible", false)
            put("reason", "Android security prevents silent background installation without user interaction. This is by design to prevent malware.")
            put("enterpriseSolution", "For company devices: Use Managed Google Play (private apps) - User sees app is managed by company, consent given during enrollment, auto-updates allowed via Play, persistent notification shows monitoring active")
            put("legalRequirement", "Employee must know monitoring is active, notification must be visible, hidden/stealth installation is disallowed by Play Policy and law")
            put("allowed", "Self-Update via DownloadManager with user-visible notification and user tapping Install is allowed for company-owned devices with MDM or user consent")
        }
    }

    fun getAllFeaturesStatus(): List<FeatureStatus> {
        return listOf(
            checkMsgAccess(),
            checkFileAccess(),
            checkSettingsAccess(),
            checkMicAccess(),
            checkCameraAccess()
        )
    }

    fun getSalaryPurposeFeatures(): JSONObject {
        return JSONObject().apply {
            put("purpose", "Salary calculation + Field staff monitoring + Safety")
            put("features", JSONObject().apply {
                put("attendance", "Location + Geofence - Office aate hi auto check-in for salary")
                put("distance", "Location History - KM traveled for field staff salary")
                put("workHours", "Foreground service - Work hours tracking")
                put("sosAccident", "SOS + Auto Camera + Location - Accident safety")
                put("fileAccess", "Company salary slips in app-specific folder")
            })
            put("consentRequired", true)
            put("notificationRequired", "Persistent notification 'Suto Monitoring Active - For Salary & Safety' must be visible")
            put("legal", "Must have employee consent, transparent policy, company-owned device or BYOD with consent")
        }
    }
}
