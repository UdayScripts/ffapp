package com.example.notification

import android.content.Context
import android.util.Log
import com.example.R
import com.example.auth.AuthManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TournamentFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token received")
        val appContext = applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()

        val activeUid = prefs.getString("last_active_uid", null)
            ?.takeIf { it.isNotBlank() }
            ?: AuthManager.loadSavedSession(appContext)?.uid
            ?: FirebaseAuth.getInstance().currentUser?.uid
            ?: ""

        if (activeUid.isNotBlank() && !activeUid.startsWith("demo_")) {
            CoroutineScope(Dispatchers.IO).launch {
                saveTokenToFirestore(appContext, activeUid, token)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val appContext = applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val activeUid = (prefs.getString("last_active_uid", "") ?: "").ifBlank {
            AuthManager.loadSavedSession(appContext)?.uid
                ?: FirebaseAuth.getInstance().currentUser?.uid
                ?: ""
        }

        val data = remoteMessage.data
        val notifId = data["id"] ?: data["notifId"] ?: remoteMessage.messageId ?: ""
        val targetUserId = data["userId"] ?: ""
        val targetTournamentId = data["tournamentId"] ?: ""
        val type = data["type"] ?: "ANNOUNCEMENT"

        val title = data["title"]
            ?: remoteMessage.notification?.title
            ?: "Free Fire Tournament Alert 🔥"
        val message = data["message"]
            ?: data["body"]
            ?: remoteMessage.notification?.body
            ?: "Open eSports Arena to check your match details."

        // Check targeting constraints
        if (targetUserId.isNotBlank() && activeUid.isNotBlank() && targetUserId != activeUid) {
            return
        }
        if (targetTournamentId.isNotBlank() && !type.equals("ANNOUNCEMENT", ignoreCase = true) && activeUid.isNotBlank()) {
            val joinedIds = prefs.getStringSet("joined_tournaments_$activeUid", emptySet()) ?: emptySet()
            if (joinedIds.isNotEmpty() && !joinedIds.contains(targetTournamentId)) {
                return
            }
        }

        if (notifId.isNotBlank()) {
            if (PushNotificationHelper.isNotificationDispatched(appContext, notifId)) {
                return
            }
            PushNotificationHelper.markNotificationDispatched(appContext, notifId)
        }

        val notificationIntId = if (notifId.isNotBlank()) notifId.hashCode() else System.currentTimeMillis().toInt()
        PushNotificationHelper.showPushNotification(
            context = appContext,
            notificationId = notificationIntId,
            title = title,
            message = message,
            type = type
        )
    }

    companion object {
        private const val TAG = "TournamentFCMService"
        private const val PREFS_NAME = "user_notifs_prefs"

        fun syncTokenAndTopicsForUser(
            context: Context,
            userId: String,
            joinedTournamentIds: Set<String>
        ) {
            if (userId.isBlank() || userId.startsWith("demo_")) return
            val appContext = context.applicationContext

            try {
                val messaging = FirebaseMessaging.getInstance()
                messaging.subscribeToTopic("all_players")
                messaging.subscribeToTopic("user_${sanitizeTopicName(userId)}")

                joinedTournamentIds.forEach { tid ->
                    if (tid.isNotBlank()) {
                        messaging.subscribeToTopic("tournament_${sanitizeTopicName(tid)}")
                    }
                }

                messaging.token.addOnSuccessListener { token ->
                    if (!token.isNullOrBlank()) {
                        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            .edit()
                            .putString("fcm_token", token)
                            .apply()
                        CoroutineScope(Dispatchers.IO).launch {
                            saveTokenToFirestore(appContext, userId, token)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "FCM topic/token sync non-fatal warning: ${e.message}")
            }
        }

        fun subscribeToTournamentTopic(tournamentId: String) {
            if (tournamentId.isBlank()) return
            try {
                FirebaseMessaging.getInstance()
                    .subscribeToTopic("tournament_${sanitizeTopicName(tournamentId)}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to subscribe to tournament topic: ${e.message}")
            }
        }

        private fun sanitizeTopicName(raw: String): String {
            return raw.replace(Regex("[^a-zA-Z0-9-_.~%]"), "_")
        }

        private suspend fun saveTokenToFirestore(context: Context, userId: String, token: String) {
            if (userId.isBlank() || token.isBlank()) return
            try {
                val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
                db.collection("users").document(userId)
                    .set(
                        mapOf(
                            "fcmToken" to token,
                            "fcmUpdatedAt" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    )
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Could not persist FCM token to Firestore: ${e.message}")
            }
        }
    }
}
