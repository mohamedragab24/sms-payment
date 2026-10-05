package com.example.smstotelegram

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * يرسل نص رسالة التحويل مباشرة إلى موقع فهمني (POST /api/payments/ingest)
 * ليتم تأكيد الطلب تلقائيًا. تليجرام لا يمكن استخدامه لهذا الغرض لأن البوت
 * لا يستقبل الرسائل التي يرسلها هو بنفسه، فالـ webhook لا يُستدعى.
 */
object PaymentIngest {
    private const val TAG = "PaymentIngest"
    private const val MAX_ATTEMPTS = 4
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    /** callback(success, detail) — detail رسالة مختصرة تُعرض في الاختبار */
    fun send(url: String, secret: String, text: String, callback: ((Boolean, String?) -> Unit)?) {
        if (url.isBlank() || secret.isBlank()) {
            callback?.invoke(false, "رابط الموقع أو كلمة السر غير مضبوطة")
            return
        }
        attempt(url.trim(), secret.trim(), text, 1, callback)
    }

    private fun attempt(url: String, secret: String, text: String, n: Int, callback: ((Boolean, String?) -> Unit)?) {
        val body = JSONObject().put("text", text).toString().toRequestBody(JSON)
        val request = Request.Builder()
            .url(url)
            .header("x-ingest-secret", secret)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "فشل الاتصال بالموقع (محاولة $n): ${e.message}")
                retryOrFail(url, secret, text, n, callback, "تعذر الاتصال بالموقع: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val raw = response.use { it.body?.string() ?: "" }
                if (response.code == 401) {
                    callback?.invoke(false, "كلمة السر غير مطابقة لـ PAYMENT_INGEST_SECRET في Vercel")
                    return
                }
                if (!response.isSuccessful) {
                    Log.e(TAG, "الموقع رفض الطلب: ${response.code} - $raw")
                    // أخطاء الخادم المؤقتة نعيد المحاولة، أما 4xx فلا
                    if (response.code >= 500) retryOrFail(url, secret, text, n, callback, "خطأ من الموقع (${response.code})")
                    else callback?.invoke(false, "الموقع رفض الطلب (${response.code})")
                    return
                }
                val detail = try {
                    val j = JSONObject(raw)
                    when {
                        j.optBoolean("duplicate", false) -> "الرسالة مرسلة من قبل (مكررة)"
                        j.has("parseOk") && !j.optBoolean("parseOk", true) -> "وصلت للموقع لكن لم يستطع قراءة (المبلغ/الرقم/الوقت)"
                        (j.optJSONArray("confirmed")?.length() ?: 0) > 0 -> "تم تأكيد الطلب تلقائيًا ✅"
                        else -> "وصلت للموقع ولم يوجد طلب مطابق (تحقق من المبلغ ورقم الدفع ووقت الطلب)"
                    }
                } catch (e: Exception) { "وصلت للموقع" }
                callback?.invoke(true, detail)
            }
        })
    }

    private fun retryOrFail(url: String, secret: String, text: String, n: Int, callback: ((Boolean, String?) -> Unit)?, error: String) {
        if (n >= MAX_ATTEMPTS) {
            callback?.invoke(false, error)
            return
        }
        // انتظار تصاعدي ثم إعادة المحاولة (الرسالة نفسها لا تتكرر في الخادم لأنه يمنع التكرار)
        Thread {
            try { Thread.sleep(3000L * n) } catch (e: InterruptedException) {}
            attempt(url, secret, text, n + 1, callback)
        }.start()
    }
}
