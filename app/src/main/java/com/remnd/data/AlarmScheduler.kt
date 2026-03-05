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

        val fireMillis = when (reminder.frequencyType) {
            FrequencyType.DAILY -> nextOccurrenceMillis(storedMillis)
            FrequencyType.HOURLY -> nextHourlyOccurrenceMillis(storedMillis)
            else -> {
                if (storedMillis <= now) return
                storedMillis
            }
        }

        scheduleAlarmAt(reminder, storedMillis, fireMillis, isEarly = false)

        val earlyMillis = fireMillis - EARLY_MINUTES_BEFORE * 60_000L
        if (earlyMillis > now) {
            scheduleAlarmAt(reminder, storedMillis, earlyMillis, isEarly = true)
        }
    }

    private fun scheduleAlarmAt(reminder: Reminder, storedMillis: Long, fireMillis: Long, isEarly: Boolean) {
        val requestCode = reminder.id.toInt() + if (isEarly) EARLY_REQUEST_CODE_OFFSET else 0
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(AlarmReceiver.EXTRA_TITLE, reminder.title)
            putExtra(AlarmReceiver.EXTRA_DESCRIPTION, reminder.description)
            putExtra(AlarmReceiver.EXTRA_FREQUENCY_TYPE, reminder.frequencyType)
            putExtra(AlarmReceiver.EXTRA_DUE_TIME_MILLIS, storedMillis)
            putExtra(AlarmReceiver.EXTRA_IS_EARLY, isEarly)
        }
        val pi = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireMillis, pi)
    }

    fun cancel(reminderId: Long) {
        cancelByRequestCode(reminderId.toInt())
        cancelByRequestCode(reminderId.toInt() + EARLY_REQUEST_CODE_OFFSET)
    }

    private fun cancelByRequestCode(requestCode: Int) {
        val pi = PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }

    companion object {
        const val EARLY_REQUEST_CODE_OFFSET = 1_000_000
        private const val EARLY_MINUTES_BEFORE = 5L

        fun nextOccurrenceMillis(storedMillis: Long): Long {
            val cal = Calendar.getInstance().apply { timeInMillis = storedMillis }
            val now = Calendar.getInstance()
            while (!cal.after(now)) cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }

        fun nextHourlyOccurrenceMillis(storedMillis: Long): Long {
            val cal = Calendar.getInstance().apply { timeInMillis = storedMillis }
            val now = Calendar.getInstance()
            while (!cal.after(now)) cal.add(Calendar.HOUR_OF_DAY, 1)
            return cal.timeInMillis
        }
    }
}
