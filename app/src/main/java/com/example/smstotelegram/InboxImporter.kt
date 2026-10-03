package com.example.smstotelegram

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.example.smstotelegram.db.AppDatabase
import com.example.smstotelegram.db.MessageEntity

/** يستورد الرسائل القديمة الموجودة في صندوق الوارد من مرسل المزود (من غير إرسال لتليجرام). */
object InboxImporter {
    suspend fun import(context: Context, provider: Provider): Int {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) return 0
        val dao = AppDatabase.getInstance(context.applicationContext).messageDao()
        var added = 0
        context.contentResolver.query(
            Uri.parse("content://sms/inbox"), arrayOf("address", "body", "date"), null, null, "date ASC"
        )?.use { c ->
            while (c.moveToNext()) {
                val address = c.getString(0) ?: continue
                val body = c.getString(1) ?: continue
                val date = c.getLong(2)
                if (SmsParser.findProvider(address, body, listOf(provider)) == null) continue
                if (dao.countExisting(date, body) > 0) continue
                dao.insert(
                    MessageEntity(
                        providerId = provider.id, providerName = provider.name, sender = address,
                        recipientNumber = provider.recipientNumber,
                        amount = SmsParser.findAmount(body) ?: 0.0, body = body, timestamp = date
                    )
                )
                added++
            }
        }
        return added
    }
}
