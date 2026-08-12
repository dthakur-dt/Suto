package com.example.telegramservice

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TelegramBotHelper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // BuildConfig se secure token leta hai, hardcode nahi
    private fun getBotToken(): String = BuildConfig.BOT_TOKEN
    private fun getChatId(): String = BuildConfig.CHAT_ID

    data class Result(val ok: Boolean, val message: String)

    suspend fun sendBookingMessage(name: String, phone: String, service: String): Result = withContext(Dispatchers.IO) {
        try {
            val token = getBotToken()
            val chatId = getChatId()

            if (token.contains("YOUR_NEW") || token.isEmpty() || token.length < 20) {
                return@withContext Result(false, "Bot Token set nahi hai! local.properties check karo")
            }
            if (chatId.contains("YOUR_CHAT") || chatId.isEmpty()) {
                return@withContext Result(false, "Chat ID set nahi hai!")
            }

            val fullMessage = """
                🔔 *Nayi Service Booking Aayi Hai!*
                
                👤 Name: $name
                📞 Phone: $phone
                🛠 Service: $service
                ⏰ Time: ${java.util.Date()}
                📱 App: TelegramServiceApp Fixed v2.0
            """.trimIndent()

            val url = "https://api.telegram.org/bot$token/sendMessage"

            val formBody = FormBody.Builder()
                .add("chat_id", chatId)
                .add("text", fullMessage)
                .add("parse_mode", "Markdown")
                .build()

            val request = Request.Builder()
                .url(url)
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(body)
                if (json.optBoolean("ok", false)) {
                    Result(true, "Telegram pe bhej diya ✅ Message ID: ${json.getJSONObject("result").optInt("message_id")}")
                } else {
                    Result(false, "Telegram Error: $body")
                }
            } else {
                Result(false, "HTTP ${response.code}: $body")
            }

        } catch (e: Exception) {
            Result(false, "Exception: ${e.message}")
        }
    }

    suspend fun testBotConnection(): Result = withContext(Dispatchers.IO) {
        try {
            val token = getBotToken()
            val url = "https://api.telegram.org/bot$token/getMe"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                Result(true, "Bot Connected: $body")
            } else {
                Result(false, body)
            }
        } catch (e: Exception) {
            Result(false, e.message ?: "Unknown error")
        }
    }
}
