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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firestore.formatCurrency
import com.example.notification.PushNotificationHelper
import com.example.ui.components.RealTimeNotificationBanner
import com.example.ui.components.TournamentRegistrationSuccessDialog
import com.example.ui.screens.admin.AdminAppSettingsScreen
import com.example.ui.screens.admin.AdminAuthScreen
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.admin.AdminSettlementScreen
import com.example.ui.screens.admin.AdminStaffManagerScreen
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
import com.example.ui.screens.player.TopPlayersScreen
import com.example.ui.screens.player.TournamentDetailScreen
import com.example.ui.screens.player.TournamentLobbyScreen
import com.example.ui.screens.player.WalletScreen
import com.example.ui.theme.FFTournamentsTheme
import com.example.ui.theme.FireOrange
import com.example.ui.theme.FireOrangeDark
import com.example.ui.theme.HighContrastDark
import com.example.ui.theme.SlateCardBg
import com.example.ui.theme.SlateMutedBorder
import com.example.ui.theme.SlateSubtle
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import com.example.ui.theme.TrophyGold
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.Screen
import com.example.viewmodel.UiNotificationAlert
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        com.example.data.ads.UnityAdsManager.sanitizeWebViewVariationsSeed(this)
        super.onCreate(savedInstanceState)
        PushNotificationHelper.createNotificationChannel(this)
        com.example.notification.NotificationBackgroundManager.scheduleBackgroundSync(this)
        com.example.data.ads.UnityAdsManager.registerSafeIntentHost(this)
        com.example.data.ads.UnityAdsManager.initialize(this)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            FFTournamentsTheme(darkTheme = isDarkMode) {
                RootAppGate(viewModel = viewModel)
            }
        }
    }

    override fun startActivity(intent: android.content.Intent?) {
        startActivity(intent, null)
    }

    override fun startActivity(intent: android.content.Intent?, options: Bundle?) {
        if (intent == null) return
        try {
            val targetPkg = intent.getPackage()
            val dataUri = intent.dataString ?: ""

            // Check if the intent explicitly targets Google Play Store (com.android.vending)
            // on an emulator/device where Play Store is not installed or cannot resolve the intent.
            if (targetPkg == "com.android.vending" && packageManager.resolveActivity(intent, 0) == null) {
                // Unity Ads automatically triggers Play Store inline mini-details overlays (inline=true or /d?id=).
                // When Play Store is absent, ignore inline overlay intents silently so the video ad continues smoothly.
                if (dataUri.contains("inline=true") || dataUri.contains("play.google.com/d?")) {
                    android.util.Log.d("MainActivity", "Play Store not installed; skipping inline overlay intent: $dataUri")
                    return
                }

                // For explicit store clicks without Play Store, strip the package restriction so a browser can handle it.
                val fallbackUri = if (dataUri.startsWith("market://details")) {
                    android.net.Uri.parse(
                        dataUri.replaceFirst("market://details", "https://play.google.com/store/apps/details")
                    )
                } else {
                    intent.data
                }
                val browserIntent = android.content.Intent(intent).apply {
                    setPackage(null)
                    component = null
                    if (fallbackUri != null) {
                        data = fallbackUri
                    }
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (packageManager.resolveActivity(browserIntent, 0) != null) {
                    super.startActivity(browserIntent, options)
                } else {
                    android.util.Log.d("MainActivity", "No browser or store activity available to handle: $dataUri")
                }
                return
            }

            super.startActivity(intent, options)
        } catch (e: android.content.ActivityNotFoundException) {
            // Never let unresolvable external/ad store intents crash or throw back into Unity Ads WebView bridge.
            android.util.Log.w("MainActivity", "Ignored unresolvable external Intent: ${intent.dataString}", e)
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Failed to launch external Intent: ${intent.dataString}", e)
        }
    }
}

