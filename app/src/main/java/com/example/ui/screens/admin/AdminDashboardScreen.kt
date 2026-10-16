package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.testTag
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
import com.example.ui.theme.SlateLightBg
import com.example.ui.theme.SlateMutedBorder
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

    val liveCount = tournaments.count { it.status == "LIVE" }
    val totalPrize = tournaments.sumOf { it.prizePool }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(HighContrastDark, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = FireOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
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
                },
                actions = {
                    // Return to Matches View
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { viewModel.navigateTo(Screen.Dashboard) }
                            .testTag("switch_to_player_button"),
                        color = Color(0xFFF1F5F9),
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
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
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

                    // User Management & Balances
                    AdminActionCard(
                        title = "User & Wallet Management",
                        subtitle = "View player UIDs, adjust wallet balance, and toggle suspensions",
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
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showBroadcastDialog) {
        BroadcastNotificationModal(
            onDismiss = { showBroadcastDialog = false },
            onBroadcast = { title, msg ->
                viewModel.broadcastCustomNotification(title, msg)
                showBroadcastDialog = false
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
    onDismiss: () -> Unit,
    onBroadcast: (title: String, message: String) -> Unit
) {
    var titleInput by remember { mutableStateOf("Tournament Room Update 🚨") }
    var messageInput by remember { mutableStateOf("Match is starting in 15 minutes! Please check Room ID & Password.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Broadcast Real-Time Alert", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "This will send an instant in-app banner alert and notification inbox item to all players.",
                    fontSize = 12.sp,
                    color = TextSecondaryMuted
                )

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Alert Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("broadcast_title_input")
                )

                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    label = { Text("Alert Message") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("broadcast_msg_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titleInput.isNotBlank() && messageInput.isNotBlank()) {
                        onBroadcast(titleInput, messageInput)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                modifier = Modifier.testTag("send_broadcast_button")
            ) {
                Text("Broadcast Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
