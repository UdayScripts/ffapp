package com.example.ui.screens.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.SportsEsports
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.HighContrastCard
import com.example.ui.components.MatchTypeBadge
import com.example.ui.components.RoomCredentialsBox
import com.example.ui.components.StatusBadge
import com.example.ui.theme.FireOrange
import com.example.ui.theme.FireOrangeDark
import com.example.ui.theme.HighContrastDark
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
fun TournamentDetailScreen(viewModel: AppViewModel) {
    val tournament by viewModel.selectedTournament.collectAsStateWithLifecycle()
    val participants by viewModel.selectedTournamentParticipants.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var showJoinDialog by remember { mutableStateOf(false) }
    var inputIgn by remember(currentUser) { mutableStateOf(currentUser?.ffIgn ?: "") }
    var inputUid by remember(currentUser) { mutableStateOf(currentUser?.ffUid ?: "") }

    val hasJoined = participants.any { it.userId == currentUser?.id }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = tournament?.title ?: "Tournament Details",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimaryDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (tournament != null && tournament?.status == "UPCOMING") {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ENTRY FEE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondaryMuted)
                            Text(
                                text = if (tournament!!.entryFee == 0) "FREE" else "₹${tournament!!.entryFee}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = SuccessGreen
                            )
                        }

                        if (hasJoined) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SuccessGreen.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, SuccessGreen)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Joined Slot #${participants.find { it.userId == currentUser?.id }?.slotNumber ?: 0}",
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            val isFull = tournament!!.joinedSlots >= tournament!!.maxSlots
                            Button(
                                onClick = { showJoinDialog = true },
                                enabled = !isFull,
                                colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("join_tournament_cta_button")
                            ) {
                                Text(
                                    text = if (isFull) "Match Full" else "Join Tournament",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (tournament == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Match not found.")
            }
        } else {
            val t = tournament!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SlateLightBg)
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // High-Contrast Hero Header
                item {
                    HighContrastCard {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    MatchTypeBadge(t.matchType)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Map: ${t.map}",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                StatusBadge(t.status)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = t.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "⏰ Schedule: ${t.scheduleTime}",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Prize Pool & Per Kill Highlight
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("TOTAL PRIZE", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                    Text("₹${t.prizePool}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TrophyGold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("PER KILL", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                    Text("₹${t.perKillReward}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = FireOrange)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("SLOTS", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                    Text("${t.joinedSlots}/${t.maxSlots}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Room Credentials (if revealed)
                item {
                    RoomCredentialsBox(
                        roomId = t.roomId,
                        roomPass = t.roomPassword,
                        roomNotice = t.roomNotice
                    )
                }

                // Prize Distribution Breakdown
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, SlateMutedBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = TrophyGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Prize Distribution",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("🥇 1st Rank (Booyah)", fontSize = 13.sp, color = TextPrimaryDark)
                                Text("₹${t.firstPrize}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TrophyGold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("🥈 2nd Rank", fontSize = 13.sp, color = TextPrimaryDark)
                                Text("₹${t.secondPrize}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("🥉 3rd Rank", fontSize = 13.sp, color = TextPrimaryDark)
                                Text("₹${t.thirdPrize}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("🎯 Each Kill Reward", fontSize = 13.sp, color = TextPrimaryDark)
                                Text("₹${t.perKillReward}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FireOrangeDark)
                            }
                        }
                    }
                }

                // Match Rules & Guidelines
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, SlateMutedBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Gavel,
                                    contentDescription = null,
                                    tint = FireOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Rules & Guidelines",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = t.rules,
                                fontSize = 12.sp,
                                color = Color(0xFF475569),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Registered Participants List
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = TextSecondaryMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Registered Players (${participants.size}/${t.maxSlots})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                    }
                }

                if (participants.isEmpty()) {
                    item {
                        Text(
                            text = "No players registered yet. Be the first to join!",
                            fontSize = 12.sp,
                            color = TextSecondaryMuted
                        )
                    }
                } else {
                    items(participants) { participant ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SlateMutedBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = HighContrastDark
                                    ) {
                                        Text(
                                            text = "#${participant.slotNumber}",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = participant.ffIgn,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TextPrimaryDark
                                        )
                                        Text(
                                            text = "UID: ${participant.ffUid}",
                                            fontSize = 11.sp,
                                            color = TextSecondaryMuted
                                        )
                                    }
                                }

                                if (t.status == "COMPLETED") {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Rank #${participant.placement} | ${participant.kills} Kills",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = TextPrimaryDark
                                        )
                                        if (participant.prizeWon > 0) {
                                            Text(
                                                text = "+₹${participant.prizeWon}",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp,
                                                color = SuccessGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Join Match Modal Dialog
    if (showJoinDialog && tournament != null) {
        val t = tournament!!
        val userBal = currentUser?.walletBalance ?: 0
        val hasEnough = userBal >= t.entryFee

        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = {
                Text(
                    text = "Confirm Match Registration",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Match: ${t.title}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Entry Fee: ₹${t.entryFee}  •  Your Balance: ₹$userBal",
                        fontSize = 12.sp,
                        color = if (hasEnough) SuccessGreen else Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = inputIgn,
                        onValueChange = { inputIgn = it },
                        label = { Text("Free Fire In-Game Name (IGN)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("join_ign_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputUid,
                        onValueChange = { inputUid = it },
                        label = { Text("Free Fire Player UID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("join_uid_input")
                    )

                    if (!hasEnough) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠️ Insufficient balance! Please top-up in Wallet.",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputIgn.isNotBlank() && inputUid.isNotBlank() && hasEnough) {
                            viewModel.joinTournament(t.id, inputIgn, inputUid)
                            showJoinDialog = false
                        } else if (!hasEnough) {
                            showJoinDialog = false
                            viewModel.navigateTo(Screen.Wallet)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasEnough) FireOrange else SuccessGreen
                    ),
                    modifier = Modifier.testTag("confirm_join_button")
                ) {
                    Text(if (hasEnough) "Confirm & Pay ₹${t.entryFee}" else "Go to Wallet Top-Up")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
