package com.example.smstotelegram.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.smstotelegram.ForwarderService
import com.example.smstotelegram.Prefs
import com.example.smstotelegram.TelegramSender
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
        binding.etIngestUrl.setText(Prefs.getIngestUrl(context))
        binding.etIngestSecret.setText(Prefs.getIngestSecret(context))
        binding.btnSaveIngest.setOnClickListener {
            val url = binding.etIngestUrl.text.toString().trim()
            val secret = binding.etIngestSecret.text.toString().trim()
            if (!url.startsWith("https://") || secret.isBlank()) {
                Toast.makeText(context, "اكتب رابط الموقع (يبدأ بـ https://) وكلمة السر", Toast.LENGTH_LONG).show()
            } else {
                Prefs.setIngestUrl(context, url)
                Prefs.setIngestSecret(context, secret)
                Toast.makeText(context, "تم حفظ إعدادات الموقع", Toast.LENGTH_SHORT).show()
                startServiceIfConfigured()
            }
        }
        binding.switchEnabled.isChecked = Prefs.isEnabled(context)

        updatePermissionStatus()
        setTelegramLocked(Prefs.isTelegramConfigured(context))

        binding.btnGrantPermissions.setOnClickListener { requestNeededPermissions() }

        binding.btnSave.setOnClickListener { saveSettings() }
        binding.btnTestTelegram.setOnClickListener { showTelegramTestDialog() }
        binding.btnEditTelegram.setOnClickListener {
            setTelegramLocked(false)
            binding.btnEditTelegram.text = "🔓 إلغاء القفل"
        }

        binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setEnabled(context, isChecked)
        }
    }

    private fun showTelegramTestDialog() {
        val context = requireContext()
        if (!Prefs.isConfigured(context)) {
            Toast.makeText(context, "احفظ إعدادات الموقع أولاً", Toast.LENGTH_LONG).show()
            return
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0)
        }

        fun field(hint: String, inputType: Int): EditText {
            return EditText(context).apply {
                this.hint = hint
                this.inputType = inputType
                setSingleLine(true)
            }.also { container.addView(it, LinearLayout.LayoutParams(-1, -2)) }
        }

        container.addView(TextView(context).apply {
            text = "طريقة الدفع"
            textSize = 13f
        })
        val paymentMethod = Spinner(context)
        paymentMethod.adapter = ArrayAdapter(
            context, android.R.layout.simple_spinner_dropdown_item,
            arrayOf("فودافون كاش", "اورنج كاش", "اتصالات كاش", "وي باي", "انستا باي", "أخرى")
        )
        container.addView(paymentMethod, LinearLayout.LayoutParams(-1, -2))

        val fromNumber = field("الرقم الذي حوّل منه", android.text.InputType.TYPE_CLASS_PHONE)
        val amount = field("المبلغ", android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL)
        val transaction = field("رقم العملية (اختياري)", android.text.InputType.TYPE_CLASS_TEXT)

        container.addView(TextView(context).apply {
            text = "التاريخ والوقت يُكتبان تلقائيًا"
            textSize = 12f
            setPadding(0, 12, 0, 0)
        })

        val scroll = android.widget.ScrollView(context).apply { addView(container) }

        val dialog = AlertDialog.Builder(context)
            .setTitle("🧪 رسالة دفع تجريبية")
            .setView(scroll)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("إرسال للموقع", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val from = fromNumber.text.toString().trim()
                val amountValue = amount.text.toString().trim()
                if (from.isBlank() || amountValue.isBlank()) {
                    Toast.makeText(context, "اكتب الرقم الذي حوّل منه والمبلغ", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val txId = transaction.text.toString().trim().ifBlank { "TEST" + (100000..999999).random() }
                val now = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())

                val message = buildString {
                    append("🧪 رسالة اختبار\n\n")
                    append("رقم العملية: $txId\n")
                    append("المبلغ: $amountValue\n")
                    append("تاريخ العملية: $now\n")
                    append("من رقم: $from\n")
                    append("طريقة الدفع: ${paymentMethod.selectedItem}")
                }

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                TelegramSender.sendWithResult(context, message) { success, error ->
                    requireActivity().runOnUiThread {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        if (success) {
                            Toast.makeText(context, "تم الإرسال ✅ ${error ?: ""}", Toast.LENGTH_LONG).show()
                            dialog.dismiss()
                        } else {
                            Toast.makeText(context, "فشل الإرسال: ${error ?: "خطأ غير معروف"}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
        dialog.show()
    }

    private fun saveSettings() {
        val context = requireContext()
        val token = binding.etBotToken.text.toString().trim()
        val chatId = binding.etChatId.text.toString().trim()

        // تليجرام اختياري الآن (نسخة احتياطية فقط)، فيمكن ترك الحقلين فارغين
        if (token.isBlank() != chatId.isBlank()) {
            Toast.makeText(context, "أدخل Bot Token و Chat ID معًا أو اتركهما فارغين", Toast.LENGTH_SHORT).show()
            return
        }

        Prefs.setBotToken(context, token)
        Prefs.setChatId(context, chatId)

        Toast.makeText(context, "تم الحفظ وتأمين بيانات الربط", Toast.LENGTH_SHORT).show()
        setTelegramLocked(token.isNotBlank())
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
