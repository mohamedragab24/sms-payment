package com.example.smstotelegram

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.smstotelegram.db.AppDatabase
import com.example.smstotelegram.db.MessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION || !Prefs.isEnabled(context)) return
        val providers = Prefs.getProviders(context)
        if (providers.isEmpty()) return
        val appContext = context.applicationContext

        // الرسالة الطويلة بتوصل أجزاء، نجمعها قبل التحليل
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (parts.isNullOrEmpty()) return
        val sender = parts[0].originatingAddress ?: "غير معروف"
        val body = parts.joinToString("") { it.messageBody ?: "" }

        val provider = SmsParser.findProvider(sender, body, providers)
        if (provider == null) {
            Log.w("SmsReceiver", "مفيش مزود مطابق للرسالة من $sender")
            return
        }
        val account = provider.recipientNumber
        val amount = SmsParser.findAmount(body) ?: 0.0
        val transactionId = SmsParser.findTransactionId(body) ?: "غير متوفر"
        val fromNumber = SmsParser.findFromNumber(body) ?: sender
        val operationDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(parts[0].timestampMillis))

        // إرسال البيانات المطلوبة فقط إلى تيليجرام، بدون نص الرسالة الأصلي.
        val formatted = buildString {
            append("رقم العملية: $transactionId\n")
            append("المبلغ: ${formatAmount(amount)}\n")
            append("تاريخ العملية: $operationDate\n")
            append("من رقم: $fromNumber")
        }
        TelegramSender.send(appContext, formatted)

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.getInstance(appContext).messageDao().insert(
                    MessageEntity(providerId = provider.id, providerName = provider.name, sender = sender,
                        recipientNumber = account, amount = amount, body = body, timestamp = System.currentTimeMillis())
                )
            } finally {
                pending.finish()
            }
        }
    }

    private fun formatAmount(v: Double) = if (v % 1.0 == 0.0) v.toLong().toString() else String.format("%.2f", v)
}
