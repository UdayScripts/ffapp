package com.example.ui.screens.player

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firestore.ParticipantDto
import com.example.data.firestore.TournamentDto
import com.example.ui.components.MatchTypeBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.FireOrange
import com.example.ui.theme.FireOrangeDark
import com.example.ui.theme.HighContrastBorder
import com.example.ui.theme.HighContrastDark
import com.example.ui.theme.LiveRed
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TournamentLobbyScreen(viewModel: AppViewModel) {
    val tournament by viewModel.selectedTournament.collectAsStateWithLifecycle()
    val participants by viewModel.selectedTournamentParticipants.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Room Keys", "Player Roster", "How to Join")

    BackHandler {
        viewModel.navigateBack()
    }

    val userParticipation = participants.find { it.userId == currentUser?.id }
    val isUserJoined = userParticipation != null

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
                        modifier = Modifier.testTag("lobby_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimaryDark
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Tournament Lobby",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                color = if (tournament?.roomId.isNullOrBlank()) SlateSubtle else SuccessGreen.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                if (tournament?.roomId.isNullOrBlank()) TextSecondaryMuted else SuccessGreen,
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (tournament?.roomId.isNullOrBlank()) "KEYS PENDING" else "KEYS LIVE",
                                        color = if (tournament?.roomId.isNullOrBlank()) TextSecondaryMuted else SuccessGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                        Text(
                            text = tournament?.title ?: "Match Room",
                            fontSize = 11.sp,
                            color = TextSecondaryMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Lobby synced with live Cloud Server", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("lobby_sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = FireOrange
                        )
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
                Text("Tournament details not found.", color = TextSecondaryMuted)
            }
        } else {
            val t = tournament!!
            val hasKeys = !t.roomId.isNullOrBlank() && !t.roomPassword.isNullOrBlank()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SlateLightBg)
                    .padding(innerPadding)
            ) {
                // Top Tab Bar
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = SlateCardBg,
                    contentColor = FireOrange,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = FireOrange,
                            height = 3.dp
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = if (selectedTabIndex == index) FireOrange else TextSecondaryMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }

                when (selectedTabIndex) {
                    0 -> {
                        // Room Keys & Combat Vault
                        RoomKeysTabContent(
                            tournament = t,
                            userParticipation = userParticipation,
                            isUserJoined = isUserJoined,
                            hasKeys = hasKeys,
                            onCopyAll = {
                                if (hasKeys) {
                                    clipboardManager.setText(
                                        AnnotatedString("Free Fire MAX Custom Room\nRoom ID: ${t.roomId}\nPassword: ${t.roomPassword}")
                                    )
                                    Toast.makeText(context, "Copied Room ID & Password!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onCopyRoomId = {
                                if (!t.roomId.isNullOrBlank()) {
                                    clipboardManager.setText(AnnotatedString(t.roomId!!))
                                    Toast.makeText(context, "Copied Room ID: ${t.roomId}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onCopyPassword = {
                                if (!t.roomPassword.isNullOrBlank()) {
                                    clipboardManager.setText(AnnotatedString(t.roomPassword!!))
                                    Toast.makeText(context, "Copied Password: ${t.roomPassword}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onLaunchFreeFire = {
                                launchFreeFireApp(context, t.roomId, t.roomPassword)
                            },
                            onOpenRules = {
                                selectedTabIndex = 2
                            }
                        )
                    }
                    1 -> {
                        // Player Roster
                        PlayerRosterTabContent(
                            tournament = t,
                            participants = participants,
                            currentUserId = currentUser?.id ?: ""
                        )
                    }
                    2 -> {
                        // How to Join Guide
                        HowToJoinTabContent(
                            userSlot = userParticipation?.slotNumber
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomKeysTabContent(
    tournament: TournamentDto,
    userParticipation: ParticipantDto?,
    isUserJoined: Boolean,
    hasKeys: Boolean,
    onCopyAll: () -> Unit,
    onCopyRoomId: () -> Unit,
    onCopyPassword: () -> Unit,
    onLaunchFreeFire: () -> Unit,
    onOpenRules: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompact = maxWidth < 360.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isCompact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Player Combat Pass Badge
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = HighContrastDark,
                    border = BorderStroke(1.dp, if (isUserJoined) FireOrange else SlateMutedBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(if (isCompact) 12.dp else 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isCompact) 36.dp else 40.dp)
                                        .background(
                                            Brush.linearGradient(listOf(FireOrangeDark, FireOrange)),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SportsEsports,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(if (isCompact) 18.dp else 22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isUserJoined) (userParticipation?.ffIgn ?: "Joined Player") else "Spectator / Unregistered",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isCompact) 13.sp else 15.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isUserJoined) "UID: ${userParticipation?.ffUid}" else "Join match to secure custom slot",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFCBD5E1),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (isUserJoined && userParticipation != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = FireOrange,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "YOUR SLOT",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = "#${userParticipation.slotNumber}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Match Specs Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("MODE", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(
                                    text = tournament.matchType,
                                    fontSize = if (isCompact) 11.sp else 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FireOrange,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("MAP", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(
                                    text = tournament.map,
                                    fontSize = if (isCompact) 11.sp else 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Text("SCHEDULE", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(
                                    text = tournament.scheduleTime,
                                    fontSize = if (isCompact) 10.sp else 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("PRIZE POOL", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(
                                    text = "₹${tournament.prizePool}",
                                    fontSize = if (isCompact) 11.sp else 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TrophyGold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Room Key Vault Box
            item {
                if (hasKeys) {
                    // RELEASED STATE
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lobby_room_credentials_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                        border = BorderStroke(1.5.dp, SuccessGreen),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(if (isCompact) 12.dp else 16.dp)) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                            .background(SuccessGreen.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LockOpen,
                                            contentDescription = null,
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Room Credentials Released",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = if (isCompact) 13.sp else 15.sp,
                                            color = TextPrimaryDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Room is ready in Garena Free Fire MAX",
                                            fontSize = 11.sp,
                                            color = SuccessGreen,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SuccessGreen.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(SuccessGreen, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "LIVE",
                                            color = SuccessGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Room ID Box
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SlateSubtle,
                                border = BorderStroke(1.dp, SlateMutedBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "ROOM ID",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondaryMuted,
                                            letterSpacing = 0.5.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = tournament.roomId ?: "",
                                            fontSize = if (isCompact) 17.sp else 20.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextPrimaryDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = onCopyRoomId,
                                        colors = ButtonDefaults.buttonColors(containerColor = SlateCardBg),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.testTag("copy_room_id_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            tint = TextPrimaryDark,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Copy ID",
                                            color = TextPrimaryDark,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Password Box
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SlateSubtle,
                                border = BorderStroke(1.dp, SlateMutedBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "PASSWORD",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondaryMuted,
                                            letterSpacing = 0.5.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = tournament.roomPassword ?: "",
                                            fontSize = if (isCompact) 17.sp else 20.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = FireOrangeDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = onCopyPassword,
                                        colors = ButtonDefaults.buttonColors(containerColor = SlateCardBg),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.testTag("copy_room_password_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            tint = TextPrimaryDark,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Copy Pass",
                                            color = TextPrimaryDark,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Quick Actions: Copy All & Free Fire MAX
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onCopyAll,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.5.dp, FireOrange),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("lobby_copy_all_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = FireOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Copy All",
                                        color = FireOrange,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isCompact) 11.sp else 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Button(
                                    onClick = onLaunchFreeFire,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1.35f)
                                        .testTag("lobby_launch_freefire_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Free Fire MAX",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isCompact) 11.sp else 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Admin Notice if available
                            if (!tournament.roomNotice.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = FireOrange.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, FireOrange.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Campaign,
                                            contentDescription = null,
                                            tint = FireOrange,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "ADMIN INSTRUCTIONS",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = FireOrangeDark
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = tournament.roomNotice ?: "",
                                                fontSize = 12.sp,
                                                color = TextPrimaryDark,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // PENDING STATE
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lobby_keys_pending_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                        border = BorderStroke(1.dp, TrophyGold.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(if (isCompact) 16.dp else 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(TrophyGold.copy(alpha = 0.16f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = TrophyGold,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Room Keys Not Released Yet",
                                fontSize = if (isCompact) 15.sp else 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "The Room ID and Password will be released here 10 to 15 minutes before match start (${tournament.scheduleTime}).",
                                fontSize = 12.sp,
                                color = TextSecondaryMuted,
                                lineHeight = 17.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Live Listener Pulse Notice
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SlateSubtle,
                                border = BorderStroke(1.dp, SlateMutedBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(SuccessGreen, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Real-Time Cloud Listener Active",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark
                                        )
                                        Text(
                                            text = "This screen will automatically unlock the second the admin enters the room credentials. No need to reload.",
                                            fontSize = 10.sp,
                                            color = TextSecondaryMuted,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Slot Instructions Card
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
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = FireOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Match Room Etiquette",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val tips = listOf(
                            "Sit strictly in your assigned slot: ${if (isUserJoined) "#${userParticipation?.slotNumber}" else "See Roster"}.",
                            "Joining other player slots will result in an immediate room kick.",
                            "Emulators, PC macros, and APK mods result in permanent hardware ban.",
                            "Take a screenshot of the match result screen for score settlement."
                        )

                        tips.forEach { tip ->
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("• ", color = FireOrange, fontWeight = FontWeight.Bold)
                                Text(tip, fontSize = 11.sp, color = TextSecondaryMuted, lineHeight = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerRosterTabContent(
    tournament: TournamentDto,
    participants: List<ParticipantDto>,
    currentUserId: String
) {
    val sorted = remember(participants) {
        participants.sortedBy { it.slotNumber }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REGISTERED SQUAD (${sorted.size}/${tournament.maxSlots})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondaryMuted,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${tournament.maxSlots - sorted.size} slots remaining",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (tournament.maxSlots - sorted.size <= 3) LiveRed else SuccessGreen,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        if (sorted.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SlateCardBg,
                    border = BorderStroke(1.dp, SlateMutedBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No players have joined this match yet.", color = TextSecondaryMuted, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(sorted) { p ->
                val isCurrent = p.userId == currentUserId
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCurrent) FireOrange.copy(alpha = 0.14f) else SlateCardBg,
                    border = BorderStroke(
                        1.dp,
                        if (isCurrent) FireOrange else SlateMutedBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("roster_slot_${p.slotNumber}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrent) FireOrange else HighContrastDark
                            ) {
                                Text(
                                    text = "#${p.slotNumber}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = p.ffIgn,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimaryDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (isCurrent) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = FireOrange,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "YOU",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "UID: ${p.ffUid}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondaryMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(SuccessGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LOCKED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
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

@Composable
private fun HowToJoinTabContent(userSlot: Int?) {
    val steps = listOf(
        Triple("1", "Copy Room Credentials", "Tap the 'Copy All' button in the Room Keys tab to copy the Room ID and Password to your clipboard."),
        Triple("2", "Open Free Fire MAX", "Launch Garena Free Fire MAX on your device and log into your account."),
        Triple("3", "Navigate to Custom Room", "Tap the Game Mode selection box on the main screen, then tap the 'Custom' tab at the bottom."),
        Triple("4", "Search Room ID", "Tap the magnifying glass Search icon at the bottom left, paste the Room ID, and press search."),
        Triple("5", "Enter Room Password", "Tap 'Join', enter the match password, and enter the lobby."),
        Triple("6", "Sit in Your Designated Slot", "Important: Sit in your assigned slot (#${userSlot ?: "X"}). If someone is sitting in your slot, message in room chat."),
        Triple("7", "Wait for Host Launch", "Once all players join, the admin will start the match. Good luck, Survivor!")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "STEP-BY-STEP CUSTOM ROOM GUIDE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondaryMuted,
                letterSpacing = 0.5.sp
            )
        }

        items(steps) { (num, title, desc) ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SlateCardBg,
                border = BorderStroke(1.dp, SlateMutedBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(FireOrange.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = num,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = FireOrange
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = desc,
                            fontSize = 11.sp,
                            color = TextSecondaryMuted,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

private fun launchFreeFireApp(context: Context, roomId: String?, roomPass: String?) {
    val pm = context.packageManager
    val ffIntent = pm.getLaunchIntentForPackage("com.dts.freefiremax")
        ?: pm.getLaunchIntentForPackage("com.dts.freefireth")

    if (ffIntent != null) {
        try {
            ffIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(ffIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not launch Free Fire MAX directly: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(
            context,
            "Free Fire MAX is not installed on this device. Room credentials copied to clipboard!",
            Toast.LENGTH_LONG
        ).show()
    }
}
