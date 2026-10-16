package com.example.ui.screens.player

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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.firestore.ParticipantDto
import com.example.data.firestore.TournamentDto
import com.example.ui.components.HighContrastCard
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: AppViewModel) {
    val userParticipations by viewModel.userParticipations.collectAsStateWithLifecycle()
    val allTournaments by viewModel.allTournaments.collectAsStateWithLifecycle()
    val userTransactions by viewModel.userTransactions.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("My Matches", "All Match Results", "Balance Log")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Match & Balance History",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("history_back_button")
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateLightBg)
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
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
                                fontSize = 13.sp,
                                color = if (selectedTabIndex == index) FireOrange else TextSecondaryMuted
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        // My Matches Tab
                        if (userParticipations.isEmpty()) {
                            item {
                                EmptyHistoryView("No registered matches yet. Join a tournament from the dashboard!")
                            }
                        } else {
                            items(userParticipations) { participation ->
                                val tournament = allTournaments.find { it.id == participation.tournamentId }
                                MyMatchCard(participation, tournament) {
                                    if (tournament != null) {
                                        viewModel.selectTournament(tournament.id)
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // All Match Results Tab
                        val completedMatches = allTournaments.filter { it.status == "COMPLETED" }
                        if (completedMatches.isEmpty()) {
                            item {
                                EmptyHistoryView("No concluded tournament matches yet.")
                            }
                        } else {
                            items(completedMatches) { match ->
                                CompletedMatchCard(match) {
                                    viewModel.selectTournament(match.id)
                                }
                            }
                        }
                    }
                    2 -> {
                        // Balance Log Tab
                        if (userTransactions.isEmpty()) {
                            item {
                                EmptyHistoryView("No transaction history recorded yet.")
                            }
                        } else {
                            items(userTransactions) { txn ->
                                TransactionItemCard(txn)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun MyMatchCard(
    participation: ParticipantDto,
    tournament: TournamentDto?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("my_match_card_${participation.tournamentId}"),
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
                    Surface(
                        color = HighContrastDark,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "SLOT #${participation.slotNumber}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tournament?.matchType ?: "BATTLE ROYALE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FireOrangeDark
                    )
                }

                StatusBadge(tournament?.status ?: "UPCOMING")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = tournament?.title ?: "Tournament #${participation.tournamentId}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )

            Text(
                text = "Player: ${participation.ffIgn} (UID: ${participation.ffUid})",
                fontSize = 11.sp,
                color = TextSecondaryMuted
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PLACEMENT", fontSize = 9.sp, color = TextSecondaryMuted, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (participation.placement > 0) "#${participation.placement}" else "Pending",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (participation.placement == 1) TrophyGold else TextPrimaryDark
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("KILLS", fontSize = 9.sp, color = TextSecondaryMuted, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${participation.kills}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = FireOrangeDark
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PRIZE WON", fontSize = 9.sp, color = TextSecondaryMuted, fontWeight = FontWeight.Bold)
                    Text(
                        text = "₹${participation.prizeWon}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (participation.prizeWon > 0) SuccessGreen else TextSecondaryMuted
                    )
                }
            }
        }
    }
}

@Composable
fun CompletedMatchCard(
    match: TournamentDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("completed_match_card_${match.id}"),
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
                    MatchTypeBadge(match.matchType)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = match.map,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                }
                StatusBadge(match.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = match.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🏆 Prize Disbursed: ₹${match.prizePool}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TrophyGold
                )
                Text(
                    text = "Tap for Scoreboard →",
                    fontSize = 11.sp,
                    color = FireOrangeDark,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmptyHistoryView(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.MilitaryTech,
            contentDescription = null,
            tint = Color(0xFFCBD5E1),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            fontSize = 13.sp,
            color = TextSecondaryMuted,
            fontWeight = FontWeight.Medium
        )
    }
}
