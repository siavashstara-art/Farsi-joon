package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

private const val CHANNEL_ID = "farsi_joon_daily_reminder"
private const val NOTIFICATION_ID = 1001
private const val REQUEST_CODE = 2001

class DailyReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    ensureChannel(context)
    val tapIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingTap = PendingIntent.getActivity(
      context,
      0,
      tapIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("Time for your 10-minute Farsi!")
      .setContentText("سلام! Practice a few words in Farsi-Joon today.")
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setAutoCancel(true)
      .setContentIntent(pendingTap)
      .build()

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(NOTIFICATION_ID, notification)
  }

  companion object {
    fun ensureChannel(context: Context) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
          CHANNEL_ID,
          "Daily Farsi Practice Reminders",
          NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
          description = "Gentle daily reminder for 10 minutes of Persian practice"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
      }
    }
  }
}

object DailyReminderScheduler {
  fun scheduleDaily(context: Context, hour: Int = 19, minute: Int = 0) {
    DailyReminderReceiver.ensureChannel(context)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, DailyReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      REQUEST_CODE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val cal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, hour)
      set(Calendar.MINUTE, minute)
      set(Calendar.SECOND, 0)
      if (timeInMillis <= System.currentTimeMillis()) {
        add(Calendar.DAY_OF_YEAR, 1)
      }
    }

    alarmManager.setInexactRepeating(
      AlarmManager.RTC_WAKEUP,
      cal.timeInMillis,
      AlarmManager.INTERVAL_DAY,
      pendingIntent
    )
  }

  fun cancel(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, DailyReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      REQUEST_CODE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
  }
}
