package com.example.smstotelegram

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** خطأ مشروح: ماذا حدث + السبب + المطلوب للحل (+ تفاصيل تقنية). */
data class Explained(
    val title: String,
    val cause: String,
    val fix: String,
    val technical: String = "",
    val warning: Boolean = false,
) {
    fun toText(): String = buildString {
        append(title)
        if (cause.isNotBlank()) append("\nالسبب: ").append(cause)
        if (fix.isNotBlank()) append("\nالمطلوب: ").append(fix)
    }
}

/** يحوّل أي استثناء شبكة أو رد HTTP إلى شرح مفهوم. */
object ErrorExplainer {

    fun fromException(e: Throwable, url: String): Explained {
        val host = try { java.net.URI(url).host ?: url } catch (ignored: Exception) { url }
        val tech = "${e.javaClass.simpleName}: ${e.message ?: ""}".take(300)
        return when (e) {
            is UnknownHostException -> Explained(
                "تعذّر الوصول إلى عنوان الموقع",
                "لا يوجد إنترنت على الهاتف، أو اسم الموقع ($host) غير صحيح/متوقف.",
                "تأكد من اتصال الإنترنت، وافتح https://$host في متصفح الهاتف. لو لا يعمل: صحّح «رابط الموقع» من الإعدادات.", tech)
            is SocketTimeoutException -> Explained(
                "انتهت مهلة الاتصال بالموقع",
                "الإنترنت بطيء جدًا أو الموقع لا يرد (قد يكون Vercel متوقفًا).",
                "تأكد من قوة الإنترنت. الرسالة ستُعاد محاولتها تلقائيًا. لو تكرر افحص Vercel ← Deployments.", tech)
            is ConnectException -> Explained(
                "رُفض الاتصال بالموقع",
                "الخادم لا يقبل الاتصال (عنوان خاطئ أو الموقع غير منشور).",
                "راجع «رابط الموقع» في الإعدادات وتأكد أن النشر على Vercel ناجح.", tech)
            is SSLException -> Explained(
                "فشل الاتصال الآمن (SSL)",
                "تاريخ ووقت الهاتف غير صحيح، أو شبكة تعترض الاتصال.",
                "اضبط التاريخ والوقت على «تلقائي» وجرّب شبكة أخرى (بيانات الهاتف بدل واي فاي).", tech)
            is IllegalArgumentException -> Explained(
                "رابط الموقع غير صالح",
                "القيمة المكتوبة في «رابط الموقع» ليست رابطًا صحيحًا (ينقصها https:// أو فيها مسافات).",
                "الإعدادات ← رابط الموقع: اكتب مثل https://fahemny86.vercel.app/api/payments/ingest", tech)
            else -> Explained("تعذّر إرسال العملية للموقع", "خطأ غير متوقع في الشبكة.", "أعد المحاولة، ولو تكرر أرسل التفاصيل للمطوّر.", tech)
        }
    }

    /** يرجع شرحًا لأي رد غير ناجح. isRetryable=false للأخطاء التي لن تُحل بالإعادة. */
    fun fromHttp(code: Int, raw: String, url: String): Pair<Explained, Boolean> {
        val j = try { JSONObject(raw) } catch (ignored: Exception) { null }
        val msg = j?.optString("message").orEmpty()
        val cause = j?.optString("cause").orEmpty()
        val fix = j?.optString("fix").orEmpty()
        val tech = "HTTP $code ${j?.optString("error").orEmpty()} ${raw.take(160)}".trim()
        if (msg.isNotBlank() && (cause.isNotBlank() || fix.isNotBlank())) {
            // السيرفر شرح المشكلة بنفسه (سر خاطئ، متغير ناقص، ...): الإعادة لن تفيد
            return Pair(Explained(msg, cause, fix, tech), false)
        }
        val low = raw.lowercase()
        return when {
            code == 401 && (low.contains("vercel") || low.contains("authentication required") || low.contains("<html")) -> Pair(Explained(
                "Vercel رفض الطلب قبل وصوله للموقع (401)",
                "حماية النشر (Deployment Protection) مفعّلة على الموقع فتطلب تسجيل دخول من أي زائر، والتطبيق لا يستطيع ذلك.",
                "Vercel ← Settings ← Deployment Protection ← أوقفها لبيئة Production ثم أعد المحاولة.", tech), false)
            code == 401 -> Pair(Explained(
                "كلمة السر غير مطابقة",
                "«كلمة سر الربط» في التطبيق تختلف عن PAYMENT_INGEST_SECRET في Vercel.",
                "اجعل القيمة واحدة في الاثنين (الإعدادات في التطبيق + Vercel ← Environment Variables ثم Redeploy).", tech), false)
            code == 404 -> Pair(Explained(
                "المسار غير موجود على الموقع (404)",
                "الرابط لا يشير إلى /api/payments/ingest، أو نسخة المنصة المنشورة لا تحتوي هذا المسار.",
                "راجع «رابط الموقع» في الإعدادات، وارفع آخر نسخة من fahmni-platform إلى Vercel.", tech), false)
            code == 400 -> Pair(Explained(
                "الموقع رفض البيانات (400)", "نص الرسالة المرسل غير مكتمل.", "أرسل التفاصيل التقنية للمطوّر.", tech), false)
            code >= 500 -> Pair(Explained(
                "خطأ داخلي في الموقع ($code)",
                "السيرفر واجه مشكلة أثناء المعالجة (إعدادات ناقصة أو انهيار).",
                "افتح Vercel ← Logs لمسار /api/payments/ingest. ستُعاد المحاولة تلقائيًا عدة مرات.", tech), true)
            else -> Pair(Explained("الموقع رد برمز غير متوقع ($code)", "", "أرسل التفاصيل التقنية للمطوّر.", tech), false)
        }
    }
}

