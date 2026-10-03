package com.example.smstotelegram

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.smstotelegram.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissionsLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "تم منح الصلاحيات", Toast.LENGTH_SHORT).show()
            startServiceIfConfigured()
        } else {
            Toast.makeText(
                this,
                "لازم تسمح بصلاحية قراءة الرسائل عشان التطبيق يشتغل",
                Toast.LENGTH_LONG
            ).show()
        }
        updatePermissionStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadSettingsIntoFields()
        updatePermissionStatus()

        binding.btnGrantPermissions.setOnClickListener {
            requestNeededPermissions()
        }

        binding.btnSave.setOnClickListener {
            saveSettings()
        }

        binding.switchEnabled.isChecked = Prefs.isEnabled(this)
        binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setEnabled(this, isChecked)
        }
    }

    private fun loadSettingsIntoFields() {
        binding.etBotToken.setText(Prefs.getBotToken(this))
        binding.etChatId.setText(Prefs.getChatId(this))
        binding.etSenderFilter.setText(Prefs.getSenderFilter(this))
    }

    private fun saveSettings() {
        val token = binding.etBotToken.text.toString().trim()
        val chatId = binding.etChatId.text.toString().trim()
        val sender = binding.etSenderFilter.text.toString().trim()

        if (token.isBlank() || chatId.isBlank()) {
            Toast.makeText(this, "لازم تدخل Bot Token و Chat ID", Toast.LENGTH_SHORT).show()
            return
        }

        Prefs.setBotToken(this, token)
        Prefs.setChatId(this, chatId)
        Prefs.setSenderFilter(this, sender)

        Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show()
        startServiceIfConfigured()
    }

    private fun requestNeededPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionsLauncher.launch(permissions.toTypedArray())
    }

    private fun hasAllPermissions(): Boolean {
        val smsGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECEIVE_SMS
            ) == PackageManager.PERMISSION_GRANTED

        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        return smsGranted && notificationsGranted
    }

    private fun updatePermissionStatus() {
        binding.tvPermissionStatus.text = if (hasAllPermissions()) {
            "الصلاحيات: ممنوحة ✅"
        } else {
            "الصلاحيات: غير ممنوحة ❌ (اضغط الزر فوق)"
        }
    }

    private fun startServiceIfConfigured() {
        if (hasAllPermissions() && Prefs.isConfigured(this)) {
            val intent = Intent(this, ForwarderService::class.java)
            ContextCompat.startForegroundService(this, intent)
        }
    }
}
