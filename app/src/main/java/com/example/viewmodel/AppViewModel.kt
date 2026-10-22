package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthManager
import com.example.data.firestore.AppSettingsDto
import com.example.data.firestore.FirestoreRepository
import com.example.data.firestore.NotificationDto
import com.example.data.firestore.ParticipantDto
import com.example.data.firestore.SettleData
import com.example.data.firestore.SupportMessageDto
import com.example.data.firestore.SupportThreadDto
import com.example.data.firestore.TournamentDto
import com.example.data.firestore.TransactionDto
import com.example.data.firestore.UserDto
import com.example.data.firestore.WithdrawalRequestDto
import com.example.data.payment.PaytmPaymentService
import com.example.data.payment.PaytmTxnStatus
import com.example.notification.PushNotificationHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object TournamentDetails : Screen("tournament_details")
    object TournamentLobby : Screen("tournament_lobby")
    object Wallet : Screen("wallet")
    object History : Screen("history")
    object Notifications : Screen("notifications")
    object Profile : Screen("profile")
    object SupportChat : Screen("support_chat")

    // Admin screens
    object AdminAuth : Screen("admin_auth")
    object AdminDashboard : Screen("admin_dashboard")
    object AdminTournaments : Screen("admin_tournaments")
    object AdminTournamentEditor : Screen("admin_tournament_editor")
    object AdminSettlement : Screen("admin_settlement")
    object AdminUsers : Screen("admin_users")
    object AdminSettings : Screen("admin_settings")
    object AdminWithdrawals : Screen("admin_withdrawals")
    object AdminSupportInbox : Screen("admin_support_inbox")
    object AdminSupportChat : Screen("admin_support_chat")
}

