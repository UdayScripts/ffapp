package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.HighContrastCard
import com.example.ui.theme.FireOrange
import com.example.ui.theme.FireOrangeDark
import com.example.ui.theme.HighContrastBorder
import com.example.ui.theme.HighContrastDark
import com.example.ui.theme.LiveRed
import com.example.data.firestore.TournamentDto
import com.example.ui.theme.SlateCardBg
import com.example.ui.theme.SlateLightBg
import com.example.ui.theme.SlateMutedBorder
import com.example.ui.theme.SlateSubtle
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import com.example.ui.theme.TrophyGold
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(viewModel: AppViewModel) {
    val tournaments by viewModel.allTournaments.collectAsStateWithLifecycle()
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var showBroadcastDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var isDeletingAll by remember { mutableStateOf(false) }

    val liveCount = tournaments.count { it.status == "LIVE" }
    val totalPrize = tournaments.sumOf { it.prizePool }

    val pendingWithdrawals by viewModel.pendingWithdrawalsCount.collectAsStateWithLifecycle()
    val unreadSupportChats by viewModel.adminUnreadSupportCount.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SlateCardBg,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "eSports Arena Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Admin Control Panel",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "Organizer: ${currentUser?.username ?: "Admin"}",
                                fontSize = 10.sp,
                                color = TextSecondaryMuted
                            )
                        }
                    }

                    // Return to Matches View
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { viewModel.navigateTo(Screen.Dashboard) }
                            .testTag("switch_to_player_button"),
                        color = SlateSubtle,
                        border = BorderStroke(1.dp, SlateMutedBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = FireOrangeDark,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Player View",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateLightBg)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // High-Contrast Metrics Overview
            item {
                HighContrastCard {
                    Column {
                        Text(
                            text = "TOURNAMENT PLATFORM METRICS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FireOrange,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricItem("TOTAL MATCHES", "${tournaments.size}", Icons.Default.SportsEsports, Color.White)
                            MetricItem("LIVE MATCHES", "$liveCount", Icons.Default.LocalFireDepartment, LiveRed)
                            MetricItem("PLAYERS", "${users.size}", Icons.Default.Group, Color(0xFF60A5FA))
                            MetricItem("PRIZE POOL", "₹$totalPrize", Icons.Default.EmojiEvents, TrophyGold)
                        }
                    }
                }
            }

            // Quick Actions Title
            item {
                Text(
                    text = "Platform Management",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }

            // Grid Cards for Modules
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Manage Tournaments
                    AdminActionCard(
                        title = "Tournament Manager",
                        subtitle = "Create, edit, cancel matches, and publish Room ID & Password",
                        icon = Icons.Default.SportsEsports,
                        accentColor = FireOrange,
                        onClick = { viewModel.navigateTo(Screen.AdminTournaments) },
                        tag = "admin_menu_tournaments"
                    )

                    // Settle Match & Distribute Prizes
                    AdminActionCard(
                        title = "Match Results Settlement",
                        subtitle = "Input kills, set player ranks, and automatically pay out winnings to user wallets",
                        icon = Icons.Default.EmojiEvents,
                        accentColor = TrophyGold,
                        onClick = { viewModel.navigateTo(Screen.AdminSettlement) },
                        tag = "admin_menu_settlement"
                    )

                    // Manage UPI Withdrawals
                    AdminActionCard(
                        title = if (pendingWithdrawals > 0) "UPI Withdrawal Requests ($pendingWithdrawals Pending)" else "UPI Withdrawal Requests",
                        subtitle = "Review player winning withdrawal requests, accept & mark paid, or reject with instant refund",
                        icon = Icons.Default.Payments,
                        accentColor = SuccessGreen,
                        onClick = { viewModel.navigateTo(Screen.AdminWithdrawals) },
                        tag = "admin_menu_withdrawals"
                    )

                    // Live Player Support Chat Inbox (with Photo Support)
                    AdminActionCard(
                        title = if (unreadSupportChats > 0) "Live Support Chat Inbox ($unreadSupportChats Unread)" else "Live Support Chat Inbox",
                        subtitle = "Chat with players in real time, inspect payment/match screenshots, and send photo replies",
                        icon = Icons.Default.SupportAgent,
                        accentColor = Color(0xFF0EA5E9),
                        onClick = { viewModel.navigateTo(Screen.AdminSupportInbox) },
                        tag = "admin_menu_support_inbox"
                    )

                    // User Management & Balances
                    AdminActionCard(
                        title = if (users.isNotEmpty()) "User & Wallet Management (${users.size} Players)" else "User & Wallet Management",
                        subtitle = "View player UIDs, edit Free Fire IGN & UID, adjust wallet balance, and toggle suspensions",
                        icon = Icons.Default.ManageAccounts,
                        accentColor = Color(0xFF3B82F6),
                        onClick = { viewModel.navigateTo(Screen.AdminUsers) },
                        tag = "admin_menu_users"
                    )

                    // Real-Time Notification Broadcast
                    AdminActionCard(
                        title = "Broadcast Real-Time Alert",
                        subtitle = "Send instant notifications & custom room alerts to all players",
                        icon = Icons.Default.Campaign,
                        accentColor = Color(0xFF10B981),
                        onClick = { showBroadcastDialog = true },
                        tag = "admin_menu_broadcast"
                    )

                    // Test Real-Time Push Notification Engine
                    AdminActionCard(
                        title = "Test Real-Time Push Notification",
                        subtitle = "Fire instant Android system push notification with sound & vibration",
                        icon = Icons.Default.NotificationsActive,
                        accentColor = FireOrange,
                        onClick = { viewModel.testPushNotification() },
                        tag = "admin_menu_test_push"
                    )

                    // Application Settings Customizer
                    AdminActionCard(
                        title = "Application Settings",
                        subtitle = "Customize app name, announcement marquee banner, fee %, and rules",
                        icon = Icons.Default.Settings,
                        accentColor = Color(0xFF8B5CF6),
                        onClick = { viewModel.navigateTo(Screen.AdminSettings) },
                        tag = "admin_menu_settings"
                    )

                    // Delete All Platform Data
                    AdminActionCard(
                        title = "Delete All Data (Reset All)",
                        subtitle = "Permanently wipe all users, match history, transactions, withdrawals, tournaments & alerts",
                        icon = Icons.Default.DeleteForever,
                        accentColor = LiveRed,
                        onClick = { showDeleteAllDialog = true },
                        tag = "admin_menu_delete_all_data"
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showBroadcastDialog) {
        BroadcastNotificationModal(
            tournaments = tournaments,
            onDismiss = { showBroadcastDialog = false },
            onBroadcast = { title, msg, type, targetUid, targetTid, roomId, roomPass ->
                viewModel.broadcastCustomNotification(
                    title = title,
                    message = msg,
                    type = type,
                    targetUserId = targetUid,
                    targetTournamentId = targetTid,
                    roomId = roomId,
                    roomPassword = roomPass
                )
                showBroadcastDialog = false
            }
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingAll) showDeleteAllDialog = false },
            containerColor = SlateCardBg,
            title = {
                Text(
                    text = "Delete All Platform Data?",
                    fontWeight = FontWeight.Bold,
                    color = LiveRed
                )
            },
            text = {
                Text(
                    text = "This will permanently delete all users, their match & balance history, withdrawal requests, tournaments, and notifications from the database.",
                    fontSize = 13.sp,
                    color = TextPrimaryDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeletingAll = true
                        viewModel.deleteAllPlatformData { _, _ ->
                            isDeletingAll = false
                            showDeleteAllDialog = false
                        }
                    },
                    enabled = !isDeletingAll,
                    colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                    modifier = Modifier.testTag("confirm_delete_all_data_button")
                ) {
                    Text(if (isDeletingAll) "Deleting..." else "Delete Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAllDialog = false },
                    enabled = !isDeletingAll
                ) {
                    Text("Cancel", color = TextSecondaryMuted)
                }
            }
        )
    }
}

