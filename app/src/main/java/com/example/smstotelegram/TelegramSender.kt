package com.example.smstotelegram

import android.content.Context
import android.util.Log
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * مسؤول عن إرسال الرسالة النصية لبوت تليجرام باستخدام Bot API
 * https://core.telegram.org/bots/api#sendmessage
 */
object TelegramSender {
    private const val TAG = "TelegramSender"
    private val client = OkHttpClient()

    fun send(context: Context, text: String) {
        val token = Prefs.getBotToken(context)
        val chatId = Prefs.getChatId(context)

        if (token.isBlank() || chatId.isBlank()) {
            Log.w(TAG, "لسه معملتش إعداد التوكن أو الـ chat id")
            return
        }

        val url = "https://api.telegram.org/bot$token/sendMessage"

        val body = FormBody.Builder()
            .add("chat_id", chatId)
            .add("text", text)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "فشل إرسال الرسالة لتليجرام: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (!response.isSuccessful) {
                    Log.e(TAG, "تليجرام رفض الطلب: ${response.code} - ${response.body?.string()}")
                }
                response.close()
            }
        })
    }
}
