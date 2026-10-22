package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object PushNotificationHelper {
    const val CHANNEL_ID = "ff_tournaments_alerts_channel"
    private const val CHANNEL_NAME = "Free Fire Tournament Alerts"
    private const val CHANNEL_DESC = "Real-time notifications for Room ID, passwords, prize settlements & announcements"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                enableLights(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun showPushNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        type: String = "ALERT"
    ) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // System notification icon
        val iconRes = android.R.drawable.ic_dialog_info

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted or revoked
        } catch (e: Exception) {
            // Non-fatal notification failure
        }
    }

    fun isNotificationDispatched(context: Context, notifId: String): Boolean {
        val prefs = context.getSharedPreferences("user_notifs_prefs", Context.MODE_PRIVATE)
        val notified = prefs.getStringSet("notified_notif_ids", emptySet()) ?: emptySet()
        return notified.contains(notifId)
    }

    fun markNotificationDispatched(context: Context, notifId: String) {
        val prefs = context.getSharedPreferences("user_notifs_prefs", Context.MODE_PRIVATE)
        val notified = (prefs.getStringSet("notified_notif_ids", emptySet()) ?: emptySet()).toMutableSet()
        notified.add(notifId)
        prefs.edit().putStringSet("notified_notif_ids", notified).apply()
    }
}
