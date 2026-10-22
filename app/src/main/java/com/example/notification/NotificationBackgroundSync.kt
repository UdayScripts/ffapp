package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.R
import com.example.data.firestore.NotificationDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object NotificationBackgroundManager {
    private const val TAG = "NotifBackgroundSync"
    const val JOB_ID = 8844
    const val ALARM_REQ_CODE = 8845
    private const val PREFS_NAME = "user_notifs_prefs"

    fun scheduleBackgroundSync(context: Context) {
        val appContext = context.applicationContext

        // 1. Native Android JobScheduler (Survives app complete exit and device sleep)
        val jobScheduler = appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
        if (jobScheduler != null) {
            val componentName = ComponentName(appContext, TournamentNotificationJobService::class.java)
            val jobInfoBuilder = JobInfo.Builder(JOB_ID, componentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(15 * 60 * 1000) // 15 mins (Standard Android periodic minimum)
                .setPersisted(true)

            try {
                jobScheduler.schedule(jobInfoBuilder.build())
                Log.d(TAG, "JobScheduler periodic sync scheduled successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule JobScheduler", e)
            }
        }

        // 2. AlarmManager Periodic Check for fast background responsiveness (every 5 minutes)
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager != null) {
            val intent = Intent(appContext, NotificationAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                appContext,
                ALARM_REQ_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val intervalMs = 5 * 60 * 1000L
            val triggerAt = System.currentTimeMillis() + intervalMs

            try {
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    intervalMs,
                    pendingIntent
                )
                Log.d(TAG, "AlarmManager periodic sync scheduled successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule AlarmManager", e)
            }
        }
    }

    suspend fun performNotificationCheck(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val uid = prefs.getString("last_active_uid", "") ?: ""
        if (uid.isBlank()) {
            return
        }

        val lastCheckTs = prefs.getLong(
            "last_background_check_ts",
            System.currentTimeMillis() - (2 * 60 * 60 * 1000L) // Lookback up to 2 hours
        )
        val joinedTournamentIds = prefs.getStringSet("joined_tournaments_$uid", emptySet()) ?: emptySet()
        val clearedIds = prefs.getStringSet("cleared_notifs_$uid", emptySet()) ?: emptySet()
        val notifiedIds = (prefs.getStringSet("notified_notif_ids", emptySet()) ?: emptySet()).toMutableSet()

        try {
            val db = FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))
            val snapshot = db.collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()

            var newlyNotified = false

            for (doc in snapshot.documents) {
                val notif = doc.toObject(NotificationDto::class.java)?.copy(id = doc.id) ?: continue
                if (notif.timestamp <= lastCheckTs) continue

                // Check if already notified or cleared by user
                if (notifiedIds.contains(notif.id) || clearedIds.contains(notif.id) || notif.clearedByUsers.contains(uid)) {
                    continue
                }

                // Check target audience
                val isTargetedToUser = when {
                    !notif.userId.isNullOrBlank() -> notif.userId == uid
                    !notif.tournamentId.isNullOrBlank() -> {
                        if (notif.type.equals("ANNOUNCEMENT", ignoreCase = true)) true
                        else joinedTournamentIds.contains(notif.tournamentId)
                    }
                    else -> true
                }

                if (isTargetedToUser) {
                    notifiedIds.add(notif.id)
                    newlyNotified = true

                    PushNotificationHelper.showPushNotification(
                        context = appContext,
                        notificationId = notif.id.hashCode(),
                        title = notif.title,
                        message = notif.message,
                        type = notif.type
                    )
                }
            }

            val editor = prefs.edit().putLong("last_background_check_ts", System.currentTimeMillis())
            if (newlyNotified) {
                editor.putStringSet("notified_notif_ids", notifiedIds)
            }
            editor.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking background notifications", e)
        }
    }
}

class TournamentNotificationJobService : JobService() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onStartJob(params: JobParameters?): Boolean {
        scope.launch {
            try {
                NotificationBackgroundManager.performNotificationCheck(applicationContext)
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true
    }
}

class NotificationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationBackgroundManager.performNotificationCheck(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class NotificationBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            NotificationBackgroundManager.scheduleBackgroundSync(context)
        }
    }
}
