package com.example.data.firestore

import android.content.Context
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    ) {
        com.example.auth.AuthManager.loadSavedSession(context.applicationContext)
    }

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val wipeMutex = Mutex()

    private fun isSessionActive(): Boolean {
        return auth.currentUser != null || com.example.auth.AuthManager.currentSession() != null
    }

    fun currentAuthUser() = auth.currentUser

    // 1. Observe Tournaments from Cloud Firestore in Real-Time (Strictly real database data)
    fun observeTournaments(): Flow<List<TournamentDto>> {
        return db.collection("tournaments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val t = doc.toSafeTournamentDto()
                    if (t != null && !isDemoTournament(doc.id, t.title)) t else null
                }
            }
            .catch { error ->
                if (error is Exception && isSessionActive()) {
                    handleFirestoreError(error, OperationType.LIST, "tournaments")
                }
                emit(emptyList())
            }
    }

    fun observeTournament(id: String): Flow<TournamentDto?> {
        if (id.isBlank() || id.startsWith("DEMO-")) return flowOf<TournamentDto?>(null)
        return db.collection("tournaments").document(id)
            .snapshots()
            .map { doc ->
                doc.toSafeTournamentDto()
            }
            .catch { error ->
                if (error is Exception && isSessionActive()) {
                    handleFirestoreError(error, OperationType.GET, "tournaments/$id")
                }
                emit(null)
            }
    }

    // 2. Observe Participants for a tournament
    fun observeParticipants(tournamentId: String): Flow<List<ParticipantDto>> {
        if (tournamentId.isBlank() || tournamentId.startsWith("DEMO-")) {
            return flowOf<List<ParticipantDto>>(emptyList())
        }
        return db.collection("tournaments").document(tournamentId).collection("participants")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toSafeParticipantDto()
                }.sortedWith(compareBy({ if (it.placement > 0) it.placement else 999 }, { -it.kills }))
            }
            .catch { error ->
                if (error is Exception && isSessionActive()) {
                    handleFirestoreError(error, OperationType.LIST, "tournaments/$tournamentId/participants")
                }
                emit(emptyList())
            }
    }

    fun observeUserParticipations(userId: String): Flow<List<ParticipantDto>> =
        if (userId.isBlank() || userId.startsWith("demo_")) {
            flowOf<List<ParticipantDto>>(emptyList())
        } else db.collection("users").document(userId).collection("participations")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toSafeParticipantDto()
                }.sortedByDescending { it.joinedAt }
            }
            .catch { error ->
                if (error is Exception && isSessionActive()) {
                    handleFirestoreError(error, OperationType.LIST, "users/$userId/participations")
                }
                emit(emptyList())
            }

    // 3. Observe User Profile in Real-Time
    fun observeUser(userId: String): Flow<UserDto?> {
        if (userId.isBlank() || userId.startsWith("demo_")) return flowOf<UserDto?>(null)
        return db.collection("users").document(userId)
            .snapshots()
            .map { doc ->
                doc.toSafeUserDto()
            }
            .catch { error ->
                if (error is Exception && isSessionActive()) {
                    handleFirestoreError(error, OperationType.GET, "users/$userId")
                }
                emit(null)
            }
    }

    fun observeAllUsers(): Flow<List<UserDto>> {
        return db.collection("users")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val u = doc.toSafeUserDto()
                    if (u != null && !isDemoUser(doc.id, u.email)) u else null
                }.sortedWith(
                    compareByDescending<UserDto> { it.isAdmin }
                        .thenByDescending { it.signupTimestamp }
                        .thenBy { it.username.lowercase() }
                )
            }
            .catch { error ->
                if (error is Exception && isSessionActive()) {
                    handleFirestoreError(error, OperationType.LIST, "users")
                }
                emit(emptyList())
            }
    }

    // 4. Observe User Transactions in Real-Time
    fun observeTransactions(userId: String): Flow<List<TransactionDto>> {
        if (userId.isBlank() || userId.startsWith("demo_")) {
            return flowOf<List<TransactionDto>>(emptyList())
        }
        return db.collection("users").document(userId).collection("transactions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toSafeTransactionDto()
                }
            }
            .catch { error ->
                if (error is Exception && isSessionActive()) {
                    handleFirestoreError(error, OperationType.LIST, "users/$userId/transactions")
                }
                emit(emptyList())
            }
    }

    // 5. Observe Notifications in Real-Time
    fun observeNotifications(): Flow<List<NotificationDto>> {
        return db.collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toSafeNotificationDto()
                }
            }
            .catch { error ->
                if (error is Exception && isSessionActive()) {
                    handleFirestoreError(error, OperationType.LIST, "notifications")
                }
                emit(emptyList())
            }
    }

    // 6. Observe Application Settings in Real-Time
    fun observeAppSettings(): Flow<AppSettingsDto?> {
        return db.collection("app_settings").document("main")
            .snapshots()
            .map { doc ->
                doc.toSafeAppSettingsDto()
            }
            .catch { error ->
                if (error is Exception && isSessionActive()) {
                    handleFirestoreError(error, OperationType.GET, "app_settings/main")
                }
                emit(null)
            }
    }

    // Update User Free Fire Gaming Profile (IGN & UID)
    suspend fun updateUserGamingProfile(
        userId: String,
        ffIgn: String,
        ffUid: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.failure(Exception("User ID is missing"))
        try {
            val cleanIgn = ffIgn.trim()
            val cleanUid = ffUid.trim()
            val authUser = auth.currentUser?.takeIf { it.uid == userId }
            val activeSession = com.example.auth.AuthManager.currentSession()?.takeIf { it.uid == userId }
            val email = authUser?.email ?: activeSession?.email ?: ""
            val username = authUser?.displayName?.ifBlank { activeSession?.username ?: email.substringBefore("@") }
                ?: activeSession?.username
                ?: "Player"
            val isAdmin = com.example.auth.AuthManager.isMasterAdminIdentity(email = email, username = username, uid = userId) ||
                (activeSession?.isAdmin == true)
            val updates = mutableMapOf<String, Any>(
                "id" to userId,
                "userId" to userId,
                "ffIgn" to cleanIgn,
                "ffUid" to cleanUid,
                "inGameName" to cleanIgn,
                "inGameId" to cleanUid
            )
            if (email.isNotBlank()) {
                updates["email"] = email
                updates["isAdmin"] = isAdmin
            }
            val userDocRef = db.collection("users").document(userId)
            val snap = userDocRef.get().await()
            if (!snap.exists()) {
                updates["username"] = username
                updates["usernameLower"] = com.example.auth.AuthManager.normalizeUsernameKey(username)
                updates["walletBalance"] = 0
                updates["winningBalance"] = 0
                updates["totalWithdrawn"] = 0
                updates["matchesPlayed"] = 0
                updates["totalKills"] = 0
                updates["totalWinnings"] = 0
                updates["isSuspended"] = false
                updates["bonusClaimed"] = true
                updates["signupTimestamp"] = System.currentTimeMillis()
            } else {
                val existingUsername = snap.getString("username") ?: ""
                if (existingUsername.isBlank() && username.isNotBlank()) {
                    updates["username"] = username
                    updates["usernameLower"] = com.example.auth.AuthManager.normalizeUsernameKey(username)
                }
            }
            userDocRef.set(updates, SetOptions.merge()).await()

            // Also sync open support chat metadata if it exists
            try {
                val supportRef = db.collection("support_chats").document(userId)
                val supportSnap = supportRef.get().await()
                if (supportSnap.exists()) {
                    supportRef.set(
                        mapOf(
                            "userFfIgn" to cleanIgn,
                            "userFfUid" to cleanUid
                        ),
                        SetOptions.merge()
                    ).await()
                }
            } catch (_: Exception) {}

            Result.success("Free Fire gaming profile updated!")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$userId")
            Result.failure(e)
        }
    }

    // Join Tournament on live Cloud Firestore with optional chosen slot
    suspend fun joinTournament(
        tournamentId: String,
        userId: String,
        ffIgn: String,
        ffUid: String,
        username: String,
        chosenSlot: Int? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val userDocRef = db.collection("users").document(userId)
        val tourneyDocRef = db.collection("tournaments").document(tournamentId)
        val participantsRef = tourneyDocRef.collection("participants")

        try {
            val userSnap = userDocRef.get().await()
            val tourneySnap = tourneyDocRef.get().await()

            if (!tourneySnap.exists()) return@withContext Result.failure(Exception("Tournament not found"))
            val tournament = tourneySnap.toSafeTournamentDto()
                ?: return@withContext Result.failure(Exception("Invalid tournament data"))

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

            // Deduct balance, increment match count, and sync Free Fire IGN & UID
            val newBalance = userBalance - tournament.entryFee
            val newMatchesCount = (userSnap.getLong("matchesPlayed")?.toInt() ?: 0) + 1
            userDocRef.set(
                mapOf(
                    "walletBalance" to newBalance,
                    "matchesPlayed" to newMatchesCount,
                    "ffIgn" to ffIgn.trim(),
                    "ffUid" to ffUid.trim()
                ),
                SetOptions.merge()
            ).await()

            // Calculate slot: use user's chosen slot if valid and untaken, else first available
            val participantsSnap = participantsRef.get().await()
            val takenSlots = participantsSnap.documents.mapNotNull { it.getLong("slotNumber")?.toInt() }.toSet()
            val assignedSlot = if (chosenSlot != null && chosenSlot in 1..tournament.maxSlots && !takenSlots.contains(chosenSlot)) {
                chosenSlot
            } else {
                (1..tournament.maxSlots).firstOrNull { !takenSlots.contains(it) } ?: (tournament.joinedSlots + 1)
            }

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
            val nextSlotCount = tournament.joinedSlots + 1
            tourneyDocRef.update("joinedSlots", nextSlotCount).await()

            // Create participant record
            val participantId = "P-" + UUID.randomUUID().toString().take(8)
            val participant = ParticipantDto(
                id = participantId,
                tournamentId = tournamentId,
                userId = userId,
                playerName = username,
                ffUid = ffUid.trim(),
                ffIgn = ffIgn.trim(),
                slotNumber = assignedSlot,
                joinedAt = System.currentTimeMillis()
            )
            participantsRef.document(participantId).set(participant).await()
            userDocRef.collection("participations").document(participantId).set(participant).await()

            // Send notification
            val notifId = "NOTIF-" + UUID.randomUUID().toString().take(8)
            val notif = NotificationDto(
                id = notifId,
                title = "Match Registration Confirmed! 🎮",
                message = "You joined ${tournament.title}. Slot #$assignedSlot selected. Custom room keys will be shared here.",
                type = "MATCH_UPDATE",
                tournamentId = tournamentId,
                userId = userId,
                timestamp = System.currentTimeMillis()
            )
            db.collection("notifications").document(notifId).set(notif).await()

            Result.success("Registered successfully! You booked Slot #$assignedSlot.")
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
            val authUser = auth.currentUser?.takeIf { it.uid == userId }
            val email = userSnap.getString("email") ?: authUser?.email ?: ""
            val username = userSnap.getString("username") ?: authUser?.displayName?.ifBlank { email.substringBefore("@") } ?: "Player"
            userDocRef.set(
                mapOf(
                    "id" to userId,
                    "username" to username,
                    "email" to email,
                    "isAdmin" to com.example.auth.AuthManager.isMasterAdminIdentity(email = email, username = username, uid = userId),
                    "walletBalance" to newBalance
                ),
                SetOptions.merge()
            ).await()

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

    // Real Paytm UPI Deposit verification & credit
    suspend fun verifyAndCreditRealDeposit(
        userId: String,
        orderId: String,
        amount: Int,
        txnId: String,
        method: String = "Paytm UPI"
    ): Result<String> = withContext(Dispatchers.IO) {
        val userDocRef = db.collection("users").document(userId)
        val txnDocRef = userDocRef.collection("transactions").document(orderId)

        try {
            // Check idempotency - has this order already been credited?
            val existingTxn = txnDocRef.get().await()
            if (existingTxn.exists()) {
                return@withContext Result.success("Transaction already processed and credited!")
            }

            val userSnap = userDocRef.get().await()
            val curBalance = userSnap.getLong("walletBalance")?.toInt() ?: 0
            val newBalance = curBalance + amount
            val authUser = auth.currentUser?.takeIf { it.uid == userId }
            val email = userSnap.getString("email") ?: authUser?.email ?: ""
            val username = userSnap.getString("username") ?: authUser?.displayName?.ifBlank { email.substringBefore("@") } ?: "Player"
            userDocRef.set(
                mapOf(
                    "id" to userId,
                    "username" to username,
                    "email" to email,
                    "isAdmin" to com.example.auth.AuthManager.isMasterAdminIdentity(email = email, username = username, uid = userId),
                    "walletBalance" to newBalance
                ),
                SetOptions.merge()
            ).await()

            val txn = TransactionDto(
                id = orderId,
                userId = userId,
                amount = amount,
                type = "DEPOSIT",
                description = "Paytm UPI Deposit (Txn: $txnId)",
                referenceId = txnId,
                status = "SUCCESS",
                timestamp = System.currentTimeMillis(),
                balanceAfter = newBalance
            )
            txnDocRef.set(txn).await()

            // Notification
            val notifId = "NOTIF-DEP-" + orderId.takeLast(8)
            val notif = NotificationDto(
                id = notifId,
                title = "Payment Verified! ₹$amount Credited 💳",
                message = "Paytm transaction $txnId verified! ₹$amount has been added to your wallet balance.",
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

            Result.success("Tournament published live!")
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

                    val curWinBal = userSnap.getLong("winningBalance")?.toInt() ?: curWon

                    val newBal = curBal + item.prizeWon
                    val newKills = curKills + item.kills
                    val newWon = curWon + item.prizeWon
                    val newWinBal = curWinBal + item.prizeWon

                    userDocRef.update(
                        mapOf(
                            "walletBalance" to newBal,
                            "totalKills" to newKills,
                            "totalWinnings" to newWon,
                            "winningBalance" to newWinBal
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

                        val winNotifId = "NOTIF-" + UUID.randomUUID().toString().take(8)
                        val winNotif = NotificationDto(
                            id = winNotifId,
                            title = "Prize Winnings Credited! 🏆",
                            message = "You won ₹${item.prizeWon} (Rank #${item.placement}, ${item.kills} kills) in '$tourneyTitle'!",
                            type = "WINNINGS",
                            tournamentId = tournamentId,
                            userId = item.userId,
                            timestamp = System.currentTimeMillis()
                        )
                        db.collection("notifications").document(winNotifId).set(winNotif).await()
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

    // Observe User Withdrawal Requests
    fun observeUserWithdrawals(userId: String): Flow<List<WithdrawalRequestDto>> {
        if (userId.isBlank() || userId.startsWith("demo_")) return flowOf(emptyList())
        return db.collection("withdrawals")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toSafeWithdrawalRequestDto()
                }.sortedByDescending { it.timestamp }
            }
            .catch { error ->
                if (error is Exception && isSessionActive()) {
                    handleFirestoreError(error, OperationType.LIST, "withdrawals/user/$userId")
                }
                emit(emptyList())
            }
    }

    // Observe All Withdrawal Requests (for Admin)
    fun observeAllWithdrawals(): Flow<List<WithdrawalRequestDto>> {
        return db.collection("withdrawals")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toSafeWithdrawalRequestDto()
                }.sortedByDescending { it.timestamp }
            }
            .catch { error ->
                if (error is Exception && isSessionActive()) {
                    handleFirestoreError(error, OperationType.LIST, "withdrawals")
                }
                emit(emptyList())
            }
    }

    // Request Withdrawal using UPI (User can withdraw winnings amount only)
    suspend fun requestWithdrawal(
        userId: String,
        amount: Int,
        upiId: String,
        accountHolderName: String,
        saveUpiId: Boolean = true
    ): Result<WithdrawalRequestDto> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.failure(Exception("Authentication required"))
        if (amount < 20) return@withContext Result.failure(Exception("Minimum withdrawal amount is ₹20"))
        val trimmedUpi = upiId.trim()
        if (!trimmedUpi.contains("@") || trimmedUpi.length < 5) {
            return@withContext Result.failure(Exception("Please enter a valid UPI ID (e.g. yourname@upi)"))
        }
        val trimmedName = accountHolderName.trim().ifBlank { "Player" }

        val userDocRef = db.collection("users").document(userId)
        val wdrId = "WDR-" + UUID.randomUUID().toString().take(8).uppercase()
        val wdrDocRef = db.collection("withdrawals").document(wdrId)
        val txnId = "TXN-" + UUID.randomUUID().toString().take(8).uppercase()
        val txnDocRef = userDocRef.collection("transactions").document(txnId)

        try {
            val withdrawal = db.runTransaction { transaction ->
                val userSnap = transaction.get(userDocRef)
                if (!userSnap.exists()) {
                    throw IllegalStateException("User profile not found")
                }
                val curBal = userSnap.getLong("walletBalance")?.toInt() ?: 0
                val totalWon = userSnap.getLong("totalWinnings")?.toInt() ?: 0
                val totalWithdrawn = userSnap.getLong("totalWithdrawn")?.toInt() ?: 0
                val storedWinBal = userSnap.getLong("winningBalance")?.toInt()
                val isSuspended = userSnap.getBoolean("isSuspended") ?: false

                if (isSuspended) {
                    throw IllegalStateException("Account is suspended. Contact support.")
                }

                // Strictly enforce: User can withdraw winnings amount only
                val fromLifetime = (totalWon - totalWithdrawn).coerceAtLeast(0)
                val effectiveWinnings = if (storedWinBal != null) {
                    val base = if (totalWithdrawn > 0) minOf(storedWinBal, fromLifetime) else storedWinBal
                    minOf(base.coerceAtLeast(0), curBal.coerceAtLeast(0))
                } else {
                    minOf(fromLifetime, curBal.coerceAtLeast(0))
                }

                if (amount > effectiveWinnings) {
                    throw IllegalStateException("Cannot withdraw ₹$amount. Your withdrawable winning balance is ₹$effectiveWinnings. You can only withdraw winnings earned from tournaments.")
                }
                if (amount > curBal) {
                    throw IllegalStateException("Insufficient total balance in wallet.")
                }

                val newBal = (curBal - amount).coerceAtLeast(0)
                val newWinBal = (effectiveWinnings - amount).coerceAtLeast(0)
                val newTotalWithdrawn = totalWithdrawn + amount

                val updates = mutableMapOf<String, Any>(
                    "walletBalance" to newBal,
                    "winningBalance" to newWinBal,
                    "totalWithdrawn" to newTotalWithdrawn
                )
                if (saveUpiId && trimmedUpi.isNotBlank()) {
                    updates["savedUpiId"] = trimmedUpi
                }

                transaction.update(userDocRef, updates)

                val userName = userSnap.getString("username") ?: "Player"
                val userEmail = userSnap.getString("email") ?: ""
                val ffIgn = userSnap.getString("ffIgn") ?: ""
                val ffUid = userSnap.getString("ffUid") ?: ""

                val req = WithdrawalRequestDto(
                    id = wdrId,
                    userId = userId,
                    userName = userName,
                    userEmail = userEmail,
                    userFfIgn = ffIgn,
                    userFfUid = ffUid,
                    amount = amount,
                    upiId = trimmedUpi,
                    accountHolderName = trimmedName,
                    status = "PENDING",
                    timestamp = System.currentTimeMillis()
                )
                transaction.set(wdrDocRef, req)

                val txn = TransactionDto(
                    id = txnId,
                    userId = userId,
                    amount = -amount,
                    type = "WITHDRAWAL",
                    description = "UPI Withdrawal Request (Pending): $trimmedUpi",
                    referenceId = wdrId,
                    status = "PENDING",
                    timestamp = System.currentTimeMillis(),
                    balanceAfter = newBal
                )
                transaction.set(txnDocRef, txn)

                req
            }.await()

            // Notification for user
            val notifId = "NOTIF-WDR-" + wdrId.takeLast(6)
            val notif = NotificationDto(
                id = notifId,
                title = "Withdrawal Request Submitted ⏳",
                message = "Your request to withdraw ₹$amount to $trimmedUpi is under review. Admin will approve shortly.",
                type = "WITHDRAWAL",
                userId = userId,
                timestamp = System.currentTimeMillis()
            )
            db.collection("notifications").document(notifId).set(notif).await()

            Result.success(withdrawal)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "withdrawals/$wdrId")
            Result.failure(e)
        }
    }

    // Admin: Approve & Mark Withdrawal Paid
    suspend fun approveWithdrawal(
        withdrawalId: String,
        adminRefId: String = "",
        adminNote: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val wdrDocRef = db.collection("withdrawals").document(withdrawalId)
        try {
            val (userId, amount, upiId) = db.runTransaction { transaction ->
                val wdrSnap = transaction.get(wdrDocRef)
                if (!wdrSnap.exists()) {
                    throw IllegalStateException("Withdrawal request not found")
                }
                val curStatus = wdrSnap.getString("status") ?: "PENDING"
                if (curStatus != "PENDING") {
                    throw IllegalStateException("Request is already $curStatus")
                }
                val uid = wdrSnap.getString("userId") ?: ""
                val amt = wdrSnap.getLong("amount")?.toInt() ?: 0
                val upi = wdrSnap.getString("upiId") ?: ""

                val userDocRef = db.collection("users").document(uid)
                val userSnap = transaction.get(userDocRef)
                val txnDocRef = userDocRef.collection("transactions").document("TXN-WDR-" + withdrawalId.takeLast(8))
                val txnSnap = transaction.get(txnDocRef)

                if (userSnap.exists()) {
                    val curBal = userSnap.getLong("walletBalance")?.toInt() ?: 0
                    val totalWon = userSnap.getLong("totalWinnings")?.toInt() ?: 0
                    val storedWinBal = userSnap.getLong("winningBalance")?.toInt()
                    val curWithdrawn = userSnap.getLong("totalWithdrawn")?.toInt() ?: 0

                    // Ensure totalWithdrawn and winningBalance are strictly synced upon approval
                    val syncedWithdrawn = maxOf(curWithdrawn, amt)
                    val maxRemainingWin = (totalWon - syncedWithdrawn).coerceAtLeast(0)
                    val syncedWinBal = if (storedWinBal != null) {
                        minOf(storedWinBal, maxRemainingWin, curBal).coerceAtLeast(0)
                    } else {
                        minOf(maxRemainingWin, curBal).coerceAtLeast(0)
                    }
                    transaction.update(userDocRef, mapOf(
                        "winningBalance" to syncedWinBal,
                        "totalWithdrawn" to syncedWithdrawn
                    ))
                }

                if (txnSnap.exists()) {
                    transaction.update(txnDocRef, mapOf(
                        "status" to "SUCCESS",
                        "description" to "UPI Withdrawal Approved & Paid: $upi${if (adminRefId.isNotBlank()) " (UTR: $adminRefId)" else ""}"
                    ))
                }

                val now = System.currentTimeMillis()
                val updates = mutableMapOf<String, Any>(
                    "status" to "APPROVED",
                    "processedAt" to now
                )
                if (adminRefId.isNotBlank()) updates["adminRefId"] = adminRefId
                if (adminNote.isNotBlank()) updates["adminNote"] = adminNote

                transaction.update(wdrDocRef, updates)
                Triple(uid, amt, upi)
            }.await()

            // Broadcast approval notification to user
            val notifId = "NOTIF-WDR-APP-" + withdrawalId.takeLast(6)
            val notif = NotificationDto(
                id = notifId,
                title = "Withdrawal Approved! ₹$amount Transferred 💸",
                message = "Your withdrawal of ₹$amount to $upiId has been approved and paid!${if (adminRefId.isNotBlank()) " UTR / Reference: $adminRefId." else ""}",
                type = "WITHDRAWAL",
                userId = userId,
                timestamp = System.currentTimeMillis()
            )
            db.collection("notifications").document(notifId).set(notif).await()

            Result.success("Withdrawal approved and marked as paid!")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "withdrawals/$withdrawalId")
            Result.failure(e)
        }
    }

    // Admin: Reject Withdrawal & Refund amount to user wallet
    suspend fun rejectWithdrawal(
        withdrawalId: String,
        reason: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmedReason = reason.trim().ifBlank { "Request declined by admin" }
        val wdrDocRef = db.collection("withdrawals").document(withdrawalId)
        try {
            val (userId, amount, upiId) = db.runTransaction { transaction ->
                val wdrSnap = transaction.get(wdrDocRef)
                if (!wdrSnap.exists()) {
                    throw IllegalStateException("Withdrawal request not found")
                }
                val curStatus = wdrSnap.getString("status") ?: "PENDING"
                if (curStatus != "PENDING") {
                    throw IllegalStateException("Request is already $curStatus")
                }
                val uid = wdrSnap.getString("userId") ?: ""
                val amt = wdrSnap.getLong("amount")?.toInt() ?: 0
                val upi = wdrSnap.getString("upiId") ?: ""

                val userDocRef = db.collection("users").document(uid)
                val userSnap = transaction.get(userDocRef)
                if (userSnap.exists()) {
                    val curBal = userSnap.getLong("walletBalance")?.toInt() ?: 0
                    val totalWon = userSnap.getLong("totalWinnings")?.toInt() ?: 0
                    val curWithdrawn = userSnap.getLong("totalWithdrawn")?.toInt() ?: 0
                    val newWithdrawn = (curWithdrawn - amt).coerceAtLeast(0)
                    val curWin = userSnap.getLong("winningBalance")?.toInt() ?: (totalWon - curWithdrawn).coerceAtLeast(0)
                    val newBal = curBal + amt
                    val newWin = (curWin + amt).coerceAtMost(totalWon.coerceAtLeast(curWin + amt)).coerceAtMost(newBal)
                    transaction.update(userDocRef, mapOf(
                        "walletBalance" to newBal,
                        "winningBalance" to newWin,
                        "totalWithdrawn" to newWithdrawn
                    ))

                    val refundTxnId = "TXN-REF-" + UUID.randomUUID().toString().take(8).uppercase()
                    val refundTxn = TransactionDto(
                        id = refundTxnId,
                        userId = uid,
                        amount = amt,
                        type = "WITHDRAWAL_REFUND",
                        description = "Withdrawal Refund: $trimmedReason",
                        referenceId = withdrawalId,
                        status = "SUCCESS",
                        timestamp = System.currentTimeMillis(),
                        balanceAfter = newBal
                    )
                    transaction.set(userDocRef.collection("transactions").document(refundTxnId), refundTxn)
                }

                val now = System.currentTimeMillis()
                transaction.update(wdrDocRef, mapOf(
                    "status" to "REJECTED",
                    "adminNote" to trimmedReason,
                    "processedAt" to now
                ))

                Triple(uid, amt, upi)
            }.await()

            // Notification to user
            val notifId = "NOTIF-WDR-REJ-" + withdrawalId.takeLast(6)
            val notif = NotificationDto(
                id = notifId,
                title = "Withdrawal Rejected & Refunded ⚠️",
                message = "Your withdrawal request of ₹$amount to $upiId was rejected: $trimmedReason. ₹$amount has been refunded to your wallet balance.",
                type = "WITHDRAWAL",
                userId = userId,
                timestamp = System.currentTimeMillis()
            )
            db.collection("notifications").document(notifId).set(notif).await()

            Result.success("Withdrawal rejected and ₹$amount refunded to user wallet.")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "withdrawals/$withdrawalId")
            Result.failure(e)
        }
    }

    // Save or update user's UPI ID for withdrawals
    suspend fun saveUserUpiId(userId: String, upiId: String): Result<String> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.failure(Exception("Authentication required"))
        val trimmedUpi = upiId.trim()
        if (!trimmedUpi.contains("@") || trimmedUpi.length < 5) {
            return@withContext Result.failure(Exception("Please enter a valid UPI ID (e.g. yourname@upi)"))
        }
        try {
            db.collection("users").document(userId)
                .update("savedUpiId", trimmedUpi)
                .await()
            Result.success("UPI ID saved for future withdrawals")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$userId")
            Result.failure(e)
        }
    }

    // Save App Settings
    suspend fun updateAppSettings(settings: AppSettingsDto): Result<String> = withContext(Dispatchers.IO) {
        try {
            db.collection("app_settings").document("main").set(settings, SetOptions.merge()).await()
            Result.success("App settings saved to Cloud Server!")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "app_settings/main")
            Result.failure(e)
        }
    }

    // Broadcast Custom Notification (All Players, Specific Tournament, or Specific Player)
    suspend fun broadcastNotification(
        title: String,
        message: String,
        type: String = "ANNOUNCEMENT",
        targetUserId: String? = null,
        targetTournamentId: String? = null,
        roomId: String? = null,
        roomPassword: String? = null
    ): Result<String> =
        withContext(Dispatchers.IO) {
            val notifId = "NOTIF-" + UUID.randomUUID().toString().take(8).uppercase()
            val notif = NotificationDto(
                id = notifId,
                title = title.trim(),
                message = message.trim(),
                type = type.ifBlank { "ANNOUNCEMENT" },
                userId = targetUserId?.takeIf { it.isNotBlank() },
                tournamentId = targetTournamentId?.takeIf { it.isNotBlank() },
                roomId = roomId?.takeIf { it.isNotBlank() },
                roomPassword = roomPassword?.takeIf { it.isNotBlank() },
                timestamp = System.currentTimeMillis()
            )
            try {
                db.collection("notifications").document(notifId).set(notif).await()
                val targetLabel = when {
                    !targetUserId.isNullOrBlank() -> "selected player"
                    !targetTournamentId.isNullOrBlank() -> "registered tournament players"
                    else -> "all players"
                }
                Result.success("Custom push notification sent to $targetLabel!")
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, "notifications/$notifId")
                Result.failure(e)
            }
        }

    suspend fun markNotificationAsRead(id: String, userId: String = "") = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext
        try {
            val docRef = db.collection("notifications").document(id)
            val updates = mutableMapOf<String, Any>()
            if (userId.isNotBlank()) {
                updates["readByUsers"] = FieldValue.arrayUnion(userId)
            } else {
                updates["isRead"] = true
            }
            docRef.set(updates, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications/$id")
        }
    }

    suspend fun markAllNotificationsAsRead(notifications: List<NotificationDto>, userId: String = "") = withContext(Dispatchers.IO) {
        val unreadList = notifications.filter { !it.isRead }
        if (unreadList.isEmpty()) return@withContext
        try {
            unreadList.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { n ->
                    val docRef = db.collection("notifications").document(n.id)
                    val updates = mutableMapOf<String, Any>()
                    if (userId.isNotBlank()) {
                        updates["readByUsers"] = FieldValue.arrayUnion(userId)
                    } else {
                        updates["isRead"] = true
                    }
                    batch.set(docRef, updates, SetOptions.merge())
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications")
        }
    }

    suspend fun clearNotificationForUser(id: String, userId: String) = withContext(Dispatchers.IO) {
        if (id.isBlank() || userId.isBlank()) return@withContext
        try {
            val docRef = db.collection("notifications").document(id)
            docRef.set(
                mapOf("clearedByUsers" to FieldValue.arrayUnion(userId)),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications/$id")
        }
    }

    suspend fun clearAllNotificationsForUser(notifications: List<NotificationDto>, userId: String) = withContext(Dispatchers.IO) {
        if (notifications.isEmpty() || userId.isBlank()) return@withContext
        try {
            notifications.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { n ->
                    val docRef = db.collection("notifications").document(n.id)
                    batch.set(
                        docRef,
                        mapOf("clearedByUsers" to FieldValue.arrayUnion(userId)),
                        SetOptions.merge()
                    )
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications")
        }
    }

    // Sync or create authenticated user in Cloud Firestore with clean 0 balance and 0 history
    suspend fun syncUserProfile(
        userId: String,
        email: String,
        displayName: String,
        isAdmin: Boolean = false,
        deviceId: String = "",
        ffIgn: String = "",
        ffUid: String = "",
        passwordHash: String = ""
    ): UserDto = withContext(Dispatchers.IO) {
        wipeMutex.withLock {
            val userDocRef = db.collection("users").document(userId)
            val isRealAdminEmail = com.example.auth.AuthManager.isMasterAdminIdentity(
                email = email,
                username = displayName,
                uid = userId
            ) || isAdmin
            val nowTs = System.currentTimeMillis()
            val resolvedUsername = displayName.ifBlank { email.substringBefore("@").ifBlank { "Player" } }
            val usernameLower = com.example.auth.AuthManager.normalizeUsernameKey(resolvedUsername)

            try {
                val snap = userDocRef.get().await()
                val existing = snap.toSafeUserDto()
                if (existing != null) {
                    val resolvedSignupTs = if (existing.signupTimestamp > 0L) {
                        existing.signupTimestamp
                    } else {
                        existing.createdAt?.toDate()?.time ?: nowTs
                    }
                    val updates = mutableMapOf<String, Any>()
                    if (existing.signupTimestamp <= 0L) {
                        updates["signupTimestamp"] = resolvedSignupTs
                    }
                    if (existing.isAdmin != isRealAdminEmail) {
                        updates["isAdmin"] = isRealAdminEmail
                    }
                    if (existing.usernameLower.isBlank() && usernameLower.isNotBlank()) {
                        updates["usernameLower"] = usernameLower
                    }
                    if (passwordHash.isNotBlank() && existing.passwordHash != passwordHash) {
                        updates["passwordHash"] = passwordHash
                    }
                    if (existing.ffIgn.isBlank() && ffIgn.isNotBlank()) {
                        updates["ffIgn"] = ffIgn.trim()
                        updates["inGameName"] = ffIgn.trim()
                    }
                    if (existing.ffUid.isBlank() && ffUid.isNotBlank()) {
                        updates["ffUid"] = ffUid.trim()
                        updates["inGameId"] = ffUid.trim()
                    }
                    if (updates.isNotEmpty()) {
                        userDocRef.set(updates, SetOptions.merge()).await()
                    }
                    return@withLock existing.copy(
                        isAdmin = isRealAdminEmail,
                        usernameLower = existing.usernameLower.ifBlank { usernameLower },
                        passwordHash = passwordHash.ifBlank { existing.passwordHash },
                        signupTimestamp = resolvedSignupTs,
                        ffIgn = if (existing.ffIgn.isNotBlank()) existing.ffIgn else ffIgn.trim(),
                        ffUid = if (existing.ffUid.isNotBlank()) existing.ffUid else ffUid.trim()
                    )
                }

                // Determine Sign-Up Bonus (Fixed or Random) from App Settings
                var bonusAmount = 0
                var bonusModeUsed = "FIXED"
                try {
                    val settingsSnap = db.collection("app_settings").document("main").get().await()
                    val settings = settingsSnap.toSafeAppSettingsDto() ?: AppSettingsDto()
                    if (settings.signUpBonusEnabled) {
                        bonusModeUsed = settings.signUpBonusMode.uppercase()
                        bonusAmount = if (bonusModeUsed == "RANDOM") {
                            val minB = minOf(settings.signUpBonusMin, settings.signUpBonusMax).coerceAtLeast(0)
                            val maxB = maxOf(settings.signUpBonusMin, settings.signUpBonusMax).coerceAtLeast(minB)
                            if (maxB > minB) (minB..maxB).random() else minB
                        } else {
                            settings.signUpBonusFixed.coerceAtLeast(0)
                        }
                    }
                } catch (_: Exception) {
                    bonusAmount = 0
                }

                val newUser = UserDto(
                    id = userId,
                    username = resolvedUsername,
                    usernameLower = usernameLower,
                    email = email,
                    passwordHash = passwordHash,
                    ffUid = ffUid.trim(),
                    ffIgn = ffIgn.trim(),
                    walletBalance = bonusAmount,
                    winningBalance = 0,
                    totalWithdrawn = 0,
                    isAdmin = isRealAdminEmail,
                    isSuspended = false,
                    matchesPlayed = 0,
                    totalKills = 0,
                    totalWinnings = 0,
                    bonusClaimed = true,
                    signupTimestamp = nowTs
                )
                userDocRef.set(newUser).await()

                // Record Sign-Up Bonus transaction and welcome notification if bonus > 0
                if (bonusAmount > 0) {
                    try {
                        val txnId = "BONUS-" + UUID.randomUUID().toString().take(8).uppercase()
                        val bonusTxn = TransactionDto(
                            id = txnId,
                            userId = userId,
                            type = "SIGNUP_BONUS",
                            description = if (bonusModeUsed == "RANDOM") {
                                "Mystery Sign-Up Bonus Credited 🎁"
                            } else {
                                "Welcome Sign-Up Bonus Credited 🎁"
                            },
                            amount = bonusAmount,
                            balanceAfter = bonusAmount,
                            referenceId = txnId,
                            timestamp = nowTs
                        )
                        userDocRef.collection("transactions").document(txnId).set(bonusTxn).await()

                        val notifId = "NOTIF-BONUS-" + userId.take(8)
                        val welcomeNotif = NotificationDto(
                            id = notifId,
                            title = if (bonusModeUsed == "RANDOM") "🎁 Mystery Sign-Up Bonus Unlocked!" else "🎁 Welcome Sign-Up Bonus!",
                            message = "Welcome $resolvedUsername! ₹$bonusAmount Sign-Up Bonus has been credited to your wallet.",
                            type = "WALLET",
                            userId = userId,
                            timestamp = nowTs
                        )
                        db.collection("notifications").document(notifId).set(welcomeNotif).await()
                    } catch (_: Exception) {
                        // Non-fatal if bonus log fails
                    }
                }

                newUser
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, "users/$userId")
                UserDto(
                    id = userId,
                    email = email,
                    username = displayName.ifBlank { email.substringBefore("@").ifBlank { "Player" } },
                    isAdmin = isRealAdminEmail,
                    walletBalance = 0,
                    winningBalance = 0,
                    totalWithdrawn = 0,
                    bonusClaimed = true,
                    signupTimestamp = nowTs
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // Real-Time Customer Support Chat System (Text + Photo Support)
    // -------------------------------------------------------------------------

    fun observeSupportThread(userId: String): Flow<SupportThreadDto?> {
        if (userId.isBlank() || userId.startsWith("demo_")) return flowOf(null)
        return db.collection("support_chats").document(userId)
            .snapshots()
            .map { doc ->
                doc.toSupportThreadDto()
            }
            .catch { error ->
                if (error is Exception && isSessionActive()) {
                    handleFirestoreError(error, OperationType.GET, "support_chats/$userId")
                }
                emit(null)
            }
    }

    fun observeAllSupportThreads(): Flow<List<SupportThreadDto>> {
        return db.collection("support_chats")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toSupportThreadDto()
                }.sortedWith(
                    compareByDescending<SupportThreadDto> { it.unreadByAdmin > 0 }
                        .thenByDescending { it.lastMessageTimestamp }
                )
            }
            .catch { error ->
                if (error is Exception && isSessionActive()) {
                    handleFirestoreError(error, OperationType.LIST, "support_chats")
                }
                emit(emptyList())
            }
    }

    fun observeSupportMessages(threadUserId: String): Flow<List<SupportMessageDto>> {
        if (threadUserId.isBlank() || threadUserId.startsWith("demo_")) return flowOf(emptyList())
        return db.collection("support_chats").document(threadUserId).collection("messages")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toSupportMessageDto()
                }.sortedBy { it.timestamp }
            }
            .catch { error ->
                if (error is Exception && isSessionActive()) {
                    handleFirestoreError(error, OperationType.LIST, "support_chats/$threadUserId/messages")
                }
                emit(emptyList())
            }
    }

    suspend fun sendSupportMessage(
        threadUserId: String,
        userName: String,
        userEmail: String = "",
        userFfIgn: String = "",
        userFfUid: String = "",
        senderId: String,
        senderName: String,
        isAdminSender: Boolean,
        messageText: String,
        imageBase64: String? = null
    ): Result<SupportMessageDto> = withContext(Dispatchers.IO) {
        val cleanThreadUserId = threadUserId.trim()
        val cleanSenderId = senderId.trim()
        val cleanText = messageText.trim()
        val cleanImage = imageBase64?.takeIf { it.isNotBlank() }

        if (cleanThreadUserId.isBlank() || cleanSenderId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Authentication required to send message"))
        }
        if (cleanText.isBlank() && cleanImage == null) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a message or attach a photo"))
        }

        val now = System.currentTimeMillis()
        val msgId = "MSG-" + UUID.randomUUID().toString().replace("-", "").take(12).uppercase()
        val threadDocRef = db.collection("support_chats").document(cleanThreadUserId)
        val msgDocRef = threadDocRef.collection("messages").document(msgId)

        try {
            val existingSnap = try {
                threadDocRef.get().await()
            } catch (_: Exception) {
                null
            }

            val resolvedUserName = userName.trim().ifBlank {
                existingSnap?.getString("userName")?.takeIf { it.isNotBlank() } ?: "Player"
            }
            val resolvedEmail = userEmail.trim().ifBlank {
                existingSnap?.getString("userEmail") ?: ""
            }
            val resolvedIgn = userFfIgn.trim().ifBlank {
                existingSnap?.getString("userFfIgn") ?: ""
            }
            val resolvedUid = userFfUid.trim().ifBlank {
                existingSnap?.getString("userFfUid") ?: ""
            }

            val prevUnreadAdmin = existingSnap?.getLong("unreadByAdmin")?.toInt() ?: 0
            val prevUnreadUser = existingSnap?.getLong("unreadByUser")?.toInt() ?: 0

            val newUnreadAdmin = if (isAdminSender) 0 else (prevUnreadAdmin + 1)
            val newUnreadUser = if (isAdminSender) (prevUnreadUser + 1) else 0

            val previewText = when {
                cleanImage != null && cleanText.isBlank() -> "📷 Sent a photo"
                cleanImage != null -> "📷 $cleanText".take(200)
                else -> cleanText.take(200)
            }

            val threadPayload = mutableMapOf<String, Any>(
                "userId" to cleanThreadUserId,
                "userName" to resolvedUserName.take(100),
                "userEmail" to resolvedEmail.take(150),
                "userFfIgn" to resolvedIgn.take(60),
                "userFfUid" to resolvedUid.take(60),
                "lastMessage" to previewText,
                "lastSenderId" to cleanSenderId,
                "lastSenderIsAdmin" to isAdminSender,
                "lastMessageTimestamp" to now,
                "unreadByAdmin" to newUnreadAdmin,
                "unreadByUser" to newUnreadUser,
                "status" to "OPEN",
                "hasImage" to (cleanImage != null),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val msgPayload = mutableMapOf<String, Any>(
                "id" to msgId,
                "threadUserId" to cleanThreadUserId,
                "senderId" to cleanSenderId,
                "senderName" to senderName.trim().ifBlank { if (isAdminSender) "Admin Support" else resolvedUserName }.take(100),
                "isAdminSender" to isAdminSender,
                "messageText" to cleanText.take(2000),
                "timestamp" to now,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (cleanImage != null) {
                msgPayload["imageBase64"] = cleanImage
            }

            val batch = db.batch()
            batch.set(threadDocRef, threadPayload, SetOptions.merge())
            batch.set(msgDocRef, msgPayload)
            batch.commit().await()

            // When Admin replies, notify the user in real time
            if (isAdminSender) {
                try {
                    val notifId = "NOTIF-SUP-" + msgId.takeLast(8)
                    val notif = NotificationDto(
                        id = notifId,
                        title = "💬 Support Team Replied",
                        message = previewText,
                        type = "ANNOUNCEMENT",
                        userId = cleanThreadUserId,
                        timestamp = now
                    )
                    db.collection("notifications").document(notifId).set(notif).await()
                } catch (_: Exception) {}
            }

            Result.success(
                SupportMessageDto(
                    id = msgId,
                    threadUserId = cleanThreadUserId,
                    senderId = cleanSenderId,
                    senderName = senderName.trim().ifBlank { if (isAdminSender) "Admin Support" else resolvedUserName },
                    isAdminSender = isAdminSender,
                    messageText = cleanText,
                    imageBase64 = cleanImage,
                    timestamp = now
                )
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "support_chats/$cleanThreadUserId/messages/$msgId")
            Result.failure(e)
        }
    }

    suspend fun markSupportThreadRead(threadUserId: String, byAdmin: Boolean) = withContext(Dispatchers.IO) {
        if (threadUserId.isBlank()) return@withContext
        val threadDocRef = db.collection("support_chats").document(threadUserId)
        try {
            val snap = threadDocRef.get().await()
            if (!snap.exists()) return@withContext
            val fieldToClear = if (byAdmin) "unreadByAdmin" else "unreadByUser"
            val currentVal = snap.getLong(fieldToClear)?.toInt() ?: 0
            if (currentVal > 0) {
                threadDocRef.update(fieldToClear, 0).await()
            }
        } catch (_: Exception) {}
    }

    suspend fun updateSupportThreadStatus(threadUserId: String, status: String): Result<String> =
        withContext(Dispatchers.IO) {
            if (threadUserId.isBlank()) return@withContext Result.failure(IllegalArgumentException("User ID missing"))
            val threadDocRef = db.collection("support_chats").document(threadUserId)
            try {
                threadDocRef.update(
                    mapOf(
                        "status" to status,
                        "unreadByAdmin" to 0,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success("Support ticket marked as $status")
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, "support_chats/$threadUserId")
                Result.failure(e)
            }
        }

    // Delete a single user and all their associated history (transactions, participations, withdrawals, notifications, support chats)
    suspend fun deleteSingleUserAndHistory(userId: String): Result<String> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.failure(Exception("User ID is blank"))
        wipeMutex.withLock {
            try {
                val userDocRef = db.collection("users").document(userId)
                // 1. Delete user's transactions subcollection
                try {
                    val txns = userDocRef.collection("transactions").get().await()
                    for (doc in txns.documents) {
                        doc.reference.delete().await()
                    }
                } catch (_: Exception) {}

                // 2. Delete user's participations subcollection
                try {
                    val parts = userDocRef.collection("participations").get().await()
                    for (doc in parts.documents) {
                        doc.reference.delete().await()
                    }
                } catch (_: Exception) {}

                // 3. Delete user's withdrawal requests
                try {
                    val wdrs = db.collection("withdrawals").whereEqualTo("userId", userId).get().await()
                    for (doc in wdrs.documents) {
                        doc.reference.delete().await()
                    }
                } catch (_: Exception) {}

                // 4. Delete user's personal notifications
                try {
                    val notifs = db.collection("notifications").whereEqualTo("userId", userId).get().await()
                    for (doc in notifs.documents) {
                        doc.reference.delete().await()
                    }
                } catch (_: Exception) {}

                // 5. Delete user's support chat messages and thread
                try {
                    val supportRef = db.collection("support_chats").document(userId)
                    val msgs = supportRef.collection("messages").get().await()
                    for (doc in msgs.documents) {
                        doc.reference.delete().await()
                    }
                    supportRef.delete().await()
                } catch (_: Exception) {}

                // 6. Delete user document
                userDocRef.delete().await()
                Result.success("User and all associated history deleted.")
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, "users/$userId")
                Result.failure(e)
            }
        }
    }

    // Completely wipe all data: all users, their transactions & match participations, all tournaments & participants, all withdrawals, support chats, and all notifications
    suspend fun deleteAllDatabaseData(additionalUserIds: Set<String> = emptySet()): Result<String> = withContext(Dispatchers.IO) {
        wipeMutex.withLock {
            try {
                val allKnownUserIds = mutableSetOf<String>()
                allKnownUserIds.addAll(additionalUserIds.filter { it.isNotBlank() })
                auth.currentUser?.uid?.takeIf { it.isNotBlank() }?.let { allKnownUserIds.add(it) }
                allKnownUserIds.addAll(
                    listOf("demo_admin_user", "demo_player_user", "user_demo_1", "user_demo_2", "user_demo_3", "admin_master_1")
                )

                // Gather user IDs from users collection
                val userDocs = try {
                    db.collection("users").get().await().documents
                } catch (_: Exception) {
                    emptyList()
                }
                userDocs.forEach { allKnownUserIds.add(it.id) }

                // Gather user IDs from bonus_claims & device_bonus_claims (discovers previously deleted users who might still have subcollections)
                try {
                    val bonusDocs = db.collection("bonus_claims").get().await().documents
                    bonusDocs.forEach { doc ->
                        allKnownUserIds.add(doc.id)
                        doc.getString("userId")?.takeIf { it.isNotBlank() }?.let { allKnownUserIds.add(it) }
                    }
                } catch (_: Exception) {}

                try {
                    val deviceBonusDocs = db.collection("device_bonus_claims").get().await().documents
                    deviceBonusDocs.forEach { doc ->
                        doc.getString("userId")?.takeIf { it.isNotBlank() }?.let { allKnownUserIds.add(it) }
                    }
                } catch (_: Exception) {}

                // Gather user IDs from withdrawals and delete all withdrawals
                try {
                    val wdrDocs = db.collection("withdrawals").get().await().documents
                    for (doc in wdrDocs) {
                        doc.getString("userId")?.takeIf { it.isNotBlank() }?.let { allKnownUserIds.add(it) }
                        try { doc.reference.delete().await() } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}

                // Gather user IDs from support_chats and delete all support chats & messages
                try {
                    val supportDocs = db.collection("support_chats").get().await().documents
                    for (doc in supportDocs) {
                        allKnownUserIds.add(doc.id)
                        try {
                            val msgs = doc.reference.collection("messages").get().await().documents
                            for (m in msgs) {
                                try { m.reference.delete().await() } catch (_: Exception) {}
                            }
                        } catch (_: Exception) {}
                        try { doc.reference.delete().await() } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}

                // Gather user IDs from tournaments & participants, then delete all tournament participants and tournaments
                val allTourneyIds = mutableSetOf("FF-T100", "FF-T101", "FF-T102", "FF-T103")
                try {
                    val tourneyDocs = db.collection("tournaments").get().await().documents
                    tourneyDocs.forEach { allTourneyIds.add(it.id) }
                } catch (_: Exception) {}

                for (tid in allTourneyIds) {
                    if (tid.isBlank()) continue
                    val tourneyRef = db.collection("tournaments").document(tid)
                    try {
                        val partDocs = tourneyRef.collection("participants").get().await().documents
                        for (pDoc in partDocs) {
                            pDoc.getString("userId")?.takeIf { it.isNotBlank() }?.let { allKnownUserIds.add(it) }
                            try { pDoc.reference.delete().await() } catch (_: Exception) {}
                        }
                    } catch (_: Exception) {}
                    try { tourneyRef.delete().await() } catch (_: Exception) {}
                }

                // Delete all subcollections (transactions, participations, support messages) and user documents for every discovered user ID
                for (uid in allKnownUserIds) {
                    if (uid.isBlank()) continue
                    val uRef = db.collection("users").document(uid)
                    try {
                        val txnDocs = uRef.collection("transactions").get().await().documents
                        for (tDoc in txnDocs) {
                            try { tDoc.reference.delete().await() } catch (_: Exception) {}
                        }
                    } catch (_: Exception) {}

                    try {
                        val partDocs = uRef.collection("participations").get().await().documents
                        for (pDoc in partDocs) {
                            try { pDoc.reference.delete().await() } catch (_: Exception) {}
                        }
                    } catch (_: Exception) {}

                    try {
                        val sRef = db.collection("support_chats").document(uid)
                        val mDocs = sRef.collection("messages").get().await().documents
                        for (mDoc in mDocs) {
                            try { mDoc.reference.delete().await() } catch (_: Exception) {}
                        }
                        sRef.delete().await()
                    } catch (_: Exception) {}

                    try { uRef.delete().await() } catch (_: Exception) {}
                }

                // Delete all notifications
                try {
                    val notifDocs = db.collection("notifications").get().await().documents
                    for (doc in notifDocs) {
                        try { doc.reference.delete().await() } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}

                // Delete any top-level transactions or participants
                try {
                    val topTxns = db.collection("transactions").get().await().documents
                    for (doc in topTxns) {
                        try { doc.reference.delete().await() } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}

                try {
                    val topParts = db.collection("participants").get().await().documents
                    for (doc in topParts) {
                        try { doc.reference.delete().await() } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}

                // Reset app_settings to clean default
                try {
                    val settingsDocs = db.collection("app_settings").get().await().documents
                    for (doc in settingsDocs) {
                        try { doc.reference.delete().await() } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}

                val defaultSettings = AppSettingsDto(
                    id = "main",
                    appName = "eSports Arena",
                    announcementText = "Welcome to eSports Arena! Join official matches and win cash prizes.",
                    isMaintenance = false,
                    supportContact = "WhatsApp: +91 98765 43210 / Discord: @esportsarena",
                    minDeposit = 20,
                    platformFeePercent = 10
                )
                try {
                    db.collection("app_settings").document("main").set(defaultSettings).await()
                } catch (_: Exception) {}

                Result.success("All users, match history, transactions, withdrawals, tournaments, and notifications have been completely deleted!")
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, "all_collections")
                Result.failure(e)
            }
        }
    }

    // Demo data identification to guarantee only real database data is displayed
    private fun isDemoTournament(id: String, title: String): Boolean {
        val demoIds = setOf("FF-T101", "FF-T102", "FF-T103", "FF-T100", "DEMO-TOURNEY-101", "DEMO-TOURNEY-102", "DEMO-TOURNEY-103")
        return id in demoIds || id.startsWith("DEMO-", ignoreCase = true) || title.contains("Rush #101", ignoreCase = true)
    }

    private fun isDemoUser(id: String, email: String): Boolean {
        val demoIds = setOf("demo_admin_user", "demo_player_user", "user_demo_1", "user_demo_2", "user_demo_3", "admin_master_1", "demo_player_uid", "demo_admin_uid")
        return id in demoIds || id.startsWith("demo_", ignoreCase = true) || email.endsWith("@fftourney.com", ignoreCase = true) || email.endsWith(".demo", ignoreCase = true)
    }

    suspend fun cleanupDemoData() = withContext(Dispatchers.IO) {
        try {
            val demoTourneyIds = listOf(
                "FF-T101", "FF-T102", "FF-T103", "FF-T100",
                "DEMO-TOURNEY-101", "DEMO-TOURNEY-102", "DEMO-TOURNEY-103"
            )
            for (id in demoTourneyIds) {
                try { db.collection("tournaments").document(id).delete().await() } catch (_: Exception) {}
            }
            val demoUserIds = listOf(
                "demo_admin_user", "demo_player_user", "user_demo_1", "user_demo_2", "user_demo_3", "admin_master_1",
                "demo_player_uid", "demo_admin_uid"
            )
            for (id in demoUserIds) {
                try { db.collection("users").document(id).delete().await() } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            // Best effort cleanup
        }
    }

    // Initialize App Settings document if brand new (without seeding any fake tournaments)
    suspend fun initAppSettingsIfMissing() = withContext(Dispatchers.IO) {
        try {
            val settingsDocRef = db.collection("app_settings").document("main")
            val settingsDoc = settingsDocRef.get().await()
            if (!settingsDoc.exists()) {
                val settings = AppSettingsDto(
                    id = "main",
                    appName = "eSports Arena",
                    announcementText = "Welcome to eSports Arena! Join official matches and win cash prizes.",
                    isMaintenance = false,
                    supportContact = "WhatsApp: +91 98765 43210 / Discord: @esportsarena",
                    minDeposit = 20,
                    platformFeePercent = 10
                )
                settingsDocRef.set(settings).await()
            } else {
                val existingName = settingsDoc.getString("appName") ?: ""
                if (existingName.isBlank() || existingName == "FF Tournaments") {
                    val updates = mutableMapOf<String, Any>("appName" to "eSports Arena")
                    val existingAnnounce = settingsDoc.getString("announcementText") ?: ""
                    if (existingAnnounce.contains("FF Tournaments")) {
                        updates["announcementText"] = existingAnnounce.replace("FF Tournaments", "eSports Arena")
                    }
                    settingsDocRef.update(updates).await()
                }
            }
        } catch (e: Exception) {
            if (isSessionActive()) {
                handleFirestoreError(e, OperationType.WRITE, "app_settings/main")
            }
        }
    }
}

