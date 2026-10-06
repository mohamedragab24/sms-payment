package com.example.smstotelegram

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * تحديث تلقائي: يفحص آخر Release على GitHub (tag بصيغة build-N)،
 * وإذا كان أحدث من المثبت ينزّل الـ APK ويفتح شاشة التثبيت.
 * ملاحظة: أندرويد يطلب موافقتك على التثبيت، ولا يسمح بالتثبيت الصامت.
 */
object AppUpdater {
    private const val OWNER = "mohamedragab24"
    private const val REPO = "sms-payment"
    private const val LATEST_API = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    data class Info(val build: Long, val apkUrl: String, val name: String)

    @Suppress("DEPRECATION")
    private fun installedBuild(context: Context): Long {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
    }

    /** onResult(info, error): info=null و error=null يعني أنت على آخر إصدار */
    fun fetchLatest(context: Context, onResult: (Info?, String?) -> Unit) {
        val request = Request.Builder()
            .url(LATEST_API)
            .header("Accept", "application/vnd.github+json")
            .build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onResult(null, "تعذر الاتصال: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val raw = response.use { it.body?.string() ?: "" }
                if (!response.isSuccessful) {
                    onResult(null, "تعذر قراءة آخر إصدار (${response.code})")
                    return
                }
                try {
                    val json = JSONObject(raw)
                    val tag = json.optString("tag_name")
                    val match = Regex("^build-(\\d+)$").find(tag)
                    if (match == null) {
                        onResult(null, null)
                        return
                    }
                    val latest = match.groupValues[1].toLong()
                    if (latest <= installedBuild(context)) {
                        onResult(null, null)
                        return
                    }
                    var url = ""
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            if (asset.optString("name").endsWith(".apk")) {
                                url = asset.optString("browser_download_url")
                                break
                            }
                        }
                    }
                    if (url.isBlank()) {
                        onResult(null, null)
                        return
                    }
                    onResult(Info(latest, url, json.optString("name", tag)), null)
                } catch (e: Exception) {
                    onResult(null, "خطأ في قراءة بيانات التحديث")
                }
            }
        })
    }

    /** manual=true يعرض رسالة حتى لو لا يوجد تحديث (للزر اليدوي) */
    fun checkAndPrompt(activity: Activity, manual: Boolean) {
        fetchLatest(activity) { info, error ->
            activity.runOnUiThread {
                if (activity.isFinishing) return@runOnUiThread
                if (info != null) {
                    AlertDialog.Builder(activity)
                        .setTitle("تحديث جديد متاح")
                        .setMessage("يوجد إصدار أحدث (${info.name}). هل تريد تحميله وتثبيته الآن؟")
                        .setPositiveButton("تحديث الآن") { _, _ -> downloadAndInstall(activity, info) }
                        .setNegativeButton("لاحقًا", null)
                        .show()
                } else if (manual) {
                    Toast.makeText(activity, error ?: "أنت على آخر إصدار ✅", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun downloadAndInstall(activity: Activity, info: Info) {
        // أندرويد 8+ يحتاج إذن "تثبيت تطبيقات غير معروفة" لهذا التطبيق
        if (Build.VERSION.SDK_INT >= 26 && !activity.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(activity, "فعّل السماح بالتثبيت لهذا التطبيق ثم اضغط تحديث مرة أخرى", Toast.LENGTH_LONG).show()
            activity.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}"))
            )
            return
        }

        Toast.makeText(activity, "جاري تحميل التحديث...", Toast.LENGTH_LONG).show()
        val dir = File(activity.cacheDir, "updates").apply { mkdirs() }
        val file = File(dir, "update.apk")

        client.newCall(Request.Builder().url(info.apkUrl).build()).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                activity.runOnUiThread {
                    Toast.makeText(activity, "فشل تحميل التحديث: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                try {
                    response.use { r ->
                        val body = r.body
                        if (!r.isSuccessful || body == null) throw IOException("HTTP ${r.code}")
                        body.byteStream().use { input ->
                            file.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                    activity.runOnUiThread { install(activity, file) }
                } catch (e: Exception) {
                    activity.runOnUiThread {
                        Toast.makeText(activity, "فشل تحميل التحديث: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun install(activity: Activity, file: File) {
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }
}
