package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tournaments")
data class TournamentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val matchType: String, // "SOLO", "DUO", "SQUAD"
    val map: String, // "Bermuda", "Purgatory", "Kalahari", "Alpine"
    val entryFee: Int,
    val prizePool: Int,
    val perKillReward: Int,
    val maxSlots: Int,
    val joinedSlots: Int,
    val scheduleTime: String,
    val timestamp: Long,
    val status: String, // "UPCOMING", "LIVE", "COMPLETED", "CANCELLED"
    val roomId: String? = null,
    val roomPassword: String? = null,
    val roomNotice: String? = null,
    val firstPrize: Int = 0,
    val secondPrize: Int = 0,
    val thirdPrize: Int = 0,
    val rules: String = "1. Emulators strictly forbidden.\n2. Arrive in room 10 minutes prior.\n3. Fair play only."
)

@Entity(tableName = "participants")
data class ParticipantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tournamentId: String,
    val userId: String,
    val playerName: String,
    val ffUid: String,
    val ffIgn: String,
    val slotNumber: Int,
    val kills: Int = 0,
    val placement: Int = 0,
    val prizeWon: Int = 0,
    val joinedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val ffUid: String,
    val ffIgn: String,
    val walletBalance: Int,
    val isAdmin: Boolean = false,
    val isSuspended: Boolean = false,
    val matchesPlayed: Int = 0,
    val totalKills: Int = 0,
    val totalWinnings: Int = 0,
    val passwordHash: String = "admin123"
)

@Entity(tableName = "transactions")
data class DepositTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val amount: Int,
    val type: String, // "DEPOSIT", "ENTRY_FEE", "PRIZE_WIN", "REFUND"
    val description: String,
    val referenceId: String,
    val status: String, // "SUCCESS", "PENDING", "FAILED"
    val timestamp: Long,
    val balanceAfter: Int
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val type: String, // "ROOM_KEY", "MATCH_UPDATE", "WINNINGS", "ANNOUNCEMENT"
    val tournamentId: String? = null,
    val roomId: String? = null,
    val roomPassword: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val appName: String = "FF Tournaments",
    val announcementText: String = "🔥 Season 12 Mega Battle Royale: Bermuda Squad starts tonight at 9:00 PM! Room keys dropping soon.",
    val isMaintenance: Boolean = false,
    val supportContact: String = "WhatsApp: +91 98765 43210 / Discord: @fftourneys",
    val minDeposit: Int = 20,
    val platformFeePercent: Int = 10,
    val rulesAndGuidelines: String = "1. Mobile devices only; emulators lead to immediate disqualification.\n2. Teaming up in Solo matches results in an account suspension.\n3. Room ID & Password released 15 min before match starts.\n4. Take screenshot of match scoreboard for verification."
)
