package com.example.ui.screens.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firestore.UserDto
import com.example.data.firestore.formatCurrency
import com.example.ui.components.TopPlayersLeaderboardCard
import com.example.ui.theme.FireOrange
import com.example.ui.theme.FireOrangeDark
import com.example.ui.theme.HighContrastBorder
import com.example.ui.theme.HighContrastDark
import com.example.ui.theme.SlateCardBg
import com.example.ui.theme.SlateLightBg
import com.example.ui.theme.SlateMutedBorder
import com.example.ui.theme.SlateSubtle
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import com.example.ui.theme.TrophyGold
import com.example.viewmodel.AppViewModel

@Composable
fun TopPlayersScreen(viewModel: AppViewModel) {
    val topPlayers by viewModel.topPlayers.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val userParticipations by viewModel.userParticipations.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentUserId = currentUser?.id ?: ""

    val fullRankedPlayers = remember(allUsers, userParticipations, currentUserId) {
        val myWinCountFromParticipations = userParticipations.count {
            it.placement == 1 || (it.placement == 0 && it.prizeWon > 0)
        }
        allUsers
            .filter { !it.isSuspended }
            .map { user ->
                val resolvedWins = if (user.id == currentUserId && currentUserId.isNotBlank()) {
                    maxOf(user.effectiveTournamentWins, myWinCountFromParticipations)
                } else {
                    user.effectiveTournamentWins
                }
                user.copy(totalWins = resolvedWins)
            }
            .sortedWith(
                compareByDescending<UserDto> { it.totalWins }
                    .thenByDescending { it.totalWinnings }
                    .thenByDescending { it.totalKills }
                    .thenByDescending { it.matchesPlayed }
                    .thenBy { it.username.lowercase() }
            )
    }

    val myRankIndex = remember(fullRankedPlayers, currentUserId) {
        if (currentUserId.isBlank()) -1 else fullRankedPlayers.indexOfFirst { it.id == currentUserId }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("top_players_screen"),
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
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("top_players_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimaryDark
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(TrophyGold.copy(alpha = 0.25f), FireOrange.copy(alpha = 0.2f))
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Hall of Fame",
                            tint = TrophyGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Top Players Leaderboard",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Official Tournament Champions & Standings",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FireOrangeDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateLightBg)
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Current User Standing Banner
            if (currentUser != null && myRankIndex >= 0) {
                val myResolvedUser = fullRankedPlayers[myRankIndex]
                item {
                    CurrentUserStandingBanner(
                        rank = myRankIndex + 1,
                        totalPlayers = fullRankedPlayers.size,
                        user = myResolvedUser
                    )
                }
            }

            // Dedicated Top 5 Players Leaderboard Card
            item {
                TopPlayersLeaderboardCard(
                    users = topPlayers,
                    currentUserId = currentUserId
                )
            }

            // Additional Standings Beyond Top 5 (if more than 5 active players exist)
            if (fullRankedPlayers.size > 5) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Whatshot,
                                contentDescription = null,
                                tint = FireOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Challenger Standings (#6+)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimaryDark
                            )
                        }
                        Text(
                            text = "${fullRankedPlayers.size} Total Players",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondaryMuted
                        )
                    }
                }

                itemsIndexed(fullRankedPlayers.drop(5)) { index, player ->
                    val rank = index + 6
                    ChallengerStandingCard(
                        rank = rank,
                        player = player,
                        isCurrentUser = currentUserId.isNotBlank() && player.id == currentUserId
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CurrentUserStandingBanner(
    rank: Int,
    totalPlayers: Int,
    user: UserDto
) {
    val wins = user.effectiveTournamentWins
    val displayName = user.ffIgn.ifBlank { user.username.ifBlank { "Player" } }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("my_leaderboard_standing_card"),
        shape = RoundedCornerShape(16.dp),
        color = HighContrastDark,
        border = BorderStroke(1.dp, HighContrastBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FireOrange.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, FireOrange)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "YOUR RANK",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1),
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "#$rank",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = TrophyGold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Ranked #$rank of $totalPlayers active contenders",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = TrophyGold.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, TrophyGold.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MilitaryTech,
                        contentDescription = null,
                        tint = TrophyGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$wins ${if (wins == 1) "Win" else "Wins"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = TrophyGold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun ChallengerStandingCard(
    rank: Int,
    player: UserDto,
    isCurrentUser: Boolean
) {
    val wins = player.effectiveTournamentWins
    val displayName = player.ffIgn.ifBlank { player.username.ifBlank { "Player" } }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("challenger_row_$rank"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) FireOrange.copy(alpha = 0.08f) else SlateCardBg
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isCurrentUser) FireOrange.copy(alpha = 0.6f) else SlateMutedBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SlateSubtle,
                    border = BorderStroke(1.dp, SlateMutedBorder),
                    modifier = Modifier.size(width = 34.dp, height = 28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "#$rank",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isCurrentUser) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = FireOrange,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "YOU",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${player.matchesPlayed} Matches • ₹${player.totalWinnings.formatCurrency()} Won",
                        fontSize = 10.sp,
                        color = if (player.totalWinnings > 0) SuccessGreen else TextSecondaryMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SlateSubtle,
                border = BorderStroke(1.dp, SlateMutedBorder)
            ) {
                Text(
                    text = "$wins ${if (wins == 1) "Win" else "Wins"}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
        }
    }
}
