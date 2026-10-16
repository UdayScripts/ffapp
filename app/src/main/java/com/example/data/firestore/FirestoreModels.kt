package com.example.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class TournamentDto(
    @DocumentId val id: String = "",
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
    @DocumentId val id: String = "",
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
    @DocumentId val id: String = "",
    val username: String = "",
    val email: String = "",
    val ffUid: String = "",
    val ffIgn: String = "",
    val walletBalance: Int = 200,
    val isAdmin: Boolean = false,
    val isSuspended: Boolean = false,
    val matchesPlayed: Int = 0,
    val totalKills: Int = 0,
    val totalWinnings: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TransactionDto(
    @DocumentId val id: String = "",
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
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "ANNOUNCEMENT", // ROOM_KEY, MATCH_UPDATE, WINNINGS, ANNOUNCEMENT
    val tournamentId: String? = null,
    val roomId: String? = null,
    val roomPassword: String? = null,
    val userId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@IgnoreExtraProperties
data class AppSettingsDto(
    @DocumentId val id: String = "main",
    val appName: String = "FF Tournaments",
    val announcementText: String = "🔥 Grand Championship Live! Join Bermuda and Purgatory custom matches.",
    val isMaintenance: Boolean = false,
    val supportContact: String = "WhatsApp: +91 98765 43210 / Discord: @fftourneys",
    val minDeposit: Int = 20,
    val platformFeePercent: Int = 10,
    val rulesAndGuidelines: String = "1. Mobile devices only.\n2. Teaming up in Solo matches is banned.\n3. Room ID & Password released 15m prior to start.",
    @ServerTimestamp val updatedAt: Timestamp? = null
)
