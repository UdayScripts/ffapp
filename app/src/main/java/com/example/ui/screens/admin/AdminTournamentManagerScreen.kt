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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import com.example.data.firestore.TournamentDto
import com.example.ui.components.MatchTypeBadge
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
fun AdminTournamentManagerScreen(viewModel: AppViewModel) {
    val tournaments by viewModel.allTournaments.collectAsStateWithLifecycle()

    var showRoomDialogForTournament by remember { mutableStateOf<TournamentDto?>(null) }
    var deleteConfirmTournament by remember { mutableStateOf<TournamentDto?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tournament Manager",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("admin_tourneys_back_button")
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.selectTournamentForEdit("") // blank = new
                },
                containerColor = FireOrange,
                contentColor = Color.White,
                modifier = Modifier.testTag("create_tournament_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Create Tournament")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateLightBg)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "${tournaments.size} Total Tournaments",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondaryMuted
                )
            }

            items(tournaments) { t ->
                AdminTournamentItemCard(
                    tournament = t,
                    onPublishRoom = { showRoomDialogForTournament = t },
                    onEdit = { viewModel.selectTournamentForEdit(t.id) },
                    onSettle = { viewModel.selectTournamentForSettlement(t.id) },
                    onDelete = { deleteConfirmTournament = t }
                )
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }

    // Room ID & Pass Update Dialog with Instant Real-Time Alert Broadcast
    showRoomDialogForTournament?.let { t ->
        var roomIdInput by remember(t.id) { mutableStateOf(t.roomId ?: "") }
        var roomPassInput by remember(t.id) { mutableStateOf(t.roomPassword ?: "") }
        var roomNoticeInput by remember(t.id) { mutableStateOf(t.roomNotice ?: "Join Custom Room now!") }

        AlertDialog(
            onDismissRequest = { showRoomDialogForTournament = null },
            title = {
                Text("Publish Room Credentials", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Publishing will immediately reveal keys in player dashboard and broadcast an alert.",
                        fontSize = 12.sp,
                        color = TextSecondaryMuted
                    )

                    OutlinedTextField(
                        value = roomIdInput,
                        onValueChange = { roomIdInput = it },
                        label = { Text("Custom Room ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_room_id_input")
                    )

                    OutlinedTextField(
                        value = roomPassInput,
                        onValueChange = { roomPassInput = it },
                        label = { Text("Room Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_room_pass_input")
                    )

                    OutlinedTextField(
                        value = roomNoticeInput,
                        onValueChange = { roomNoticeInput = it },
                        label = { Text("Notice / Instructions") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("admin_room_notice_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (roomIdInput.isNotBlank() && roomPassInput.isNotBlank()) {
                            viewModel.updateRoomCredentials(t.id, roomIdInput, roomPassInput, roomNoticeInput)
                            showRoomDialogForTournament = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                    modifier = Modifier.testTag("admin_save_room_button")
                ) {
                    Text("Publish & Notify Players")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRoomDialogForTournament = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirm Delete Dialog
    deleteConfirmTournament?.let { t ->
        AlertDialog(
            onDismissRequest = { deleteConfirmTournament = null },
            title = { Text("Delete Tournament?") },
            text = { Text("Are you sure you want to delete '${t.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTournament(t.id)
                        deleteConfirmTournament = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmTournament = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminTournamentItemCard(
    tournament: TournamentDto,
    onPublishRoom: () -> Unit,
    onEdit: () -> Unit,
    onSettle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateMutedBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MatchTypeBadge(tournament.matchType)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tournament.map,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                }
                StatusBadge(tournament.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = tournament.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimaryDark
            )

            Text(
                text = "Time: ${tournament.scheduleTime}  •  Slots: ${tournament.joinedSlots}/${tournament.maxSlots}",
                fontSize = 12.sp,
                color = TextSecondaryMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Room status line
            if (!tournament.roomId.isNullOrBlank()) {
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Room ID: ${tournament.roomId}  |  Password: ${tournament.roomPassword}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Publish / Edit Room Credentials
                Button(
                    onClick = onPublishRoom,
                    colors = ButtonDefaults.buttonColors(containerColor = HighContrastDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp), tint = FireOrange)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (tournament.roomId.isNullOrBlank()) "Add Room" else "Room Keys",
                        fontSize = 12.sp
                    )
                }

                // Settle
                if (tournament.status != "COMPLETED") {
                    OutlinedButton(
                        onClick = onSettle,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(14.dp), tint = TrophyGold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Settle", fontSize = 12.sp, color = TextPrimaryDark)
                    }
                }

                // Edit Button
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = TextPrimaryDark, modifier = Modifier.size(18.dp))
                }

                // Delete Button
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
