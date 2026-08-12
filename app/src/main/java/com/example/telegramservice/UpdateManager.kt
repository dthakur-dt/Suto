package com.example.telegramservice

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Silent Self-Update System
 * Telegram se /update_app bhejoge to sab devices pe silent auto-update
 * 
 * Flow:
 * 1. data/app_version.json GitHub pe check karta hai
 * 2. Agar naya version hai to APK download karta hai silently
 * 3. Download complete hone par install prompt ya auto-install
 * 4. User ko pata bhi nahi chalega - silent update!
 */

class UpdateManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val GITHUB_USER = "dthakur-dt"
    private val GITHUB_REPO = "Suto"
    private val VERSION_URL = "https://raw.githubusercontent.com/$GITHUB_USER/$GITHUB_REPO/main/data/app_version.json"
    private val LOCAL_VERSION_URL = "https://dthakur-dt.github.io/Suto/data/app_version.json"

    data class AppVersion(
        val versionCode: Int,
        val versionName: String,
        val apkUrl: String,
        val apkUrlBackup: String,
        val releaseNotes: String,
        val mandatory: Boolean,
        val sizeMB: Double
    )

    suspend fun checkForUpdate(): AppVersion? = withContext(Dispatchers.IO) {
        try {
            // Try GitHub raw first, then Pages
            var json: JSONObject? = null
            
            for (url in listOf(VERSION_URL, LOCAL_VERSION_URL)) {
                try {
                    val request = Request.Builder().url(url).get().build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: continue
                        json = JSONObject(body)
                        break
                    }
                } catch (e: Exception) {
                    continue
                }
            }

            if (json == null) return@withContext null

            val version = AppVersion(
                versionCode = json.optInt("versionCode", 0),
                versionName = json.optString("versionName", ""),
                apkUrl = json.optString("apkUrl", "https://dthakur-dt.github.io/Suto/app-debug.apk"),
                apkUrlBackup = json.optString("apkUrlBackup", ""),
                releaseNotes = json.optString("releaseNotes", ""),
                mandatory = json.optBoolean("mandatory", false),
                sizeMB = json.optDouble("sizeMB", 6.6)
            )

            // Check if update needed
            val currentCode = BuildConfig.VERSION_CODE
            if (version.versionCode > currentCode) {
                return@withContext version
            }

            return@withContext null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun downloadAndInstallApk(version: AppVersion, onProgress: (Int) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        try {
            onProgress(10)
            
            // Use DownloadManager for silent download
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            
            val uri = Uri.parse(version.apkUrl)
            val request = DownloadManager.Request(uri).apply {
                setTitle("Suto Update v${version.versionName}")
                setDescription("Downloading ${version.sizeMB}MB - ${version.releaseNotes.take(50)}")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Suto_${version.versionName}.apk")
                setMimeType("application/vnd.android.package-archive")
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadId = downloadManager.enqueue(request)
            onProgress(30)

            // Poll for download completion (simplified - in real app use BroadcastReceiver)
            // For now, return true and let system handle install via notification
            onProgress(100)
            
            true
        } catch (e: Exception) {
            // Fallback: Open browser to download
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(version.apkUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (ex: Exception) {
                false
            }
        }
    }

    fun getCurrentVersion(): String {
        return "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }

    fun isSelfUpdateEnabled(): Boolean {
        return try {
            // Check remote config
            val remoteConfigFile = File(context.filesDir, "remote_config.json")
            if (remoteConfigFile.exists()) {
                val json = JSONObject(remoteConfigFile.readText())
                json.optJSONObject("features")?.optBoolean("selfUpdate", true) ?: true
            } else {
                true
            }
        } catch (e: Exception) {
            true
        }
    }
}
