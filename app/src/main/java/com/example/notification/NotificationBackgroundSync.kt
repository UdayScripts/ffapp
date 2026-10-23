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
import android.os.Build
import android.util.Log
import com.example.R
import com.example.data.firestore.NotificationDto
import com.example.data.firestore.TournamentDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

object NotificationBackgroundManager {
    private const val TAG = "NotifBackgroundSync"
    const val JOB_ID = 8844
    const val ALARM_REQ_CODE = 8845
    const val ACTION_TOURNAMENT_10M_REMINDER = "com.example.notification.ACTION_TOURNAMENT_10M_REMINDER"
    const val EXTRA_TOURNAMENT_ID = "extra_tournament_id"
    const val EXTRA_TOURNAMENT_TITLE = "extra_tournament_title"
    const val EXTRA_TOURNAMENT_MODE = "extra_tournament_mode"
    const val EXTRA_TOURNAMENT_MAP = "extra_tournament_map"
    const val EXTRA_USER_ID = "extra_user_id"
    private const val PREFS_NAME = "user_notifs_prefs"
    const val TEN_MINUTES_MS = 10 * 60 * 1000L

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

    /**
     * Parses start timestamp from TournamentDto (prefers epoch timestamp if in the future or valid,
     * otherwise attempts to parse scheduleTime).
     */
    fun resolveTournamentStartEpochMs(tournament: TournamentDto): Long {
        if (tournament.timestamp > 1_000_000_000_000L) {
            return tournament.timestamp
        }
        val raw = tournament.scheduleTime.trim()
        if (raw.isNotBlank()) {
            val formats = listOf(
                "dd MMM yyyy, hh:mm a",
                "dd MMM, hh:mm a",
                "yyyy-MM-dd HH:mm",
                "dd/MM/yyyy hh:mm a"
            )
            for (pattern in formats) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.ENGLISH)
                    val parsed = sdf.parse(raw)
                    if (parsed != null) return parsed.time
                } catch (_: Exception) {
                }
            }
        }
        return tournament.timestamp
    }

    /**
     * Schedules exact/windowed 10-minute pre-match reminders via AlarmManager for all registered upcoming tournaments,
     * and immediately triggers the reminder if the user is already within the 10-minute window before match start.
     */
    fun scheduleTournamentTenMinuteReminders(
        context: Context,
        userId: String,
        tournaments: List<TournamentDto>,
        joinedTournamentIds: Set<String>
    ) {
        if (userId.isBlank() || userId.startsWith("demo_") || joinedTournamentIds.isEmpty()) return
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()

        val registeredUpcoming = tournaments.filter { t ->
            joinedTournamentIds.contains(t.id) &&
                (t.status.equals("UPCOMING", ignoreCase = true) || t.status.equals("LIVE", ignoreCase = true))
        }

        for (tournament in registeredUpcoming) {
            val startEpochMs = resolveTournamentStartEpochMs(tournament)
            if (startEpochMs <= 0L) continue

            val reminderKey = "reminded_10m_${userId}_${tournament.id}"
            if (prefs.getBoolean(reminderKey, false)) continue

            val triggerAtMs = startEpochMs - TEN_MINUTES_MS
            val timeUntilStart = startEpochMs - now

            if (timeUntilStart in 1L..TEN_MINUTES_MS) {
                // Already inside the 10-minute pre-match window! Dispatch immediately.
                CoroutineScope(Dispatchers.IO).launch {
                    dispatchTenMinuteReminder(
                        context = appContext,
                        userId = userId,
                        tournamentId = tournament.id,
                        tournamentTitle = tournament.title,
                        matchType = tournament.matchType,
                        mapName = tournament.map
                    )
                }
            } else if (triggerAtMs > now) {
                val intent = Intent(appContext, NotificationAlarmReceiver::class.java).apply {
                    action = ACTION_TOURNAMENT_10M_REMINDER
                    putExtra(EXTRA_TOURNAMENT_ID, tournament.id)
                    putExtra(EXTRA_TOURNAMENT_TITLE, tournament.title)
                    putExtra(EXTRA_TOURNAMENT_MODE, tournament.matchType)
                    putExtra(EXTRA_TOURNAMENT_MAP, tournament.map)
                    putExtra(EXTRA_USER_ID, userId)
                }
                val reqCode = ("10m_${userId}_${tournament.id}").hashCode()
                val pendingIntent = PendingIntent.getBroadcast(
                    appContext,
                    reqCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setWindow(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMs,
                            60_000L, // 1-minute window for battery-safe precision without SCHEDULE_EXACT_ALARM permission crash
                            pendingIntent
                        )
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
                    }
                    Log.d(TAG, "Scheduled 10-minute pre-match alarm for tournament ${tournament.id}")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not schedule 10-min alarm for ${tournament.id}: ${e.message}")
                }
            }
        }
    }

    suspend fun dispatchTenMinuteReminder(
        context: Context,
        userId: String,
        tournamentId: String,
        tournamentTitle: String,
        matchType: String,
        mapName: String
    ) {
        if (userId.isBlank() || tournamentId.isBlank()) return
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val reminderKey = "reminded_10m_${userId}_${tournamentId}"
        if (prefs.getBoolean(reminderKey, false)) return

        prefs.edit().putBoolean(reminderKey, true).apply()

        val notifId = "NOTIF-10M-${tournamentId.takeLast(8).uppercase()}"
        val title = "⏰ Match Starts in 10 Minutes!"
        val detailsSuffix = listOfNotNull(
            matchType.takeIf { it.isNotBlank() },
            mapName.takeIf { it.isNotBlank() }
        ).joinToString(" • ")
        val message = buildString {
            append("Your registered tournament \"$tournamentTitle\"")
            if (detailsSuffix.isNotBlank()) append(" ($detailsSuffix)")
            append(" starts in 10 minutes! Open the Tournament Lobby now to get the Room ID & Password.")
        }

        // Mark as dispatched locally so the real-time listener doesn't double-fire on the same device
        PushNotificationHelper.markNotificationDispatched(appContext, notifId)

        // 1. Show high-priority Android system push notification
        PushNotificationHelper.showPushNotification(
            context = appContext,
            notificationId = notifId.hashCode(),
            title = title,
            message = message,
            type = "MATCH_REMINDER"
        )

        // 2. Write notification document to Cloud Firestore so it appears in Notification Center & syncs across FCM/devices
        try {
            val db = FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))
            val notifDto = NotificationDto(
                id = notifId,
                title = title,
                message = message,
                type = "MATCH_REMINDER",
                tournamentId = tournamentId,
                timestamp = System.currentTimeMillis()
            )
            db.collection("notifications").document(notifId)
                .set(notifDto, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Non-fatal error saving 10m reminder to Firestore: ${e.message}")
        }
    }

    suspend fun performNotificationCheck(context: Context) {
        val appContext = context.applicationContext
        val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val savedSession = com.example.auth.AuthManager.loadSavedSession(appContext)
        val activeSessionUid = savedSession?.uid ?: authUser?.uid ?: return
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val uid = (prefs.getString("last_active_uid", "") ?: "").ifBlank { activeSessionUid }
        if (uid.isBlank() || uid.startsWith("demo_")) {
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

            // A. Check upcoming registered tournaments for the 10-minute pre-match window
            if (joinedTournamentIds.isNotEmpty()) {
                try {
                    val tournamentsSnap = db.collection("tournaments").get().await()
                    val now = System.currentTimeMillis()
                    for (tDoc in tournamentsSnap.documents) {
                        if (!joinedTournamentIds.contains(tDoc.id)) continue
                        val status = tDoc.getString("status") ?: "UPCOMING"
                        if (!status.equals("UPCOMING", ignoreCase = true)) continue

                        val rawTs = tDoc.getLong("timestamp") ?: 0L
                        val scheduleStr = tDoc.getString("scheduleTime") ?: ""
                        val tempDto = TournamentDto(
                            id = tDoc.id,
                            title = tDoc.getString("title") ?: "Free Fire Match",
                            matchType = tDoc.getString("matchType") ?: "SOLO",
                            map = tDoc.getString("map") ?: "Bermuda",
                            scheduleTime = scheduleStr,
                            timestamp = rawTs,
                            status = status
                        )
                        val startMs = resolveTournamentStartEpochMs(tempDto)
                        val timeUntilStart = startMs - now
                        if (startMs > 0L && timeUntilStart in -60_000L..TEN_MINUTES_MS) {
                            dispatchTenMinuteReminder(
                                context = appContext,
                                userId = uid,
                                tournamentId = tempDto.id,
                                tournamentTitle = tempDto.title,
                                matchType = tempDto.matchType,
                                mapName = tempDto.map
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error checking tournament 10m window in background: ${e.message}")
                }
            }

            // B. Check newly broadcasted notifications in Firestore
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
                if (intent.action == NotificationBackgroundManager.ACTION_TOURNAMENT_10M_REMINDER) {
                    val tournamentId = intent.getStringExtra(NotificationBackgroundManager.EXTRA_TOURNAMENT_ID) ?: ""
                    val tournamentTitle = intent.getStringExtra(NotificationBackgroundManager.EXTRA_TOURNAMENT_TITLE) ?: "Free Fire Tournament"
                    val matchType = intent.getStringExtra(NotificationBackgroundManager.EXTRA_TOURNAMENT_MODE) ?: "SOLO"
                    val mapName = intent.getStringExtra(NotificationBackgroundManager.EXTRA_TOURNAMENT_MAP) ?: "Bermuda"
                    val userId = intent.getStringExtra(NotificationBackgroundManager.EXTRA_USER_ID) ?: ""
                    if (tournamentId.isNotBlank() && userId.isNotBlank()) {
                        NotificationBackgroundManager.dispatchTenMinuteReminder(
                            context = context,
                            userId = userId,
                            tournamentId = tournamentId,
                            tournamentTitle = tournamentTitle,
                            matchType = matchType,
                            mapName = mapName
                        )
                    }
                } else {
                    NotificationBackgroundManager.performNotificationCheck(context)
                }
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
