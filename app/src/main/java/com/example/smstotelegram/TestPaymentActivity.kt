package com.example.smstotelegram

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.smstotelegram.databinding.ActivityTestPaymentBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * صفحة اختبار مستقلة: ترسل عملية دفع تجريبية لموقع فهمني، وإذا طابقت طلبًا معلّقًا
 * (نفس المبلغ ونفس رقم الدفع) يؤكده الموقع تلقائيًا.
 */
class TestPaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTestPaymentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.spMethod.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("فودافون كاش", "اورنج كاش", "اتصالات كاش", "وي باي", "انستا باي", "أخرى")
        )
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSend.setOnClickListener { send() }
    }

    private fun send() {
        val from = binding.etFrom.text.toString().trim()
        val amount = binding.etAmount.text.toString().trim()
        if (from.isBlank() || amount.isBlank()) {
            showResult("اكتب الرقم الذي حوّل منه والمبلغ", "#D32F2F")
            return
        }
        val txId = binding.etTxId.text.toString().trim().ifBlank { "TEST" + (100000..999999).random() }
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        val message = buildString {
            append("🧪 رسالة اختبار\n\n")
            append("رقم العملية: $txId\n")
            append("المبلغ: $amount\n")
            append("تاريخ العملية: $now\n")
            append("من رقم: $from\n")
            append("طريقة الدفع: ${binding.spMethod.selectedItem}")
        }

        binding.btnSend.isEnabled = false
        showResult("جاري الإرسال للموقع...", "#546E7A")

        TelegramSender.sendWithResult(this, message) { success, detail ->
            runOnUiThread {
                binding.btnSend.isEnabled = true
                when {
                    success && (detail ?: "").contains("تم تأكيد") -> showResult(detail ?: "تم تأكيد الطلب ✅", "#2E7D32")
                    success -> showResult(detail ?: "وصلت للموقع", "#EF6C00")
                    else -> showResult("فشل الإرسال: ${detail ?: "خطأ غير معروف"}", "#D32F2F")
                }
            }
        }
    }

    private fun showResult(text: String, color: String) {
        binding.tvResult.visibility = View.VISIBLE
        binding.tvResult.text = text
        binding.tvResult.setTextColor(Color.parseColor(color))
    }
}
