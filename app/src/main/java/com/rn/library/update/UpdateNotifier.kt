package com.rn.library.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.rn.library.MainActivity
import com.rn.library.R

object UpdateNotifier {
    const val CHANNEL_ID = "app_updates"
    const val EXTRA_OPEN_UPDATE = "open_update"
    private const val PREFS = "app_prefs"
    private const val KEY_LAST_NOTIFIED_TAG = "last_notified_release_tag"
    private const val NOTIFICATION_ID = 2101

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.update_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.update_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    fun notifyIfNew(context: Context, result: UpdateCheckResult.Available) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastTag = prefs.getString(KEY_LAST_NOTIFIED_TAG, null)
        if (lastTag == result.release.tag) return
        ensureChannel(context)
        val launch = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_UPDATE, true)
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.getString(R.string.update_notification_title))
            .setContentText(
                context.getString(
                    R.string.update_notification_text,
                    result.release.tag,
                )
            )
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            prefs.edit { putString(KEY_LAST_NOTIFIED_TAG, result.release.tag) }
        }
    }
}
