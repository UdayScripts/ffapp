package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.RealTimeNotificationBanner
import com.example.ui.screens.admin.AdminAppSettingsScreen
import com.example.ui.screens.admin.AdminAuthScreen
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.admin.AdminSettlementScreen
import com.example.ui.screens.admin.AdminTournamentEditorScreen
import com.example.ui.screens.admin.AdminTournamentManagerScreen
import com.example.ui.screens.admin.AdminUserManagerScreen
import com.example.ui.screens.player.DashboardScreen
import com.example.ui.screens.player.HistoryScreen
import com.example.ui.screens.player.NotificationCenterScreen
import com.example.ui.screens.player.ProfileScreen
import com.example.ui.screens.player.TournamentDetailScreen
import com.example.ui.screens.player.WalletScreen
import com.example.ui.theme.FFTournamentsTheme
import com.example.ui.theme.FireOrange
import com.example.ui.theme.HighContrastDark
import com.example.ui.theme.SlateLightBg
import com.example.ui.theme.TextSecondaryMuted
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.Screen
import com.example.viewmodel.UiNotificationAlert
import kotlinx.coroutines.flow.collectLatest

import androidx.compose.runtime.DisposableEffect
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.notification.PushNotificationHelper
import com.example.ui.screens.auth.SignInScreen
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PushNotificationHelper.createNotificationChannel(this)
        enableEdgeToEdge()
        setContent {
            FFTournamentsTheme {
                RootAppGate(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun RootAppGate(viewModel: AppViewModel) {
    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

    // Request POST_NOTIFICATIONS on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission granted or denied */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            auth.currentUser?.let { user ->
                viewModel.onUserAuthenticated(user)
            }
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        onDispose {
            FirebaseAuth.getInstance().removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        SignInScreen(
            onAuthSuccess = { user ->
                currentUser = user
                viewModel.onUserAuthenticated(user)
            }
        )
    } else {
        MainAppContent(viewModel = viewModel)
    }
}

@Composable
fun MainAppContent(viewModel: AppViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val unreadNotifs by viewModel.unreadNotifCount.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var currentLiveAlert by remember { mutableStateOf<UiNotificationAlert?>(null) }

    // Collect Toast messages
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Collect Real-time notification banners
    LaunchedEffect(Unit) {
        viewModel.liveAlert.collectLatest { alert ->
            currentLiveAlert = alert
        }
    }

    // Hardware BackHandler
    BackHandler(enabled = currentScreen != Screen.Dashboard) {
        viewModel.navigateBack()
    }

    // Whether bottom navigation bar should be visible (on core player screens)
    val showBottomBar = currentScreen in listOf(
        Screen.Dashboard,
        Screen.History,
        Screen.Wallet,
        Screen.Notifications,
        Screen.Profile
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars).testTag("bottom_nav_bar"),
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen == Screen.Dashboard,
                        onClick = { viewModel.navigateTo(Screen.Dashboard) },
                        icon = { Icon(Icons.Default.SportsEsports, contentDescription = "Tournaments") },
                        label = { Text("Matches", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = FireOrange,
                            indicatorColor = HighContrastDark,
                            unselectedIconColor = TextSecondaryMuted,
                            unselectedTextColor = TextSecondaryMuted
                        ),
                        modifier = Modifier.testTag("nav_matches")
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.History,
                        onClick = { viewModel.navigateTo(Screen.History) },
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("History", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = FireOrange,
                            indicatorColor = HighContrastDark,
                            unselectedIconColor = TextSecondaryMuted,
                            unselectedTextColor = TextSecondaryMuted
                        ),
                        modifier = Modifier.testTag("nav_history")
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.Wallet,
                        onClick = { viewModel.navigateTo(Screen.Wallet) },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet") },
                        label = { Text("Wallet", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = FireOrange,
                            indicatorColor = HighContrastDark,
                            unselectedIconColor = TextSecondaryMuted,
                            unselectedTextColor = TextSecondaryMuted
                        ),
                        modifier = Modifier.testTag("nav_wallet")
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.Notifications,
                        onClick = { viewModel.navigateTo(Screen.Notifications) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifs > 0) {
                                        Badge(containerColor = FireOrange) {
                                            Text("$unreadNotifs", color = Color.White, fontSize = 9.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "Alerts")
                            }
                        },
                        label = { Text("Alerts", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = FireOrange,
                            indicatorColor = HighContrastDark,
                            unselectedIconColor = TextSecondaryMuted,
                            unselectedTextColor = TextSecondaryMuted
                        ),
                        modifier = Modifier.testTag("nav_alerts")
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.Profile,
                        onClick = { viewModel.navigateTo(Screen.Profile) },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = FireOrange,
                            indicatorColor = HighContrastDark,
                            unselectedIconColor = TextSecondaryMuted,
                            unselectedTextColor = TextSecondaryMuted
                        ),
                        modifier = Modifier.testTag("nav_profile")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Dashboard -> DashboardScreen(viewModel = viewModel)
                Screen.TournamentDetails -> TournamentDetailScreen(viewModel = viewModel)
                Screen.Wallet -> WalletScreen(viewModel = viewModel)
                Screen.History -> HistoryScreen(viewModel = viewModel)
                Screen.Notifications -> NotificationCenterScreen(viewModel = viewModel)
                Screen.Profile -> ProfileScreen(viewModel = viewModel)

                // Admin Screens
                Screen.AdminAuth -> AdminAuthScreen(viewModel = viewModel)
                Screen.AdminDashboard -> AdminDashboardScreen(viewModel = viewModel)
                Screen.AdminTournaments -> AdminTournamentManagerScreen(viewModel = viewModel)
                Screen.AdminTournamentEditor -> AdminTournamentEditorScreen(viewModel = viewModel)
                Screen.AdminSettlement -> AdminSettlementScreen(viewModel = viewModel)
                Screen.AdminUsers -> AdminUserManagerScreen(viewModel = viewModel)
                Screen.AdminSettings -> AdminAppSettingsScreen(viewModel = viewModel)
            }

            // Real-Time Notification Banner Float on top
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                RealTimeNotificationBanner(
                    alert = currentLiveAlert,
                    onDismiss = { currentLiveAlert = null }
                )
            }
        }
    }
}
