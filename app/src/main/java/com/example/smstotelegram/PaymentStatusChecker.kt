package com.example.smstotelegram

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * الاستعلام عن حالة عملية من موقع فهمني (POST /api/payments/status بنفس سر الـ ingest).
 * النتيجة: تمت / لم تتم / قيد التنفيذ / غير موجودة — اعتمادًا على سجلات المنصة، وعلى واجهة مزود الدفع الرسمية فقط إن كانت مضبوطة في الخادم.
 * عند الفشل: label = عنوان المشكلة، detail = السبب والمطلوب.
 */
object PaymentStatusChecker {
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

    fun statusUrl(context: Context): String {
        val ingest = Prefs.getIngestUrl(context).trim()
        return if (ingest.contains("/api/payments/ingest")) ingest.replace("/api/payments/ingest", "/api/payments/status")
        else ingest.trimEnd('/') + "/api/payments/status"
    }

    /** callback(label, detail) على أي thread — الواجهة تتولى التحويل لـ UI thread */
    fun check(context: Context, txId: String, provider: String, callback: (ok: Boolean, label: String, detail: String) -> Unit) {
        val secret = Prefs.getIngestSecret(context).trim()
        if (txId.isBlank() || txId == "غير متوفر") { callback(false, "غير ممكن", "هذه الرسالة لا تحتوي على رقم عملية"); return }
        val url = statusUrl(context)
        val req = try {
            val body = JSONObject().put("txId", txId).put("provider", provider).toString().toRequestBody(JSON)
            Request.Builder().url(url).header("x-ingest-secret", secret).post(body).build()
        } catch (e: Exception) {
            val ex = ErrorExplainer.fromException(e, url)
            callback(false, ex.title, "السبب: ${ex.cause}\nالمطلوب: ${ex.fix}")
            return
        }
        client.newCall(req).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                val ex = ErrorExplainer.fromException(e, url)
                callback(false, ex.title, "السبب: ${ex.cause}\nالمطلوب: ${ex.fix}")
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val raw = response.use { it.body?.string() ?: "" }
                if (!response.isSuccessful) {
                    val (ex, _) = ErrorExplainer.fromHttp(response.code, raw, url)
                    callback(false, ex.title, "السبب: ${ex.cause}\nالمطلوب: ${ex.fix}")
                    return
                }
                try {
                    val j = JSONObject(raw)
                    val note = j.optJSONObject("details")?.optString("note").orEmpty()
                    callback(true, j.optString("label", "غير معروفة"), note)
                } catch (e: Exception) {
                    callback(false, "رد غير مفهوم من الموقع", "السبب: الموقع لم يرجع JSON (غالبًا الرابط لا يشير للموقع الصحيح).\nالمطلوب: راجع «رابط الموقع» في الإعدادات.\n${raw.take(120)}")
                }
            }
        })
    }
}