data class ErrorEntry(
    val where: String, val title: String, val cause: String, val fix: String,
    val technical: String, val at: Long, val warning: Boolean,
) {
    fun toText(): String = buildString {
        append(title)
        if (where.isNotBlank()) append("\nالمكان: ").append(where)
        if (cause.isNotBlank()) append("\nالسبب: ").append(cause)
        if (fix.isNotBlank()) append("\nالمطلوب: ").append(fix)
        if (technical.isNotBlank()) append("\nالتفاصيل: ").append(technical)
        append("\nالوقت: ").append(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(at)))
    }
}

/** سجل أخطاء دائم (يبقى بعد إغلاق التطبيق) + إشعار فوري + التقاط انهيار التطبيق. */
object ErrorLog {
    private const val PREFS = "error_log"
    private const val KEY = "entries"
    private const val KEY_CRASH = "pending_crash"
    private const val CHANNEL = "payment_errors"
    private const val MAX = 30
    @Volatile private var installed = false

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** يُستدعى مرة عند بدء أي عملية (Activity أو Receiver): يحفظ سبب الانهيار ليظهر عند الفتح التالي. */
    fun install(context: Context) {
        if (installed) return
        installed = true
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val o = JSONObject()
                    .put("title", "التطبيق أُغلق بسبب خطأ غير متوقع")
                    .put("cause", "${e.javaClass.simpleName}: ${e.message ?: ""}".take(300))
                    .put("fix", "أرسل «نسخ التفاصيل» للمطوّر ليصلح الخطأ.")
                    .put("tech", Log.getStackTraceString(e).take(1500))
                    .put("at", System.currentTimeMillis())
                prefs(app).edit().putString(KEY_CRASH, o.toString()).commit()
            } catch (ignored: Throwable) {}
            previous?.uncaughtException(t, e)
        }
    }

    fun record(context: Context, where: String, ex: Explained, notify: Boolean = false) {
        try {
            val entry = ErrorEntry(where, ex.title, ex.cause, ex.fix, ex.technical, System.currentTimeMillis(), ex.warning)
            Log.e("ErrorLog", entry.toText())
            val arr = try { JSONArray(prefs(context).getString(KEY, "[]")) } catch (ignored: Exception) { JSONArray() }
            val out = JSONArray()
            out.put(toJson(entry))
            for (i in 0 until minOf(arr.length(), MAX - 1)) out.put(arr.get(i))
            prefs(context).edit().putString(KEY, out.toString()).apply()
            if (notify) notifyError(context, entry)
        } catch (ignored: Throwable) {}
    }

    fun all(context: Context): List<ErrorEntry> {
        val arr = try { JSONArray(prefs(context).getString(KEY, "[]")) } catch (ignored: Exception) { JSONArray() }
        return (0 until arr.length()).mapNotNull { fromJson(arr.optJSONObject(it)) }
    }

    fun last(context: Context): ErrorEntry? = all(context).firstOrNull()

    /** انهيار سابق لم يُعرض بعد (يُعرض مرة واحدة ثم يُحذف) */
    fun takePendingCrash(context: Context): ErrorEntry? {
        val raw = prefs(context).getString(KEY_CRASH, null) ?: return null
        prefs(context).edit().remove(KEY_CRASH).apply()
        return try {
            val o = JSONObject(raw)
            ErrorEntry("انهيار التطبيق", o.optString("title"), o.optString("cause"), o.optString("fix"), o.optString("tech"), o.optLong("at"), false)
        } catch (ignored: Exception) { null }
    }

    fun clear(context: Context) { prefs(context).edit().remove(KEY).apply() }

    private fun toJson(e: ErrorEntry) = JSONObject().put("where", e.where).put("title", e.title).put("cause", e.cause)
        .put("fix", e.fix).put("tech", e.technical).put("at", e.at).put("warn", e.warning)

    private fun fromJson(o: JSONObject?): ErrorEntry? = o?.let {
        ErrorEntry(it.optString("where"), it.optString("title"), it.optString("cause"), it.optString("fix"), it.optString("tech"), it.optLong("at"), it.optBoolean("warn"))
    }

    private fun notifyError(context: Context, e: ErrorEntry) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(NotificationChannel(CHANNEL, "أخطاء بوابة الدفع", NotificationManager.IMPORTANCE_HIGH))
            }
            val n = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(e.title)
                .setContentText(e.fix.ifBlank { e.cause })
                .setStyle(NotificationCompat.BigTextStyle().bigText(e.toText()))
                .setAutoCancel(true)
                .build()
            nm.notify(2000 + (System.currentTimeMillis() % 1000).toInt(), n)
        } catch (ignored: Throwable) {
            // الإشعارات غير مسموحة: يبقى الخطأ ظاهرًا في الشاشة الرئيسية
        }
    }
}
