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
        sendWithResult(context, text, null)
    }

    /**
     * 1) الإرسال للموقع (PaymentIngest) — هو المسار الذي يؤكد الدفع تلقائيًا.
     * 2) نسخة احتياطية لتليجرام (للمراجعة فقط) إن كان البوت مضبوطًا.
     * النتيجة المُرجعة للـ callback هي نتيجة الإرسال للموقع.
     */
    fun sendWithResult(context: Context, text: String, callback: ((Boolean, String?) -> Unit)?) {
        val app = context.applicationContext
        // أي فشل أو تحذير يُسجَّل في سجل الأخطاء ويظهر إشعارًا وفي الشاشة الرئيسية (حتى لو لم يوجد callback كما في استقبال SMS)
        val wrapped: (Boolean, String?) -> Unit = { ok, detail ->
            val d = detail.orEmpty()
            if (!ok) {
                val lines = d.split("\n")
                ErrorLog.record(app, "إرسال عملية الدفع للموقع",
                    Explained(lines.firstOrNull().orEmpty(),
                        lines.firstOrNull { it.startsWith("السبب:") }?.removePrefix("السبب:")?.trim().orEmpty(),
                        lines.firstOrNull { it.startsWith("المطلوب:") }?.removePrefix("المطلوب:")?.trim().orEmpty(),
                        technical = text.lineSequence().firstOrNull().orEmpty()), notify = true)
            } else if (d.startsWith("⚠️")) {
                val lines = d.removePrefix("⚠️").trim().split("\n")
                ErrorLog.record(app, "عملية دفع لم تتحول لطلب",
                    Explained(lines.firstOrNull().orEmpty(),
                        lines.firstOrNull { it.startsWith("السبب:") }?.removePrefix("السبب:")?.trim().orEmpty(),
                        lines.firstOrNull { it.startsWith("المطلوب:") }?.removePrefix("المطلوب:")?.trim().orEmpty(),
                        technical = text.lineSequence().firstOrNull().orEmpty(), warning = true), notify = false)
            }
            callback?.invoke(ok, detail)
        }
        PaymentIngest.send(Prefs.getIngestUrl(context), Prefs.getIngestSecret(context), text, wrapped)
        sendToTelegram(context, text)
    }

    private fun sendToTelegram(context: Context, text: String) {
        val token = Prefs.getBotToken(context)
        val chatId = Prefs.getChatId(context)

        if (token.isBlank() || chatId.isBlank()) {
            Log.w(TAG, "تليجرام غير مضبوط: سيتم الإرسال للموقع فقط")
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
                ErrorLog.record(context.applicationContext, "نسخة تليجرام الاحتياطية", ErrorExplainer.fromException(e, "https://api.telegram.org").copy(warning = true))
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful) {
                    Log.e(TAG, "تليجرام رفض الطلب: ${response.code} - $responseBody")
                    val (title, cause, fix) = when (response.code) {
                        401, 404 -> Triple("تليجرام: توكن البوت غير صحيح", "قيمة «توكن البوت» في الإعدادات خطأ أو البوت اتحذف.", "أنشئ/انسخ التوكن من @BotFather وضعه في الإعدادات.")
                        400, 403 -> Triple("تليجرام: لا يمكن الإرسال لهذا الشات", "Chat ID خطأ أو أنك لم تبدأ محادثة مع البوت (Start).", "افتح البوت واضغط Start، وتأكد من Chat ID في الإعدادات.")
                        else -> Triple("تليجرام رفض الطلب (${response.code})", "", "هذه نسخة احتياطية فقط؛ تأكيد الدفع لا يتأثر.")
                    }
                    ErrorLog.record(context.applicationContext, "نسخة تليجرام الاحتياطية", Explained(title, cause, fix, (responseBody ?: "").take(120), warning = true))
                }
                response.close()
            }
        })
    }
}
