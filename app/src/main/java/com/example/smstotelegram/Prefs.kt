package com.example.smstotelegram

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * مسؤول عن حفظ وقراءة إعدادات الربط مع تليجرام وقائمة مزودي الخدمة (فلاتر المرسلين)
 * كل القيم بتتخزن محليًا على الجهاز فقط (SharedPreferences)
 */
object Prefs {
    private const val FILE_NAME = "sms_to_telegram_prefs"

    private const val KEY_BOT_TOKEN = "bot_token"
    private const val KEY_CHAT_ID = "chat_id"
    private const val KEY_ENABLED = "forwarding_enabled"
    private const val KEY_PROVIDERS = "providers_json"
    private const val KEY_INGEST_URL = "ingest_url"
    private const val KEY_INGEST_SECRET = "ingest_secret"

    // الموقع الافتراضي وكلمة السر (يجب أن تطابق PAYMENT_INGEST_SECRET في Vercel)
    const val DEFAULT_INGEST_URL = "https://fahemny86.vercel.app/api/payments/ingest"
    const val DEFAULT_INGEST_SECRET = "4f3fec63148da2afa2ebe9e0dcbe33d285038f11f286f3c3"

    private val gson = Gson()

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun getBotToken(context: Context): String =
        prefs(context).getString(KEY_BOT_TOKEN, "") ?: ""

    fun setBotToken(context: Context, value: String) {
        prefs(context).edit().putString(KEY_BOT_TOKEN, value.trim()).apply()
    }

    fun getChatId(context: Context): String =
        prefs(context).getString(KEY_CHAT_ID, "") ?: ""

    fun setChatId(context: Context, value: String) {
        prefs(context).edit().putString(KEY_CHAT_ID, value.trim()).apply()
    }

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, value).apply()
    }

    fun isTelegramConfigured(context: Context): Boolean =
        getBotToken(context).isNotBlank() && getChatId(context).isNotBlank()

    fun getIngestUrl(context: Context): String =
        (prefs(context).getString(KEY_INGEST_URL, "") ?: "").ifBlank { DEFAULT_INGEST_URL }

    fun setIngestUrl(context: Context, value: String) {
        prefs(context).edit().putString(KEY_INGEST_URL, value.trim()).apply()
    }

    fun getIngestSecret(context: Context): String =
        (prefs(context).getString(KEY_INGEST_SECRET, "") ?: "").ifBlank { DEFAULT_INGEST_SECRET }

    fun setIngestSecret(context: Context, value: String) {
        prefs(context).edit().putString(KEY_INGEST_SECRET, value.trim()).apply()
    }

    fun isIngestConfigured(context: Context): Boolean =
        getIngestUrl(context).isNotBlank() && getIngestSecret(context).isNotBlank()

    /** التطبيق جاهز للعمل إذا كان الإرسال للموقع أو لتليجرام مضبوطًا */
    fun isConfigured(context: Context): Boolean =
        isIngestConfigured(context) || isTelegramConfigured(context)

    // ---------------- مزودي الخدمة ----------------

    fun getProviders(context: Context): List<Provider> {
        val json = prefs(context).getString(KEY_PROVIDERS, null) ?: return emptyList()
        val type = object : TypeToken<List<Provider>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveProviders(context: Context, providers: List<Provider>) {
        prefs(context).edit().putString(KEY_PROVIDERS, gson.toJson(providers)).apply()
    }

    fun addProvider(context: Context, provider: Provider) {
        val current = getProviders(context).toMutableList()
        current.add(provider)
        saveProviders(context, current)
    }

    fun deleteProvider(context: Context, providerId: String) {
        val current = getProviders(context).filterNot { it.id == providerId }
        saveProviders(context, current)
    }

    fun getProvider(context: Context, providerId: String): Provider? =
        getProviders(context).find { it.id == providerId }
}
