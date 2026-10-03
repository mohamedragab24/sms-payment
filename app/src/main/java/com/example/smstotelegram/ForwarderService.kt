package com.example.smstotelegram

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * خدمة شغالة باستمرار (Foreground Service) عشان نظام أندرويد ميقفلش
 * التطبيق من الخلفية ويمنع استقبال الرسائل.
 * الإشعار الثابت ده مطلوب من نظام أندرويد نفسه لأي Foreground Service - مينفعش نخفيه.
 */
class ForwarderService : Service() {

    private val channelId = "sms_forwarder_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // لو الخدمة اتقفلت من النظام، أندرويد هيحاول يعيد تشغيلها تلقائيًا
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "توصيل الرسائل لتليجرام",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("SMS to Telegram شغال")
            .setContentText("بيراقب الرسائل الجديدة ويبعتها لتليجرام")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setOngoing(true)
            .build()
    }
}
