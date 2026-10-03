package com.example.smstotelegram

import android.content.Context

/**
 * مسؤول عن حفظ وقراءة إعدادات الربط مع تليجرام وفلتر المرسل
 * كل القيم بتتخزن محليًا على الجهاز فقط (SharedPreferences)
 */
object Prefs {
    private const val FILE_NAME = "sms_to_telegram_prefs"

    private const val KEY_BOT_TOKEN = "bot_token"
    private const val KEY_CHAT_ID = "chat_id"
    private const val KEY_SENDER_FILTER = "sender_filter"
    private const val KEY_ENABLED = "forwarding_enabled"

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

    /** رقم أو اسم المرسل المسموح نقبل رسائله فقط. سيبه فاضي لو عايز توسعة لاحقًا لأكتر من رقم */
    fun getSenderFilter(context: Context): String =
        prefs(context).getString(KEY_SENDER_FILTER, "") ?: ""

    fun setSenderFilter(context: Context, value: String) {
        prefs(context).edit().putString(KEY_SENDER_FILTER, value.trim()).apply()
    }

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, value).apply()
    }

    fun isConfigured(context: Context): Boolean =
        getBotToken(context).isNotBlank() && getChatId(context).isNotBlank()
}