@Composable
fun RootAppGate(viewModel: AppViewModel) {
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
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
        if (currentUserId.isBlank()) {
            SignInScreen(
                viewModel = viewModel,
                onAuthSuccess = { session ->
                    viewModel.onSessionAuthenticated(session, isExplicitSignIn = true)
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

private data class NavGridOption(
    val screen: Screen,
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val testTag: String,
    val badgeCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: AppViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val unreadNotifs by viewModel.unreadNotifCount.collectAsStateWithLifecycle()
    val unreadSupport by viewModel.userUnreadSupportCount.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isRealAdmin by viewModel.isRealAdmin.collectAsStateWithLifecycle()
    val registrationSuccessEvent by viewModel.registrationSuccessEvent.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var currentLiveAlert by remember { mutableStateOf<UiNotificationAlert?>(null) }
    var showNavBottomSheet by remember { mutableStateOf(false) }
    val navSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val navOptions = remember(currentUser?.walletBalance, unreadNotifs, unreadSupport, isRealAdmin) {
        buildList {
            add(
                NavGridOption(
                    screen = Screen.Dashboard,
                    label = "Matches",
                    subtitle = "Live & Upcoming",
                    icon = Icons.Default.SportsEsports,
                    accentColor = FireOrange,
                    testTag = "nav_matches"
                )
            )
            add(
                NavGridOption(
                    screen = Screen.TopPlayers,
                    label = "Top Players",
                    subtitle = "Top 5 Leaderboard",
                    icon = Icons.Default.EmojiEvents,
                    accentColor = TrophyGold,
                    testTag = "nav_top_players"
                )
            )
            add(
                NavGridOption(
                    screen = Screen.History,
                    label = "History",
                    subtitle = "Joined & Results",
                    icon = Icons.Default.History,
                    accentColor = Color(0xFF0EA5E9),
                    testTag = "nav_history"
                )
            )
            add(
                NavGridOption(
                    screen = Screen.Wallet,
                    label = "Wallet",
                    subtitle = "₹${currentUser?.walletBalance.formatCurrency()} Balance",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = SuccessGreen,
                    testTag = "nav_wallet"
                )
            )
            add(
                NavGridOption(
                    screen = Screen.Notifications,
                    label = "Alerts",
                    subtitle = if (unreadNotifs > 0) "$unreadNotifs New Alerts" else "Room ID & News",
                    icon = Icons.Default.Notifications,
                    accentColor = FireOrangeDark,
                    testTag = "nav_alerts",
                    badgeCount = unreadNotifs
                )
            )
            add(
                NavGridOption(
                    screen = Screen.Profile,
                    label = "Profile",
                    subtitle = "FF Roster & Account",
                    icon = Icons.Default.Person,
                    accentColor = Color(0xFF8B5CF6),
                    testTag = "nav_profile"
                )
            )
            if (isRealAdmin) {
                add(
                    NavGridOption(
                        screen = Screen.AdminDashboard,
                        label = "Admin Panel",
                        subtitle = "Manage Matches & Staff",
                        icon = Icons.Default.AdminPanelSettings,
                        accentColor = FireOrange,
                        testTag = "nav_admin"
                    )
                )
                add(
                    NavGridOption(
                        screen = Screen.SupportChat,
                        label = "Support Desk",
                        subtitle = if (unreadSupport > 0) "$unreadSupport Unread" else "24/7 Live Help",
                        icon = Icons.Default.SupportAgent,
                        accentColor = SuccessGreen,
                        testTag = "nav_support",
                        badgeCount = unreadSupport
                    )
                )
            }
        }
    }

    val activeNavOption = navOptions.firstOrNull { it.screen == currentScreen } ?: navOptions.first()

    // Collect Toast messages
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Collect Real-time notification banners and automatically hide after 3 seconds
    LaunchedEffect(Unit) {
        viewModel.liveAlert.collectLatest { alert ->
            currentLiveAlert = alert
            delay(3000L)
            if (currentLiveAlert == alert) {
                currentLiveAlert = null
            }
        }
    }

    // Hardware BackHandler
    BackHandler(enabled = currentScreen != Screen.Dashboard) {
        viewModel.navigateBack()
    }

    // Show bottom sheet launcher dock on core player screens
    val showBottomBar = currentScreen in listOf(
        Screen.Dashboard,
        Screen.TopPlayers,
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
                Surface(
                    color = SlateCardBg,
                    shadowElevation = 16.dp,
                    border = BorderStroke(1.dp, SlateMutedBorder),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bottom_nav_bar")
                        .clickable { showNavBottomSheet = true }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount < -8f) {
                                    showNavBottomSheet = true
                                }
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Instagram-Comments-Style Drag Handle Pill
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(TextSecondaryMuted.copy(alpha = 0.45f))
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_nav_sheet_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: Current Page Indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(
                                            activeNavOption.accentColor.copy(alpha = 0.16f),
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = activeNavOption.icon,
                                        contentDescription = activeNavOption.label,
                                        tint = activeNavOption.accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activeNavOption.label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimaryDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Swipe up or tap for all options",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextSecondaryMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Right: Grid Menu Trigger Pill with Unread Badge
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = HighContrastDark,
                                border = BorderStroke(1.dp, FireOrange.copy(alpha = 0.7f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (unreadNotifs > 0) {
                                                Badge(containerColor = FireOrange) {
                                                    Text(
                                                        text = "$unreadNotifs",
                                                        color = Color.White,
                                                        fontSize = 8.sp
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GridView,
                                            contentDescription = "All Options Grid",
                                            tint = FireOrange,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "All Pages",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Open Sheet",
                                        tint = TrophyGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Subtle UdayScripts Signature Strip at Bottom of App
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (currentScreen) {
                Screen.Dashboard -> DashboardScreen(viewModel = viewModel)
                Screen.TopPlayers -> TopPlayersScreen(viewModel = viewModel)
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
                Screen.AdminStaff -> AdminStaffManagerScreen(viewModel = viewModel)
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

            // Lottie-Based Tournament Registration Success Celebration Modal
            TournamentRegistrationSuccessDialog(
                event = registrationSuccessEvent,
                onDismiss = { viewModel.dismissRegistrationSuccess() },
                onEnterLobby = { joinedTournamentId ->
                    viewModel.dismissRegistrationSuccess()
                    viewModel.openTournamentLobby(joinedTournamentId)
                }
            )
        }
    }

    // Instagram-Comments-Style Modal Bottom Sheet with Grid Boxes
    if (showNavBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNavBottomSheet = false },
            sheetState = navSheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = SlateCardBg,
            contentColor = TextPrimaryDark,
            dragHandle = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Classic Instagram Comments Sheet Drag Pill
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(4.5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(TextSecondaryMuted.copy(alpha = 0.45f))
                    )
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp)
                    .testTag("insta_nav_bottom_sheet")
            ) {
                // Sheet Header (Instagram Comments Header Layout)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Arena Navigation",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Choose any section below",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondaryMuted
                        )
                    }

                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                navSheetState.hide()
                                showNavBottomSheet = false
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(32.dp)
                            .testTag("close_nav_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Menu",
                            tint = TextSecondaryMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = SlateMutedBorder.copy(alpha = 0.7f), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // 2-Column Grid Boxes for all navigation options
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    navOptions.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { option ->
                                val isSelected = currentScreen == option.screen
                                NavGridBoxCard(
                                    option = option,
                                    isSelected = isSelected,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        coroutineScope.launch {
                                            navSheetState.hide()
                                            showNavBottomSheet = false
                                            viewModel.navigateTo(option.screen)
                                        }
                                    }
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavGridBoxCard(
    option: NavGridOption,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(option.testTag),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) option.accentColor.copy(alpha = 0.12f) else SlateSubtle,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) option.accentColor else SlateMutedBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = option.accentColor.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = option.label,
                        tint = option.accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (option.badgeCount > 0) {
                    Surface(
                        color = FireOrange,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${option.badgeCount}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                } else if (isSelected) {
                    Surface(
                        color = option.accentColor,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = option.label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = option.subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) option.accentColor else TextSecondaryMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
