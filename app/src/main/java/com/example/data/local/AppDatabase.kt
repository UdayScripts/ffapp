package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TournamentEntity::class,
        ParticipantEntity::class,
        UserEntity::class,
        DepositTransactionEntity::class,
        NotificationEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tournamentDao(): TournamentDao
    abstract fun participantDao(): ParticipantDao
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun notificationDao(): NotificationDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ff_tournaments_database"
                )
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            val now = System.currentTimeMillis()

            // 1. Initial Users
            val demoUser = UserEntity(
                id = "user_demo_1",
                username = "ApexShadow",
                email = "player@fftourney.com",
                ffUid = "2948103982",
                ffIgn = "Apex⚡Shadow",
                walletBalance = 350,
                isAdmin = false,
                matchesPlayed = 8,
                totalKills = 24,
                totalWinnings = 850
            )
            val adminUser = UserEntity(
                id = "admin_master_1",
                username = "FF_Admin",
                email = "admin@fftourney.com",
                ffUid = "1000000001",
                ffIgn = "Tourney★Admin",
                walletBalance = 15000,
                isAdmin = true,
                matchesPlayed = 0,
                totalKills = 0,
                totalWinnings = 0,
                passwordHash = "admin123"
            )
            val user2 = UserEntity(
                id = "user_demo_2",
                username = "ViperSniper",
                email = "viper@fftourney.com",
                ffUid = "5920194832",
                ffIgn = "꧁Viper꧂",
                walletBalance = 120,
                isAdmin = false,
                matchesPlayed = 14,
                totalKills = 42,
                totalWinnings = 1200
            )
            val user3 = UserEntity(
                id = "user_demo_3",
                username = "GhostRaider",
                email = "ghost@fftourney.com",
                ffUid = "8492018471",
                ffIgn = "亗 GHOST 亗",
                walletBalance = 50,
                isAdmin = false,
                matchesPlayed = 5,
                totalKills = 9,
                totalWinnings = 200
            )
            database.userDao().insertUsers(listOf(demoUser, adminUser, user2, user3))

            // 2. Initial Tournaments
            val t1 = TournamentEntity(
                id = "FF-T101",
                title = "Bermuda Solo Rush #101",
                matchType = "SOLO",
                map = "Bermuda",
                entryFee = 30,
                prizePool = 1200,
                perKillReward = 10,
                maxSlots = 48,
                joinedSlots = 42,
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
            val t2 = TournamentEntity(
                id = "FF-T102",
                title = "Purgatory Duo Showdown #42",
                matchType = "DUO",
                map = "Purgatory",
                entryFee = 60,
                prizePool = 2400,
                perKillReward = 20,
                maxSlots = 24,
                joinedSlots = 20,
                scheduleTime = "Today, 09:30 PM",
                timestamp = now + 7200000,
                status = "UPCOMING",
                roomId = null,
                roomPassword = null,
                roomNotice = null,
                firstPrize = 1100,
                secondPrize = 600,
                thirdPrize = 300
            )
            val t3 = TournamentEntity(
                id = "FF-T103",
                title = "Kalahari Squad Havoc #18",
                matchType = "SQUAD",
                map = "Kalahari",
                entryFee = 100,
                prizePool = 4500,
                perKillReward = 35,
                maxSlots = 12,
                joinedSlots = 11,
                scheduleTime = "Tomorrow, 07:00 PM",
                timestamp = now + 86400000,
                status = "UPCOMING",
                roomId = null,
                roomPassword = null,
                firstPrize = 2200,
                secondPrize = 1200,
                thirdPrize = 600
            )
            val t4 = TournamentEntity(
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
                roomNotice = "Match concluded successfully.",
                firstPrize = 700,
                secondPrize = 400,
                thirdPrize = 200
            )
            val t5 = TournamentEntity(
                id = "FF-T104",
                title = "Bermuda Clash Solo Elite",
                matchType = "SOLO",
                map = "Bermuda",
                entryFee = 50,
                prizePool = 2000,
                perKillReward = 20,
                maxSlots = 48,
                joinedSlots = 48,
                scheduleTime = "Now Playing",
                timestamp = now - 600000,
                status = "LIVE",
                roomId = "654321",
                roomPassword = "555",
                roomNotice = "Match in progress! Live streaming on official YouTube channel.",
                firstPrize = 900,
                secondPrize = 500,
                thirdPrize = 250
            )
            database.tournamentDao().insertTournaments(listOf(t1, t2, t3, t4, t5))

            // 3. Initial Participants for Demo User & Completed Match
            val p1 = ParticipantEntity(
                tournamentId = "FF-T101",
                userId = "user_demo_1",
                playerName = "ApexShadow",
                ffUid = "2948103982",
                ffIgn = "Apex⚡Shadow",
                slotNumber = 7,
                joinedAt = now - 1800000
            )
            val p2 = ParticipantEntity(
                tournamentId = "FF-T100",
                userId = "user_demo_1",
                playerName = "ApexShadow",
                ffUid = "2948103982",
                ffIgn = "Apex⚡Shadow",
                slotNumber = 12,
                kills = 5,
                placement = 1,
                prizeWon = 775, // 700 1st place + 5*15 kill bonus
                joinedAt = now - 90000000
            )
            val p3 = ParticipantEntity(
                tournamentId = "FF-T100",
                userId = "user_demo_2",
                playerName = "ViperSniper",
                ffUid = "5920194832",
                ffIgn = "꧁Viper꧂",
                slotNumber = 14,
                kills = 4,
                placement = 2,
                prizeWon = 460,
                joinedAt = now - 90000000
            )
            database.participantDao().insertParticipants(listOf(p1, p2, p3))

            // 4. Initial Wallet Transactions
            val tx1 = DepositTransactionEntity(
                id = "TXN-1001",
                userId = "user_demo_1",
                amount = 200,
                type = "DEPOSIT",
                description = "Instant Wallet Top-Up (Demo UPI)",
                referenceId = "UPI928374619",
                status = "SUCCESS",
                timestamp = now - 172800000,
                balanceAfter = 200
            )
            val tx2 = DepositTransactionEntity(
                id = "TXN-1002",
                userId = "user_demo_1",
                amount = -40,
                type = "ENTRY_FEE",
                description = "Entry Fee: Alpine Solo Cup #FF-T100",
                referenceId = "MATCH-FF-T100",
                status = "SUCCESS",
                timestamp = now - 95000000,
                balanceAfter = 160
            )
            val tx3 = DepositTransactionEntity(
                id = "TXN-1003",
                userId = "user_demo_1",
                amount = 775,
                type = "PRIZE_WIN",
                description = "1st Place Winner + 5 Kills (Alpine Solo)",
                referenceId = "WIN-FF-T100",
                status = "SUCCESS",
                timestamp = now - 82000000,
                balanceAfter = 935
            )
            val tx4 = DepositTransactionEntity(
                id = "TXN-1004",
                userId = "user_demo_1",
                amount = -30,
                type = "ENTRY_FEE",
                description = "Entry Fee: Bermuda Solo Rush #FF-T101",
                referenceId = "MATCH-FF-T101",
                status = "SUCCESS",
                timestamp = now - 1800000,
                balanceAfter = 350
            )
            val tx5 = DepositTransactionEntity(
                id = "TXN-1005",
                userId = "user_demo_1",
                amount = 500,
                type = "DEPOSIT",
                description = "Instant Wallet Top-Up (Demo Card)",
                referenceId = "CARD819203810",
                status = "SUCCESS",
                timestamp = now - 3600000,
                balanceAfter = 380
            )
            database.transactionDao().insertTransactions(listOf(tx1, tx2, tx3, tx4, tx5))

            // 5. Initial Notifications
            val n1 = NotificationEntity(
                id = "NOTIF-1",
                title = "Room ID & Password Released! 🔑",
                message = "Room details for Bermuda Solo Rush #101 are now ready: Room ID 891402, Password 990.",
                type = "ROOM_KEY",
                tournamentId = "FF-T101",
                roomId = "891402",
                roomPassword = "990",
                timestamp = now - 900000,
                isRead = false
            )
            val n2 = NotificationEntity(
                id = "NOTIF-2",
                title = "Prize Credited! 🏆",
                message = "Congratulations! 775 coins credited for #1 Booyah in Alpine Battle Royale Solo Cup.",
                type = "WINNINGS",
                tournamentId = "FF-T100",
                timestamp = now - 82000000,
                isRead = true
            )
            val n3 = NotificationEntity(
                id = "NOTIF-3",
                title = "Weekend Grand Championship Announced",
                message = "Registration is open for Kalahari Squad Havoc with ₹4,500 prize pool.",
                type = "ANNOUNCEMENT",
                timestamp = now - 43200000,
                isRead = true
            )
            database.notificationDao().insertNotifications(listOf(n1, n2, n3))

            // 6. Initial App Settings
            database.appSettingsDao().insertOrUpdate(AppSettingsEntity())
        }
    }
}
