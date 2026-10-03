package com.example.smstotelegram

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

/**
 * يستقبل كل رسالة SMS جديدة توصل للجهاز.
 * لو التوصيل مفعّل، ولو المرسل مطابق للفلتر المحفوظ (أو الفلتر فاضي يعني كل الرسائل)،
 * بيبعت نص الرسالة لبوت تليجرام.
 */
class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        if (!Prefs.isEnabled(context)) {
            Log.d(TAG, "التوصيل متوقف من الإعدادات")
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val senderFilter = Prefs.getSenderFilter(context).trim()

        for (sms in messages) {
            val sender = sms.originatingAddress ?: "غير معروف"
            val body = sms.messageBody ?: ""

            val matchesFilter = senderFilter.isBlank() ||
                sender.contains(senderFilter, ignoreCase = true)

            if (!matchesFilter) {
                Log.d(TAG, "الرسالة من $sender اتجاهلت (مش مطابقة للفلتر)")
                continue
            }

            val formatted = "رسالة جديدة\nمن: $sender\n\n$body"
            TelegramSender.send(context, formatted)
        }
    }
}