@Composable
fun MetricItem(label: String, value: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text(text = label, fontSize = 8.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AdminActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    tag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(tag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCardBg),
        border = BorderStroke(1.dp, SlateMutedBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimaryDark
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondaryMuted,
                    lineHeight = 15.sp
                )
            }

            Text(
                text = "Manage →",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = accentColor
            )
        }
    }
}

@Composable
fun BroadcastNotificationModal(
    tournaments: List<TournamentDto> = emptyList(),
    onDismiss: () -> Unit,
    onBroadcast: (
        title: String,
        message: String,
        type: String,
        targetUserId: String?,
        targetTournamentId: String?,
        roomId: String?,
        roomPassword: String?
    ) -> Unit
) {
    var titleInput by remember { mutableStateOf("Tournament Room Update 🚨") }
    var messageInput by remember { mutableStateOf("Match starts in 10 minutes! Check Lobby for Room ID & Password.") }
    var targetMode by remember { mutableStateOf("ALL") } // "ALL", "TOURNAMENT", "USER"
    var selectedTournamentId by remember { mutableStateOf(tournaments.firstOrNull()?.id ?: "") }
    var targetUserIdInput by remember { mutableStateOf("") }
    var roomIdInput by remember { mutableStateOf("") }
    var roomPasswordInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(24.dp),
                color = SlateCardBg,
                border = BorderStroke(1.2.dp, SlateMutedBorder),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(HighContrastDark, Color(0xFF1E293B))
                                )
                            )
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(FireOrange.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Campaign,
                                        contentDescription = null,
                                        tint = FireOrange,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Send Custom Push Notification",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Dispatches In-App Banner & FCM Push",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Live In-App Banner Preview
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HighContrastDark,
                            border = BorderStroke(1.dp, FireOrange.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Campaign,
                                            contentDescription = null,
                                            tint = FireOrange,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = titleInput.ifBlank { "Alert Title" },
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = when (targetMode) {
                                            "TOURNAMENT" -> FireOrange
                                            "USER" -> Color(0xFF0284C7)
                                            else -> LiveRed
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = when (targetMode) {
                                                "TOURNAMENT" -> "MATCH PLAYERS"
                                                "USER" -> "DIRECT USER"
                                                else -> "ALL PLAYERS"
                                            },
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = messageInput.ifBlank { "Alert message preview here..." },
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                                if (roomIdInput.isNotBlank() || roomPasswordInput.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (roomIdInput.isNotBlank()) {
                                            Surface(
                                                color = Color(0xFF0F172A),
                                                shape = RoundedCornerShape(4.dp),
                                                border = BorderStroke(0.8.dp, FireOrange.copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    text = "Room ID: $roomIdInput",
                                                    fontSize = 10.sp,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        if (roomPasswordInput.isNotBlank()) {
                                            Surface(
                                                color = Color(0xFF0F172A),
                                                shape = RoundedCornerShape(4.dp),
                                                border = BorderStroke(0.8.dp, FireOrange.copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    text = "Pass: $roomPasswordInput",
                                                    fontSize = 10.sp,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Target Audience Segment
                        Text(
                            text = "Target Audience",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TextPrimaryDark
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "ALL" to "All Players",
                                "TOURNAMENT" to "By Match",
                                "USER" to "Single User"
                            ).forEach { (mode, label) ->
                                val isSelected = targetMode == mode
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) FireOrange.copy(alpha = 0.2f) else Color.Transparent,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) FireOrange else SlateMutedBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { targetMode = mode }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) FireOrange else TextPrimaryDark
                                        )
                                    }
                                }
                            }
                        }

                        // Specific Tournament Selection
                        if (targetMode == "TOURNAMENT") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Select Tournament",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryDark
                                )
                                if (tournaments.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        tournaments.take(5).forEach { tourney ->
                                            val isTourneySelected = selectedTournamentId == tourney.id
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isTourneySelected) FireOrange.copy(alpha = 0.15f) else Color.Transparent,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isTourneySelected) FireOrange else SlateMutedBorder
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedTournamentId = tourney.id
                                                        tourney.roomId?.takeIf { it.isNotBlank() }?.let { roomIdInput = it }
                                                        tourney.roomPassword?.takeIf { it.isNotBlank() }?.let { roomPasswordInput = it }
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = tourney.title,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextPrimaryDark,
                                                            maxLines = 1
                                                        )
                                                        Text(
                                                            text = "${tourney.matchType} • ${tourney.scheduleTime}",
                                                            fontSize = 10.sp,
                                                            color = Color(0xFF94A3B8)
                                                        )
                                                    }
                                                    if (isTourneySelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = null,
                                                            tint = FireOrange,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = selectedTournamentId,
                                    onValueChange = { selectedTournamentId = it },
                                    label = { Text("Tournament ID") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Specific User UID Input
                        if (targetMode == "USER") {
                            OutlinedTextField(
                                value = targetUserIdInput,
                                onValueChange = { targetUserIdInput = it },
                                label = { Text("Target Player UID") },
                                placeholder = { Text("Enter user Firebase UID") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Optional Room Credentials
                        if (targetMode == "TOURNAMENT") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = roomIdInput,
                                    onValueChange = { roomIdInput = it },
                                    label = { Text("Room ID (Optional)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = roomPasswordInput,
                                    onValueChange = { roomPasswordInput = it },
                                    label = { Text("Room Pass (Optional)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("Alert Title") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("broadcast_title_input")
                        )

                        OutlinedTextField(
                            value = messageInput,
                            onValueChange = { messageInput = it },
                            label = { Text("Alert Message") },
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("broadcast_msg_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Cancel", color = TextPrimaryDark)
                            }

                            Button(
                                onClick = {
                                    if (titleInput.isNotBlank() && messageInput.isNotBlank()) {
                                        val type = if (roomIdInput.isNotBlank() || roomPasswordInput.isNotBlank()) {
                                            "ROOM_CREDENTIALS"
                                        } else {
                                            "ANNOUNCEMENT"
                                        }
                                        val targetUid = if (targetMode == "USER") targetUserIdInput.trim().takeIf { it.isNotBlank() } else null
                                        val targetTid = if (targetMode == "TOURNAMENT") selectedTournamentId.trim().takeIf { it.isNotBlank() } else null
                                        val room = roomIdInput.trim().takeIf { it.isNotBlank() }
                                        val pass = roomPasswordInput.trim().takeIf { it.isNotBlank() }

                                        onBroadcast(
                                            titleInput.trim(),
                                            messageInput.trim(),
                                            type,
                                            targetUid,
                                            targetTid,
                                            room,
                                            pass
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp)
                                    .testTag("send_broadcast_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Broadcast", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
