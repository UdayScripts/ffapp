package com.example.data.firestore

import android.content.Context
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

data class SettleData(
    val participantId: String,
    val userId: String,
    val kills: Int,
    val placement: Int,
    val prizeWon: Int
)

class FirestoreRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    fun currentAuthUser() = auth.currentUser

    // 1. Observe Tournaments from Cloud Firestore in Real-Time
    fun observeTournaments(): Flow<List<TournamentDto>> =
        db.collection("tournaments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(TournamentDto::class.java)?.copy(id = doc.id)
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, "tournaments")
                }
                emit(emptyList())
            }

    fun observeTournament(id: String): Flow<TournamentDto?> {
        if (id.isBlank()) return flowOf<TournamentDto?>(null)
        return db.collection("tournaments").document(id)
            .snapshots()
            .map { doc ->
                if (doc.exists()) doc.toObject(TournamentDto::class.java)?.copy(id = doc.id) else null
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.GET, "tournaments/$id")
                }
                emit(null)
            }
    }

    // 2. Observe Participants for a tournament
    fun observeParticipants(tournamentId: String): Flow<List<ParticipantDto>> {
        if (tournamentId.isBlank()) return flowOf<List<ParticipantDto>>(emptyList())
        return db.collection("tournaments").document(tournamentId).collection("participants")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ParticipantDto::class.java)?.copy(id = doc.id)
                }.sortedWith(compareBy({ if (it.placement > 0) it.placement else 999 }, { -it.kills }))
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, "tournaments/$tournamentId/participants")
                }
                emit(emptyList())
            }
    }

    fun observeUserParticipations(userId: String): Flow<List<ParticipantDto>> =
        if (userId.isBlank()) flowOf<List<ParticipantDto>>(emptyList())
        else db.collection("users").document(userId).collection("participations")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ParticipantDto::class.java)?.copy(id = doc.id)
                }.sortedByDescending { it.joinedAt }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, "users/$userId/participations")
                }
                emit(emptyList())
            }

    // 3. Observe User Profile in Real-Time
    fun observeUser(userId: String): Flow<UserDto?> {
        if (userId.isBlank()) return flowOf<UserDto?>(null)
        return db.collection("users").document(userId)
            .snapshots()
            .map { doc ->
                if (doc.exists()) doc.toObject(UserDto::class.java)?.copy(id = doc.id) else null
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.GET, "users/$userId")
                }
                emit(null)
            }
    }

    fun observeAllUsers(): Flow<List<UserDto>> =
        db.collection("users")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(UserDto::class.java)?.copy(id = doc.id)
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, "users")
                }
                emit(emptyList())
            }

    // 4. Observe User Transactions in Real-Time
    fun observeTransactions(userId: String): Flow<List<TransactionDto>> {
        if (userId.isBlank()) return flowOf<List<TransactionDto>>(emptyList())
        return db.collection("users").document(userId).collection("transactions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(TransactionDto::class.java)?.copy(id = doc.id)
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, "users/$userId/transactions")
                }
                emit(emptyList())
            }
    }

    // 5. Observe Notifications in Real-Time
    fun observeNotifications(): Flow<List<NotificationDto>> =
        db.collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(NotificationDto::class.java)?.copy(id = doc.id)
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, "notifications")
                }
                emit(emptyList())
            }

    // 6. Observe Application Settings in Real-Time
    fun observeAppSettings(): Flow<AppSettingsDto?> =
        db.collection("app_settings").document("main")
            .snapshots()
            .map { doc ->
                if (doc.exists()) doc.toObject(AppSettingsDto::class.java)?.copy(id = doc.id) else null
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.GET, "app_settings/main")
                }
                emit(null)
            }

    // Join Tournament on live Cloud Firestore
    suspend fun joinTournament(
        tournamentId: String,
        userId: String,
        ffIgn: String,
        ffUid: String,
        username: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val userDocRef = db.collection("users").document(userId)
        val tourneyDocRef = db.collection("tournaments").document(tournamentId)
        val participantsRef = tourneyDocRef.collection("participants")

        try {
            val userSnap = userDocRef.get().await()
            val tourneySnap = tourneyDocRef.get().await()

            if (!tourneySnap.exists()) return@withContext Result.failure(Exception("Tournament not found"))
            val tournament = tourneySnap.toObject(TournamentDto::class.java)!!

            if (tournament.status != "UPCOMING") {
                return@withContext Result.failure(Exception("Registration closed for this match"))
            }

            if (tournament.joinedSlots >= tournament.maxSlots) {
                return@withContext Result.failure(Exception("Tournament slots are completely full!"))
            }

            val userBalance = userSnap.getLong("walletBalance")?.toInt() ?: 0
            if (userBalance < tournament.entryFee) {
                return@withContext Result.failure(Exception("Insufficient wallet balance. Please top up first."))
            }

            // Deduct balance and increment match count
            val newBalance = userBalance - tournament.entryFee
            val newMatchesCount = (userSnap.getLong("matchesPlayed")?.toInt() ?: 0) + 1
            userDocRef.update(
                mapOf(
                    "walletBalance" to newBalance,
                    "matchesPlayed" to newMatchesCount
                )
            ).await()

            // Record transaction in user's subcollection
            val txnId = "TXN-" + UUID.randomUUID().toString().take(8).uppercase()
            val txn = TransactionDto(
                id = txnId,
                userId = userId,
                amount = -tournament.entryFee,
                type = "ENTRY_FEE",
                description = "Entry Fee: ${tournament.title}",
                referenceId = "MATCH-${tournament.id}",
                status = "SUCCESS",
                timestamp = System.currentTimeMillis(),
                balanceAfter = newBalance
            )
            userDocRef.collection("transactions").document(txnId).set(txn).await()

            // Increment tournament slot count
            val nextSlot = tournament.joinedSlots + 1
            tourneyDocRef.update("joinedSlots", nextSlot).await()

            // Create participant record
            val participantId = "P-" + UUID.randomUUID().toString().take(8)
            val participant = ParticipantDto(
                id = participantId,
                tournamentId = tournamentId,
                userId = userId,
                playerName = username,
                ffUid = ffUid,
                ffIgn = ffIgn,
                slotNumber = nextSlot,
                joinedAt = System.currentTimeMillis()
            )
            participantsRef.document(participantId).set(participant).await()
            userDocRef.collection("participations").document(participantId).set(participant).await()

            // Send notification
            val notifId = "NOTIF-" + UUID.randomUUID().toString().take(8)
            val notif = NotificationDto(
                id = notifId,
                title = "Match Registration Confirmed! 🎮",
                message = "You joined ${tournament.title}. Slot #$nextSlot. Custom room keys will be shared here.",
                type = "MATCH_UPDATE",
                tournamentId = tournamentId,
                userId = userId,
                timestamp = System.currentTimeMillis()
            )
            db.collection("notifications").document(notifId).set(notif).await()

            Result.success("Successfully registered for ${tournament.title}!")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "tournaments/$tournamentId/join")
            Result.failure(e)
        }
    }

    // Instant Top-Up on Cloud Firestore
    suspend fun depositMoney(
        userId: String,
        amount: Int,
        paymentMethod: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val userDocRef = db.collection("users").document(userId)
        try {
            val userSnap = userDocRef.get().await()
            val curBalance = userSnap.getLong("walletBalance")?.toInt() ?: 0
            val newBalance = curBalance + amount
            userDocRef.update("walletBalance", newBalance).await()

            val ref = "DEP-${paymentMethod.take(3).uppercase()}-" + (100000..999999).random()
            val txnId = "TXN-" + UUID.randomUUID().toString().take(8).uppercase()
            val txn = TransactionDto(
                id = txnId,
                userId = userId,
                amount = amount,
                type = "DEPOSIT",
                description = "Instant Top-Up ($paymentMethod)",
                referenceId = ref,
                status = "SUCCESS",
                timestamp = System.currentTimeMillis(),
                balanceAfter = newBalance
            )
            userDocRef.collection("transactions").document(txnId).set(txn).await()

            // Notification
            val notifId = "NOTIF-" + UUID.randomUUID().toString().take(8)
            val notif = NotificationDto(
                id = notifId,
                title = "Deposit Credited! 💳",
                message = "₹$amount credited to your wallet via $paymentMethod. Ref: $ref",
                type = "WINNINGS",
                userId = userId,
                timestamp = System.currentTimeMillis()
            )
            db.collection("notifications").document(notifId).set(notif).await()

            Result.success("₹$amount credited to wallet!")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$userId")
            Result.failure(e)
        }
    }

    // Update Room Credentials & broadcast notification
    suspend fun updateRoomCredentials(
        tournamentId: String,
        roomId: String,
        roomPass: String,
        notice: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val tourneyDocRef = db.collection("tournaments").document(tournamentId)
        try {
            val tourneySnap = tourneyDocRef.get().await()
            val title = tourneySnap.getString("title") ?: "Free Fire Tournament"
            tourneyDocRef.update(
                mapOf(
                    "roomId" to roomId,
                    "roomPassword" to roomPass,
                    "roomNotice" to notice
                )
            ).await()

            // Broadcast real-time notification
            val notifId = "NOTIF-" + UUID.randomUUID().toString().take(8)
            val notif = NotificationDto(
                id = notifId,
                title = "🔑 Room ID & Password Live!",
                message = "Match '$title': Room ID: $roomId | Password: $roomPass. Join immediately in Free Fire!",
                type = "ROOM_KEY",
                tournamentId = tournamentId,
                roomId = roomId,
                roomPassword = roomPass,
                timestamp = System.currentTimeMillis()
            )
            db.collection("notifications").document(notifId).set(notif).await()

            Result.success("Room credentials published & broadcast sent!")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "tournaments/$tournamentId")
            Result.failure(e)
        }
    }

    // Create Tournament
    suspend fun createTournament(tournament: TournamentDto): Result<String> = withContext(Dispatchers.IO) {
        val id = if (tournament.id.isNotBlank()) tournament.id else "FF-T" + (100..999).random()
        try {
            db.collection("tournaments").document(id).set(tournament.copy(id = id)).await()

            try {
                val notifId = "NOTIF-" + UUID.randomUUID().toString().take(8)
                val notif = NotificationDto(
                    id = notifId,
                    title = "New Tournament Announced! 🏆",
                    message = "New ${tournament.matchType} on ${tournament.map} added. Prize: ₹${tournament.prizePool}!",
                    type = "ANNOUNCEMENT",
                    tournamentId = id,
                    timestamp = System.currentTimeMillis()
                )
                db.collection("notifications").document(notifId).set(notif).await()
            } catch (notifEx: Exception) {
                // Non-fatal if announcement notification fails
            }

            Result.success("Tournament published on live Cloud Firestore!")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "tournaments/$id")
            Result.failure(e)
        }
    }

    // Update Tournament
    suspend fun updateTournament(tournament: TournamentDto): Result<String> = withContext(Dispatchers.IO) {
        if (tournament.id.isBlank()) return@withContext Result.failure(Exception("Tournament ID is missing"))
        try {
            db.collection("tournaments").document(tournament.id).set(tournament, SetOptions.merge()).await()
            Result.success("Tournament updated in database!")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "tournaments/${tournament.id}")
            Result.failure(e)
        }
    }

    // Delete Tournament
    suspend fun deleteTournament(id: String): Result<String> = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext Result.failure(Exception("Tournament ID is missing"))
        try {
            db.collection("tournaments").document(id).delete().await()
            Result.success("Tournament deleted from database.")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "tournaments/$id")
            Result.failure(e)
        }
    }

    // Settle Match & Distribute Winnings
    suspend fun settleMatch(
        tournamentId: String,
        settleList: List<SettleData>
    ): Result<String> = withContext(Dispatchers.IO) {
        val tourneyDocRef = db.collection("tournaments").document(tournamentId)
        try {
            val tourneySnap = tourneyDocRef.get().await()
            val tourneyTitle = tourneySnap.getString("title") ?: "Tournament"

            for (item in settleList) {
                // Update participant record
                tourneyDocRef.collection("participants").document(item.participantId).update(
                    mapOf(
                        "kills" to item.kills,
                        "placement" to item.placement,
                        "prizeWon" to item.prizeWon
                    )
                ).await()

                // Credit user balance
                val userDocRef = db.collection("users").document(item.userId)
                val userSnap = userDocRef.get().await()
                if (userSnap.exists()) {
                    val curBal = userSnap.getLong("walletBalance")?.toInt() ?: 0
                    val curKills = userSnap.getLong("totalKills")?.toInt() ?: 0
                    val curWon = userSnap.getLong("totalWinnings")?.toInt() ?: 0

                    val newBal = curBal + item.prizeWon
                    val newKills = curKills + item.kills
                    val newWon = curWon + item.prizeWon

                    userDocRef.update(
                        mapOf(
                            "walletBalance" to newBal,
                            "totalKills" to newKills,
                            "totalWinnings" to newWon
                        )
                    ).await()

                    if (item.prizeWon > 0) {
                        val txnId = "TXN-" + UUID.randomUUID().toString().take(8).uppercase()
                        val txn = TransactionDto(
                            id = txnId,
                            userId = item.userId,
                            amount = item.prizeWon,
                            type = "PRIZE_WIN",
                            description = "Winnings: Rank #${item.placement} & ${item.kills} kills in $tourneyTitle",
                            referenceId = "SETTLE-$tournamentId",
                            status = "SUCCESS",
                            timestamp = System.currentTimeMillis(),
                            balanceAfter = newBal
                        )
                        userDocRef.collection("transactions").document(txnId).set(txn).await()
                    }
                }
            }

            // Mark tournament completed
            tourneyDocRef.update("status", "COMPLETED").await()

            // Broadcast notification
            val notifId = "NOTIF-" + UUID.randomUUID().toString().take(8)
            val notif = NotificationDto(
                id = notifId,
                title = "Match Results Settled! 🥇",
                message = "Results for '$tourneyTitle' have been finalized and prize pools credited to wallets.",
                type = "MATCH_UPDATE",
                tournamentId = tournamentId,
                timestamp = System.currentTimeMillis()
            )
            db.collection("notifications").document(notifId).set(notif).await()

            Result.success("Match settled and winnings disbursed to player wallets!")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "tournaments/$tournamentId/settle")
            Result.failure(e)
        }
    }

    // Admin Balance Adjustment
    suspend fun adjustUserBalance(userId: String, delta: Int, reason: String): Result<String> =
        withContext(Dispatchers.IO) {
            val userDocRef = db.collection("users").document(userId)
            try {
                val userSnap = userDocRef.get().await()
                val curBal = userSnap.getLong("walletBalance")?.toInt() ?: 0
                val newBal = (curBal + delta).coerceAtLeast(0)
                userDocRef.update("walletBalance", newBal).await()

                val txnId = "TXN-" + UUID.randomUUID().toString().take(8).uppercase()
                val txn = TransactionDto(
                    id = txnId,
                    userId = userId,
                    amount = delta,
                    type = if (delta >= 0) "DEPOSIT" else "ENTRY_FEE",
                    description = "Admin Adjustment: $reason",
                    referenceId = "ADMIN-ADJ",
                    status = "SUCCESS",
                    timestamp = System.currentTimeMillis(),
                    balanceAfter = newBal
                )
                userDocRef.collection("transactions").document(txnId).set(txn).await()

                Result.success("User balance updated to ₹$newBal")
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, "users/$userId")
                Result.failure(e)
            }
        }

    // Suspend / Activate User
    suspend fun toggleUserSuspension(userId: String): Result<String> = withContext(Dispatchers.IO) {
        val userDocRef = db.collection("users").document(userId)
        try {
            val userSnap = userDocRef.get().await()
            val isSuspended = userSnap.getBoolean("isSuspended") ?: false
            val newState = !isSuspended
            userDocRef.update("isSuspended", newState).await()
            Result.success("User ${if (newState) "suspended" else "activated"}")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$userId")
            Result.failure(e)
        }
    }

    // Save App Settings
    suspend fun updateAppSettings(settings: AppSettingsDto): Result<String> = withContext(Dispatchers.IO) {
        try {
            db.collection("app_settings").document("main").set(settings, SetOptions.merge()).await()
            Result.success("App settings saved to Cloud Firestore!")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "app_settings/main")
            Result.failure(e)
        }
    }

    // Broadcast Notification
    suspend fun broadcastNotification(title: String, message: String, type: String): Result<String> =
        withContext(Dispatchers.IO) {
            val notifId = "NOTIF-" + UUID.randomUUID().toString().take(8)
            val notif = NotificationDto(
                id = notifId,
                title = title,
                message = message,
                type = type,
                timestamp = System.currentTimeMillis()
            )
            try {
                db.collection("notifications").document(notifId).set(notif).await()
                Result.success("Real-time alert broadcasted across all users!")
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, "notifications/$notifId")
                Result.failure(e)
            }
        }

    suspend fun markNotificationAsRead(id: String) = withContext(Dispatchers.IO) {
        try {
            db.collection("notifications").document(id).update("isRead", true).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications/$id")
        }
    }

    suspend fun markAllNotificationsAsRead(notifications: List<NotificationDto>) = withContext(Dispatchers.IO) {
        try {
            val batch = db.batch()
            notifications.filter { !it.isRead }.forEach { n ->
                batch.update(db.collection("notifications").document(n.id), "isRead", true)
            }
            batch.commit().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications")
        }
    }

    // Sync or create authenticated user in Cloud Firestore
    suspend fun syncUserProfile(
        userId: String,
        email: String,
        displayName: String,
        isAdmin: Boolean = false
    ): UserDto = withContext(Dispatchers.IO) {
        val userDocRef = db.collection("users").document(userId)
        val snap = userDocRef.get().await()
        val isRealAdminEmail = email.equals("udaynath381@gmail.com", ignoreCase = true)
        if (snap.exists()) {
            val existing = snap.toObject(UserDto::class.java)!!.copy(id = snap.id)
            if (existing.isAdmin != isRealAdminEmail) {
                userDocRef.update("isAdmin", isRealAdminEmail).await()
                existing.copy(isAdmin = isRealAdminEmail)
            } else {
                existing
            }
        } else {
            val newUser = UserDto(
                id = userId,
                username = displayName.ifBlank { email.substringBefore("@") },
                email = email,
                ffUid = (2000000000L..9999999999L).random().toString(),
                ffIgn = displayName.ifBlank { "Player_${userId.take(4)}" },
                walletBalance = if (isRealAdminEmail) 10000 else 250,
                isAdmin = isRealAdminEmail,
                matchesPlayed = 0,
                totalKills = 0,
                totalWinnings = 0
            )
            userDocRef.set(newUser).await()

            // Welcome Bonus
            val txnId = "TXN-" + UUID.randomUUID().toString().take(8).uppercase()
            val txn = TransactionDto(
                id = txnId,
                userId = userId,
                amount = newUser.walletBalance,
                type = "DEPOSIT",
                description = "Sign-up Welcome Bonus",
                referenceId = "BONUS-${userId.take(6)}",
                status = "SUCCESS",
                timestamp = System.currentTimeMillis(),
                balanceAfter = newUser.walletBalance
            )
            userDocRef.collection("transactions").document(txnId).set(txn).await()

            newUser
        }
    }

    // Seed initial real tournaments to Cloud Firestore if database is brand new
    suspend fun seedInitialDataIfEmpty(currentUid: String) = withContext(Dispatchers.IO) {
        try {
            val existingTourneys = db.collection("tournaments").limit(1).get().await()
            if (existingTourneys.isEmpty) {
                val now = System.currentTimeMillis()
                val t1 = TournamentDto(
                    id = "FF-T101",
                    title = "Bermuda Solo Rush #101",
                    matchType = "SOLO",
                    map = "Bermuda",
                    entryFee = 30,
                    prizePool = 1200,
                    perKillReward = 10,
                    maxSlots = 48,
                    joinedSlots = 28,
                    scheduleTime = "Today, 08:30 PM",
                    timestamp = now + 3600000,
                    status = "UPCOMING",
                    roomId = "891402",
                    roomPassword = "990",
                    roomNotice = "Room is live! Join slot between 1 to 48. Match starts sharp at 8:30 PM.",
                    firstPrize = 500,
                    secondPrize = 300,
                    thirdPrize = 150
                )
                val t2 = TournamentDto(
                    id = "FF-T102",
                    title = "Purgatory Duo Showdown #42",
                    matchType = "DUO",
                    map = "Purgatory",
                    entryFee = 60,
                    prizePool = 2400,
                    perKillReward = 20,
                    maxSlots = 24,
                    joinedSlots = 16,
                    scheduleTime = "Today, 09:30 PM",
                    timestamp = now + 7200000,
                    status = "UPCOMING",
                    firstPrize = 1100,
                    secondPrize = 600,
                    thirdPrize = 300
                )
                val t3 = TournamentDto(
                    id = "FF-T103",
                    title = "Kalahari Squad Havoc #18",
                    matchType = "SQUAD",
                    map = "Kalahari",
                    entryFee = 100,
                    prizePool = 4500,
                    perKillReward = 35,
                    maxSlots = 12,
                    joinedSlots = 9,
                    scheduleTime = "Tomorrow, 07:00 PM",
                    timestamp = now + 86400000,
                    status = "UPCOMING",
                    firstPrize = 2200,
                    secondPrize = 1200,
                    thirdPrize = 600
                )
                val t4 = TournamentDto(
                    id = "FF-T100",
                    title = "Alpine Battle Royale Solo Cup",
                    matchType = "SOLO",
                    map = "Alpine",
                    entryFee = 40,
                    prizePool = 1600,
                    perKillReward = 15,
                    maxSlots = 48,
                    joinedSlots = 48,
                    scheduleTime = "Yesterday, 08:00 PM",
                    timestamp = now - 86400000,
                    status = "COMPLETED",
                    roomId = "773190",
                    roomPassword = "123",
                    firstPrize = 700,
                    secondPrize = 400,
                    thirdPrize = 200
                )

                db.collection("tournaments").document(t1.id).set(t1).await()
                db.collection("tournaments").document(t2.id).set(t2).await()
                db.collection("tournaments").document(t3.id).set(t3).await()
                db.collection("tournaments").document(t4.id).set(t4).await()

                // Initial App Settings document
                val settings = AppSettingsDto(
                    id = "main",
                    appName = "FF Tournaments",
                    announcementText = "🔥 Real-time Cloud Match Engine Online! Bermuda Solo starts at 8:30 PM. Join now.",
                    isMaintenance = false,
                    supportContact = "WhatsApp: +91 98765 43210 / Discord: @fftourneys",
                    minDeposit = 20,
                    platformFeePercent = 10
                )
                db.collection("app_settings").document("main").set(settings).await()

                // Initial Notification
                val notif = NotificationDto(
                    id = "NOTIF-WELCOME",
                    title = "🔑 Room ID & Password Ready!",
                    message = "Room details for Bermuda Solo Rush #101 are now live: Room ID 891402, Pass: 990.",
                    type = "ROOM_KEY",
                    tournamentId = "FF-T101",
                    roomId = "891402",
                    roomPassword = "990",
                    timestamp = now
                )
                db.collection("notifications").document(notif.id).set(notif).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "tournaments/seed")
        }
    }
}
