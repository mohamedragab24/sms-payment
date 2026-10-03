package com.example.smstotelegram

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
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
        for (sms in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
            val sender = sms.originatingAddress ?: "غير معروف"
            val body = sms.messageBody ?: ""
            val recipient = SmsParser.findRecipient(body, providers.map { it.recipientNumber }) ?: continue
            val provider = providers.firstOrNull {
                it.recipientNumber.filter(Char::isDigit) == recipient.filter(Char::isDigit)
            } ?: continue
            val amount = SmsParser.findAmount(body) ?: 0.0
            val formatted = "رسالة جديدة من ${provider.name}\nرقم الحساب/المستلم: $recipient\nالمبلغ: ${formatAmount(amount)}\nالمرسل: $sender\n\n$body"
            TelegramSender.send(appContext, formatted)
            CoroutineScope(Dispatchers.IO).launch {
                AppDatabase.getInstance(appContext).messageDao().insert(
                    MessageEntity(providerId=provider.id, providerName=provider.name, sender=sender,
                        recipientNumber=recipient, amount=amount, body=body, timestamp=System.currentTimeMillis())
                )
            }
        }
    }
    private fun formatAmount(value: Double) = if (value % 1.0 == 0.0) value.toLong().toString() else String.format("%.2f", value)
}
