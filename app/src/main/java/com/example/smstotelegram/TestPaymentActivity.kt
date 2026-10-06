package com.example.smstotelegram

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
        // لازم نفس الرقم الذي أدخلته في الطلب ونفس المبلغ المطلوب (أي رقم وأي مبلغ، حسب طلبك أنت)
        val from = binding.etFrom.text.toString().trim()
        val amount = binding.etAmount.text.toString().trim()
        if (from.isBlank() || amount.isBlank()) {
            showResult("اكتب نفس الرقم الذي أدخلته في الطلب والمبلغ المطلوب", R.color.result_err)
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
        showResult("جاري الإرسال للموقع...", R.color.result_info)

        TelegramSender.sendWithResult(this, message) { success, detail ->
            runOnUiThread {
                binding.btnSend.isEnabled = true
                when {
                    success && (detail ?: "").startsWith("تم تأكيد") -> showResult(detail ?: "تم تأكيد الطلب ✅", R.color.result_ok)
                    success -> showResult(detail ?: "وصلت للموقع", R.color.result_warn)
                    else -> showResult("فشل الإرسال: ${detail ?: "خطأ غير معروف"}", R.color.result_err)
                }
            }
        }
    }

    private fun showResult(text: String, colorRes: Int) {
        binding.tvResult.visibility = View.VISIBLE
        binding.tvResult.text = text
        binding.tvResult.setTextColor(ContextCompat.getColor(this, colorRes))
    }
}
