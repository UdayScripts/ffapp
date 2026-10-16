package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthManager
import com.example.data.firestore.AppSettingsDto
import com.example.data.firestore.FirestoreRepository
import com.example.data.firestore.NotificationDto
import com.example.data.firestore.ParticipantDto
import com.example.data.firestore.SettleData
import com.example.data.firestore.TournamentDto
import com.example.data.firestore.TransactionDto
import com.example.data.firestore.UserDto
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
    object Wallet : Screen("wallet")
    object History : Screen("history")
    object Notifications : Screen("notifications")
    object Profile : Screen("profile")

    // Admin screens
    object AdminAuth : Screen("admin_auth")
    object AdminDashboard : Screen("admin_dashboard")
    object AdminTournaments : Screen("admin_tournaments")
    object AdminTournamentEditor : Screen("admin_tournament_editor")
    object AdminSettlement : Screen("admin_settlement")
    object AdminUsers : Screen("admin_users")
    object AdminSettings : Screen("admin_settings")
}

data class UiNotificationAlert(
    val title: String,
    val message: String,
    val roomId: String? = null,
    val roomPass: String? = null
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

    val allNotifications: StateFlow<List<NotificationDto>> = firestoreRepo.observeNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotifCount: StateFlow<Int> = allNotifications.flatMapLatest { list ->
        flowOf(list.count { !it.isRead })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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
    val selectedTournament: StateFlow<TournamentDto?> = _selectedTournamentId.flatMapLatest { id ->
        if (id.isNullOrBlank()) flowOf(null) else firestoreRepo.observeTournament(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedTournamentParticipants: StateFlow<List<ParticipantDto>> = _selectedTournamentId.flatMapLatest { id ->
        if (id.isNullOrBlank()) flowOf(emptyList()) else firestoreRepo.observeParticipants(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isRealAdmin: StateFlow<Boolean> = currentUser.flatMapLatest { user ->
        val authEmail = FirebaseAuth.getInstance().currentUser?.email
        val email = user?.email ?: authEmail
        flowOf(email.equals("udaynath381@gmail.com", ignoreCase = true))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun isUserRealAdmin(): Boolean {
        val currentAuthEmail = FirebaseAuth.getInstance().currentUser?.email
        val userDocEmail = currentUser.value?.email
        val email = userDocEmail ?: currentAuthEmail ?: ""
        return email.equals("udaynath381@gmail.com", ignoreCase = true)
    }

    init {
        val authUser = FirebaseAuth.getInstance().currentUser
        if (authUser != null) {
            onUserAuthenticated(authUser)
        }

        // Real-Time Push Notification Engine:
        // Listens to Cloud Firestore notifications stream and dispatches OS push notifications
        viewModelScope.launch {
            var isInitialLoad = true
            val seenNotifIds = mutableSetOf<String>()

            firestoreRepo.observeNotifications().collectLatest { notifs ->
                if (isInitialLoad) {
                    notifs.forEach { seenNotifIds.add(it.id) }
                    isInitialLoad = false
                } else {
                    for (notif in notifs) {
                        if (!seenNotifIds.contains(notif.id)) {
                            seenNotifIds.add(notif.id)
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
                                    title = notif.title,
                                    message = notif.message,
                                    roomId = notif.roomId,
                                    roomPass = notif.roomPassword
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun onUserAuthenticated(user: com.google.firebase.auth.FirebaseUser) {
        viewModelScope.launch {
            _currentUserId.value = user.uid
            val isUserAdmin = user.email.equals("udaynath381@gmail.com", ignoreCase = true)
            firestoreRepo.syncUserProfile(
                userId = user.uid,
                email = user.email ?: "",
                displayName = user.displayName ?: "Player",
                isAdmin = isUserAdmin
            )
            firestoreRepo.seedInitialDataIfEmpty(user.uid)
            _toastMessage.emit("Signed in as ${user.displayName ?: user.email}")
        }
    }

    fun navigateTo(screen: Screen) {
        val isAdminScreen = screen is Screen.AdminDashboard ||
                screen is Screen.AdminTournaments ||
                screen is Screen.AdminTournamentEditor ||
                screen is Screen.AdminSettlement ||
                screen is Screen.AdminUsers ||
                screen is Screen.AdminSettings ||
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

    fun joinTournament(tournamentId: String, ign: String, uid: String) {
        viewModelScope.launch {
            val user = currentUser.value
            val res = firestoreRepo.joinTournament(
                tournamentId = tournamentId,
                userId = _currentUserId.value,
                ffIgn = ign,
                ffUid = uid,
                username = user?.username ?: "Player"
            )
            res.onSuccess { msg ->
                _toastMessage.emit(msg)
                _liveAlert.emit(UiNotificationAlert("Match Joined! 🎯", msg))
            }.onFailure { err ->
                _toastMessage.emit(err.message ?: "Failed to join")
            }
        }
    }

    fun depositMoney(amount: Int, paymentMethod: String) {
        viewModelScope.launch {
            val res = firestoreRepo.depositMoney(_currentUserId.value, amount, paymentMethod)
            res.onSuccess { msg ->
                _toastMessage.emit(msg)
                _liveAlert.emit(UiNotificationAlert("Deposit Credited! 💳", "₹$amount added to live Firestore wallet."))
            }.onFailure { err ->
                _toastMessage.emit(err.message ?: "Deposit failed")
            }
        }
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
                _liveAlert.emit(UiNotificationAlert("Match Completed! 🏆", "Results settled & prizes disbursed on Cloud Firestore."))
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
        viewModelScope.launch {
            firestoreRepo.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            firestoreRepo.markAllNotificationsAsRead(allNotifications.value)
            _toastMessage.emit("All notifications marked as read")
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
