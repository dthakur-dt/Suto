package com.example.telegramservice

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.telegramservice.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val botHelper = TelegramBotHelper()
    private val featureManager = FeatureManager()
    private var currentFeatures = AppFeatures()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if token is properly set
        if (BuildConfig.BOT_TOKEN.contains("YOUR_NEW") || BuildConfig.BOT_TOKEN.length < 20) {
            binding.tvStatus.text = "⚠️ NAYA Token local.properties me dalo! Purana leak ho gaya hai. README dekho."
            binding.tvStatus.setBackgroundColor(0xFFFFEBEE.toInt())
        }

        // Telegram se Feature Control check karo
        checkFeaturesFromTelegram()

        binding.btnBook.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val service = binding.etService.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty() || service.isEmpty()) {
                Toast.makeText(this, "Sab fields bharo!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Telegram se control - kya booking allowed hai?
            val (canBook, reason) = featureManager.canBook(currentFeatures)
            if (!canBook) {
                binding.tvStatus.text = reason
                Toast.makeText(this, reason, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            binding.btnBook.isEnabled = false
            binding.btnBook.text = "Bhej raha hu..."
            binding.tvStatus.text = "Telegram pe bhej raha hu... Price: ${currentFeatures.servicePrice}"

            lifecycleScope.launch {
                val result = botHelper.sendBookingMessage(name, phone, "$service | Price: ${currentFeatures.servicePrice}")
                binding.btnBook.isEnabled = true
                binding.btnBook.text = getString(R.string.btn_book)
                binding.tvStatus.text = result.message + "\nLast update by: ${currentFeatures.lastUpdatedBy}"

                if (result.ok) {
                    Toast.makeText(this@MainActivity, "Booking Success ✅", Toast.LENGTH_LONG).show()
                    binding.etName.text?.clear()
                    binding.etPhone.text?.clear()
                    binding.etService.text?.clear()
                } else {
                    Toast.makeText(this@MainActivity, "Fail: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkFeaturesFromTelegram() {
        lifecycleScope.launch {
            binding.tvStatus.text = "🔄 Telegram se features check kar raha hu..."
            val features = featureManager.fetchFeatures()
            currentFeatures = features
            
            // UI update based on Telegram control
            val (canBook, reason) = featureManager.canBook(features)
            
            if (!canBook) {
                binding.btnBook.isEnabled = false
                binding.tvStatus.text = reason
                if (features.announcement.isNotEmpty()) {
                    binding.tvStatus.text = "${binding.tvStatus.text}\n📢 ${features.announcement}"
                }
            } else {
                binding.btnBook.isEnabled = true
                var statusText = "✅ Ready - Telegram Control Active\n💰 Price: ${features.servicePrice}"
                if (features.announcement.isNotEmpty()) {
                    statusText += "\n📢 ${features.announcement}"
                }
                statusText += "\n🔄 Last: ${features.lastUpdatedBy}"
                binding.tvStatus.text = statusText
            }
        }
    }
}
