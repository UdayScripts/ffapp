package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notification.PushNotificationHelper
import com.example.ui.components.RealTimeNotificationBanner
import com.example.ui.screens.admin.AdminAppSettingsScreen
import com.example.ui.screens.admin.AdminAuthScreen
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.admin.AdminSettlementScreen
import com.example.ui.screens.admin.AdminSupportChatScreen
import com.example.ui.screens.admin.AdminSupportInboxScreen
import com.example.ui.screens.admin.AdminTournamentEditorScreen
import com.example.ui.screens.admin.AdminTournamentManagerScreen
import com.example.ui.screens.admin.AdminUserManagerScreen
import com.example.ui.screens.admin.AdminWithdrawalsScreen
import com.example.ui.screens.auth.SignInScreen
import com.example.ui.screens.player.DashboardScreen
import com.example.ui.screens.player.HistoryScreen
import com.example.ui.screens.player.NotificationCenterScreen
import com.example.ui.screens.player.ProfileScreen
import com.example.ui.screens.player.SupportChatScreen
import com.example.ui.screens.player.TournamentDetailScreen
import com.example.ui.screens.player.TournamentLobbyScreen
import com.example.ui.screens.player.WalletScreen
import com.example.ui.theme.FFTournamentsTheme
import com.example.ui.theme.FireOrange
import com.example.ui.theme.FireOrangeDark
import com.example.ui.theme.HighContrastDark
import com.example.ui.theme.SlateCardBg
import com.example.ui.theme.SlateMutedBorder
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import com.example.ui.theme.TrophyGold
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.Screen
import com.example.viewmodel.UiNotificationAlert
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PushNotificationHelper.createNotificationChannel(this)
        com.example.notification.NotificationBackgroundManager.scheduleBackgroundSync(this)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            FFTournamentsTheme(darkTheme = isDarkMode) {
                RootAppGate(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun RootAppGate(viewModel: AppViewModel) {
    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    var showStartupSplash by remember { mutableStateOf(true) }

    // Request POST_NOTIFICATIONS on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission granted or denied */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        delay(1800)
        showStartupSplash = false
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

    Box(modifier = Modifier.fillMaxSize()) {
        if (currentUser == null) {
            SignInScreen(
                onAuthSuccess = { user ->
                    currentUser = user
                    viewModel.onUserAuthenticated(user, isExplicitSignIn = true)
                }
            )
        } else {
            MainAppContent(viewModel = viewModel)
        }

        // Animated App Start Splash Overlay with "UdayScripts" at Bottom
        AnimatedVisibility(
            visible = showStartupSplash,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0A0F1D), HighContrastDark, Color(0xFF090D16))
                        )
                    )
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "eSports Arena Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(24.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "eSports Arena",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "OFFICIAL TOURNAMENT PLATFORM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        color = FireOrange
                    )
                }

                // Prominent "UdayScripts" Branding at the Bottom on App Start
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.5.dp, FireOrange),
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 36.dp)
                        .testTag("startup_udayscripts_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(FireOrange, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DEVELOPED & POWERED BY",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "UdayScripts",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = TrophyGold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }
        }
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = if (isDarkMode) FireOrange else Color.White,
                    selectedTextColor = FireOrange,
                    indicatorColor = if (isDarkMode) FireOrange.copy(alpha = 0.22f) else HighContrastDark,
                    unselectedIconColor = TextSecondaryMuted,
                    unselectedTextColor = TextSecondaryMuted
                )

                Surface(
                    color = SlateCardBg,
                    shadowElevation = 14.dp,
                    border = BorderStroke(1.dp, SlateMutedBorder),
                    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        NavigationBar(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bottom_nav_bar"),
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp
                        ) {
                            val items = listOf(
                                Triple(Screen.Dashboard, "Matches", Icons.Default.SportsEsports to "nav_matches"),
                                Triple(Screen.History, "History", Icons.Default.History to "nav_history"),
                                Triple(Screen.Wallet, "Wallet", Icons.Default.AccountBalanceWallet to "nav_wallet"),
                                Triple(Screen.Notifications, "Alerts", Icons.Default.Notifications to "nav_alerts"),
                                Triple(Screen.Profile, "Profile", Icons.Default.Person to "nav_profile")
                            )

                            items.forEach { (screen, label, iconAndTag) ->
                                val (icon, tag) = iconAndTag
                                val isSelected = currentScreen == screen
                                val iconScale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.12f else 1.0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "nav_icon_scale_$label"
                                )

                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { viewModel.navigateTo(screen) },
                                    icon = {
                                        if (screen == Screen.Notifications) {
                                            BadgedBox(
                                                badge = {
                                                    if (unreadNotifs > 0) {
                                                        Badge(containerColor = FireOrange) {
                                                            Text("$unreadNotifs", color = Color.White, fontSize = 9.sp)
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = label,
                                                    modifier = Modifier.scale(iconScale)
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = label,
                                                modifier = Modifier.scale(iconScale)
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                        )
                                    },
                                    colors = navItemColors,
                                    modifier = Modifier.testTag(tag)
                                )
                            }
                        }

                        // Subtle UdayScripts Signature Strip at Bottom of App
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "UdayScripts",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = FireOrange.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (currentScreen) {
                Screen.Dashboard -> DashboardScreen(viewModel = viewModel)
                Screen.TournamentDetails -> TournamentDetailScreen(viewModel = viewModel)
                Screen.TournamentLobby -> TournamentLobbyScreen(viewModel = viewModel)
                Screen.Wallet -> WalletScreen(viewModel = viewModel)
                Screen.History -> HistoryScreen(viewModel = viewModel)
                Screen.Notifications -> NotificationCenterScreen(viewModel = viewModel)
                Screen.Profile -> ProfileScreen(viewModel = viewModel)
                Screen.SupportChat -> SupportChatScreen(viewModel = viewModel)

                // Admin Screens
                Screen.AdminAuth -> AdminAuthScreen(viewModel = viewModel)
                Screen.AdminDashboard -> AdminDashboardScreen(viewModel = viewModel)
                Screen.AdminTournaments -> AdminTournamentManagerScreen(viewModel = viewModel)
                Screen.AdminTournamentEditor -> AdminTournamentEditorScreen(viewModel = viewModel)
                Screen.AdminSettlement -> AdminSettlementScreen(viewModel = viewModel)
                Screen.AdminUsers -> AdminUserManagerScreen(viewModel = viewModel)
                Screen.AdminWithdrawals -> AdminWithdrawalsScreen(viewModel = viewModel)
                Screen.AdminSettings -> AdminAppSettingsScreen(viewModel = viewModel)
                Screen.AdminSupportInbox -> AdminSupportInboxScreen(viewModel = viewModel)
                Screen.AdminSupportChat -> AdminSupportChatScreen(viewModel = viewModel)
            }

            // Real-Time Notification Banner Float on top
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                RealTimeNotificationBanner(
                    alert = currentLiveAlert,
                    onDismiss = { currentLiveAlert = null },
                    onMarkAsRead = { alertId ->
                        if (alertId.isNotBlank()) {
                            viewModel.markNotificationRead(alertId)
                        }
                        currentLiveAlert = null
                    }
                )
            }
        }
    }
}
