package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthManager
import com.example.auth.AuthenticatedSession
import com.example.data.firestore.AppSettingsDto
import com.example.data.firestore.FirestoreRepository
import com.example.data.firestore.NotificationDto
import com.example.data.firestore.ParticipantDto
import com.example.data.firestore.SettleData
import com.example.data.firestore.StaffMemberDto
import com.example.data.firestore.SupportMessageDto
import com.example.data.firestore.SupportThreadDto
import com.example.data.firestore.TournamentDto
import com.example.data.firestore.TransactionDto
import com.example.data.firestore.UserDto
import com.example.data.firestore.WithdrawalRequestDto
import com.example.data.firestore.formatCurrency
import com.example.data.payment.PaytmPaymentService
import com.example.data.payment.PaytmTxnStatus
import com.example.notification.PushNotificationHelper
import com.example.ui.components.RegistrationSuccessEvent
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object TopPlayers : Screen("top_players")
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
    object AdminStaff : Screen("admin_staff")
}

data class UiNotificationAlert(
    val title: String,
    val message: String,
    val roomId: String? = null,
    val roomPass: String? = null,
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val firestoreRepo = FirestoreRepository(application)

    // Navigation and screen state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _screenHistory = mutableListOf<Screen>()

    // Current active user ID (real authenticated session or Google Sign-In user)
    private val _currentUserId = MutableStateFlow(
        AuthManager.loadSavedSession(application)?.uid
            ?: FirebaseAuth.getInstance().currentUser?.uid
            ?: ""
    )
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    suspend fun registerUser(
        username: String,
        email: String,
        password: String,
        ffIgn: String,
        ffUid: String
    ): Result<AuthenticatedSession> {
        val result = AuthManager.registerWithUsernameAndPassword(
            context = getApplication(),
            username = username,
            email = email,
            password = password,
            ffIgn = ffIgn.trim(),
            ffUid = ffUid.trim()
        )
        result.onSuccess { session ->
            val deviceId = try {
                android.provider.Settings.Secure.getString(
                    getApplication<Application>().contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                ) ?: ""
            } catch (_: Exception) {
                ""
            }
            val isUserAdmin = AuthManager.isMasterAdminIdentity(
                email = session.email,
                username = session.username,
                uid = session.uid
            ) || session.isAdmin
            firestoreRepo.syncUserProfile(
                userId = session.uid,
                email = session.email,
                displayName = session.username.ifBlank { "Player" },
                isAdmin = isUserAdmin,
                deviceId = deviceId,
                ffIgn = ffIgn.trim(),
                ffUid = ffUid.trim(),
                passwordHash = session.passwordHash
            )
            onSessionAuthenticated(session, isExplicitSignIn = true)
        }
        return result
    }

    suspend fun loginWithUsernameOrEmail(
        usernameOrEmail: String,
        password: String
    ): Result<AuthenticatedSession> {
        val result = AuthManager.signInWithUsernameOrEmail(
            context = getApplication(),
            usernameOrEmail = usernameOrEmail,
            password = password
        )
        result.onSuccess { session ->
            onSessionAuthenticated(session, isExplicitSignIn = true)
        }
        return result
    }

    suspend fun sendPasswordReset(emailOrUsername: String): Result<String> {
        return AuthManager.sendPasswordReset(emailOrUsername)
    }

    fun onUserSignedOut() {
        _currentUserId.value = ""
        _optimisticUserOverride.value = null
        _selectedTournamentId.value = null
        _selectedAdminSupportUserId.value = null
        _screenHistory.clear()
        _currentScreen.value = Screen.Dashboard
    }



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

    // Lottie-based Tournament Registration Celebration State
    private val _registrationSuccessEvent = MutableStateFlow<RegistrationSuccessEvent?>(null)
    val registrationSuccessEvent: StateFlow<RegistrationSuccessEvent?> = _registrationSuccessEvent.asStateFlow()

    fun dismissRegistrationSuccess() {
        _registrationSuccessEvent.value = null
    }

    // Live Cloud Firestore Data Streams (Real Data Only)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allTournaments: StateFlow<List<TournamentDto>> = _currentUserId.flatMapLatest { uid ->
        if (uid.isBlank()) {
            flowOf(emptyList())
        } else {
            firestoreRepo.observeTournaments()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val appSettings: StateFlow<AppSettingsDto?> = _currentUserId.flatMapLatest { uid ->
        if (uid.isBlank()) {
            flowOf(null)
        } else {
            firestoreRepo.observeAppSettings()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _optimisticUserOverride = MutableStateFlow<UserDto?>(null)

    private fun buildSessionFallbackUser(uid: String): UserDto? {
        if (uid.isBlank()) return null
        val session = AuthManager.currentSession()?.takeIf { it.uid == uid }
            ?: AuthManager.loadSavedSession(getApplication())?.takeIf { it.uid == uid }
        val fbUser = FirebaseAuth.getInstance().currentUser?.takeIf { it.uid == uid }
        if (session == null && fbUser == null) return null
        val email = session?.email ?: fbUser?.email ?: ""
        val username = session?.username?.ifBlank { email.substringBefore("@").ifBlank { "Player" } }
            ?: fbUser?.displayName?.ifBlank { email.substringBefore("@").ifBlank { "Player" } }
            ?: "Player"
        val isAdmin = AuthManager.isMasterAdminIdentity(email = email, username = username, uid = uid) ||
            (session?.isAdmin == true)
        return UserDto(
            id = uid,
            username = username,
            usernameLower = AuthManager.normalizeUsernameKey(username),
            email = email,
            passwordHash = session?.passwordHash ?: "",
            ffIgn = session?.ffIgn ?: "",
            ffUid = session?.ffUid ?: "",
            isAdmin = isAdmin
        )
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val cloudCurrentUser: kotlinx.coroutines.flow.Flow<UserDto?> = _currentUserId.flatMapLatest { uid ->
        if (uid.isBlank()) {
            flowOf(null)
        } else {
            firestoreRepo.observeUser(uid).onStart { emit(null) }
        }
    }

    val currentUser: StateFlow<UserDto?> = combine(
        _currentUserId,
        cloudCurrentUser,
        _optimisticUserOverride
    ) { uid, cloudUser, optimistic ->
        if (uid.isBlank()) {
            null
        } else {
            val validOptimistic = optimistic?.takeIf { it.id == uid }
            val baseUser = cloudUser ?: validOptimistic ?: buildSessionFallbackUser(uid)
            if (baseUser != null && validOptimistic != null) {
                baseUser.copy(
                    id = uid,
                    ffIgn = validOptimistic.ffIgn.ifBlank { baseUser.ffIgn },
                    ffUid = validOptimistic.ffUid.ifBlank { baseUser.ffUid },
                    savedUpiId = validOptimistic.savedUpiId.ifBlank { baseUser.savedUpiId },
                    username = baseUser.username.ifBlank { validOptimistic.username },
                    email = baseUser.email.ifBlank { validOptimistic.email }
                )
            } else {
                baseUser?.copy(id = uid)
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        buildSessionFallbackUser(_currentUserId.value)
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val staffMembers: StateFlow<List<StaffMemberDto>> = _currentUserId.flatMapLatest { uid ->
        if (uid.isBlank()) {
            flowOf(emptyList())
        } else {
            firestoreRepo.observeStaffMembers()
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val isMainAdmin: StateFlow<Boolean> = combine(_currentUserId, currentUser) { uid, user ->
        if (uid.isBlank()) {
            false
        } else {
            val session = AuthManager.currentSession()?.takeIf { it.uid == uid }
                ?: AuthManager.loadSavedSession(getApplication())?.takeIf { it.uid == uid }
            val currentAuthEmail = FirebaseAuth.getInstance().currentUser?.takeIf { it.uid == uid }?.email
            val email = user?.email ?: session?.email ?: currentAuthEmail ?: ""
            val username = user?.username ?: session?.username ?: ""
            AuthManager.isMasterAdminIdentity(email = email, username = username, uid = uid)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        run {
            val uid = _currentUserId.value
            val session = AuthManager.currentSession()?.takeIf { it.uid == uid }
            uid.isNotBlank() && AuthManager.isMasterAdminIdentity(
                email = session?.email,
                username = session?.username,
                uid = uid
            )
        }
    )

    val currentStaffMember: StateFlow<StaffMemberDto?> = combine(
        _currentUserId,
        currentUser,
        staffMembers
    ) { uid, user, staffList ->
        if (uid.isBlank() || staffList.isEmpty()) {
            null
        } else {
            val session = AuthManager.currentSession()?.takeIf { it.uid == uid }
                ?: AuthManager.loadSavedSession(getApplication())?.takeIf { it.uid == uid }
            val currentAuthEmail = FirebaseAuth.getInstance().currentUser?.takeIf { it.uid == uid }?.email
            val email = (user?.email ?: session?.email ?: currentAuthEmail ?: "").trim().lowercase()
            val username = (user?.username ?: session?.username ?: "").trim()
            val usernameKey = AuthManager.normalizeUsernameKey(username)

            staffList.firstOrNull { staff ->
                (staff.userId.isNotBlank() && staff.userId == uid) ||
                    (usernameKey.isNotBlank() && staff.usernameLower == usernameKey) ||
                    (email.isNotBlank() && staff.email.isNotBlank() && staff.email.equals(email, ignoreCase = true))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isRealAdmin: StateFlow<Boolean> = combine(
        _currentUserId,
        currentUser,
        currentStaffMember
    ) { uid, user, staffRecord ->
        if (uid.isBlank()) {
            false
        } else {
            val session = AuthManager.currentSession()?.takeIf { it.uid == uid }
                ?: AuthManager.loadSavedSession(getApplication())?.takeIf { it.uid == uid }
            val currentAuthEmail = FirebaseAuth.getInstance().currentUser?.takeIf { it.uid == uid }?.email
            val email = user?.email ?: session?.email ?: currentAuthEmail ?: ""
            val username = user?.username ?: session?.username ?: ""
            val isMaster = AuthManager.isMasterAdminIdentity(email = email, username = username, uid = uid)
            if (isMaster) {
                true
            } else if (staffRecord != null) {
                staffRecord.isActive
            } else {
                (user?.isAdmin == true) || (session?.isAdmin == true)
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        run {
            val uid = _currentUserId.value
            val session = AuthManager.currentSession()?.takeIf { it.uid == uid }
            uid.isNotBlank() && (
                AuthManager.isMasterAdminIdentity(email = session?.email, username = session?.username, uid = uid) ||
                session?.isAdmin == true
            )
        }
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val cloudAllUsers: kotlinx.coroutines.flow.Flow<List<UserDto>> = _currentUserId.flatMapLatest { uid ->
        if (uid.isBlank()) {
            flowOf(emptyList())
        } else {
            firestoreRepo.observeAllUsers().onStart { emit(emptyList()) }
        }
    }

    val allUsers: StateFlow<List<UserDto>> = combine(
        cloudAllUsers,
        currentUser,
        _optimisticUserOverride
    ) { usersFromCloud, activeUser, optimistic ->
        val merged = usersFromCloud.map { u ->
            if (optimistic != null && u.id == optimistic.id) {
                u.copy(
                    ffIgn = optimistic.ffIgn.ifBlank { u.ffIgn },
                    ffUid = optimistic.ffUid.ifBlank { u.ffUid }
                )
            } else {
                u
            }
        }.toMutableList()
        val currentOrFallback = activeUser ?: buildSessionFallbackUser(_currentUserId.value)
        if (currentOrFallback != null && currentOrFallback.id.isNotBlank() && merged.none { it.id == currentOrFallback.id }) {
            merged.add(0, currentOrFallback)
        }
        merged
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        listOfNotNull(buildSessionFallbackUser(_currentUserId.value))
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userTransactions: StateFlow<List<TransactionDto>> = _currentUserId.flatMapLatest { uid ->
        if (uid.isBlank()) {
            flowOf(emptyList())
        } else {
            firestoreRepo.observeTransactions(uid)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userParticipations: StateFlow<List<ParticipantDto>> = _currentUserId.flatMapLatest { uid ->
        if (uid.isBlank()) {
            flowOf(emptyList())
        } else {
            firestoreRepo.observeUserParticipations(uid)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topPlayers: StateFlow<List<UserDto>> = combine(allUsers, userParticipations) { users, myParticipations ->
        val myWinCountFromParticipations = myParticipations.count { it.placement == 1 || (it.placement == 0 && it.prizeWon > 0) }
        val activeUid = _currentUserId.value
        users
            .filter { !it.isSuspended }
            .map { user ->
                val resolvedWins = if (user.id == activeUid) {
                    maxOf(user.effectiveTournamentWins, myWinCountFromParticipations)
                } else {
                    user.effectiveTournamentWins
                }
                user.copy(totalWins = resolvedWins)
            }
            .sortedWith(
                compareByDescending<UserDto> { it.totalWins }
                    .thenByDescending { it.totalWinnings }
                    .thenByDescending { it.totalKills }
                    .thenByDescending { it.matchesPlayed }
                    .thenBy { it.username.lowercase() }
            )
            .take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userWithdrawals: StateFlow<List<WithdrawalRequestDto>> = _currentUserId.flatMapLatest { uid ->
        if (uid.isBlank()) {
            flowOf(emptyList())
        } else {
            firestoreRepo.observeUserWithdrawals(uid)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allWithdrawals: StateFlow<List<WithdrawalRequestDto>> = combine(_currentUserId, isRealAdmin) { uid, isAdmin ->
        uid to (isAdmin || isUserRealAdmin())
    }.flatMapLatest { (uid, isAdmin) ->
        if (!isAdmin || uid.isBlank()) {
            flowOf(emptyList())
        } else {
            firestoreRepo.observeAllWithdrawals()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val pendingWithdrawalsCount: StateFlow<Int> = allWithdrawals.flatMapLatest { list ->
        flowOf(list.count { it.status == "PENDING" })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Support Chat StateFlows (User & Admin)
    private val _selectedAdminSupportUserId = MutableStateFlow<String?>(null)
    val selectedAdminSupportUserId: StateFlow<String?> = _selectedAdminSupportUserId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userSupportThread: StateFlow<SupportThreadDto?> = _currentUserId.flatMapLatest { uid ->
        if (uid.isBlank()) {
            flowOf(null)
        } else {
            firestoreRepo.observeSupportThread(uid)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userSupportMessages: StateFlow<List<SupportMessageDto>> = _currentUserId.flatMapLatest { uid ->
        if (uid.isBlank()) {
            flowOf(emptyList())
        } else {
            firestoreRepo.observeSupportMessages(uid)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userUnreadSupportCount: StateFlow<Int> = userSupportThread.flatMapLatest { thread ->
        flowOf(thread?.unreadByUser ?: 0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allSupportThreads: StateFlow<List<SupportThreadDto>> = combine(_currentUserId, isRealAdmin) { uid, isAdmin ->
        uid to (isAdmin || isUserRealAdmin())
    }.flatMapLatest { (uid, isAdmin) ->
        if (!isAdmin || uid.isBlank()) {
            flowOf(emptyList())
        } else {
            firestoreRepo.observeAllSupportThreads()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val adminUnreadSupportCount: StateFlow<Int> = allSupportThreads.flatMapLatest { threads ->
        flowOf(threads.count { it.unreadByAdmin > 0 })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedAdminSupportThread: StateFlow<SupportThreadDto?> = combine(_currentUserId, _selectedAdminSupportUserId) { activeUid, targetUid ->
        activeUid to targetUid
    }.flatMapLatest { (activeUid, targetUid) ->
        if (targetUid.isNullOrBlank() || activeUid.isBlank()) {
            flowOf(null)
        } else {
            firestoreRepo.observeSupportThread(targetUid)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedAdminSupportMessages: StateFlow<List<SupportMessageDto>> = combine(_currentUserId, _selectedAdminSupportUserId) { activeUid, targetUid ->
        activeUid to targetUid
    }.flatMapLatest { (activeUid, targetUid) ->
        if (targetUid.isNullOrBlank() || activeUid.isBlank()) {
            flowOf(emptyList())
        } else {
            firestoreRepo.observeSupportMessages(targetUid)
        }
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
    private val activeNotificationsSource: kotlinx.coroutines.flow.Flow<List<NotificationDto>> = _currentUserId.flatMapLatest { uid ->
        if (uid.isBlank()) {
            flowOf(emptyList())
        } else {
            firestoreRepo.observeNotifications()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allNotifications: StateFlow<List<NotificationDto>> = combine(
        activeNotificationsSource,
        userNotifFilterState
    ) { sourceList, filterState ->
        sourceList.filter { notif ->
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
    val selectedTournament: StateFlow<TournamentDto?> = combine(_currentUserId, _selectedTournamentId) { uid, selectedId ->
        uid to selectedId
    }.flatMapLatest { (uid, selectedId) ->
        if (selectedId.isNullOrBlank() || uid.isBlank()) {
            flowOf(null)
        } else {
            firestoreRepo.observeTournament(selectedId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedTournamentParticipants: StateFlow<List<ParticipantDto>> = combine(_currentUserId, _selectedTournamentId) { uid, selectedId ->
        uid to selectedId
    }.flatMapLatest { (uid, selectedId) ->
        if (selectedId.isNullOrBlank() || uid.isBlank()) {
            flowOf(emptyList())
        } else {
            firestoreRepo.observeParticipants(selectedId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun isUserMainAdmin(): Boolean {
        val uid = _currentUserId.value
        if (uid.isBlank()) return false
        val session = AuthManager.currentSession()?.takeIf { it.uid == uid }
            ?: AuthManager.loadSavedSession(getApplication())?.takeIf { it.uid == uid }
        val currentAuthEmail = FirebaseAuth.getInstance().currentUser?.takeIf { it.uid == uid }?.email
        val userDoc = currentUser.value
        val email = userDoc?.email ?: session?.email ?: currentAuthEmail ?: ""
        val username = userDoc?.username ?: session?.username ?: ""
        return AuthManager.isMasterAdminIdentity(email = email, username = username, uid = uid)
    }

    fun resolveCurrentStaffMember(): StaffMemberDto? {
        val uid = _currentUserId.value
        if (uid.isBlank()) return null
        currentStaffMember.value?.let { return it }
        val list = staffMembers.value
        if (list.isEmpty()) return null
        val session = AuthManager.currentSession()?.takeIf { it.uid == uid }
            ?: AuthManager.loadSavedSession(getApplication())?.takeIf { it.uid == uid }
        val currentAuthEmail = FirebaseAuth.getInstance().currentUser?.takeIf { it.uid == uid }?.email
        val userDoc = currentUser.value
        val email = (userDoc?.email ?: session?.email ?: currentAuthEmail ?: "").trim().lowercase()
        val username = (userDoc?.username ?: session?.username ?: "").trim()
        val usernameKey = AuthManager.normalizeUsernameKey(username)
        return list.firstOrNull { staff ->
            (staff.userId.isNotBlank() && staff.userId == uid) ||
                (usernameKey.isNotBlank() && staff.usernameLower == usernameKey) ||
                (email.isNotBlank() && staff.email.isNotBlank() && staff.email.equals(email, ignoreCase = true))
        }
    }

    fun isUserRealAdmin(): Boolean {
        if (isUserMainAdmin()) return true
        val staff = resolveCurrentStaffMember()
        if (staff != null) return staff.isActive
        val uid = _currentUserId.value
        if (uid.isBlank()) return false
        val session = AuthManager.currentSession()?.takeIf { it.uid == uid }
            ?: AuthManager.loadSavedSession(getApplication())?.takeIf { it.uid == uid }
        val userDoc = currentUser.value
        return (userDoc?.isAdmin == true) || (session?.isAdmin == true)
    }

    fun canAccessMatchManagement(): Boolean {
        if (isUserMainAdmin()) return true
        val staff = resolveCurrentStaffMember()
        return if (staff != null) staff.isActive && staff.canCreateMatch else isUserRealAdmin()
    }

    fun canAccessBalanceManagement(): Boolean {
        if (isUserMainAdmin()) return true
        val staff = resolveCurrentStaffMember()
        return if (staff != null) staff.isActive && staff.canManageBalance else isUserRealAdmin()
    }

    fun canAccessSupportManagement(): Boolean {
        if (isUserMainAdmin()) return true
        val staff = resolveCurrentStaffMember()
        return if (staff != null) staff.isActive && staff.canManageSupport else isUserRealAdmin()
    }

    fun canAccessBroadcast(): Boolean {
        if (isUserMainAdmin()) return true
        val staff = resolveCurrentStaffMember()
        return if (staff != null) staff.isActive && staff.canBroadcast else isUserRealAdmin()
    }

    fun canAccessWithdrawalManagement(): Boolean {
        if (isUserMainAdmin()) return true
        val staff = resolveCurrentStaffMember()
        return if (staff != null) staff.isActive && staff.canManageWithdrawals else isUserRealAdmin()
    }

    fun canAccessSettingsManagement(): Boolean {
        if (isUserMainAdmin()) return true
        val staff = resolveCurrentStaffMember()
        return if (staff != null) staff.isActive && staff.canManageSettings else isUserRealAdmin()
    }

    @Volatile
    private var isWipingData = false

    init {
        val savedSession = AuthManager.loadSavedSession(getApplication())
        prefs.edit().putBoolean("complete_factory_wipe_v3", true).apply()

        val activeSession = savedSession ?: FirebaseAuth.getInstance().currentUser?.let { fbUser ->
            AuthenticatedSession(
                uid = fbUser.uid,
                username = fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "Player",
                email = fbUser.email ?: "",
                isAdmin = AuthManager.isMasterAdminIdentity(email = fbUser.email, username = fbUser.displayName, uid = fbUser.uid)
            )
        }
        if (activeSession != null) {
            viewModelScope.launch {
                firestoreRepo.cleanupDemoData()
                firestoreRepo.initAppSettingsIfMissing()
            }
        }
        _locallyReadNotifIds.value = loadLocalReadNotifs(_currentUserId.value)
        _locallyClearedNotifIds.value = loadLocalClearedNotifs(_currentUserId.value)
        if (activeSession != null) {
            onSessionAuthenticated(activeSession, isExplicitSignIn = false)
        }

        val appStartTime = System.currentTimeMillis()

        // Sync active user ID and background sync scheduling (only for real authenticated users)
        viewModelScope.launch {
            _currentUserId.collect { uid ->
                if (uid.isNotBlank() && !uid.startsWith("demo_")) {
                    prefs.edit().putString("last_active_uid", uid).apply()
                    com.example.notification.NotificationBackgroundManager.scheduleBackgroundSync(getApplication())
                }
            }
        }

        // Automatically sync Cloud AppSettings Ads Account Details with UnityAdsManager in real time
        viewModelScope.launch {
            appSettings.collect { settings ->
                if (settings != null) {
                    com.example.data.ads.UnityAdsManager.configure(
                        context = getApplication(),
                        gameId = settings.unityGameId,
                        placementId = settings.unityPlacementId,
                        interstitialPlacementId = settings.unityInterstitialId,
                        bannerPlacementId = settings.unityBannerId,
                        accountName = settings.adsAccountName,
                        accountEmail = settings.adsAccountEmail,
                        organizationId = settings.adsOrganizationId,
                        testMode = settings.unityTestMode,
                        adsEnabled = settings.adsEnabled,
                        dailyAdLimit = settings.dailyAdLimit,
                        rewardPerAd = settings.adRewardAmount
                    )
                }
            }
        }

        // Sync joined tournament IDs to SharedPreferences, subscribe to FCM topics, and schedule 10-minute pre-match reminders
        viewModelScope.launch {
            combine(_currentUserId, userParticipations, allTournaments) { uid, participations, tournaments ->
                Triple(uid, participations, tournaments)
            }.collect { (uid, participations, tournaments) ->
                if (uid.isNotBlank() && !uid.startsWith("demo_")) {
                    val joinedIds = participations.map { it.tournamentId }.filter { it.isNotBlank() }.toSet()
                    prefs.edit().putStringSet("joined_tournaments_$uid", joinedIds).apply()
                    com.example.notification.TournamentFirebaseMessagingService.syncTokenAndTopicsForUser(
                        context = getApplication(),
                        userId = uid,
                        joinedTournamentIds = joinedIds
                    )
                    com.example.notification.NotificationBackgroundManager.scheduleTournamentTenMinuteReminders(
                        context = getApplication(),
                        userId = uid,
                        tournaments = tournaments,
                        joinedTournamentIds = joinedIds
                    )
                }
            }
        }

        // Active 30-second countdown monitor for 10-minute pre-tournament reminders while app is open
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(30_000L)
                val uid = _currentUserId.value
                if (uid.isNotBlank() && !uid.startsWith("demo_")) {
                    val joinedIds = userParticipations.value.map { it.tournamentId }.filter { it.isNotBlank() }.toSet()
                    if (joinedIds.isNotEmpty()) {
                        val now = System.currentTimeMillis()
                        val upcomingJoined = allTournaments.value.filter { t ->
                            joinedIds.contains(t.id) && t.status.equals("UPCOMING", ignoreCase = true)
                        }
                        for (tournament in upcomingJoined) {
                            val startMs = com.example.notification.NotificationBackgroundManager.resolveTournamentStartEpochMs(tournament)
                            val timeUntilStart = startMs - now
                            val reminderKey = "reminded_10m_${uid}_${tournament.id}"
                            if (startMs > 0L &&
                                timeUntilStart in 1L..com.example.notification.NotificationBackgroundManager.TEN_MINUTES_MS &&
                                !prefs.getBoolean(reminderKey, false)
                            ) {
                                com.example.notification.NotificationBackgroundManager.dispatchTenMinuteReminder(
                                    context = getApplication(),
                                    userId = uid,
                                    tournamentId = tournament.id,
                                    tournamentTitle = tournament.title,
                                    matchType = tournament.matchType,
                                    mapName = tournament.map
                                )
                                _liveAlert.emit(
                                    UiNotificationAlert(
                                        id = "NOTIF-10M-${tournament.id.takeLast(8).uppercase()}",
                                        title = "⏰ Match Starts in 10 Minutes!",
                                        message = "${tournament.title} (${tournament.matchType} • ${tournament.map}) starts in 10 minutes! Check Lobby for Room ID & Password.",
                                        roomId = tournament.roomId,
                                        roomPass = tournament.roomPassword
                                    )
                                )
                            }
                        }
                    }
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

    fun onSessionAuthenticated(session: AuthenticatedSession, isExplicitSignIn: Boolean = false) {
        _currentUserId.value = session.uid
        if (session.ffIgn.isNotBlank() || session.ffUid.isNotBlank()) {
            val curOpt = _optimisticUserOverride.value?.takeIf { it.id == session.uid }
            _optimisticUserOverride.value = (curOpt ?: buildSessionFallbackUser(session.uid))?.copy(
                id = session.uid,
                ffIgn = session.ffIgn.ifBlank { curOpt?.ffIgn ?: "" },
                ffUid = session.ffUid.ifBlank { curOpt?.ffUid ?: "" }
            )
        }
        if (isWipingData) {
            return
        }
        prefs.edit().remove("session_wiped_${session.uid}").commit()

        _locallyReadNotifIds.value = loadLocalReadNotifs(session.uid)
        _locallyClearedNotifIds.value = loadLocalClearedNotifs(session.uid)

        val deviceId = try {
            android.provider.Settings.Secure.getString(
                getApplication<Application>().contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: ""
        } catch (_: Exception) {
            ""
        }

        viewModelScope.launch {
            val isUserAdmin = AuthManager.isMasterAdminIdentity(
                email = session.email,
                username = session.username,
                uid = session.uid
            ) || session.isAdmin
            val syncedUser = firestoreRepo.syncUserProfile(
                userId = session.uid,
                email = session.email,
                displayName = session.username.ifBlank { "Player" },
                isAdmin = isUserAdmin,
                deviceId = deviceId,
                ffIgn = session.ffIgn,
                ffUid = session.ffUid,
                passwordHash = session.passwordHash
            )
            if (syncedUser.ffIgn.isNotBlank() || syncedUser.ffUid.isNotBlank()) {
                AuthManager.updateSessionGamingProfile(
                    context = getApplication(),
                    ffIgn = syncedUser.ffIgn,
                    ffUid = syncedUser.ffUid
                )
            }
            prefs.edit().putBoolean("user_synced_${session.uid}", true).commit()
            firestoreRepo.initAppSettingsIfMissing()
            if (isExplicitSignIn) {
                _toastMessage.emit("Signed in as ${session.username.ifBlank { session.email }}")
            }
        }
    }

    fun onUserAuthenticated(user: com.google.firebase.auth.FirebaseUser, isExplicitSignIn: Boolean = false) {
        val session = AuthenticatedSession(
            uid = user.uid,
            username = user.displayName ?: user.email?.substringBefore("@") ?: "Player",
            email = user.email ?: "",
            isAdmin = AuthManager.isMasterAdminIdentity(email = user.email, username = user.displayName, uid = user.uid)
        )
        onSessionAuthenticated(session, isExplicitSignIn)
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
                screen is Screen.AdminStaff ||
                screen is Screen.AdminAuth
        if (isAdminScreen && !isUserRealAdmin()) {
            showToast("Access Denied: Only authorized Admin & Staff accounts can access the Admin Portal.")
            return
        }
        when (screen) {
            is Screen.AdminStaff -> {
                if (!isUserMainAdmin()) {
                    showToast("Permission Denied: Only the Main Admin can manage staff & permissions.")
                    return
                }
            }
            is Screen.AdminTournaments, is Screen.AdminTournamentEditor, is Screen.AdminSettlement -> {
                if (!canAccessMatchManagement()) {
                    showToast("Permission Denied: 'Create & Manage Match' is disabled for your staff role.")
                    return
                }
            }
            is Screen.AdminUsers -> {
                if (!canAccessBalanceManagement()) {
                    showToast("Permission Denied: 'Manage Balance & Users' is disabled for your staff role.")
                    return
                }
            }
            is Screen.AdminSupportInbox, is Screen.AdminSupportChat -> {
                if (!canAccessSupportManagement()) {
                    showToast("Permission Denied: 'Manage Support Messages' is disabled for your staff role.")
                    return
                }
            }
            is Screen.AdminWithdrawals -> {
                if (!canAccessWithdrawalManagement()) {
                    showToast("Permission Denied: 'Withdrawal Management' is disabled for your staff role.")
                    return
                }
            }
            is Screen.AdminSettings -> {
                if (!canAccessSettingsManagement()) {
                    showToast("Permission Denied: 'Application & Ads Settings' is disabled for your staff role.")
                    return
                }
            }
            else -> {}
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
        if (!canAccessSupportManagement()) {
            showToast("Permission Denied: 'Manage Support Messages' is disabled for your staff role.")
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
        if (!canAccessSupportManagement()) {
            onComplete(false, "Permission Denied: 'Manage Support Messages' is disabled for your staff role.")
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
        saveUpiId: Boolean = true,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val uid = _currentUserId.value
        if (uid.isBlank()) {
            onComplete(false, "Please sign in to withdraw balance.")
            return
        }
        val cleanUpi = upiId.trim()
        if (saveUpiId && cleanUpi.isNotBlank()) {
            val user = currentUser.value
            if (user != null) {
                _optimisticUserOverride.value = user.copy(savedUpiId = cleanUpi)
            }
        }
        viewModelScope.launch {
            val res = firestoreRepo.requestWithdrawal(
                userId = uid,
                amount = amount,
                upiId = cleanUpi,
                accountHolderName = accountHolderName,
                saveUpiId = saveUpiId
            )
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

    // User: Save or Update Saved UPI ID for future withdrawals
    fun saveUserUpiId(upiId: String, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        val uid = _currentUserId.value
        if (uid.isBlank()) {
            onComplete(false, "Please sign in to save UPI ID")
            return
        }
        val cleanUpi = upiId.trim()
        if (!cleanUpi.contains("@") || cleanUpi.length < 5) {
            val msg = "Please enter a valid UPI ID (e.g. yourname@upi)"
            showToast(msg)
            onComplete(false, msg)
            return
        }
        val user = currentUser.value
        if (user != null) {
            _optimisticUserOverride.value = user.copy(savedUpiId = cleanUpi)
        }
        viewModelScope.launch {
            val res = firestoreRepo.saveUserUpiId(uid, cleanUpi)
            res.fold(
                onSuccess = { msg ->
                    _toastMessage.emit(msg)
                    onComplete(true, msg)
                },
                onFailure = { err ->
                    val msg = err.message ?: "Failed to save UPI ID"
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
        if (!canAccessWithdrawalManagement()) {
            onComplete(false, "Permission Denied: 'Withdrawal Management' is disabled for your staff role.")
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
        if (!canAccessWithdrawalManagement()) {
            onComplete(false, "Permission Denied: 'Withdrawal Management' is disabled for your staff role.")
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
            val userId = user?.id?.takeIf { it.isNotBlank() }
                ?: _currentUserId.value.ifBlank {
                    AuthManager.currentSession()?.uid
                        ?: FirebaseAuth.getInstance().currentUser?.uid
                        ?: ""
                }
            if (userId.isBlank()) {
                _toastMessage.emit("Please sign in to register")
                return@launch
            }
            val cleanIgn = ign.trim()
            val cleanUid = uid.trim()
            if (cleanIgn.isNotBlank() || cleanUid.isNotBlank()) {
                AuthManager.updateSessionGamingProfile(getApplication(), cleanIgn, cleanUid)
                val baseUser = user ?: buildSessionFallbackUser(userId) ?: UserDto(id = userId)
                _optimisticUserOverride.value = baseUser.copy(
                    id = userId,
                    ffIgn = cleanIgn.ifBlank { baseUser.ffIgn },
                    ffUid = cleanUid.ifBlank { baseUser.ffUid }
                )
            }
            val res = firestoreRepo.joinTournament(
                tournamentId = tournamentId,
                userId = userId,
                ffIgn = cleanIgn,
                ffUid = cleanUid,
                username = user?.username?.ifBlank { cleanIgn } ?: cleanIgn,
                chosenSlot = chosenSlot
            )
            res.onSuccess { msg ->
                val bookedSlot = Regex("Slot #(\\d+)").find(msg)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: chosenSlot
                    ?: 1
                val tournament = allTournaments.value.firstOrNull { it.id == tournamentId }
                    ?: selectedTournament.value
                _registrationSuccessEvent.value = RegistrationSuccessEvent(
                    tournamentId = tournamentId,
                    tournamentTitle = tournament?.title ?: "Free Fire Custom Match",
                    slotNumber = bookedSlot,
                    entryFee = tournament?.entryFee ?: 0,
                    matchType = tournament?.matchType ?: "SOLO",
                    mapName = tournament?.map ?: "Bermuda",
                    playerIgn = cleanIgn
                )
                _toastMessage.emit(msg)
                _liveAlert.emit(UiNotificationAlert("Match Joined! 🎯", msg))
            }.onFailure { err ->
                _toastMessage.emit(err.message ?: "Failed to join")
            }
        }
    }

    fun updateGamingProfile(ign: String, uid: String) {
        val user = currentUser.value
        val userId = user?.id?.takeIf { it.isNotBlank() }
            ?: _currentUserId.value.ifBlank {
                AuthManager.currentSession()?.uid
                    ?: AuthManager.loadSavedSession(getApplication())?.uid
                    ?: FirebaseAuth.getInstance().currentUser?.uid
                    ?: ""
            }
        if (userId.isBlank()) {
            showToast("Please sign in to update profile")
            return
        }
        val cleanIgn = ign.trim().ifBlank { user?.ffIgn ?: "" }
        val cleanUid = uid.trim().ifBlank { user?.ffUid ?: "" }

        // Immediately update local session and optimistic state so UI reflects change at 0ms latency
        AuthManager.updateSessionGamingProfile(getApplication(), cleanIgn, cleanUid)
        val baseUser = user ?: buildSessionFallbackUser(userId) ?: UserDto(id = userId)
        _optimisticUserOverride.value = baseUser.copy(
            id = userId,
            ffIgn = cleanIgn,
            ffUid = cleanUid
        )

        viewModelScope.launch {
            val res = firestoreRepo.updateUserGamingProfile(userId, cleanIgn, cleanUid)
            res.onSuccess { msg ->
                _toastMessage.emit(msg)
            }.onFailure { err ->
                _toastMessage.emit(err.message ?: "Saved locally; cloud sync pending")
            }
        }
    }

    fun adminUpdateUserGamingProfile(targetUserId: String, ign: String, uid: String) {
        if (!isUserRealAdmin()) {
            showToast("Admin permission required.")
            return
        }
        val cleanIgn = ign.trim()
        val cleanUid = uid.trim()
        val existingTarget = allUsers.value.firstOrNull { it.id == targetUserId }
            ?: currentUser.value?.takeIf { it.id == targetUserId }
            ?: buildSessionFallbackUser(targetUserId)
            ?: UserDto(id = targetUserId)
        if (targetUserId == _currentUserId.value) {
            AuthManager.updateSessionGamingProfile(getApplication(), cleanIgn, cleanUid)
        }
        _optimisticUserOverride.value = existingTarget.copy(
            id = targetUserId,
            ffIgn = cleanIgn,
            ffUid = cleanUid
        )

        viewModelScope.launch {
            val res = firestoreRepo.updateUserGamingProfile(targetUserId, cleanIgn, cleanUid)
            res.onSuccess {
                _toastMessage.emit("Updated player IGN & UID!")
            }.onFailure { err ->
                _toastMessage.emit(err.message ?: "Failed to update player profile")
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

    // User: Claim reward after watching Unity Rewarded Video Ad
    fun claimAdReward(
        rewardAmount: Int,
        onComplete: (Boolean, String, Double) -> Unit = { _, _, _ -> }
    ) {
        val uid = _currentUserId.value
        if (uid.isBlank()) {
            onComplete(false, "Please sign in to earn ad rewards", 0.0)
            return
        }
        viewModelScope.launch {
            val res = firestoreRepo.claimAdReward(uid, rewardAmount)
            res.fold(
                onSuccess = { claimResult ->
                    val user = currentUser.value
                    val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    if (user != null) {
                        _optimisticUserOverride.value = user.copy(
                            walletBalance = claimResult.newBalance,
                            dailyAdsWatchedCount = claimResult.adsWatchedToday,
                            dailyAdsWatchedDate = todayDateStr
                        )
                    }
                    val formattedAmt = claimResult.amountEarned.formatCurrency()
                    _toastMessage.emit("🎉 +₹$formattedAmt added to your wallet! (${claimResult.remainingAdsToday} ads remaining today)")
                    _liveAlert.emit(UiNotificationAlert("Ad Reward Unlocked! 🎬", "+₹$formattedAmt credited to deposit balance."))
                    onComplete(true, "₹$formattedAmt added to your wallet!", claimResult.amountEarned)
                },
                onFailure = { err ->
                    val msg = err.message ?: "Failed to claim ad reward"
                    _toastMessage.emit(msg)
                    onComplete(false, msg, 0.0)
                }
            )
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
        if (!canAccessMatchManagement()) {
            showToast("Permission Denied: 'Create & Manage Match' is disabled for your staff role.")
            return
        }
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
        if (!canAccessMatchManagement()) {
            showToast("Permission Denied: 'Create & Manage Match' is disabled for your staff role.")
            return
        }
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
        if (!canAccessMatchManagement()) {
            showToast("Permission Denied: 'Create & Manage Match' is disabled for your staff role.")
            return
        }
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
        if (!canAccessMatchManagement()) {
            showToast("Permission Denied: 'Create & Manage Match' is disabled for your staff role.")
            return
        }
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
        if (!canAccessMatchManagement()) {
            showToast("Permission Denied: 'Create & Manage Match' is disabled for your staff role.")
            return
        }
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
        if (!canAccessBalanceManagement()) {
            showToast("Permission Denied: 'Manage Balance & Users' is disabled for your staff role.")
            return
        }
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
        if (!canAccessBalanceManagement()) {
            showToast("Permission Denied: 'Manage Balance & Users' is disabled for your staff role.")
            return
        }
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
        if (!canAccessBalanceManagement()) {
            showToast("Permission Denied: 'Manage Balance & Users' is disabled for your staff role.")
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
        if (!isUserMainAdmin()) {
            val msg = "Permission Denied: Only the Main Admin can wipe all platform data."
            showToast(msg)
            onComplete(false, msg)
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
        if (!canAccessSettingsManagement()) {
            showToast("Permission Denied: 'Application & Ads Settings' is disabled for your staff role.")
            return
        }
        com.example.data.ads.UnityAdsManager.configure(
            context = getApplication(),
            gameId = settings.unityGameId,
            placementId = settings.unityPlacementId,
            interstitialPlacementId = settings.unityInterstitialId,
            bannerPlacementId = settings.unityBannerId,
            accountName = settings.adsAccountName,
            accountEmail = settings.adsAccountEmail,
            organizationId = settings.adsOrganizationId,
            testMode = settings.unityTestMode,
            adsEnabled = settings.adsEnabled,
            dailyAdLimit = settings.dailyAdLimit,
            rewardPerAd = settings.adRewardAmount
        )
        viewModelScope.launch {
            val res = firestoreRepo.updateAppSettings(settings)
            res.onSuccess { msg ->
                _toastMessage.emit(msg)
            }.onFailure { err ->
                _toastMessage.emit(err.message ?: "Failed to save settings")
            }
        }
    }

    fun updateAdsAccountDetails(
        adsEnabled: Boolean,
        adsAccountName: String,
        adsAccountEmail: String,
        adsOrganizationId: String,
        unityGameId: String,
        unityPlacementId: String,
        unityInterstitialId: String,
        unityBannerId: String,
        unityTestMode: Boolean,
        dailyAdLimit: Int,
        adRewardAmount: Double
    ) {
        if (!canAccessSettingsManagement()) {
            showToast("Permission Denied: 'Application & Ads Settings' is disabled for your staff role.")
            return
        }
        val base = appSettings.value ?: AppSettingsDto(id = "main")
        val updated = base.copy(
            id = "main",
            adsEnabled = adsEnabled,
            adsAccountName = adsAccountName.trim().ifBlank { com.example.data.ads.UnityAdsManager.DEFAULT_ACCOUNT_NAME },
            adsAccountEmail = adsAccountEmail.trim().ifBlank { com.example.data.ads.UnityAdsManager.DEFAULT_ACCOUNT_EMAIL },
            adsOrganizationId = adsOrganizationId.trim(),
            unityGameId = com.example.data.ads.UnityAdsManager.normalizeGameId(unityGameId),
            unityPlacementId = com.example.data.ads.UnityAdsManager.normalizePlacementId(unityPlacementId),
            unityInterstitialId = unityInterstitialId.trim(),
            unityBannerId = unityBannerId.trim(),
            unityTestMode = unityTestMode,
            dailyAdLimit = dailyAdLimit.coerceIn(1, 50),
            adRewardAmount = adRewardAmount.takeIf { it > 0.0 } ?: com.example.data.ads.UnityAdsManager.DEFAULT_REWARD_PER_AD
        )
        com.example.data.ads.UnityAdsManager.configure(
            context = getApplication(),
            gameId = updated.unityGameId,
            placementId = updated.unityPlacementId,
            interstitialPlacementId = updated.unityInterstitialId,
            bannerPlacementId = updated.unityBannerId,
            accountName = updated.adsAccountName,
            accountEmail = updated.adsAccountEmail,
            organizationId = updated.adsOrganizationId,
            testMode = updated.unityTestMode,
            adsEnabled = updated.adsEnabled,
            dailyAdLimit = updated.dailyAdLimit,
            rewardPerAd = updated.adRewardAmount
        )
        viewModelScope.launch {
            val res = firestoreRepo.updateAppSettings(updated)
            res.onSuccess {
                _toastMessage.emit("Ads account details saved & synced with Unity Ads SDK! 🎬")
                _liveAlert.emit(
                    UiNotificationAlert(
                        title = "🎬 Ads Account Updated",
                        message = "Game ID ${updated.unityGameId} (${updated.unityPlacementId}) active in ${if (updated.unityTestMode) "Test" else "Live Real Ads"} mode."
                    )
                )
            }.onFailure { err ->
                _toastMessage.emit(err.message ?: "Saved locally; cloud sync failed")
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

    fun broadcastCustomNotification(
        title: String,
        message: String,
        type: String = "ANNOUNCEMENT",
        targetUserId: String? = null,
        targetTournamentId: String? = null,
        roomId: String? = null,
        roomPassword: String? = null
    ) {
        if (!canAccessBroadcast()) {
            showToast("Permission Denied: 'Broadcast Alerts' is disabled for your staff role.")
            return
        }
        viewModelScope.launch {
            val res = firestoreRepo.broadcastNotification(
                title = title,
                message = message,
                type = type,
                targetUserId = targetUserId,
                targetTournamentId = targetTournamentId,
                roomId = roomId,
                roomPassword = roomPassword
            )
            res.onSuccess { msg ->
                _toastMessage.emit(msg)
                PushNotificationHelper.showPushNotification(
                    context = getApplication(),
                    notificationId = System.currentTimeMillis().toInt(),
                    title = title,
                    message = message,
                    type = type
                )
                _liveAlert.emit(
                    UiNotificationAlert(
                        title = title,
                        message = message,
                        roomId = roomId,
                        roomPass = roomPassword
                    )
                )
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

    // Main Admin: Add or Update a Staff Member and their role permissions
    fun saveOrUpdateStaffMember(
        staff: StaffMemberDto,
        plainPassword: String = "",
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!isUserMainAdmin()) {
            val msg = "Only the Main Admin can add or edit staff permissions."
            showToast(msg)
            onComplete(false, msg)
            return
        }
        viewModelScope.launch {
            val res = firestoreRepo.saveOrUpdateStaffMember(staff, plainPassword)
            res.fold(
                onSuccess = { msg ->
                    _toastMessage.emit(msg)
                    onComplete(true, msg)
                },
                onFailure = { err ->
                    val msg = err.message ?: "Failed to save staff member"
                    _toastMessage.emit(msg)
                    onComplete(false, msg)
                }
            )
        }
    }

    // Main Admin: Quickly toggle a single permission for a staff member
    fun toggleStaffPermission(
        staff: StaffMemberDto,
        permissionKey: String,
        enabled: Boolean
    ) {
        if (!isUserMainAdmin()) {
            showToast("Only the Main Admin can modify staff permissions.")
            return
        }
        val updated = when (permissionKey) {
            "canCreateMatch" -> staff.copy(canCreateMatch = enabled)
            "canManageBalance" -> staff.copy(canManageBalance = enabled)
            "canManageSupport" -> staff.copy(canManageSupport = enabled)
            "canBroadcast" -> staff.copy(canBroadcast = enabled)
            "canManageWithdrawals" -> staff.copy(canManageWithdrawals = enabled)
            "canManageSettings" -> staff.copy(canManageSettings = enabled)
            else -> staff
        }
        saveOrUpdateStaffMember(updated)
    }

    // Main Admin: Enable or Disable a staff member's access
    fun toggleStaffActiveStatus(staff: StaffMemberDto, isActive: Boolean) {
        if (!isUserMainAdmin()) {
            showToast("Only the Main Admin can change staff status.")
            return
        }
        saveOrUpdateStaffMember(staff.copy(isActive = isActive))
    }

    // Main Admin: Delete a staff member
    fun deleteStaffMember(staffId: String) {
        if (!isUserMainAdmin()) {
            showToast("Only the Main Admin can remove staff members.")
            return
        }
        viewModelScope.launch {
            val res = firestoreRepo.deleteStaffMember(staffId)
            res.onSuccess { msg ->
                _toastMessage.emit(msg)
            }.onFailure { err ->
                _toastMessage.emit(err.message ?: "Failed to remove staff member")
            }
        }
    }
}
