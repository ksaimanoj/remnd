package com.remnd.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.remnd.MainActivity
import com.remnd.R
import com.remnd.RemndApplication

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
        val description = intent.getStringExtra(EXTRA_DESCRIPTION) ?: ""

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, reminderId.toInt(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            RemndApplication.NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(description.ifBlank { null })
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(reminderId.toInt(), notification)
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_DESCRIPTION = "description"
    }
}
