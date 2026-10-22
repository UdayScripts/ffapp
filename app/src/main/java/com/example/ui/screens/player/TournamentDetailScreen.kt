package com.example.ui.screens.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.widget.Toast
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
import com.example.ui.theme.SlateCardBg
import com.example.ui.theme.SlateLightBg
import com.example.ui.theme.SlateMutedBorder
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import com.example.ui.theme.TrophyGold
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TournamentDetailScreen(viewModel: AppViewModel) {
    val tournament by viewModel.selectedTournament.collectAsStateWithLifecycle()
    val participants by viewModel.selectedTournamentParticipants.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showJoinDialog by remember { mutableStateOf(false) }
    var chosenSlot by remember { mutableStateOf<Int?>(null) }
    var inputIgn by remember(currentUser) { mutableStateOf(currentUser?.ffIgn ?: "") }
    var inputUid by remember(currentUser) { mutableStateOf(currentUser?.ffUid ?: "") }

    val hasJoined = participants.any { it.userId == currentUser?.id }

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
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    Text(
                        text = tournament?.title ?: "Tournament Details",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        bottomBar = {
            if (tournament != null && tournament?.status == "UPCOMING") {
                Surface(
                    color = SlateCardBg,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ENTRY FEE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondaryMuted, maxLines = 1)
                            Text(
                                text = if (tournament!!.entryFee == 0) "FREE" else "₹${tournament!!.entryFee}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = SuccessGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        if (hasJoined) {
                            Button(
                                onClick = {
                                    viewModel.openTournamentLobby(tournament!!.id)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HighContrastDark),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, FireOrange),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("enter_lobby_cta_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsEsports,
                                    contentDescription = null,
                                    tint = FireOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Enter Lobby (#${participants.find { it.userId == currentUser?.id }?.slotNumber ?: 0})",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        } else {
                            val isFull = tournament!!.joinedSlots >= tournament!!.maxSlots
                            Button(
                                onClick = { showJoinDialog = true },
                                enabled = !isFull,
                                colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("join_tournament_cta_button")
                            ) {
                                Text(
                                    text = if (isFull) "Match Full" else "Join Tournament",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    softWrap = false
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    MatchTypeBadge(t.matchType)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Map: ${t.map}",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                StatusBadge(t.status)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = t.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "⏰ Schedule: ${t.scheduleTime}",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("TOTAL PRIZE", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text("₹${t.prizePool}", fontSize = 17.sp, fontWeight = FontWeight.Black, color = TrophyGold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("PER KILL", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text("₹${t.perKillReward}", fontSize = 17.sp, fontWeight = FontWeight.Black, color = FireOrange, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("SLOTS", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text("${t.joinedSlots}/${t.maxSlots}", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }

                // Registered player lobby banner
                if (hasJoined) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = HighContrastDark,
                            border = BorderStroke(1.5.dp, FireOrange),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { viewModel.openTournamentLobby(t.id) }
                                .testTag("detail_open_lobby_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(FireOrange.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = null,
                                            tint = FireOrange,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (t.roomId.isNullOrBlank()) "Tournament Lobby • Keys Pending" else "Tournament Lobby • Room Keys Live! 🔓",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Tap to open lobby, copy credentials & view squad",
                                            fontSize = 11.sp,
                                            color = Color(0xFFCBD5E1),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = FireOrange,
                                    modifier = Modifier.size(18.dp)
                                )
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
                        colors = CardDefaults.cardColors(containerColor = SlateCardBg),
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
                                Text("🥇 1st Rank (Booyah)", fontSize = 13.sp, color = TextPrimaryDark, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("₹${t.firstPrize}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TrophyGold, maxLines = 1, softWrap = false)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("🥈 2nd Rank", fontSize = 13.sp, color = TextPrimaryDark, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("₹${t.secondPrize}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), maxLines = 1, softWrap = false)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("🥉 3rd Rank", fontSize = 13.sp, color = TextPrimaryDark, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("₹${t.thirdPrize}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309), maxLines = 1, softWrap = false)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("🎯 Each Kill Reward", fontSize = 13.sp, color = TextPrimaryDark, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("₹${t.perKillReward}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FireOrangeDark, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }

                // Match Rules & Guidelines
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCardBg),
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
                                color = TextSecondaryMuted,
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
                                color = TextPrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                            colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                            border = BorderStroke(1.dp, SlateMutedBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = HighContrastDark
                                    ) {
                                        Text(
                                            text = "#${participant.slotNumber}",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = participant.ffIgn,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TextPrimaryDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "UID: ${participant.ffUid}",
                                            fontSize = 11.sp,
                                            color = TextSecondaryMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (t.status == "COMPLETED") {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Rank #${participant.placement} | ${participant.kills} Kills",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = TextPrimaryDark,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        if (participant.prizeWon > 0) {
                                            Text(
                                                text = "+₹${participant.prizeWon}",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp,
                                                color = SuccessGreen,
                                                maxLines = 1,
                                                softWrap = false
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

    // Official Free Fire Custom Room Card Slot Selection & Joining Modal
    if (showJoinDialog && tournament != null) {
        val t = tournament!!
        val userBal = currentUser?.walletBalance ?: 0
        val hasEnough = userBal >= t.entryFee
        val participantBySlot = remember(participants) {
            participants.associateBy { it.slotNumber }
        }
        val takenSlots = remember(participants) {
            participants.map { it.slotNumber }.toSet()
        }
        val availableCount = (t.maxSlots - takenSlots.size).coerceAtLeast(0)

        var sheetVisible by remember { mutableStateOf(false) }
        androidx.compose.runtime.LaunchedEffect(Unit) { sheetVisible = true }

        Dialog(
            onDismissRequest = { showJoinDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { showJoinDialog = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                val isCompactDialog = maxWidth < 360.dp

                androidx.compose.animation.AnimatedVisibility(
                    visible = sheetVisible,
                    enter = androidx.compose.animation.slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                        )
                    ) + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut()
                ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight * 0.92f)
                        .clickable(enabled = false) {}
                        .testTag("freefire_room_card_dialog"),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.5.dp, FireOrange)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Official Free Fire Room Card Top Bar with Drag Handle
                        Surface(
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .width(44.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color.White.copy(alpha = 0.35f))
                                )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(FireOrange.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SportsEsports,
                                            contentDescription = null,
                                            tint = FireOrange,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "FREE FIRE ROOM CARD",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.8.sp,
                                            color = TrophyGold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = t.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { showJoinDialog = false },
                                    modifier = Modifier.size(32.dp).testTag("close_room_card_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            }
                        }

                        // Tactical Room Specs & Quick Seat Banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF131824))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Surface(
                                    color = FireOrange.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = t.matchType,
                                        color = FireOrange,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    color = Color(0xFF334155),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = t.map.uppercase(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "PLAYERS: ${participants.size}/${t.maxSlots} • OPEN: $availableCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (availableCount > 0) SuccessGreen else Color(0xFFEF4444),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Main Scrollable Body
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Legend & Auto Seat Button using FlowRow so it never overflows
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (chosenSlot == null) FireOrange else Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, if (chosenSlot == null) TrophyGold else Color(0xFF475569)),
                                    modifier = Modifier
                                        .clickable { chosenSlot = null }
                                        .testTag("auto_seat_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FlashOn,
                                            contentDescription = null,
                                            tint = if (chosenSlot == null) Color.White else TrophyGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "⚡ AUTO-ASSIGN SEAT",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (chosenSlot == null) Color.White else Color(0xFFCBD5E1),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                // Legend Indicator
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(7.dp).background(Color(0xFFEF4444), CircleShape))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Taken", fontSize = 10.sp, color = Color(0xFF94A3B8), maxLines = 1)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(7.dp).background(SuccessGreen, CircleShape))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Open", fontSize = 10.sp, color = Color(0xFF94A3B8), maxLines = 1)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(7.dp).background(FireOrange, CircleShape))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Your Seat", fontSize = 10.sp, color = FireOrange, maxLines = 1)
                                    }
                                }
                            }

                            // Room Cards Matrix (Official Free Fire Lobby Style)
                            val teamSize = when (t.matchType) {
                                "SQUAD" -> 4
                                "DUO" -> 2
                                else -> 1
                            }

                            if (teamSize > 1) {
                                // Team based layout (Squad / Duo) - use 2 columns per row so each slot card has ample room for text & badges
                                val teamCount = (t.maxSlots + teamSize - 1) / teamSize
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    (0 until teamCount).forEach { teamIdx ->
                                        val teamNum = teamIdx + 1
                                        val teamStartSlot = (teamIdx * teamSize) + 1
                                        val teamEndSlot = minOf(teamStartSlot + teamSize - 1, t.maxSlots)
                                        val teamSlots = (teamStartSlot..teamEndSlot).toList()

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF161E2E),
                                            border = BorderStroke(1.dp, Color(0xFF334155)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(
                                                    text = "TEAM $teamNum",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = TrophyGold,
                                                    letterSpacing = 0.5.sp,
                                                    maxLines = 1
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                teamSlots.chunked(2).forEachIndexed { chunkIdx, rowSlots ->
                                                    if (chunkIdx > 0) {
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                    }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        rowSlots.forEach { slotNum ->
                                                            val participant = participantBySlot[slotNum]
                                                            val isTaken = participant != null
                                                            val isSelected = chosenSlot == slotNum

                                                            FreeFireSlotCard(
                                                                slotNum = slotNum,
                                                                isTaken = isTaken,
                                                                isSelected = isSelected,
                                                                occupantIgn = participant?.ffIgn,
                                                                currentUserName = inputIgn.ifBlank { currentUser?.ffIgn ?: "You" },
                                                                modifier = Modifier.weight(1f),
                                                                onClick = {
                                                                    if (!isTaken) {
                                                                        chosenSlot = slotNum
                                                                    } else {
                                                                        Toast.makeText(context, "Slot #$slotNum is already taken!", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            )
                                                        }
                                                        if (rowSlots.size < 2) {
                                                            Spacer(modifier = Modifier.weight(1f))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Solo room layout: Clean 2-column Free Fire Custom Room Grid
                                val totalSlots = t.maxSlots
                                val rows = (totalSlots + 1) / 2
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    (0 until rows).forEach { rowIdx ->
                                        val start = (rowIdx * 2) + 1
                                        val end = minOf(start + 1, totalSlots)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            (start..end).forEach { slotNum ->
                                                val participant = participantBySlot[slotNum]
                                                val isTaken = participant != null
                                                val isSelected = chosenSlot == slotNum

                                                FreeFireSlotCard(
                                                    slotNum = slotNum,
                                                    isTaken = isTaken,
                                                    isSelected = isSelected,
                                                    occupantIgn = participant?.ffIgn,
                                                    currentUserName = inputIgn.ifBlank { currentUser?.ffIgn ?: "You" },
                                                    modifier = Modifier.weight(1f),
                                                    onClick = {
                                                        if (!isTaken) {
                                                            chosenSlot = slotNum
                                                        } else {
                                                            Toast.makeText(context, "Slot #$slotNum is occupied by ${participant?.ffIgn}!", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                )
                                            }
                                            if (end - start + 1 < 2) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }

                            // Player UID & IGN Verification Section
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = FireOrange,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "FREE FIRE ID VERIFICATION",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Text(
                                        text = "Make sure your In-Game Name and Player UID are accurate. Room passwords & match rewards rely on this exact UID.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8),
                                        lineHeight = 15.sp
                                    )

                                    OutlinedTextField(
                                        value = inputIgn,
                                        onValueChange = { inputIgn = it },
                                        label = { Text("Free Fire In-Game Name (IGN) *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        placeholder = { Text("e.g. Thunder_Pro_99", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("join_ign_input")
                                    )

                                    OutlinedTextField(
                                        value = inputUid,
                                        onValueChange = { inputUid = it },
                                        label = { Text("Free Fire Player UID *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        placeholder = { Text("e.g. 1928471920", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("join_uid_input")
                                    )
                                }
                            }

                            if (!hasEnough) {
                                Surface(
                                    color = Color(0xFF451A1A),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "⚠️ Insufficient balance! Please top-up ₹${t.entryFee - userBal} in Wallet.",
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }

                        // Bottom Confirmation Action Bar
                        Surface(
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (chosenSlot != null) "SELECTED SEAT: SLOT #$chosenSlot" else "SEAT: AUTO-ASSIGN",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = TrophyGold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Entry: ₹${t.entryFee} • Wallet: ₹$userBal",
                                            fontSize = 11.sp,
                                            color = if (hasEnough) SuccessGreen else Color(0xFFEF4444),
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            if (!hasEnough) {
                                                showJoinDialog = false
                                                viewModel.navigateTo(Screen.Wallet)
                                                return@Button
                                            }
                                            if (inputIgn.isBlank() || inputUid.isBlank()) {
                                                Toast.makeText(context, "Please enter your Free Fire IGN and UID to proceed!", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            viewModel.updateGamingProfile(inputIgn.trim(), inputUid.trim())
                                            viewModel.joinTournament(t.id, inputIgn.trim(), inputUid.trim(), chosenSlot)
                                            showJoinDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (hasEnough) FireOrange else SuccessGreen
                                        ),
                                        contentPadding = PaddingValues(horizontal = if (isCompactDialog) 10.dp else 14.dp, vertical = 10.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("confirm_join_button")
                                    ) {
                                        val seatText = if (chosenSlot != null) "Slot #$chosenSlot" else "Auto Seat"
                                        Text(
                                            text = if (hasEnough) "CONFIRM & SIT ($seatText)" else "TOP-UP WALLET",
                                            fontWeight = FontWeight.Black,
                                            fontSize = if (isCompactDialog) 11.sp else 12.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
fun FreeFireSlotCard(
    slotNum: Int,
    isTaken: Boolean,
    isSelected: Boolean,
    occupantIgn: String?,
    currentUserName: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> Color(0xFF2E1C0A)
        isTaken -> Color(0xFF161C26)
        else -> Color(0xFF0F172A)
    }

    val borderColor = when {
        isSelected -> Color(0xFFFFB800)
        isTaken -> Color(0xFF283446)
        else -> Color(0xFF3B4A60)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = modifier
            .height(58.dp)
            .clickable(enabled = !isTaken, onClick = onClick)
            .testTag("slot_item_$slotNum")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Free Fire Tactical Slot Number Tag
                Surface(
                    color = if (isSelected) Color(0xFFFFB800) else if (isTaken) Color(0xFF2D3748) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "SLOT %02d".format(slotNum),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isSelected) Color.Black else if (isTaken) Color(0xFF94A3B8) else Color(0xFFCBD5E1),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                if (isTaken) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Taken",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "LOCKED",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else if (isSelected) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Selected",
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "YOUR SEAT",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFB800),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    Text(
                        text = "OPEN",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.Star else if (isTaken) Icons.Default.Person else Icons.Default.SportsEsports,
                    contentDescription = null,
                    tint = if (isSelected) Color(0xFFFFB800) else if (isTaken) Color(0xFF64748B) else SuccessGreen,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = when {
                        isSelected -> currentUserName
                        isTaken -> occupantIgn ?: "Occupied"
                        else -> "+ TAP TO SIT"
                    },
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                    color = when {
                        isSelected -> Color.White
                        isTaken -> Color(0xFF94A3B8)
                        else -> SuccessGreen
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
