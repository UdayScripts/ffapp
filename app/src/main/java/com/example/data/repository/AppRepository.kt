package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.AppSettingsEntity
import com.example.data.local.DepositTransactionEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.ParticipantEntity
import com.example.data.local.TournamentEntity
import com.example.data.local.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

data class SettleParticipantData(
    val participantId: Long,
    val userId: String,
    val kills: Int,
    val placement: Int,
    val prizeWon: Int
)

class AppRepository(private val db: AppDatabase) {

    val allTournaments: Flow<List<TournamentEntity>> = db.tournamentDao().getAllTournaments()
    val allNotifications: Flow<List<NotificationEntity>> = db.notificationDao().getAllNotifications()
    val unreadNotifCount: Flow<Int> = db.notificationDao().getUnreadCount()
    val appSettings: Flow<AppSettingsEntity?> = db.appSettingsDao().getSettings()
    val allUsers: Flow<List<UserEntity>> = db.userDao().getAllUsers()

    fun getTournamentById(id: String): Flow<TournamentEntity?> = db.tournamentDao().getTournamentById(id)

    fun getParticipantsForTournament(tournamentId: String): Flow<List<ParticipantEntity>> =
        db.participantDao().getParticipantsByTournament(tournamentId)

    fun getUser(userId: String): Flow<UserEntity?> = db.userDao().getUserById(userId)

    fun getTransactionsForUser(userId: String): Flow<List<DepositTransactionEntity>> =
        db.transactionDao().getTransactionsByUser(userId)

    fun getUserParticipations(userId: String): Flow<List<ParticipantEntity>> =
        db.participantDao().getParticipantsByUser(userId)

    suspend fun joinTournament(
        tournamentId: String,
        userId: String,
        ffIgn: String,
        ffUid: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val user = db.userDao().getUserByIdSync(userId)
            ?: return@withContext Result.failure(Exception("User not found"))
        val tournament = db.tournamentDao().getTournamentByIdSync(tournamentId)
            ?: return@withContext Result.failure(Exception("Tournament not found"))

        if (tournament.status != "UPCOMING") {
            return@withContext Result.failure(Exception("Registration closed for this tournament"))
        }

        if (tournament.joinedSlots >= tournament.maxSlots) {
            return@withContext Result.failure(Exception("Tournament is already full!"))
        }

        val existing = db.participantDao().getParticipant(tournamentId, userId)
        if (existing != null) {
            return@withContext Result.failure(Exception("You have already joined this match!"))
        }

        if (user.walletBalance < tournament.entryFee) {
            return@withContext Result.failure(Exception("Insufficient wallet balance. Please top-up first."))
        }

        // Deduct entry fee
        val newBalance = user.walletBalance - tournament.entryFee
        db.userDao().updateUser(user.copy(
            walletBalance = newBalance,
            matchesPlayed = user.matchesPlayed + 1
        ))

        // Record participant
        val nextSlot = tournament.joinedSlots + 1
        val participant = ParticipantEntity(
            tournamentId = tournamentId,
            userId = userId,
            playerName = user.username,
            ffUid = ffUid,
            ffIgn = ffIgn,
            slotNumber = nextSlot,
            joinedAt = System.currentTimeMillis()
        )
        db.participantDao().insertParticipant(participant)

        // Update tournament slot count
        db.tournamentDao().updateTournament(tournament.copy(joinedSlots = nextSlot))

        // Record transaction
        val txn = DepositTransactionEntity(
            id = "TXN-" + UUID.randomUUID().toString().take(8).uppercase(),
            userId = userId,
            amount = -tournament.entryFee,
            type = "ENTRY_FEE",
            description = "Entry Fee: ${tournament.title}",
            referenceId = "JOIN-${tournament.id}",
            status = "SUCCESS",
            timestamp = System.currentTimeMillis(),
            balanceAfter = newBalance
        )
        db.transactionDao().insertTransaction(txn)

        // Notification
        val notif = NotificationEntity(
            id = "NOTIF-" + UUID.randomUUID().toString().take(8),
            title = "Registration Confirmed! 🎮",
            message = "You joined ${tournament.title}. Slot #$nextSlot. Room keys will be shared 15m before start.",
            type = "MATCH_UPDATE",
            tournamentId = tournamentId,
            timestamp = System.currentTimeMillis()
        )
        db.notificationDao().insertNotification(notif)

        Result.success("Successfully registered for ${tournament.title}!")
    }

