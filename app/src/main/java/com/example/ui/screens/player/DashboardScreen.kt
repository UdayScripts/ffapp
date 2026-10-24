package com.example.ui.screens.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import com.example.data.firestore.formatCurrency
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firestore.TournamentDto
import com.example.ui.components.HighContrastCard
import com.example.ui.components.MatchTypeBadge
import com.example.ui.components.StatusBadge
import com.example.ui.components.TopPlayersLeaderboardCard
import com.example.ui.theme.FireOrange
import com.example.ui.theme.FireOrangeDark
import com.example.ui.theme.HighContrastBorder
import com.example.ui.theme.HighContrastDark
import com.example.ui.theme.HighContrastSurface
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

@Composable
fun DashboardScreen(viewModel: AppViewModel) {
    val tournaments by viewModel.filteredTournaments.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val topPlayers by viewModel.topPlayers.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val selectedMode by viewModel.selectedModeFilter.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()
    val unreadNotifs by viewModel.unreadNotifCount.collectAsStateWithLifecycle()
    val unreadSupport by viewModel.userUnreadSupportCount.collectAsStateWithLifecycle()
    val isRealAdmin by viewModel.isRealAdmin.collectAsStateWithLifecycle()

    val modeOptions = listOf("ALL", "SOLO", "DUO", "SQUAD")
    val statusTabs = listOf("UPCOMING", "LIVE", "COMPLETED")

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateLightBg)
    ) {
        val isCompact = maxWidth < 360.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SlateCardBg,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = if (isCompact) 12.dp else 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // App Logo & Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "eSports Arena Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(if (isCompact) 36.dp else 40.dp)
                                .clip(RoundedCornerShape(9.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val resolvedAppName = appSettings?.appName
                                ?.takeIf { it.isNotBlank() && it != "FF Tournaments" }
                                ?: "eSports Arena"
                            Text(
                                text = resolvedAppName,
                                fontWeight = FontWeight.Black,
                                fontSize = if (isCompact) 15.sp else 17.sp,
                                color = TextPrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Official Tournament Hub",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = FireOrangeDark,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Actions: Admin Portal, Wallet Pill, Support Chat, Notifications
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Admin Portal Button (Strictly only shown to verified Master Admin udaynath381@gmail.com)
                        if (isRealAdmin) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { viewModel.navigateTo(Screen.AdminDashboard) }
                                    .testTag("admin_switch_button"),
                                color = HighContrastDark,
                                border = BorderStroke(1.dp, HighContrastBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = "Admin Portal",
                                        tint = FireOrange,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Admin",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        // Wallet Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { viewModel.navigateTo(Screen.Wallet) }
                                .testTag("dashboard_wallet_pill"),
                            color = SlateSubtle,
                            border = BorderStroke(1.dp, SlateMutedBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Wallet",
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "₹${currentUser?.walletBalance.formatCurrency()}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = TextPrimaryDark,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        // Live Support Chat Icon with unread badge
                        IconButton(
                            onClick = { viewModel.openUserSupportChat() },
                            modifier = Modifier.size(36.dp).testTag("dashboard_support_chat_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadSupport > 0) {
                                        Badge(containerColor = LiveRed) {
                                            Text("$unreadSupport", color = Color.White, fontSize = 9.sp, maxLines = 1)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SupportAgent,
                                    contentDescription = "Live Support Chat",
                                    tint = FireOrangeDark,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }

                        // Notifications Icon with badge
                        IconButton(
                            onClick = { viewModel.navigateTo(Screen.Notifications) },
                            modifier = Modifier.size(36.dp).testTag("dashboard_notifications_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifs > 0) {
                                        Badge(containerColor = FireOrange) {
                                            Text("$unreadNotifs", color = Color.White, fontSize = 9.sp, maxLines = 1)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = TextPrimaryDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isCompact) 12.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                }


                // Announcement Marquee Hero Card
                if (!appSettings?.announcementText.isNullOrBlank()) {
                    item {
                        HighContrastCard(modifier = Modifier.testTag("announcement_banner")) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(FireOrange.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Campaign,
                                        contentDescription = "Announcement",
                                        tint = FireOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "OFFICIAL ANNOUNCEMENT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FireOrange,
                                        letterSpacing = 1.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = appSettings!!.announcementText,
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Status Tabs (Upcoming, Live, Completed)
                item {
                    ScrollableTabRow(
                        selectedTabIndex = statusTabs.indexOf(selectedStatus).coerceAtLeast(0),
                        containerColor = Color.Transparent,
                        contentColor = FireOrange,
                        edgePadding = 0.dp,
                        indicator = { tabPositions ->
                            val index = statusTabs.indexOf(selectedStatus).coerceAtLeast(0)
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[index]),
                                color = FireOrange,
                                height = 3.dp
                            )
                        },
                        divider = {}
                    ) {
                        statusTabs.forEach { status ->
                            val isSelected = selectedStatus == status
                            Tab(
                                selected = isSelected,
                                onClick = { viewModel.setStatusFilter(status) },
                                text = {
                                    Text(
                                        text = when (status) {
                                            "UPCOMING" -> "Upcoming"
                                            "LIVE" -> "● Live Now"
                                            "COMPLETED" -> "Results"
                                            else -> status
                                        },
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isSelected) FireOrange else TextSecondaryMuted,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            )
                        }
                    }
                }

                // Mode Filter Chips (All, Solo, Duo, Squad)
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(modeOptions) { mode ->
                            val isSelected = selectedMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setModeFilter(mode) },
                                label = {
                                    Text(
                                        text = if (mode == "ALL") "All Modes" else mode,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FireOrange,
                                    selectedLabelColor = Color.White,
                                    containerColor = SlateCardBg,
                                    labelColor = TextSecondaryMuted
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) FireOrange else SlateMutedBorder
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                }

                // Tournament Count Info
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${tournaments.size} ${if (tournaments.size == 1) "Match" else "Matches"} Available",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Tournament Cards
                if (tournaments.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                            border = BorderStroke(1.dp, SlateMutedBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No matches found in this category",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "Try selecting 'All Modes' or check back soon for new tournaments.",
                                    fontSize = 12.sp,
                                    color = TextSecondaryMuted
                                )
                            }
                        }
                    }
                } else {
                    items(tournaments) { tournament ->
                        TournamentCard(
                            tournament = tournament,
                            onCardClick = { viewModel.selectTournament(tournament.id) }
                        )
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
fun TournamentCard(
    tournament: TournamentDto,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCardClick() }
            .testTag("tournament_card_${tournament.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SlateCardBg,
            contentColor = TextPrimaryDark
        ),
        border = BorderStroke(1.dp, SlateMutedBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Map & MatchType & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    MatchTypeBadge(tournament.matchType)
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = SlateSubtle,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = tournament.map,
                            color = TextSecondaryMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                StatusBadge(tournament.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tournament Title
            Text(
                text = tournament.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Schedule
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = TextSecondaryMuted,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = tournament.scheduleTime,
                    fontSize = 12.sp,
                    color = TextSecondaryMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics Grid: Prize Pool, Per Kill, Entry Fee
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateSubtle, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("PRIZE POOL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondaryMuted, maxLines = 1)
                    Text(
                        text = "₹${tournament.prizePool}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = TrophyGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("PER KILL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondaryMuted, maxLines = 1)
                    Text(
                        text = "₹${tournament.perKillReward}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = FireOrangeDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("ENTRY FEE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondaryMuted, maxLines = 1)
                    Text(
                        text = if (tournament.entryFee == 0) "FREE" else "₹${tournament.entryFee}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Slots Filled Progress Bar
            val progress = (tournament.joinedSlots.toFloat() / tournament.maxSlots.toFloat()).coerceIn(0f, 1f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spots Filled: ${tournament.joinedSlots}/${tournament.maxSlots}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondaryMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${tournament.maxSlots - tournament.joinedSlots} left",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (tournament.maxSlots - tournament.joinedSlots <= 5) LiveRed else SuccessGreen,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = FireOrange,
                trackColor = Color(0xFFE2E8F0)
            )

            // Room ID alert indicator if available
            if (!tournament.roomId.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF2F2), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🔑 Room ID & Pass is ready! Tap to view.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LiveRed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
