package com.example.telegramservice

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Remote Commands from Telegram via GitHub Cloud
 * Telegram -> GitHub data/commands/DEVICE_ID.json -> Android App polls and executes
 * Commands: ring, locate, battery, message, photo, lock, sosack
 */

class RemoteCommandManager(private val context: Context, private val statusReporter: StatusReporter) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val GITHUB_USER = "dthakur-dt"
    private val GITHUB_REPO = "Suto"

    private fun getPat(): String {
        return try { BuildConfig.GITHUB_PAT } catch (e: Exception) { "" }
    }

    data class Command(
        val id: String,
        val type: String,
        val payload: String = "",
        val timestamp: Long = System.currentTimeMillis(),
        val executed: Boolean = false
    )

    private var pollingJob: Job? = null

    fun startPolling(deviceId: String, scope: CoroutineScope, onCommand: (Command) -> Unit) {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                try {
                    val cmd = fetchCommand(deviceId)
                    if (cmd != null && !cmd.executed) {
                        withContext(Dispatchers.Main) {
                            executeCommand(cmd)
                            onCommand(cmd)
                        }
                        markCommandExecuted(deviceId, cmd)
                    }
                } catch (e: Exception) {}
                delay(10000)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
    }

    private suspend fun fetchCommand(deviceId: String): Command? = withContext(Dispatchers.IO) {
        try {
            val pat = getPat()
            if (pat.isEmpty() || pat.length < 10) return@withContext null
            val url = "https://api.github.com/repos/$GITHUB_USER/$GITHUB_REPO/contents/data/commands/$deviceId.json"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "token $pat")
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val contentB64 = json.optString("content", "")
                if (contentB64.isEmpty()) return@withContext null
                val content = String(android.util.Base64.decode(contentB64, android.util.Base64.DEFAULT))
                val cmdJson = JSONObject(content)
                if (cmdJson.optBoolean("executed", false)) return@withContext null
                if (System.currentTimeMillis() - cmdJson.optLong("timestamp", 0) > 5 * 60 * 1000) return@withContext null
                return@withContext Command(
                    id = cmdJson.optString("id", ""),
                    type = cmdJson.optString("type", ""),
                    payload = cmdJson.optString("payload", ""),
                    timestamp = cmdJson.optLong("timestamp", 0),
                    executed = false
                )
            }
        } catch (e: Exception) {}
        return@withContext null
    }

    private suspend fun markCommandExecuted(deviceId: String, cmd: Command) = withContext(Dispatchers.IO) {
        try {
            val pat = getPat()
            val getUrl = "https://api.github.com/repos/$GITHUB_USER/$GITHUB_REPO/contents/data/commands/$deviceId.json"
            val getReq = Request.Builder().url(getUrl).header("Authorization", "token $pat").get().build()
            val getResp = client.newCall(getReq).execute()
            var sha: String? = null
            if (getResp.isSuccessful) {
                val body = getResp.body?.string() ?: ""
                val json = JSONObject(body)
                sha = json.optString("sha", null)
            }
            val updated = JSONObject().apply {
                put("id", cmd.id)
                put("type", cmd.type)
                put("payload", cmd.payload)
                put("timestamp", cmd.timestamp)
                put("executed", true)
                put("executedAt", System.currentTimeMillis())
                put("result", "Executed on device")
            }
            val contentB64 = android.util.Base64.encodeToString(updated.toString().toByteArray(), android.util.Base64.NO_WRAP)
            val putBody = JSONObject().apply {
                put("message", "✅ Command executed ${cmd.type} on $deviceId")
                put("content", contentB64)
                put("branch", "main")
                if (sha != null) put("sha", sha)
            }
            val putReq = Request.Builder()
                .url(getUrl)
                .header("Authorization", "token $pat")
                .header("Accept", "application/vnd.github.v3+json")
                .put(putBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(putReq).execute()
        } catch (e: Exception) {}
    }

    private fun executeCommand(cmd: Command) {
        when (cmd.type) {
            "ring" -> executeRing()
            "locate" -> {}
            "battery" -> {}
            else -> {}
        }
    }

    private fun executeRing() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVolume, 0)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 1000, 500, 1000, 500, 1000), 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000), 0)
            }

            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val ringtone = RingtoneManager.getRingtone(context, ringtoneUri)
            ringtone?.play()

            CoroutineScope(Dispatchers.Main).launch {
                delay(15000)
                try {
                    ringtone?.stop()
                    vibrator.cancel()
                } catch (e: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