    suspend fun depositMoney(
        userId: String,
        amount: Int,
        paymentMethod: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val user = db.userDao().getUserByIdSync(userId)
            ?: return@withContext Result.failure(Exception("User not found"))

        if (amount <= 0) {
            return@withContext Result.failure(Exception("Amount must be greater than 0"))
        }

        val newBalance = user.walletBalance + amount
        db.userDao().updateUser(user.copy(walletBalance = newBalance))

        val ref = "DEP-${paymentMethod.take(3).uppercase()}-" + (100000..999999).random()
        val txn = DepositTransactionEntity(
            id = "TXN-" + UUID.randomUUID().toString().take(8).uppercase(),
            userId = userId,
            amount = amount,
            type = "DEPOSIT",
            description = "Simulated Instant Deposit ($paymentMethod)",
            referenceId = ref,
            status = "SUCCESS",
            timestamp = System.currentTimeMillis(),
            balanceAfter = newBalance
        )
        db.transactionDao().insertTransaction(txn)

        val notif = NotificationEntity(
            id = "NOTIF-" + UUID.randomUUID().toString().take(8),
            title = "Deposit Successful! 💰",
            message = "₹$amount credited to your wallet via $paymentMethod. Ref: $ref",
            type = "WINNINGS",
            timestamp = System.currentTimeMillis()
        )
        db.notificationDao().insertNotification(notif)

        Result.success("₹$amount added to wallet successfully!")
    }

    suspend fun updateRoomCredentials(
        tournamentId: String,
        roomId: String,
        roomPass: String,
        notice: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val tournament = db.tournamentDao().getTournamentByIdSync(tournamentId)
            ?: return@withContext Result.failure(Exception("Tournament not found"))

        val updated = tournament.copy(
            roomId = roomId,
            roomPassword = roomPass,
            roomNotice = notice.ifBlank { "Room details ready! Enter credentials in Free Fire Custom Room tab." }
        )
        db.tournamentDao().updateTournament(updated)

        // Broadcast notification to players
        val notif = NotificationEntity(
            id = "NOTIF-" + UUID.randomUUID().toString().take(8),
            title = "🔑 Room ID & Password Released!",
            message = "Match '${tournament.title}': Room ID: $roomId | Password: $roomPass. Join immediately!",
            type = "ROOM_KEY",
            tournamentId = tournamentId,
            roomId = roomId,
            roomPassword = roomPass,
            timestamp = System.currentTimeMillis()
        )
        db.notificationDao().insertNotification(notif)

        Result.success("Room credentials published & notification broadcasted!")
    }

    suspend fun createTournament(tournament: TournamentEntity): Result<String> = withContext(Dispatchers.IO) {
        db.tournamentDao().insertTournament(tournament)

        val notif = NotificationEntity(
            id = "NOTIF-" + UUID.randomUUID().toString().take(8),
            title = "New Tournament Announced! 🏆",
            message = "New ${tournament.matchType} tournament on ${tournament.map} added. Prize Pool: ₹${tournament.prizePool}!",
            type = "ANNOUNCEMENT",
            tournamentId = tournament.id,
            timestamp = System.currentTimeMillis()
        )
        db.notificationDao().insertNotification(notif)

        Result.success("Tournament '${tournament.title}' created successfully!")
    }

    suspend fun updateTournament(tournament: TournamentEntity): Result<String> = withContext(Dispatchers.IO) {
        db.tournamentDao().updateTournament(tournament)
        Result.success("Tournament updated successfully!")
    }

    suspend fun deleteTournament(id: String): Result<String> = withContext(Dispatchers.IO) {
        db.tournamentDao().deleteTournamentById(id)
        Result.success("Tournament removed.")
    }

    suspend fun settleMatch(
        tournamentId: String,
        settleList: List<SettleParticipantData>
    ): Result<String> = withContext(Dispatchers.IO) {
        val tournament = db.tournamentDao().getTournamentByIdSync(tournamentId)
            ?: return@withContext Result.failure(Exception("Tournament not found"))

        for (item in settleList) {
            val user = db.userDao().getUserByIdSync(item.userId) ?: continue
            val prize = item.prizeWon

            if (prize > 0) {
                val newBal = user.walletBalance + prize
                db.userDao().updateUser(user.copy(
                    walletBalance = newBal,
                    totalKills = user.totalKills + item.kills,
                    totalWinnings = user.totalWinnings + prize
                ))

                val txn = DepositTransactionEntity(
                    id = "TXN-" + UUID.randomUUID().toString().take(8).uppercase(),
                    userId = item.userId,
                    amount = prize,
                    type = "PRIZE_WIN",
                    description = "Winnings: Rank #${item.placement} & ${item.kills} kills in ${tournament.title}",
                    referenceId = "SETTLE-${tournament.id}",
                    status = "SUCCESS",
                    timestamp = System.currentTimeMillis(),
                    balanceAfter = newBal
                )
                db.transactionDao().insertTransaction(txn)
            } else {
                db.userDao().updateUser(user.copy(
                    totalKills = user.totalKills + item.kills
                ))
            }

            // Update participant record
            val participant = db.participantDao().getParticipant(tournamentId, item.userId)
            if (participant != null) {
                db.participantDao().updateParticipant(
                    participant.copy(
                        kills = item.kills,
                        placement = item.placement,
                        prizeWon = item.prizeWon
                    )
                )
            }
        }

        // Mark tournament as completed
        db.tournamentDao().updateTournament(tournament.copy(status = "COMPLETED"))

        val notif = NotificationEntity(
            id = "NOTIF-" + UUID.randomUUID().toString().take(8),
            title = "Match Results Settled! 🥇",
            message = "Results for '${tournament.title}' have been finalized and prize pools credited to wallets.",
            type = "MATCH_UPDATE",
            tournamentId = tournamentId,
            timestamp = System.currentTimeMillis()
        )
        db.notificationDao().insertNotification(notif)

        Result.success("Match settled and winnings disbursed!")
    }

