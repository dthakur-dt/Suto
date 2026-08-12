package com.example.telegramservice

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Environment
import android.os.StatFs
import org.json.JSONObject
import java.io.File

/**
 * Device Health - RAM, Storage, Network, WiFi, IP
 */
class DeviceHealthReporter(private val context: Context) {

    fun getRAMInfo(): JSONObject {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            
            val totalMB = memInfo.totalMem / (1024 * 1024)
            val availMB = memInfo.availMem / (1024 * 1024)
            val usedMB = totalMB - availMB
            val percent = ((usedMB.toDouble() / totalMB) * 100).toInt()

            JSONObject().apply {
                put("totalMB", totalMB)
                put("availableMB", availMB)
                put("usedMB", usedMB)
                put("usedPercent", percent)
                put("lowMemory", memInfo.lowMemory)
                put("text", "🧠 RAM: ${usedMB}MB / ${totalMB}MB (${percent}%) ${if (memInfo.lowMemory) "🔴 Low!" else "✅ OK"}")
            }
        } catch (e: Exception) {
            JSONObject().apply { put("text", "RAM: Error ${e.message}") }
        }
    }

    fun getStorageInfo(): JSONObject {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
            val bytesTotal = stat.blockSizeLong * stat.blockCountLong
            val availGB = bytesAvailable / (1024 * 1024 * 1024)
            val totalGB = bytesTotal / (1024 * 1024 * 1024)
            val usedGB = totalGB - availGB
            val percent = if (totalGB > 0) ((usedGB.toDouble() / totalGB) * 100).toInt() else 0

            JSONObject().apply {
                put("totalGB", totalGB)
                put("availableGB", availGB)
                put("usedGB", usedGB)
                put("usedPercent", percent)
                put("text", "💾 Storage: ${usedGB}GB / ${totalGB}GB (${percent}%)")
            }
        } catch (e: Exception) {
            JSONObject().apply { put("text", "Storage: Error") }
        }
    }

    fun getNetworkInfo(): JSONObject {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(network)

            val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            val isEthernet = caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true

            var wifiName = ""
            var wifiStrength = -1
            var ip = ""

            if (isWifi) {
                try {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val wifiInfo = wifiManager.connectionInfo
                    wifiName = wifiInfo.ssid?.replace("\"", "") ?: ""
                    wifiStrength = WifiManager.calculateSignalLevel(wifiInfo.rssi, 5)
                    ip = android.text.format.Formatter.formatIpAddress(wifiInfo.ipAddress)
                } catch (e: Exception) {}
            }

            val type = when {
                isWifi -> "WiFi"
                isCellular -> "Mobile Data"
                isEthernet -> "Ethernet"
                else -> "No Internet"
            }

            JSONObject().apply {
                put("type", type)
                put("isWifi", isWifi)
                put("isCellular", isCellular)
                put("wifiName", wifiName)
                put("wifiStrength", wifiStrength)
                put("ipAddress", ip)
                put("text", "🌐 Network: $type ${if (wifiName.isNotEmpty()) "($wifiName)" else ""} ${if (ip.isNotEmpty()) "IP:$ip" else ""}")
            }
        } catch (e: Exception) {
            JSONObject().apply { put("text", "Network: Error ${e.message}") }
        }
    }

    fun getFullHealth(): JSONObject {
        return JSONObject().apply {
            put("ram", getRAMInfo())
            put("storage", getStorageInfo())
            put("network", getNetworkInfo())
            put("timestamp", System.currentTimeMillis())
        }
    }
}
