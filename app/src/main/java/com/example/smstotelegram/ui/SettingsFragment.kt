package com.example.smstotelegram.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.smstotelegram.ForwarderService
import com.example.smstotelegram.Prefs
import com.example.smstotelegram.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val permissionsLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            Toast.makeText(requireContext(), "تم منح الصلاحيات", Toast.LENGTH_SHORT).show()
            startServiceIfConfigured()
        } else {
            Toast.makeText(
                requireContext(),
                "لازم تسمح بصلاحية قراءة الرسائل عشان التطبيق يشتغل",
                Toast.LENGTH_LONG
            ).show()
        }
        updatePermissionStatus()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()

        binding.etBotToken.setText(Prefs.getBotToken(context))
        binding.etChatId.setText(Prefs.getChatId(context))
        binding.switchEnabled.isChecked = Prefs.isEnabled(context)

        updatePermissionStatus()
        setTelegramLocked(Prefs.isConfigured(context))

        binding.btnGrantPermissions.setOnClickListener { requestNeededPermissions() }

        binding.btnSave.setOnClickListener { saveSettings() }
        binding.btnEditTelegram.setOnClickListener {
            setTelegramLocked(false)
            binding.btnEditTelegram.text = "🔓 إلغاء القفل"
        }

        binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setEnabled(context, isChecked)
        }
    }

    private fun saveSettings() {
        val context = requireContext()
        val token = binding.etBotToken.text.toString().trim()
        val chatId = binding.etChatId.text.toString().trim()

        if (token.isBlank() || chatId.isBlank()) {
            Toast.makeText(context, "لازم تدخل Bot Token و Chat ID", Toast.LENGTH_SHORT).show()
            return
        }

        Prefs.setBotToken(context, token)
        Prefs.setChatId(context, chatId)

        Toast.makeText(context, "تم الحفظ وتأمين بيانات الربط", Toast.LENGTH_SHORT).show()
        setTelegramLocked(true)
        startServiceIfConfigured()
    }


    private fun setTelegramLocked(locked: Boolean) {
        binding.etBotToken.isEnabled = !locked
        binding.etChatId.isEnabled = !locked
        binding.btnSave.isEnabled = !locked
        binding.btnEditTelegram.text = if (locked) "🔒 تعديل" else "🔓 إلغاء القفل"
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
        val context = requireContext()
        val smsGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECEIVE_SMS
            ) == PackageManager.PERMISSION_GRANTED

        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
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
        val context = requireContext()
        if (hasAllPermissions() && Prefs.isConfigured(context)) {
            val intent = Intent(context, ForwarderService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
