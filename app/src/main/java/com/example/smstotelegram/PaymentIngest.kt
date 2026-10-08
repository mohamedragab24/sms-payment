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
 *
 * عند أي فشل: النص المُرجع للـ callback يحتوي (ماذا حدث / السبب / المطلوب) جاهزًا للعرض.
 * الرسائل التحذيرية (نجح الوصول لكن لم تتحول لدفع) تبدأ بـ «⚠️».
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

    /** callback(success, detail) — detail رسالة مشروحة تُعرض في الاختبار وفي سجل الأخطاء */
    fun send(url: String, secret: String, text: String, callback: ((Boolean, String?) -> Unit)?) {
        if (url.isBlank()) {
            callback?.invoke(false, Explained("رابط الموقع غير مضبوط", "حقل «رابط الموقع» فارغ.", "الإعدادات ← رابط الموقع: https://fahemny86.vercel.app/api/payments/ingest").toText())
            return
        }
        if (secret.isBlank()) {
            callback?.invoke(false, Explained("كلمة سر الربط غير مضبوطة", "حقل «كلمة السر» فارغ.", "الإعدادات ← كلمة السر: ضع نفس قيمة PAYMENT_INGEST_SECRET في Vercel.").toText())
            return
        }
        attempt(url.trim(), secret.trim(), text, 1, callback)
    }

    private fun attempt(url: String, secret: String, text: String, n: Int, callback: ((Boolean, String?) -> Unit)?) {
        val request = try {
            val body = JSONObject().put("text", text).toString().toRequestBody(JSON)
            Request.Builder().url(url).header("x-ingest-secret", secret).post(body).build()
        } catch (e: Exception) {
            // رابط غير صالح كان يسبب انهيار التطبيق؛ الآن يظهر السبب
            callback?.invoke(false, ErrorExplainer.fromException(e, url).toText())
            return
        }

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "فشل الاتصال بالموقع (محاولة $n): ${e.message}")
                retryOrFail(url, secret, text, n, callback, ErrorExplainer.fromException(e, url))
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val raw = response.use { it.body?.string() ?: "" }
                if (!response.isSuccessful) {
                    Log.e(TAG, "الموقع رفض الطلب: ${response.code} - $raw")
                    val (explained, retryable) = ErrorExplainer.fromHttp(response.code, raw, url)
                    if (retryable) retryOrFail(url, secret, text, n, callback, explained)
                    else callback?.invoke(false, explained.toText())
                    return
                }
                val detail = try {
                    val j = JSONObject(raw)
                    when {
                        j.optBoolean("ignored", false) -> "⚠️ المزود غير مضاف/غير مفعّل في لوحة التحكم: لم تتحول الرسالة لعملية دفع\nالمطلوب: لوحة الأدمن ← مراجعة عمليات الدفع ← «المزودون المقبولون» فعّل المزود، وتأكد أن اسم المزود في هذا التطبيق يطابق اسمه أو أحد ألقابه هناك."
                        j.optBoolean("duplicateTx", false) -> "⚠️ رقم العملية مستخدم من قبل لنفس طريقة الدفع (عملية مكررة)"
                        j.optBoolean("rejectedDuplicate", false) -> "⚠️ تم رفض الطلب: رقم العملية مستخدم من قبل (عملية مكررة)"
                        j.optBoolean("duplicate", false) -> "الرسالة مرسلة من قبل (مكررة)"
                        j.has("parseOk") && !j.optBoolean("parseOk", true) -> "⚠️ وصلت للموقع لكن لم يستطع قراءة (المبلغ/الرقم/الوقت)\nالسبب: صيغة الرسالة مختلفة عن المتوقعة.\nالمطلوب: أرسل نص الرسالة الأصلي للمطوّر ليضيف الصيغة."
                        (j.optJSONArray("confirmed")?.length() ?: 0) > 0 -> {
                            val order = j.optJSONArray("orders")?.optString(0) ?: ""
                            if (order.isNotBlank()) "تم تأكيد الطلب تلقائيًا ✅ (رقم الطلب: $order)" else "تم تأكيد الطلب تلقائيًا ✅"
                        }
                        else -> "وصلت للموقع ولم يوجد طلب مطابق: تأكد أن المبلغ والرقم نفس الطلب، وأن وقت العملية ليس قبل الطلب بأكثر من 10 دقائق"
                    }
                } catch (e: Exception) { "وصلت للموقع" }
                callback?.invoke(true, detail)
            }
        })
    }

    private fun retryOrFail(url: String, secret: String, text: String, n: Int, callback: ((Boolean, String?) -> Unit)?, error: Explained) {
        if (n >= MAX_ATTEMPTS) {
            callback?.invoke(false, error.copy(cause = error.cause + " (فشلت $MAX_ATTEMPTS محاولات)").toText())
            return
        }
        // انتظار تصاعدي ثم إعادة المحاولة (الرسالة نفسها لا تتكرر في الخادم لأنه يمنع التكرار)
        Thread {
            try { Thread.sleep(3000L * n) } catch (e: InterruptedException) {}
            attempt(url, secret, text, n + 1, callback)
        }.start()
    }
}
