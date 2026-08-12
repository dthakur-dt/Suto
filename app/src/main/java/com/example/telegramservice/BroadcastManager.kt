package com.example.telegramservice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Broadcast System - Telegram se sab Android users ko message
 * 
 * Flow:
 * Telegram Admin: /broadcast Diwali Offer 50% OFF!
 *   -> Bot creates data/broadcasts/broadcast_XXX.json in GitHub
 *   -> All Android Apps poll and show notification
 * 
 * Types: announcement, offer, update, alert
 */

class BroadcastManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val GITHUB_USER = "dthakur-dt"
    private val GITHUB_REPO = "Suto"
    private val BROADCAST_INDEX_URL = "https://raw.githubusercontent.com/$GITHUB_USER/$GITHUB_REPO/main/data/broadcasts/index.json"

    data class Broadcast(
        val id: String,
        val title: String,
        val message: String,
        val type: String,
        val priority: String,
        val createdAt: String,
        val createdBy: String,
        val action: String = "show_notification"
    )

    suspend fun fetchLatestBroadcasts(): List<Broadcast> = withContext(Dispatchers.IO) {
        try {
            // For simplicity, fetch broadcast_001, 002, etc. - In real, fetch index
            val broadcasts = mutableListOf<Broadcast>()
            
            // Try to fetch index first
            try {
                val request = Request.Builder().url(BROADCAST_INDEX_URL).get().build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val arr = json.optJSONArray("broadcasts")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val id = arr.getString(i)
                            val b = fetchBroadcastById(id)
                            if (b != null) broadcasts.add(b)
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to known broadcast_001
                val fallback = fetchBroadcastById("broadcast_001")
                if (fallback != null) broadcasts.add(fallback)
            }

            return@withContext broadcasts
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchBroadcastById(id: String): Broadcast? = withContext(Dispatchers.IO) {
        try {
            val url = "https://raw.githubusercontent.com/$GITHUB_USER/$GITHUB_REPO/main/data/broadcasts/$id.json"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                return@withContext Broadcast(
                    id = json.optString("id", id),
                    title = json.optString("title", "Broadcast"),
                    message = json.optString("message", ""),
                    type = json.optString("type", "announcement"),
                    priority = json.optString("priority", "normal"),
                    createdAt = json.optString("createdAt", ""),
                    createdBy = json.optString("createdBy", ""),
                    action = json.optString("action", "show_notification")
                )
            }
        } catch (e: Exception) {}
        return@withContext null
    }

    fun showBroadcastNotification(broadcast: Broadcast) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Create channel for Android O+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "suto_broadcast",
                    "Suto Broadcasts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Broadcast messages from Telegram Admin"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, "suto_broadcast")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("📢 ${broadcast.title}")
                .setContentText(broadcast.message.take(100))
                .setStyle(NotificationCompat.BigTextStyle().bigText(broadcast.message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(broadcast.id.hashCode(), notification)
        } catch (e: Exception) {}
    }

    fun isBroadcastEnabled(): Boolean {
        return true // Check remote config
    }
}