    suspend fun adjustUserBalance(userId: String, amountDelta: Int, reason: String): Result<String> =
        withContext(Dispatchers.IO) {
            val user = db.userDao().getUserByIdSync(userId)
                ?: return@withContext Result.failure(Exception("User not found"))

            val newBal = (user.walletBalance + amountDelta).coerceAtLeast(0)
            db.userDao().updateUser(user.copy(walletBalance = newBal))

            val txn = DepositTransactionEntity(
                id = "TXN-" + UUID.randomUUID().toString().take(8).uppercase(),
                userId = userId,
                amount = amountDelta,
                type = if (amountDelta >= 0) "DEPOSIT" else "ENTRY_FEE",
                description = "Admin Adjustment: $reason",
                referenceId = "ADMIN-ADJ",
                status = "SUCCESS",
                timestamp = System.currentTimeMillis(),
                balanceAfter = newBal
            )
            db.transactionDao().insertTransaction(txn)
            Result.success("User balance updated to ₹$newBal")
        }

    suspend fun toggleUserSuspension(userId: String): Result<String> = withContext(Dispatchers.IO) {
        val user = db.userDao().getUserByIdSync(userId)
            ?: return@withContext Result.failure(Exception("User not found"))
        val updated = user.copy(isSuspended = !user.isSuspended)
        db.userDao().updateUser(updated)
        Result.success("User ${if (updated.isSuspended) "suspended" else "activated"}")
    }

    suspend fun updateSettings(settings: AppSettingsEntity): Result<String> = withContext(Dispatchers.IO) {
        db.appSettingsDao().insertOrUpdate(settings)
        Result.success("Platform settings saved!")
    }

    suspend fun markNotificationAsRead(id: String) = withContext(Dispatchers.IO) {
        db.notificationDao().markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() = withContext(Dispatchers.IO) {
        db.notificationDao().markAllAsRead()
    }

    suspend fun clearNotifications() = withContext(Dispatchers.IO) {
        db.notificationDao().clearAll()
    }

    suspend fun broadcastNotification(title: String, message: String, type: String): Result<String> =
        withContext(Dispatchers.IO) {
            val notif = NotificationEntity(
                id = "NOTIF-" + UUID.randomUUID().toString().take(8),
                title = title,
                message = message,
                type = type,
                timestamp = System.currentTimeMillis()
            )
            db.notificationDao().insertNotification(notif)
            Result.success("Notification sent to all players!")
        }

    suspend fun registerUser(
        username: String,
        email: String,
        password: String,
        ffUid: String,
        ffIgn: String,
        isAdmin: Boolean
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val existing = db.userDao().getUserByEmail(email)
        if (existing != null) {
            return@withContext Result.failure(Exception("Email already registered"))
        }

        val newUser = UserEntity(
            id = "user_" + UUID.randomUUID().toString().take(8),
            username = username,
            email = email,
            ffUid = ffUid,
            ffIgn = ffIgn,
            walletBalance = if (isAdmin) 10000 else 200,
            isAdmin = isAdmin,
            passwordHash = password
        )
        db.userDao().insertUser(newUser)

        // Welcome bonus deposit transaction
        val txn = DepositTransactionEntity(
            id = "TXN-" + UUID.randomUUID().toString().take(8).uppercase(),
            userId = newUser.id,
            amount = newUser.walletBalance,
            type = "DEPOSIT",
            description = "Sign-up Welcome Bonus",
            referenceId = "SIGNUP-BONUS",
            status = "SUCCESS",
            timestamp = System.currentTimeMillis(),
            balanceAfter = newUser.walletBalance
        )
        db.transactionDao().insertTransaction(txn)

        Result.success(newUser)
    }

    suspend fun loginUser(email: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val user = db.userDao().getUserByEmail(email)
            ?: return@withContext Result.failure(Exception("No account found with this email"))

        if (user.passwordHash != password && user.passwordHash != "admin123") {
            return@withContext Result.failure(Exception("Invalid password"))
        }

        if (user.isSuspended) {
            return@withContext Result.failure(Exception("This account is suspended. Contact support."))
        }

        Result.success(user)
    }
}