data class UiNotificationAlert(
    val title: String,
    val message: String,
    val roomId: String? = null,
    val roomPass: String? = null,
    val id: String = ""
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val firestoreRepo = FirestoreRepository(application)

    // Navigation and screen state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _screenHistory = mutableListOf<Screen>()

    // Current active user ID (only real authenticated user)
    private val _currentUserId = MutableStateFlow(
        FirebaseAuth.getInstance().currentUser?.uid ?: ""
    )
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    // Persistent and optimistic read & cleared notification tracking + theme preference
    private val prefs by lazy {
        getApplication<Application>().getSharedPreferences("user_notifs_prefs", Context.MODE_PRIVATE)
    }

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    init {
        val savedDark = prefs.getBoolean("app_dark_mode_enabled", false)
        _isDarkMode.value = savedDark
        com.example.ui.theme.ThemePreferenceState.isDarkMode = savedDark
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        com.example.ui.theme.ThemePreferenceState.isDarkMode = enabled
        prefs.edit().putBoolean("app_dark_mode_enabled", enabled).apply()
    }

    fun toggleDarkMode() {
        setDarkMode(!_isDarkMode.value)
    }

    private val syncedUserIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val _locallyReadNotifIds = MutableStateFlow<Set<String>>(emptySet())
    private val _locallyClearedNotifIds = MutableStateFlow<Set<String>>(emptySet())

    private fun loadLocalReadNotifs(uid: String): Set<String> {
        if (uid.isBlank()) return emptySet()
        return (prefs.getStringSet("read_notifs_$uid", emptySet()) ?: emptySet()).toSet()
    }

    private fun saveLocalReadNotifs(newIds: Set<String>) {
        val uid = _currentUserId.value.ifBlank { "guest" }
        val updated = _locallyReadNotifIds.value + newIds
        _locallyReadNotifIds.value = updated
        prefs.edit()
            .putStringSet("read_notifs_$uid", HashSet(updated))
            .commit()
    }

    private fun loadLocalClearedNotifs(uid: String): Set<String> {
        if (uid.isBlank()) return emptySet()
        return (prefs.getStringSet("cleared_notifs_$uid", emptySet()) ?: emptySet()).toSet()
    }

    private fun saveLocalClearedNotifs(newIds: Set<String>) {
        val uid = _currentUserId.value.ifBlank { "guest" }
        val updated = _locallyClearedNotifIds.value + newIds
        _locallyClearedNotifIds.value = updated
        prefs.edit()
            .putStringSet("cleared_notifs_$uid", HashSet(updated))
            .commit()
    }

    private fun resolveUserSignupCutoff(uid: String, user: UserDto?): Long {
        if (uid.isBlank()) return Long.MAX_VALUE
        val key = "signup_cutoff_$uid"
        val storedCutoff = prefs.getLong(key, 0L)
        val cloudSignupTs = when {
            user != null && user.signupTimestamp > 0L -> user.signupTimestamp
            user?.createdAt != null -> user.createdAt.toDate().time
            else -> 0L
        }
        val authCreationTs = FirebaseAuth.getInstance().currentUser
            ?.takeIf { it.uid == uid }
            ?.metadata?.creationTimestamp ?: 0L

        val candidate = listOf(cloudSignupTs, authCreationTs, storedCutoff)
            .filter { it > 0L }
            .minOrNull()

        return if (candidate != null && candidate > 0L) {
            if (storedCutoff == 0L || candidate < storedCutoff) {
                prefs.edit().putLong(key, candidate).apply()
            }
            candidate
        } else {
            val now = System.currentTimeMillis()
            prefs.edit().putLong(key, now).apply()
            now
        }
    }

    private fun shouldShowNotificationForUser(
        notif: NotificationDto,
        uid: String,
        clearedIds: Set<String>,
        joinedTournamentIds: Set<String>,
        signupCutoffMs: Long
    ): Boolean {
        if (uid.isBlank()) return false
        // 1. Exclude if cleared locally or in Cloud Firestore by this user
        if (clearedIds.contains(notif.id) || notif.clearedByUsers.contains(uid)) {
            return false
        }
        // 2. Exclude notifications older than the user's signup timestamp (with 10s grace buffer for signup events)
        val effectiveCutoff = (signupCutoffMs - 10_000L).coerceAtLeast(0L)
        if (effectiveCutoff > 0L && notif.timestamp < effectiveCutoff) {
            return false
        }
        // 3. Personal notification check: strictly private to the target userId
        if (!notif.userId.isNullOrBlank()) {
            return notif.userId == uid
        }
        // 4. Tournament-specific notification check: Room Keys & Match Updates only for matches the user joined
        if (!notif.tournamentId.isNullOrBlank()) {
            return if (notif.type.equals("ANNOUNCEMENT", ignoreCase = true)) {
                true
            } else {
                joinedTournamentIds.contains(notif.tournamentId)
            }
        }
        // 5. General announcement broadcast created after user signup
        return true
    }

    // Mode filter for dashboard
    private val _selectedModeFilter = MutableStateFlow("ALL")
    val selectedModeFilter: StateFlow<String> = _selectedModeFilter.asStateFlow()

    // Status filter
    private val _selectedStatusFilter = MutableStateFlow("UPCOMING")
    val selectedStatusFilter: StateFlow<String> = _selectedStatusFilter.asStateFlow()

    // Selected tournament for details / editing
    private val _selectedTournamentId = MutableStateFlow<String?>(null)
    val selectedTournamentId: StateFlow<String?> = _selectedTournamentId.asStateFlow()

    // Real-time notification banner / snackbar
    private val _liveAlert = MutableSharedFlow<UiNotificationAlert>()
    val liveAlert: SharedFlow<UiNotificationAlert> = _liveAlert.asSharedFlow()

    // Action message feedback
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // Live Cloud Firestore Data Streams
    val allTournaments: StateFlow<List<TournamentDto>> = firestoreRepo.observeTournaments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTournaments: StateFlow<List<TournamentDto>> = combine(
        allTournaments,
        selectedModeFilter,
        selectedStatusFilter
    ) { tournaments, mode, status ->
        tournaments.filter { t ->
            val modeMatch = (mode == "ALL" || t.matchType.equals(mode, ignoreCase = true))
            val statusMatch = if (status == "ALL") true else t.status.equals(status, ignoreCase = true)
            modeMatch && statusMatch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appSettings: StateFlow<AppSettingsDto?> = firestoreRepo.observeAppSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allUsers: StateFlow<List<UserDto>> = firestoreRepo.observeAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentUser: StateFlow<UserDto?> = _currentUserId.flatMapLatest { id ->
        if (id.isNullOrBlank()) flowOf(null) else firestoreRepo.observeUser(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userTransactions: StateFlow<List<TransactionDto>> = _currentUserId.flatMapLatest { id ->
        if (id.isNullOrBlank()) flowOf(emptyList()) else firestoreRepo.observeTransactions(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userParticipations: StateFlow<List<ParticipantDto>> = _currentUserId.flatMapLatest { id ->
        if (id.isNullOrBlank()) flowOf(emptyList()) else firestoreRepo.observeUserParticipations(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userWithdrawals: StateFlow<List<WithdrawalRequestDto>> = _currentUserId.flatMapLatest { id ->
        if (id.isNullOrBlank()) flowOf(emptyList()) else firestoreRepo.observeUserWithdrawals(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWithdrawals: StateFlow<List<WithdrawalRequestDto>> = firestoreRepo.observeAllWithdrawals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val pendingWithdrawalsCount: StateFlow<Int> = allWithdrawals.flatMapLatest { list ->
        flowOf(list.count { it.status == "PENDING" })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Support Chat StateFlows (User & Admin)
    private val _selectedAdminSupportUserId = MutableStateFlow<String?>(null)
    val selectedAdminSupportUserId: StateFlow<String?> = _selectedAdminSupportUserId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userSupportThread: StateFlow<SupportThreadDto?> = _currentUserId.flatMapLatest { id ->
        if (id.isNullOrBlank()) flowOf(null) else firestoreRepo.observeSupportThread(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userSupportMessages: StateFlow<List<SupportMessageDto>> = _currentUserId.flatMapLatest { id ->
        if (id.isNullOrBlank()) flowOf(emptyList()) else firestoreRepo.observeSupportMessages(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userUnreadSupportCount: StateFlow<Int> = userSupportThread.flatMapLatest { thread ->
        flowOf(thread?.unreadByUser ?: 0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allSupportThreads: StateFlow<List<SupportThreadDto>> = firestoreRepo.observeAllSupportThreads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val adminUnreadSupportCount: StateFlow<Int> = allSupportThreads.flatMapLatest { threads ->
        flowOf(threads.count { it.unreadByAdmin > 0 })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedAdminSupportThread: StateFlow<SupportThreadDto?> = _selectedAdminSupportUserId.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(null) else firestoreRepo.observeSupportThread(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedAdminSupportMessages: StateFlow<List<SupportMessageDto>> = _selectedAdminSupportUserId.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptyList()) else firestoreRepo.observeSupportMessages(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class UserNotifFilterState(
        val uid: String,
        val readIds: Set<String>,
        val clearedIds: Set<String>,
        val joinedTournamentIds: Set<String>,
        val signupCutoffMs: Long
    )

    private val userNotifFilterState = combine(
        _currentUserId,
        _locallyReadNotifIds,
        _locallyClearedNotifIds,
        userParticipations,
        currentUser
    ) { uid, readIds, clearedIds, participations, user ->
        val joinedIds = participations.map { it.tournamentId }.filter { it.isNotBlank() }.toSet()
        val cutoff = resolveUserSignupCutoff(uid, user)
        UserNotifFilterState(
            uid = uid,
            readIds = readIds,
            clearedIds = clearedIds,
            joinedTournamentIds = joinedIds,
            signupCutoffMs = cutoff
        )
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allNotifications: StateFlow<List<NotificationDto>> = combine(
        firestoreRepo.observeNotifications(),
        userNotifFilterState
    ) { list, filterState ->
        list.filter { notif ->
            shouldShowNotificationForUser(
                notif = notif,
                uid = filterState.uid,
                clearedIds = filterState.clearedIds,
                joinedTournamentIds = filterState.joinedTournamentIds,
                signupCutoffMs = filterState.signupCutoffMs
            )
        }.map { notif ->
            val isReadForUser = notif.isRead ||
                    filterState.readIds.contains(notif.id) ||
                    (filterState.uid.isNotBlank() && notif.readByUsers.contains(filterState.uid))
            notif.copy(isRead = isReadForUser)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val unreadNotifCount: StateFlow<Int> = allNotifications.flatMapLatest { list ->
        flowOf(list.count { !it.isRead })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedTournament: StateFlow<TournamentDto?> = _selectedTournamentId.flatMapLatest { id ->
        if (id.isNullOrBlank()) flowOf(null) else firestoreRepo.observeTournament(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedTournamentParticipants: StateFlow<List<ParticipantDto>> = _selectedTournamentId.flatMapLatest { id ->
        if (id.isNullOrBlank()) flowOf(emptyList()) else firestoreRepo.observeParticipants(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isRealAdmin: StateFlow<Boolean> = currentUser.flatMapLatest { user ->
        val currentAuthEmail = FirebaseAuth.getInstance().currentUser?.email
        val email = user?.email ?: currentAuthEmail ?: ""
        val isAdminFlag = user?.isAdmin == true
        flowOf(email.equals("udaynath381@gmail.com", ignoreCase = true) || isAdminFlag)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun isUserRealAdmin(): Boolean {
        val currentAuthEmail = FirebaseAuth.getInstance().currentUser?.email
        val userDocEmail = currentUser.value?.email
        val isDocAdmin = currentUser.value?.isAdmin == true
        val email = userDocEmail ?: currentAuthEmail ?: ""
        return email.equals("udaynath381@gmail.com", ignoreCase = true) || isDocAdmin
    }

    @Volatile
    private var isWipingData = false

    init {
        val initialUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val lastUid = prefs.getString("last_active_uid", "") ?: ""
        val alreadyWipedV3 = prefs.getBoolean("complete_factory_wipe_v3", false)

        if (!alreadyWipedV3) {
            isWipingData = true
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    // 1. Delete all Cloud Firestore collections & subcollections (users, transactions, participations, tournaments, participants, withdrawals, notifications)
                    firestoreRepo.deleteAllDatabaseData(setOf(initialUid, lastUid))
                    // 2. Delete local Room SQLite database completely
                    try {
                        getApplication<Application>().deleteDatabase("ff_tournament_db")
                    } catch (_: Exception) {}
                    // 3. Clear local SharedPreferences and record wipe completion
                    val currentUidNow = FirebaseAuth.getInstance().currentUser?.uid ?: initialUid
                    prefs.edit().clear().commit()
                    val editor = prefs.edit().putBoolean("complete_factory_wipe_v3", true)
                    if (currentUidNow.isNotBlank()) {
                        editor.putBoolean("session_wiped_$currentUidNow", true)
                        editor.putString("last_active_uid", currentUidNow)
                    }
                    editor.commit()
                    syncedUserIds.clear()
                    _locallyReadNotifIds.value = emptySet()
                    _locallyClearedNotifIds.value = emptySet()
                } finally {
                    isWipingData = false
                }
            }
        } else {
            viewModelScope.launch {
                firestoreRepo.cleanupDemoData()
                firestoreRepo.initAppSettingsIfMissing()
            }
            _locallyReadNotifIds.value = loadLocalReadNotifs(_currentUserId.value)
            _locallyClearedNotifIds.value = loadLocalClearedNotifs(_currentUserId.value)
            val authUser = FirebaseAuth.getInstance().currentUser
            if (authUser != null && !prefs.getBoolean("session_wiped_${authUser.uid}", false)) {
                onUserAuthenticated(authUser, isExplicitSignIn = false)
            }
        }

        val appStartTime = System.currentTimeMillis()

        // Sync active user ID and background sync scheduling
        viewModelScope.launch {
            _currentUserId.collect { uid ->
                if (uid.isNotBlank()) {
                    prefs.edit().putString("last_active_uid", uid).apply()
                    com.example.notification.NotificationBackgroundManager.scheduleBackgroundSync(getApplication())
                }
            }
        }

        // Sync joined tournament IDs to SharedPreferences for background notification targeting
        viewModelScope.launch {
            userParticipations.collect { participations ->
                val uid = _currentUserId.value
                if (uid.isNotBlank()) {
                    val joinedIds = participations.map { it.tournamentId }.filter { it.isNotBlank() }.toSet()
                    prefs.edit().putStringSet("joined_tournaments_$uid", joinedIds).apply()
                }
            }
        }

        // Real-Time Push Notification Engine:
        // Prevents all notifications coming at once on app open, and ensures only newly broadcast alerts trigger notifications
        viewModelScope.launch {
            _currentUserId.collectLatest { id ->
                if (id.isNotBlank()) {
                    var isFirstSnapshotWithData = true

                    allNotifications.collectLatest { userVisibleNotifs ->
                        val notifiedIds = (prefs.getStringSet("notified_notif_ids", emptySet()) ?: emptySet()).toMutableSet()

                        if (isFirstSnapshotWithData) {
                            if (userVisibleNotifs.isNotEmpty()) {
                                // Mark all historical existing notifications as already notified so they NEVER burst when opening the app
                                userVisibleNotifs.forEach { notif ->
                                    notifiedIds.add(notif.id)
                                }
                                prefs.edit().putStringSet("notified_notif_ids", notifiedIds).apply()
                                isFirstSnapshotWithData = false
                            }
                        } else {
                            var newlyNotified = false
                            for (notif in userVisibleNotifs) {
                                val isRecentOrNew = notif.timestamp >= (appStartTime - 30_000L)
                                if (!notifiedIds.contains(notif.id) && isRecentOrNew) {
                                    notifiedIds.add(notif.id)
                                    newlyNotified = true

                                    // 1. Trigger Android System Push Notification (Tray)
                                    PushNotificationHelper.showPushNotification(
                                        context = getApplication(),
                                        notificationId = notif.id.hashCode(),
                                        title = notif.title,
                                        message = notif.message,
                                        type = notif.type
                                    )
                                    // 2. Trigger In-App Floating Heads-Up Banner
                                    _liveAlert.emit(
                                        UiNotificationAlert(
                                            id = notif.id,
                                            title = notif.title,
                                            message = notif.message,
                                            roomId = notif.roomId,
                                            roomPass = notif.roomPassword
                                        )
                                    )
                                } else if (!notifiedIds.contains(notif.id)) {
                                    // Silently mark older notifications as seen
                                    notifiedIds.add(notif.id)
                                    newlyNotified = true
                                }
                            }
                            if (newlyNotified) {
                                prefs.edit().putStringSet("notified_notif_ids", notifiedIds).apply()
                            }
                        }
                    }
                }
            }
        }
    }

    fun onUserAuthenticated(user: com.google.firebase.auth.FirebaseUser, isExplicitSignIn: Boolean = false) {
        _currentUserId.value = user.uid
        if (isWipingData || !prefs.getBoolean("complete_factory_wipe_v3", false)) {
            return
        }
        if (isExplicitSignIn) {
            prefs.edit().remove("session_wiped_${user.uid}").commit()
        } else if (prefs.getBoolean("session_wiped_${user.uid}", false)) {
            // Session data was intentionally wiped; do not auto-recreate user document until explicit action
            return
        }

        _locallyReadNotifIds.value = loadLocalReadNotifs(user.uid)
        _locallyClearedNotifIds.value = loadLocalClearedNotifs(user.uid)
        val syncKey = "user_synced_${user.uid}"
        val alreadySynced = prefs.getBoolean(syncKey, false)
        if (!syncedUserIds.add(user.uid) && alreadySynced && !isExplicitSignIn) {
            return
        }

        val deviceId = try {
            android.provider.Settings.Secure.getString(
                getApplication<Application>().contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: ""
        } catch (_: Exception) {
            ""
        }

        viewModelScope.launch {
            val isUserAdmin = user.email.equals("udaynath381@gmail.com", ignoreCase = true)
            firestoreRepo.syncUserProfile(
                userId = user.uid,
                email = user.email ?: "",
                displayName = user.displayName ?: "Player",
                isAdmin = isUserAdmin,
                deviceId = deviceId
            )
            prefs.edit().putBoolean(syncKey, true).commit()
            firestoreRepo.initAppSettingsIfMissing()
            if (isExplicitSignIn) {
                _toastMessage.emit("Signed in as ${user.displayName ?: user.email}")
            }
        }
    }

    fun navigateTo(screen: Screen) {
        val isAdminScreen = screen is Screen.AdminDashboard ||
                screen is Screen.AdminTournaments ||
                screen is Screen.AdminTournamentEditor ||
                screen is Screen.AdminSettlement ||
                screen is Screen.AdminUsers ||
                screen is Screen.AdminSettings ||
                screen is Screen.AdminWithdrawals ||
                screen is Screen.AdminSupportInbox ||
                screen is Screen.AdminSupportChat ||
                screen is Screen.AdminAuth
        if (isAdminScreen && !isUserRealAdmin()) {
            showToast("Access Denied: Only udaynath381@gmail.com is authorized as Admin.")
            return
        }
        if (_currentScreen.value != screen) {
            _screenHistory.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun openUserSupportChat() {
        navigateTo(Screen.SupportChat)
        markCurrentUserSupportRead()
    }

    fun openAdminSupportThread(userId: String) {
        if (!isUserRealAdmin()) {
            showToast("Admin permission required.")
            return
        }
        _selectedAdminSupportUserId.value = userId
        navigateTo(Screen.AdminSupportChat)
        markAdminSupportRead(userId)
    }

    fun markCurrentUserSupportRead() {
        val uid = _currentUserId.value
        if (uid.isBlank()) return
        viewModelScope.launch {
            firestoreRepo.markSupportThreadRead(uid, byAdmin = false)
        }
    }

    fun markAdminSupportRead(userId: String) {
        if (userId.isBlank() || !isUserRealAdmin()) return
        viewModelScope.launch {
            firestoreRepo.markSupportThreadRead(userId, byAdmin = true)
        }
    }

    fun sendUserSupportMessage(
        messageText: String,
        imageBase64: String? = null,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val uid = _currentUserId.value
        if (uid.isBlank()) {
            onComplete(false, "Please sign in to contact support.")
            return
        }
        val user = currentUser.value
        val authUser = FirebaseAuth.getInstance().currentUser
        val resolvedName = user?.username?.takeIf { it.isNotBlank() }
            ?: authUser?.displayName?.takeIf { it.isNotBlank() }
            ?: authUser?.email?.substringBefore("@")
            ?: "Player"
        val resolvedEmail = user?.email?.takeIf { it.isNotBlank() } ?: authUser?.email ?: ""

        viewModelScope.launch {
            val res = firestoreRepo.sendSupportMessage(
                threadUserId = uid,
                userName = resolvedName,
                userEmail = resolvedEmail,
                userFfIgn = user?.ffIgn ?: "",
                userFfUid = user?.ffUid ?: "",
                senderId = uid,
                senderName = resolvedName,
                isAdminSender = false,
                messageText = messageText,
                imageBase64 = imageBase64
            )
            res.fold(
                onSuccess = {
                    onComplete(true, "Sent")
                },
                onFailure = { err ->
                    val msg = err.message ?: "Failed to send message"
                    _toastMessage.emit(msg)
                    onComplete(false, msg)
                }
            )
        }
    }

    fun sendAdminSupportMessage(
        targetUserId: String,
        messageText: String,
        imageBase64: String? = null,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!isUserRealAdmin()) {
            onComplete(false, "Admin permission required.")
            return
        }
        val adminUid = _currentUserId.value.ifBlank { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
        if (targetUserId.isBlank() || adminUid.isBlank()) {
            onComplete(false, "Invalid user thread.")
            return
        }
        val thread = selectedAdminSupportThread.value
        val targetUser = allUsers.value.firstOrNull { it.id == targetUserId }

        viewModelScope.launch {
            val res = firestoreRepo.sendSupportMessage(
                threadUserId = targetUserId,
                userName = thread?.userName?.takeIf { it.isNotBlank() } ?: targetUser?.username ?: "Player",
                userEmail = thread?.userEmail?.takeIf { it.isNotBlank() } ?: targetUser?.email ?: "",
                userFfIgn = thread?.userFfIgn?.takeIf { it.isNotBlank() } ?: targetUser?.ffIgn ?: "",
                userFfUid = thread?.userFfUid?.takeIf { it.isNotBlank() } ?: targetUser?.ffUid ?: "",
                senderId = adminUid,
                senderName = "Admin Support",
                isAdminSender = true,
                messageText = messageText,
                imageBase64 = imageBase64
            )
            res.fold(
                onSuccess = {
                    onComplete(true, "Reply sent")
                },
                onFailure = { err ->
                    val msg = err.message ?: "Failed to send reply"
                    _toastMessage.emit(msg)
                    onComplete(false, msg)
                }
            )
        }
    }

    fun updateSupportTicketStatus(targetUserId: String, status: String) {
        if (targetUserId.isBlank()) return
        viewModelScope.launch {
            val res = firestoreRepo.updateSupportThreadStatus(targetUserId, status)
            res.onSuccess { msg ->
                _toastMessage.emit(msg)
            }.onFailure { err ->
                _toastMessage.emit(err.message ?: "Failed to update status")
            }
        }
    }

    // User: Submit withdrawal request (winnings only)
    fun requestWithdrawal(
        amount: Int,
        upiId: String,
        accountHolderName: String,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val uid = _currentUserId.value
        if (uid.isBlank()) {
            onComplete(false, "Please sign in to withdraw balance.")
            return
        }
        viewModelScope.launch {
            val res = firestoreRepo.requestWithdrawal(uid, amount, upiId, accountHolderName)
            res.fold(
                onSuccess = { req ->
                    _toastMessage.emit("Withdrawal request of ₹${req.amount} submitted successfully! ⏳")
                    onComplete(true, "Request submitted! Track status in Withdrawals tab.")
                },
                onFailure = { err ->
                    val msg = err.message ?: "Failed to submit withdrawal request."
                    _toastMessage.emit(msg)
                    onComplete(false, msg)
                }
            )
        }
    }

    // Admin: Approve withdrawal & mark paid
    fun approveWithdrawal(
        withdrawalId: String,
        adminRefId: String,
        adminNote: String,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!isUserRealAdmin()) {
            onComplete(false, "Admin permission required.")
            return
        }
        viewModelScope.launch {
            val res = firestoreRepo.approveWithdrawal(withdrawalId, adminRefId, adminNote)
            res.fold(
                onSuccess = { msg ->
                    _toastMessage.emit(msg)
                    onComplete(true, msg)
                },
                onFailure = { err ->
                    val msg = err.message ?: "Approval failed."
                    _toastMessage.emit(msg)
                    onComplete(false, msg)
                }
            )
        }
    }

    // Admin: Reject withdrawal & refund player wallet
    fun rejectWithdrawal(
        withdrawalId: String,
        reason: String,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!isUserRealAdmin()) {
            onComplete(false, "Admin permission required.")
            return
        }
        viewModelScope.launch {
            val res = firestoreRepo.rejectWithdrawal(withdrawalId, reason)
            res.fold(
                onSuccess = { msg ->
                    _toastMessage.emit(msg)
                    onComplete(true, msg)
                },
                onFailure = { err ->
                    val msg = err.message ?: "Rejection failed."
                    _toastMessage.emit(msg)
                    onComplete(false, msg)
                }
            )
        }
    }

    fun navigateBack(): Boolean {
        if (_screenHistory.isNotEmpty()) {
            _currentScreen.value = _screenHistory.removeAt(_screenHistory.lastIndex)
            return true
        }
        if (_currentScreen.value != Screen.Dashboard) {
            _currentScreen.value = Screen.Dashboard
            return true
        }
        return false
    }

    fun selectTournament(tournamentId: String) {
        _selectedTournamentId.value = tournamentId.ifBlank { null }
        navigateTo(Screen.TournamentDetails)
    }

    fun openTournamentLobby(tournamentId: String) {
        _selectedTournamentId.value = tournamentId.ifBlank { null }
        navigateTo(Screen.TournamentLobby)
    }

    fun selectTournamentForEdit(tournamentId: String) {
        _selectedTournamentId.value = tournamentId.ifBlank { null }
        navigateTo(Screen.AdminTournamentEditor)
    }

    fun selectTournamentForSettlement(tournamentId: String) {
        _selectedTournamentId.value = tournamentId.ifBlank { null }
        navigateTo(Screen.AdminSettlement)
    }

    fun setModeFilter(mode: String) {
        _selectedModeFilter.value = mode
    }

    fun setStatusFilter(status: String) {
        _selectedStatusFilter.value = status
    }

    fun joinTournament(tournamentId: String, ign: String, uid: String, chosenSlot: Int? = null) {
        viewModelScope.launch {
            val user = currentUser.value
            val userId = user?.id ?: _currentUserId.value
            if (userId.isBlank()) {
                _toastMessage.emit("Please sign in to register")
                return@launch
            }
            val res = firestoreRepo.joinTournament(
                tournamentId = tournamentId,
                userId = userId,
                ffIgn = ign,
                ffUid = uid,
                username = user?.username ?: ign,
                chosenSlot = chosenSlot
            )
            res.onSuccess { msg ->
                _toastMessage.emit(msg)
                _liveAlert.emit(UiNotificationAlert("Match Joined! 🎯", msg))
            }.onFailure { err ->
                _toastMessage.emit(err.message ?: "Failed to join")
            }
        }
    }

    fun updateGamingProfile(ign: String, uid: String) {
        viewModelScope.launch {
            val user = currentUser.value
            val userId = user?.id ?: _currentUserId.value
            if (userId.isBlank()) return@launch
            val res = firestoreRepo.updateUserGamingProfile(userId, ign, uid)
            res.onSuccess { msg ->
                _toastMessage.emit(msg)
            }.onFailure { err ->
                _toastMessage.emit(err.message ?: "Failed to update profile")
            }
        }
    }

    fun depositMoney(amount: Int, paymentMethod: String) {
        viewModelScope.launch {
            val res = firestoreRepo.depositMoney(_currentUserId.value, amount, paymentMethod)
            res.onSuccess { msg ->
                _toastMessage.emit(msg)
                _liveAlert.emit(UiNotificationAlert("Deposit Credited! 💳", "₹$amount added to your live wallet."))
            }.onFailure { err ->
                _toastMessage.emit(err.message ?: "Deposit failed")
            }
        }
    }

    suspend fun checkAndVerifyPaytmDeposit(orderId: String, amount: Int): PaytmTxnStatus {
        val checkResult = PaytmPaymentService.checkTransactionStatus(orderId)
        val status = checkResult.getOrNull() ?: PaytmTxnStatus(
            status = "ERROR",
            isSuccess = false,
            isPending = false,
            respCode = "",
            respMsg = checkResult.exceptionOrNull()?.message ?: "Network error checking transaction",
            txnId = null,
            bankTxnId = null,
            orderId = orderId,
            txnAmount = null,
            rawJson = ""
        )

        if (status.isSuccess) {
            val txnId = status.txnId ?: ("PTM" + (100000..999999).random())
            val res = firestoreRepo.verifyAndCreditRealDeposit(
                userId = _currentUserId.value,
                orderId = orderId,
                amount = status.txnAmount?.toInt() ?: amount,
                txnId = txnId,
                method = "Paytm UPI"
            )
            res.onSuccess {
                _toastMessage.emit("₹$amount deposit verified & credited to wallet! 🎉")
                _liveAlert.emit(
                    UiNotificationAlert(
                        title = "Payment Successful! 💳",
                        message = "₹$amount credited to your wallet balance. Txn ID: $txnId"
                    )
                )
            }.onFailure { err ->
                _toastMessage.emit("Error updating wallet: ${err.message}")
            }
        }

        return status
    }

    fun updateRoomCredentials(tournamentId: String, roomId: String, roomPass: String, notice: String) {
        viewModelScope.launch {
            val res = firestoreRepo.updateRoomCredentials(tournamentId, roomId, roomPass, notice)
            res.onSuccess { msg ->
                _toastMessage.emit(msg)
                _liveAlert.emit(
                    UiNotificationAlert(
                        title = "🔑 Room ID & Password Live!",
                        message = "Room ID: $roomId | Pass: $roomPass",
                        roomId = roomId,
                        roomPass = roomPass
                    )
                )
            }.onFailure { err ->
                _toastMessage.emit(err.message ?: "Failed to update room")
            }
        }
    }

    fun createTournament(tournament: TournamentDto) {
        viewModelScope.launch {
            val res = firestoreRepo.createTournament(tournament)
            res.onSuccess { msg ->
                _toastMessage.emit(msg)
                navigateBack()
            }.onFailure { err ->
                _toastMessage.emit(err.message ?: "Failed to create tournament")
            }
        }
    }

    fun updateTournament(tournament: TournamentDto) {
        viewModelScope.launch {
            val res = firestoreRepo.updateTournament(tournament)
            res.onSuccess { msg ->
                _toastMessage.emit(msg)
                navigateBack()
            }.onFailure { err ->
                _toastMessage.emit(err.message ?: "Failed to update")
            }
        }
    }

    fun showToast(msg: String) {
        viewModelScope.launch {
            _toastMessage.emit(msg)
        }
    }

    fun deleteTournament(tournamentId: String) {
        viewModelScope.launch {
            val res = firestoreRepo.deleteTournament(tournamentId)
            res.onSuccess { msg ->
                _toastMessage.emit(msg)
                if (_currentScreen.value == Screen.AdminTournamentEditor || _currentScreen.value == Screen.TournamentDetails) {
                    navigateBack()
                }
            }.onFailure { err ->
                _toastMessage.emit(err.message ?: "Failed to delete")
            }
        }
    }

    fun settleMatch(tournamentId: String, settleList: List<SettleData>) {
        viewModelScope.launch {
            val res = firestoreRepo.settleMatch(tournamentId, settleList)
            res.onSuccess { msg ->
                _toastMessage.emit(msg)
                _liveAlert.emit(UiNotificationAlert("Match Completed! 🏆", "Results settled & prizes disbursed to player wallets."))
                navigateBack()
            }.onFailure { err ->
                _toastMessage.emit(err.message ?: "Settlement failed")
            }
        }
    }

    fun adjustUserBalance(userId: String, delta: Int, reason: String) {
        viewModelScope.launch {
            val res = firestoreRepo.adjustUserBalance(userId, delta, reason)
            res.onSuccess { msg ->
                _toastMessage.emit(msg)
            }.onFailure { err ->
                _toastMessage.emit(err.message ?: "Failed to adjust balance")
            }
        }
    }

    fun toggleUserSuspension(userId: String) {
        viewModelScope.launch {
            val res = firestoreRepo.toggleUserSuspension(userId)
            res.onSuccess { msg ->
                _toastMessage.emit(msg)
            }.onFailure { err ->
                _toastMessage.emit(err.message ?: "Failed to toggle suspension")
            }
        }
    }

    fun deleteUserAndHistory(userId: String) {
        if (!isUserRealAdmin()) {
            showToast("Admin permission required.")
            return
        }
        viewModelScope.launch {
            prefs.edit().putBoolean("session_wiped_$userId", true).commit()
            syncedUserIds.remove(userId)
            val res = firestoreRepo.deleteSingleUserAndHistory(userId)
            res.onSuccess { msg ->
                _toastMessage.emit(msg)
            }.onFailure { err ->
                _toastMessage.emit(err.message ?: "Failed to delete user")
            }
        }
    }

    fun deleteAllPlatformData(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        if (!isUserRealAdmin()) {
            onComplete(false, "Admin permission required.")
            return
        }
        isWipingData = true
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val activeUid = _currentUserId.value
                val lastUid = prefs.getString("last_active_uid", "") ?: ""
                val knownUids = allUsers.value.map { it.id }.toSet() + setOf(activeUid, lastUid)
                val res = firestoreRepo.deleteAllDatabaseData(knownUids)
                try {
                    getApplication<Application>().deleteDatabase("ff_tournament_db")
                } catch (_: Exception) {}

                prefs.edit().clear().commit()
                val editor = prefs.edit().putBoolean("complete_factory_wipe_v3", true)
                if (activeUid.isNotBlank()) {
                    editor.putBoolean("session_wiped_$activeUid", true)
                    editor.putString("last_active_uid", activeUid)
                }
                editor.commit()
                syncedUserIds.clear()
                _locallyReadNotifIds.value = emptySet()
                _locallyClearedNotifIds.value = emptySet()
                _selectedTournamentId.value = null

                res.fold(
                    onSuccess = { msg ->
                        _toastMessage.emit(msg)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            onComplete(true, msg)
                        }
                    },
                    onFailure = { err ->
                        val msg = err.message ?: "Failed to delete all data"
                        _toastMessage.emit(msg)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            onComplete(false, msg)
                        }
                    }
                )
            } finally {
                isWipingData = false
            }
        }
    }

    fun updateSettings(settings: AppSettingsDto) {
        viewModelScope.launch {
            val res = firestoreRepo.updateAppSettings(settings)
            res.onSuccess { msg ->
                _toastMessage.emit(msg)
            }.onFailure { err ->
                _toastMessage.emit(err.message ?: "Failed to save settings")
            }
        }
    }

    fun markNotificationRead(id: String) {
        saveLocalReadNotifs(setOf(id))
        viewModelScope.launch {
            firestoreRepo.markNotificationAsRead(id, _currentUserId.value)
            _toastMessage.emit("Marked as read")
        }
    }

    fun markAllNotificationsRead() {
        val allIds = allNotifications.value.map { it.id }.toSet()
        saveLocalReadNotifs(allIds)
        viewModelScope.launch {
            firestoreRepo.markAllNotificationsAsRead(allNotifications.value, _currentUserId.value)
            _toastMessage.emit("All notifications marked as read")
        }
    }

    fun clearNotification(id: String) {
        if (id.isBlank()) return
        saveLocalClearedNotifs(setOf(id))
        viewModelScope.launch {
            firestoreRepo.clearNotificationForUser(id, _currentUserId.value)
            _toastMessage.emit("Notification removed")
        }
    }

    fun clearAllNotifications() {
        val currentList = allNotifications.value
        if (currentList.isEmpty()) return
        val allIds = currentList.map { it.id }.toSet()
        saveLocalClearedNotifs(allIds)
        viewModelScope.launch {
            firestoreRepo.clearAllNotificationsForUser(currentList, _currentUserId.value)
            _toastMessage.emit("All notifications cleared")
        }
    }

    fun broadcastCustomNotification(title: String, message: String) {
        viewModelScope.launch {
            val res = firestoreRepo.broadcastNotification(title, message, "ANNOUNCEMENT")
            res.onSuccess { msg ->
                _toastMessage.emit(msg)
                PushNotificationHelper.showPushNotification(
                    context = getApplication(),
                    notificationId = System.currentTimeMillis().toInt(),
                    title = title,
                    message = message,
                    type = "ANNOUNCEMENT"
                )
                _liveAlert.emit(UiNotificationAlert(title, message))
            }.onFailure { err ->
                _toastMessage.emit(err.message ?: "Broadcast failed")
            }
        }
    }

    fun testPushNotification() {
        PushNotificationHelper.showPushNotification(
            context = getApplication(),
            notificationId = (1000..9999).random(),
            title = "🔔 Real-Time Match Alert",
            message = "Push notification engine active: Free Fire room credentials and prize updates will be delivered live!",
            type = "TEST"
        )
        viewModelScope.launch {
            _liveAlert.emit(
                UiNotificationAlert(
                    title = "🔔 Push Notification Test",
                    message = "System notification dispatched to Android drawer!"
                )
            )
            _toastMessage.emit("Push notification sent to device drawer")
        }
    }
}
