package com.example.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import java.util.Calendar

data class ReminderSettings(
    val enabled: Boolean = true,
    val dailyHour: Int = 21, // 9:00 PM evening default
    val dailyMinute: Int = 0,
    val morningHour: Int = 7,
    val morningMinute: Int = 0,
    val mode: String = "daily" // "daily" (single scheduled evening reminder) or "both" (morning + evening)
)

object ReminderManager {

    private const val PREFS_NAME = "yawmiyati_reminders_pref"
    private const val KEY_ENABLED = "reminders_enabled"
    private const val KEY_DAILY_HOUR = "daily_hour"
    private const val KEY_DAILY_MINUTE = "daily_minute"
    private const val KEY_MORNING_HOUR = "morning_hour"
    private const val KEY_MORNING_MINUTE = "morning_minute"
    private const val KEY_MODE = "reminder_mode"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSettings(context: Context): ReminderSettings {
        val prefs = getPrefs(context)
        return ReminderSettings(
            enabled = prefs.getBoolean(KEY_ENABLED, true),
            dailyHour = prefs.getInt(KEY_DAILY_HOUR, 21),
            dailyMinute = prefs.getInt(KEY_DAILY_MINUTE, 0),
            morningHour = prefs.getInt(KEY_MORNING_HOUR, 7),
            morningMinute = prefs.getInt(KEY_MORNING_MINUTE, 0),
            mode = prefs.getString(KEY_MODE, "daily") ?: "daily"
        )
    }

    fun saveSettings(context: Context, settings: ReminderSettings) {
        getPrefs(context).edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putInt(KEY_DAILY_HOUR, settings.dailyHour)
            .putInt(KEY_DAILY_MINUTE, settings.dailyMinute)
            .putInt(KEY_MORNING_HOUR, settings.morningHour)
            .putInt(KEY_MORNING_MINUTE, settings.morningMinute)
            .putString(KEY_MODE, settings.mode)
            .apply()

        cancelAlarms(context)

        if (settings.enabled) {
            if (settings.mode == "daily") {
                scheduleAlarm(context, ReminderReceiver.TYPE_DAILY, settings.dailyHour, settings.dailyMinute, 200)
            } else {
                scheduleAlarm(context, ReminderReceiver.TYPE_MORNING, settings.morningHour, settings.morningMinute, 201)
                scheduleAlarm(context, ReminderReceiver.TYPE_EVENING, settings.dailyHour, settings.dailyMinute, 202)
            }
        }
    }

    fun sendTestNotification(context: Context) {
        ReminderReceiver.showNotification(
            context = context,
            title = "يوميتي 🕊️ وقت سكون المساء",
            message = "تذكير المساء لكتابة اليومية ✨ — «مَرَاحِمُهُ جَدِيدَةٌ كُلَّ صَبَاحٍ» خذ دقائق هادئة لتدوين تأملك وصلاتك لليوم في محضر الرب.",
            notificationId = 103
        )
    }

    fun sendEveningTestReminder(context: Context) {
        sendTestNotification(context)
    }

    private fun scheduleAlarm(context: Context, type: String, hour: Int, minute: Int, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TYPE, type)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    fun rescheduleNextDay(context: Context, type: String) {
        val settings = getSettings(context)
        if (!settings.enabled) return

        when (type) {
            ReminderReceiver.TYPE_DAILY -> {
                scheduleAlarm(context, type, settings.dailyHour, settings.dailyMinute, 200)
            }
            ReminderReceiver.TYPE_MORNING -> {
                scheduleAlarm(context, type, settings.morningHour, settings.morningMinute, 201)
            }
            ReminderReceiver.TYPE_EVENING -> {
                scheduleAlarm(context, type, settings.dailyHour, settings.dailyMinute, 202)
            }
        }
    }

    private fun cancelAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(200, 201, 202).forEach { reqCode ->
            val intent = Intent(context, ReminderReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pending)
        }
    }
}
