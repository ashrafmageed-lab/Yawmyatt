package com.example.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_DAILY
        val title = when (type) {
            TYPE_EVENING, TYPE_DAILY -> "يوميتي 🕊️ وقت سكون المساء"
            TYPE_MORNING -> "يوميتي 🕊️ بركة الصباح"
            TYPE_TEST -> "يوميتي 🕊️ تذكير المساء التجريبي"
            else -> "يوميتي 🕊️ وقت التدوين والتأمل"
        }
        val message = when (type) {
            TYPE_MORNING -> "صباح مبارك ☀️ — خذ دقيقة لصلاتك وتأملك في بداية يومك"
            TYPE_EVENING -> "سكون المساء 🌙 — «مَرَاحِمُهُ جَدِيدَةٌ كُلَّ صَبَاحٍ» خذ دقائق هادئة لتدوين تأملك وصلاتك لليوم في محضر الرب ✨"
            TYPE_TEST -> "تذكير المساء لكتابة اليومية ✨ — اضغط هنا للبدء مباشرة في تدوين تأملك اليومي في محضر الرب"
            else -> "تذكير المساء لكتابة اليومية 🌙 — حان وقت سكون المساء؛ دوّن صلاتك وشكرك وتأملك في يوميتك ✨"
        }

        val notificationId = when (type) {
            TYPE_MORNING -> 101
            TYPE_EVENING -> 102
            TYPE_TEST -> 103
            else -> 100
        }

        showNotification(context, title, message, notificationId)

        if (type != TYPE_TEST) {
            // Reschedule next occurrence for tomorrow
            ReminderManager.rescheduleNextDay(context, type)
        }
    }

    companion object {
        const val EXTRA_TYPE = "reminder_type"
        const val TYPE_DAILY = "daily"
        const val TYPE_MORNING = "morning"
        const val TYPE_EVENING = "evening"
        const val TYPE_TEST = "test"
        const val CHANNEL_ID = "yawmiyati_gentle_reminders"

        fun showNotification(context: Context, title: String, message: String, notificationId: Int) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "تذكيرات يوميتي الهادئة",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "تذكير يومي هادئ ومقدس لكتابة اليومية والصلاة"
                    enableLights(true)
                    lightColor = Color.parseColor("#E0B86C")
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Launch MainActivity and directly open new journal entry
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_new_entry", true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setColor(Color.parseColor("#E0B86C"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notificationManager.notify(notificationId, notification)
        }
    }
}
