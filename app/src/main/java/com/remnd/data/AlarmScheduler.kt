package com.remnd.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.remnd.receiver.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(reminder: Reminder) {
        val storedMillis = reminder.dueTimeMillis ?: return
        val now = System.currentTimeMillis()

        val fireMillis = if (reminder.frequencyType == FrequencyType.DAILY) {
            nextOccurrenceMillis(storedMillis)
        } else {
            if (storedMillis <= now) return
            storedMillis
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(AlarmReceiver.EXTRA_TITLE, reminder.title)
            putExtra(AlarmReceiver.EXTRA_DESCRIPTION, reminder.description)
            putExtra(AlarmReceiver.EXTRA_FREQUENCY_TYPE, reminder.frequencyType)
            putExtra(AlarmReceiver.EXTRA_DUE_TIME_MILLIS, storedMillis)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, reminder.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireMillis, pendingIntent)
    }

    fun cancel(reminderId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, reminderId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    companion object {
        fun nextOccurrenceMillis(storedMillis: Long): Long {
            val cal = Calendar.getInstance().apply { timeInMillis = storedMillis }
            val now = Calendar.getInstance()
            while (!cal.after(now)) cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }
    }
}
