package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.firestore.UserDto
import com.example.data.firestore.formatCurrency
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

@Composable
fun TopPlayersLeaderboardCard(
    users: List<UserDto>,
    currentUserId: String = "",
    modifier: Modifier = Modifier
) {
    val topFivePlayers = remember(users) {
        users
            .filter { !it.isSuspended }
            .sortedWith(
                compareByDescending<UserDto> { it.effectiveTournamentWins }
                    .thenByDescending { it.totalWinnings }
                    .thenByDescending { it.totalKills }
                    .thenByDescending { it.matchesPlayed }
                    .thenBy { it.username.lowercase() }
            )
            .take(5)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("top_players_leaderboard_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SlateCardBg,
            contentColor = TextPrimaryDark
        ),
        border = BorderStroke(1.dp, SlateMutedBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Card Header
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
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(TrophyGold.copy(alpha = 0.25f), FireOrange.copy(alpha = 0.18f))
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Top Players Trophy",
                            tint = TrophyGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Top Players",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Top 5 Leaderboard • Total Tournament Wins",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondaryMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = HighContrastDark,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, TrophyGold.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MilitaryTech,
                            contentDescription = null,
                            tint = TrophyGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "TOP 5",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TrophyGold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (topFivePlayers.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("top_players_empty_state"),
                    shape = RoundedCornerShape(12.dp),
                    color = SlateSubtle,
                    border = BorderStroke(1.dp, SlateMutedBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Leaderboard rankings updating",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Win tournaments to claim the #1 spot on the leaderboard!",
                            fontSize = 11.sp,
                            color = TextSecondaryMuted
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    topFivePlayers.forEachIndexed { index, player ->
                        val rank = index + 1
                        TopPlayerLeaderboardRow(
                            rank = rank,
                            player = player,
                            isCurrentUser = currentUserId.isNotBlank() && player.id == currentUserId
                        )
                        if (index < topFivePlayers.lastIndex) {
                            HorizontalDivider(
                                color = SlateMutedBorder.copy(alpha = 0.45f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopPlayerLeaderboardRow(
    rank: Int,
    player: UserDto,
    isCurrentUser: Boolean
) {
    val wins = player.effectiveTournamentWins
    val displayName = player.ffIgn.ifBlank { player.username.ifBlank { "Player" } }
    val secondaryHandle = if (player.ffIgn.isNotBlank() && !player.ffIgn.equals(player.username, ignoreCase = true) && player.username.isNotBlank()) {
        "@${player.username}"
    } else if (player.ffUid.isNotBlank()) {
        "UID: ${player.ffUid}"
    } else {
        "${player.matchesPlayed} Matches"
    }

    val (rankBgColor, rankBorderColor, rankTextColor) = when (rank) {
        1 -> Triple(TrophyGold.copy(alpha = 0.18f), TrophyGold, Color(0xFFB45309))
        2 -> Triple(Color(0xFF94A3B8).copy(alpha = 0.2f), Color(0xFF64748B), Color(0xFF334155))
        3 -> Triple(FireOrange.copy(alpha = 0.15f), FireOrange, FireOrangeDark)
        else -> Triple(SlateSubtle, SlateMutedBorder, TextSecondaryMuted)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("top_player_row_$rank"),
        shape = RoundedCornerShape(12.dp),
        color = if (isCurrentUser) FireOrange.copy(alpha = 0.08f) else SlateSubtle.copy(alpha = 0.65f),
        border = BorderStroke(
            width = if (rank == 1 || isCurrentUser) 1.dp else 0.5.dp,
            color = when {
                isCurrentUser -> FireOrange.copy(alpha = 0.6f)
                rank == 1 -> TrophyGold.copy(alpha = 0.6f)
                else -> SlateMutedBorder
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Rank Badge + Avatar + Player Name & Stats
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Rank Pill (#1 - #5)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = rankBgColor,
                    border = BorderStroke(1.dp, rankBorderColor),
                    modifier = Modifier.size(width = 32.dp, height = 28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "#$rank",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = rankTextColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Avatar Initial Circle
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            color = if (rank == 1) HighContrastDark else FireOrange.copy(alpha = 0.14f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName.trim().take(1).uppercase().ifBlank { "P" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (rank == 1) TrophyGold else FireOrangeDark,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Player Name + Stats
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
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
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = secondaryHandle,
                            fontSize = 10.sp,
                            color = TextSecondaryMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (player.totalWinnings > 0) {
                            Text(
                                text = "• ₹${player.totalWinnings.formatCurrency()} won",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SuccessGreen,
                                maxLines = 1,
                                softWrap = false
                            )
                        } else if (player.totalKills > 0) {
                            Text(
                                text = "• ${player.totalKills} Kills",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondaryMuted,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Total Tournament Wins Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (wins > 0) TrophyGold.copy(alpha = 0.16f) else SlateCardBg,
                border = BorderStroke(
                    1.dp,
                    if (wins > 0) TrophyGold.copy(alpha = 0.5f) else SlateMutedBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = if (wins > 0) TrophyGold else TextSecondaryMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$wins ${if (wins == 1) "Win" else "Wins"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (wins > 0) TextPrimaryDark else TextSecondaryMuted,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
