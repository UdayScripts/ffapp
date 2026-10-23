package com.example.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class TournamentDto(
    val id: String = "",
    val title: String = "",
    val matchType: String = "SOLO", // SOLO, DUO, SQUAD
    val map: String = "Bermuda", // Bermuda, Purgatory, Kalahari, Alpine
    val entryFee: Int = 0,
    val prizePool: Int = 0,
    val perKillReward: Int = 0,
    val maxSlots: Int = 48,
    val joinedSlots: Int = 0,
    val scheduleTime: String = "",
    val timestamp: Long = 0L,
    val status: String = "UPCOMING", // UPCOMING, LIVE, COMPLETED, CANCELLED
    val roomId: String? = null,
    val roomPassword: String? = null,
    val roomNotice: String? = null,
    val firstPrize: Int = 0,
    val secondPrize: Int = 0,
    val thirdPrize: Int = 0,
    val rules: String = "1. Mobile devices only.\n2. Fair play strictly enforced.",
    val creatorId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ParticipantDto(
    val id: String = "",
    val tournamentId: String = "",
    val userId: String = "",
    val playerName: String = "",
    val ffUid: String = "",
    val ffIgn: String = "",
    val slotNumber: Int = 1,
    val kills: Int = 0,
    val placement: Int = 0,
    val prizeWon: Int = 0,
    val joinedAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class UserDto(
    val id: String = "",
    val username: String = "",
    val usernameLower: String = "",
    val email: String = "",
    val passwordHash: String = "",
    val ffUid: String = "",
    val ffIgn: String = "",
    val walletBalance: Int = 0,
    val winningBalance: Int? = null, // Withdrawable balance from match prizes (null on legacy docs)
    val totalWithdrawn: Int = 0,
    val isAdmin: Boolean = false,
    val isSuspended: Boolean = false,
    val matchesPlayed: Int = 0,
    val totalKills: Int = 0,
    val totalWinnings: Int = 0,
    val bonusClaimed: Boolean = false,
    val savedUpiId: String = "",
    val signupTimestamp: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    // Only winnings from matches can be withdrawn, constrained by current wallet balance
    val effectiveWinningBalance: Int
        get() {
            val fromLifetime = (totalWinnings - totalWithdrawn).coerceAtLeast(0)
            val availableWin = if (winningBalance != null) {
                if (totalWithdrawn > 0) minOf(winningBalance, fromLifetime) else winningBalance
            } else {
                fromLifetime
            }
            return availableWin.coerceAtLeast(0).coerceAtMost(walletBalance.coerceAtLeast(0))
        }
}

@IgnoreExtraProperties
data class TransactionDto(
    val id: String = "",
    val userId: String = "",
    val amount: Int = 0,
    val type: String = "DEPOSIT", // DEPOSIT, ENTRY_FEE, PRIZE_WIN, ADMIN_ADJUST
    val description: String = "",
    val referenceId: String = "",
    val status: String = "SUCCESS",
    val timestamp: Long = System.currentTimeMillis(),
    val balanceAfter: Int = 0
)

@IgnoreExtraProperties
data class NotificationDto(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "ANNOUNCEMENT", // ROOM_KEY, MATCH_UPDATE, WINNINGS, ANNOUNCEMENT
    val tournamentId: String? = null,
    val roomId: String? = null,
    val roomPassword: String? = null,
    val userId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val readByUsers: List<String> = emptyList(),
    val clearedByUsers: List<String> = emptyList()
)

@IgnoreExtraProperties
data class AppSettingsDto(
    val id: String = "main",
    val appName: String = "eSports Arena",
    val announcementText: String = "🔥 Grand Championship Live! Join Bermuda and Purgatory custom matches.",
    val isMaintenance: Boolean = false,
    val supportContact: String = "WhatsApp: +91 98765 43210 / Discord: @fftourneys",
    val minDeposit: Int = 20,
    val platformFeePercent: Int = 10,
    val rulesAndGuidelines: String = "1. Mobile devices only.\n2. Teaming up in Solo matches is banned.\n3. Room ID & Password released 15m prior to start.",
    val signUpBonusEnabled: Boolean = true,
    val signUpBonusMode: String = "FIXED", // "FIXED" or "RANDOM"
    val signUpBonusFixed: Int = 10,
    val signUpBonusMin: Int = 5,
    val signUpBonusMax: Int = 25,
    val latestAppVersion: String = "2.5.0",
    val updateReleaseNotes: String = "• Faster Free Fire Room ID & Password live sync\n• Instant UPI withdrawal status updates\n• Enhanced tournament lobby & UI performance",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WithdrawalRequestDto(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userFfIgn: String = "",
    val userFfUid: String = "",
    val amount: Int = 0,
    val upiId: String = "",
    val accountHolderName: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val adminNote: String = "",
    val adminRefId: String = "", // UTR or transaction ID from admin
    val timestamp: Long = System.currentTimeMillis(),
    val processedAt: Long = 0L
)

@IgnoreExtraProperties
data class SupportThreadDto(
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userFfIgn: String = "",
    val userFfUid: String = "",
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val lastSenderIsAdmin: Boolean = false,
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadByAdmin: Int = 0,
    val unreadByUser: Int = 0,
    val status: String = "OPEN", // OPEN, RESOLVED
    val hasImage: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SupportMessageDto(
    val id: String = "",
    val threadUserId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val isAdminSender: Boolean = false,
    val messageText: String = "",
    val imageBase64: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

fun DocumentSnapshot.toSafeUserDto(): UserDto? {
    if (!exists()) return null
    return try {
        val raw = data ?: return null
        fun str(key: String, fallback: String = ""): String =
            (raw[key] as? String)?.trim() ?: fallback
        fun intVal(key: String, fallback: Int = 0): Int =
            (raw[key] as? Number)?.toInt() ?: (raw[key] as? String)?.toIntOrNull() ?: fallback
        fun intOrNull(key: String): Int? =
            (raw[key] as? Number)?.toInt() ?: (raw[key] as? String)?.toIntOrNull()
        fun longVal(key: String, fallback: Long = 0L): Long =
            (raw[key] as? Number)?.toLong()
                ?: (raw[key] as? Timestamp)?.toDate()?.time
                ?: (raw[key] as? String)?.toLongOrNull()
                ?: fallback
        fun boolVal(key: String, fallback: Boolean = false): Boolean =
            (raw[key] as? Boolean) ?: fallback

        val resolvedUsername = str("username").ifBlank {
            str("displayName").ifBlank {
                str("name").ifBlank {
                    str("email").substringBefore("@").ifBlank { "Player" }
                }
            }
        }
        val resolvedIgn = str("ffIgn").ifBlank { str("inGameName") }
        val resolvedUid = str("ffUid").ifBlank { str("inGameId") }
        val resolvedWallet = if (raw.containsKey("walletBalance")) intVal("walletBalance", 0) else intVal("balance", 0)

        UserDto(
            id = id,
            username = resolvedUsername,
            usernameLower = str("usernameLower").ifBlank {
                resolvedUsername.lowercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
            },
            email = str("email"),
            passwordHash = str("passwordHash"),
            ffUid = resolvedUid,
            ffIgn = resolvedIgn,
            walletBalance = resolvedWallet,
            winningBalance = intOrNull("winningBalance"),
            totalWithdrawn = intVal("totalWithdrawn", 0),
            isAdmin = boolVal("isAdmin", false),
            isSuspended = boolVal("isSuspended", false),
            matchesPlayed = intVal("matchesPlayed", 0),
            totalKills = intVal("totalKills", 0),
            totalWinnings = intVal("totalWinnings", 0),
            bonusClaimed = boolVal("bonusClaimed", false),
            savedUpiId = str("savedUpiId"),
            signupTimestamp = longVal("signupTimestamp", 0L),
            createdAt = raw["createdAt"] as? Timestamp,
            updatedAt = raw["updatedAt"] as? Timestamp
        )
    } catch (_: Exception) {
        try {
            toObject(UserDto::class.java)?.copy(id = id)
        } catch (_: Exception) {
            null
        }
    }
}

fun DocumentSnapshot.toSafeTournamentDto(): TournamentDto? {
    if (!exists()) return null
    return try {
        toObject(TournamentDto::class.java)?.copy(id = id)
    } catch (_: Exception) {
        try {
            val raw = data ?: return null
            fun str(key: String, fallback: String = ""): String = (raw[key] as? String) ?: fallback
            fun strOrNull(key: String): String? = raw[key] as? String
            fun intVal(key: String, fallback: Int = 0): Int = (raw[key] as? Number)?.toInt() ?: fallback
            fun longVal(key: String, fallback: Long = 0L): Long =
                (raw[key] as? Number)?.toLong() ?: (raw[key] as? Timestamp)?.toDate()?.time ?: fallback
            TournamentDto(
                id = id,
                title = str("title"),
                matchType = str("matchType", "SOLO"),
                map = str("map", "Bermuda"),
                entryFee = intVal("entryFee", 0),
                prizePool = intVal("prizePool", 0),
                perKillReward = intVal("perKillReward", 0),
                maxSlots = intVal("maxSlots", 48),
                joinedSlots = intVal("joinedSlots", 0),
                scheduleTime = str("scheduleTime"),
                timestamp = longVal("timestamp", 0L),
                status = str("status", "UPCOMING"),
                roomId = strOrNull("roomId"),
                roomPassword = strOrNull("roomPassword"),
                roomNotice = strOrNull("roomNotice"),
                firstPrize = intVal("firstPrize", 0),
                secondPrize = intVal("secondPrize", 0),
                thirdPrize = intVal("thirdPrize", 0),
                rules = str("rules", "1. Mobile devices only.\n2. Fair play strictly enforced."),
                creatorId = str("creatorId"),
                createdAt = raw["createdAt"] as? Timestamp,
                updatedAt = raw["updatedAt"] as? Timestamp
            )
        } catch (_: Exception) {
            null
        }
    }
}

fun DocumentSnapshot.toSafeParticipantDto(): ParticipantDto? {
    if (!exists()) return null
    return try {
        toObject(ParticipantDto::class.java)?.copy(id = id)
    } catch (_: Exception) {
        try {
            val raw = data ?: return null
            fun str(key: String, fallback: String = ""): String = (raw[key] as? String) ?: fallback
            fun intVal(key: String, fallback: Int = 0): Int = (raw[key] as? Number)?.toInt() ?: fallback
            fun longVal(key: String, fallback: Long = 0L): Long =
                (raw[key] as? Number)?.toLong() ?: (raw[key] as? Timestamp)?.toDate()?.time ?: fallback
            ParticipantDto(
                id = id,
                tournamentId = str("tournamentId"),
                userId = str("userId"),
                playerName = str("playerName"),
                ffUid = str("ffUid"),
                ffIgn = str("ffIgn"),
                slotNumber = intVal("slotNumber", 1),
                kills = intVal("kills", 0),
                placement = intVal("placement", 0),
                prizeWon = intVal("prizeWon", 0),
                joinedAt = longVal("joinedAt", System.currentTimeMillis())
            )
        } catch (_: Exception) {
            null
        }
    }
}

fun DocumentSnapshot.toSafeTransactionDto(): TransactionDto? {
    if (!exists()) return null
    return try {
        toObject(TransactionDto::class.java)?.copy(id = id)
    } catch (_: Exception) {
        try {
            val raw = data ?: return null
            fun str(key: String, fallback: String = ""): String = (raw[key] as? String) ?: fallback
            fun intVal(key: String, fallback: Int = 0): Int = (raw[key] as? Number)?.toInt() ?: fallback
            fun longVal(key: String, fallback: Long = 0L): Long =
                (raw[key] as? Number)?.toLong() ?: (raw[key] as? Timestamp)?.toDate()?.time ?: fallback
            TransactionDto(
                id = id,
                userId = str("userId"),
                amount = intVal("amount", 0),
                type = str("type", "DEPOSIT"),
                description = str("description"),
                referenceId = str("referenceId"),
                status = str("status", "SUCCESS"),
                timestamp = longVal("timestamp", System.currentTimeMillis()),
                balanceAfter = intVal("balanceAfter", 0)
            )
        } catch (_: Exception) {
            null
        }
    }
}

fun DocumentSnapshot.toSafeNotificationDto(): NotificationDto? {
    if (!exists()) return null
    return try {
        toObject(NotificationDto::class.java)?.copy(id = id)
    } catch (_: Exception) {
        try {
            val raw = data ?: return null
            fun str(key: String, fallback: String = ""): String = (raw[key] as? String) ?: fallback
            fun strOrNull(key: String): String? = raw[key] as? String
            fun longVal(key: String, fallback: Long = 0L): Long =
                (raw[key] as? Number)?.toLong() ?: (raw[key] as? Timestamp)?.toDate()?.time ?: fallback
            val readBy = (raw["readByUsers"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val clearedBy = (raw["clearedByUsers"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            NotificationDto(
                id = id,
                title = str("title"),
                message = str("message"),
                type = str("type", "ANNOUNCEMENT"),
                tournamentId = strOrNull("tournamentId"),
                roomId = strOrNull("roomId"),
                roomPassword = strOrNull("roomPassword"),
                userId = strOrNull("userId"),
                timestamp = longVal("timestamp", System.currentTimeMillis()),
                isRead = (raw["isRead"] as? Boolean) ?: false,
                readByUsers = readBy,
                clearedByUsers = clearedBy
            )
        } catch (_: Exception) {
            null
        }
    }
}

fun DocumentSnapshot.toSafeAppSettingsDto(): AppSettingsDto? {
    if (!exists()) return null
    return try {
        toObject(AppSettingsDto::class.java)?.copy(id = id)
    } catch (_: Exception) {
        null
    }
}

fun DocumentSnapshot.toSafeWithdrawalRequestDto(): WithdrawalRequestDto? {
    if (!exists()) return null
    return try {
        toObject(WithdrawalRequestDto::class.java)?.copy(id = id)
    } catch (_: Exception) {
        try {
            val raw = data ?: return null
            fun str(key: String, fallback: String = ""): String = (raw[key] as? String) ?: fallback
            fun intVal(key: String, fallback: Int = 0): Int = (raw[key] as? Number)?.toInt() ?: fallback
            fun longVal(key: String, fallback: Long = 0L): Long =
                (raw[key] as? Number)?.toLong() ?: (raw[key] as? Timestamp)?.toDate()?.time ?: fallback
            WithdrawalRequestDto(
                id = id,
                userId = str("userId"),
                userName = str("userName"),
                userEmail = str("userEmail"),
                userFfIgn = str("userFfIgn"),
                userFfUid = str("userFfUid"),
                amount = intVal("amount", 0),
                upiId = str("upiId"),
                accountHolderName = str("accountHolderName"),
                status = str("status", "PENDING"),
                adminNote = str("adminNote"),
                adminRefId = str("adminRefId"),
                timestamp = longVal("timestamp", System.currentTimeMillis()),
                processedAt = longVal("processedAt", 0L)
            )
        } catch (_: Exception) {
            null
        }
    }
}

fun DocumentSnapshot.toSupportThreadDto(): SupportThreadDto? {
    if (!exists()) return null
    return SupportThreadDto(
        userId = getString("userId")?.takeIf { it.isNotBlank() } ?: id,
        userName = getString("userName") ?: "",
        userEmail = getString("userEmail") ?: "",
        userFfIgn = getString("userFfIgn") ?: "",
        userFfUid = getString("userFfUid") ?: "",
        lastMessage = getString("lastMessage") ?: "",
        lastSenderId = getString("lastSenderId") ?: "",
        lastSenderIsAdmin = getBoolean("lastSenderIsAdmin") ?: false,
        lastMessageTimestamp = getLong("lastMessageTimestamp") ?: 0L,
        unreadByAdmin = getLong("unreadByAdmin")?.toInt() ?: 0,
        unreadByUser = getLong("unreadByUser")?.toInt() ?: 0,
        status = getString("status") ?: "OPEN",
        hasImage = getBoolean("hasImage") ?: false,
        updatedAt = getTimestamp("updatedAt")
    )
}

fun DocumentSnapshot.toSupportMessageDto(): SupportMessageDto? {
    if (!exists()) return null
    return SupportMessageDto(
        id = getString("id")?.takeIf { it.isNotBlank() } ?: id,
        threadUserId = getString("threadUserId") ?: "",
        senderId = getString("senderId") ?: "",
        senderName = getString("senderName") ?: "",
        isAdminSender = getBoolean("isAdminSender") ?: false,
        messageText = getString("messageText") ?: "",
        imageBase64 = getString("imageBase64")?.takeIf { it.isNotBlank() },
        timestamp = getLong("timestamp") ?: 0L,
        createdAt = getTimestamp("createdAt")
    )
}
