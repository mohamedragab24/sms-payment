package com.example.smstotelegram

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.room.Room
import com.example.smstotelegram.db.AppDatabase
import com.example.smstotelegram.db.MessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * يستقبل كل رسالة SMS جديدة توصل للجهاز.
 * بيدور على أول مزود خدمة (Provider) مرسله مطابق لمرسل الرسالة،
 * ولو لقى مطابقة: يخزن الرسالة محليًا ويبعتها لبوت تليجرام.
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

        val providers = Prefs.getProviders(context)
        if (providers.isEmpty()) {
            Log.d(TAG, "مفيش مزودي خدمة مضافين لسه")
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val appContext = context.applicationContext

        for (sms in messages) {
            val sender = sms.originatingAddress ?: "غير معروف"
            val body = sms.messageBody ?: ""

            val matchedProvider = providers.find { provider ->
                provider.senderPattern.isNotBlank() &&
                    sender.contains(provider.senderPattern, ignoreCase = true)
            } ?: continue

            val formatted = "رسالة جديدة من ${matchedProvider.name}\nالمرسل: $sender\n\n$body"
            TelegramSender.send(appContext, formatted)

            CoroutineScope(Dispatchers.IO).launch {
                val dao = AppDatabase.getInstance(appContext).messageDao()
                dao.insert(
                    MessageEntity(
                        providerId = matchedProvider.id,
                        providerName = matchedProvider.name,
                        sender = sender,
                        body = body,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}
