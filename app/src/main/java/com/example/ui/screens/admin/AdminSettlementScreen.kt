package com.example.ui.screens.admin

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firestore.SettleData
import com.example.ui.components.HighContrastCard
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSettlementScreen(viewModel: AppViewModel) {
    val allTournaments by viewModel.allTournaments.collectAsStateWithLifecycle()
    val selectedTourney by viewModel.selectedTournament.collectAsStateWithLifecycle()
    val participants by viewModel.selectedTournamentParticipants.collectAsStateWithLifecycle()

    var dropdownExpanded by remember { mutableStateOf(false) }

    // Map participantId -> Pair(Kills, Rank)
    val killsMap = remember(participants) {
        mutableStateMapOf<String, String>().apply {
            participants.forEach { this[it.id] = it.kills.toString() }
        }
    }
    val rankMap = remember(participants) {
        mutableStateMapOf<String, String>().apply {
            participants.forEach { this[it.id] = if (it.placement > 0) it.placement.toString() else "" }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Match Result Settlement",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("admin_settlement_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimaryDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateCardBg)
            )
        },
        bottomBar = {
            if (selectedTourney != null && participants.isNotEmpty() && selectedTourney?.status != "COMPLETED") {
                Surface(
                    color = SlateCardBg,
                    shadowElevation = 10.dp,
                    border = BorderStroke(1.dp, SlateMutedBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp)
                    ) {
                        Button(
                            onClick = {
                                val t = selectedTourney!!
                                val settleList = participants.map { p ->
                                    val k = killsMap[p.id]?.toIntOrNull() ?: 0
                                    val r = rankMap[p.id]?.toIntOrNull() ?: 0
                                    val rankPrize = when (r) {
                                        1 -> t.firstPrize
                                        2 -> t.secondPrize
                                        3 -> t.thirdPrize
                                        else -> 0
                                    }
                                    val killBonus = k * t.perKillReward
                                    val total = rankPrize + killBonus
                                    SettleData(
                                        participantId = p.id,
                                        userId = p.userId,
                                        kills = k,
                                        placement = r,
                                        prizeWon = total
                                    )
                                }
                                viewModel.settleMatch(t.id, settleList)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("disburse_winnings_button")
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Distribute Prizes & Conclude Match",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Tournament Selector
            item {
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedTourney?.title ?: "Select Tournament to Settle",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Match") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("settle_tourney_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        allTournaments.forEach { t ->
                            DropdownMenuItem(
                                text = { Text("${t.title} (${t.status})") },
                                onClick = {
                                    viewModel.selectTournamentForSettlement(t.id)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (selectedTourney != null) {
                val t = selectedTourney!!
                item {
                    HighContrastCard {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "PRIZE RULES FOR THIS MATCH",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FireOrange,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Status: ${t.status}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("1st: ₹${t.firstPrize}", color = TrophyGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("2nd: ₹${t.secondPrize}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("3rd: ₹${t.thirdPrize}", color = Color(0xFFF97316), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Per Kill: ₹${t.perKillReward}", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Participants Scorecard (${participants.size} registered)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                if (participants.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                            border = BorderStroke(1.dp, SlateMutedBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No players registered in this match yet.",
                                    fontSize = 13.sp,
                                    color = TextSecondaryMuted
                                )
                            }
                        }
                    }
                } else {
                    items(participants) { p ->
                        val k = killsMap[p.id]?.toIntOrNull() ?: 0
                        val r = rankMap[p.id]?.toIntOrNull() ?: 0
                        val rankPrize = when (r) {
                            1 -> t.firstPrize
                            2 -> t.secondPrize
                            3 -> t.thirdPrize
                            else -> 0
                        }
                        val estTotal = rankPrize + (k * t.perKillReward)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                            border = BorderStroke(1.dp, SlateMutedBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = HighContrastDark,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Slot #${p.slotNumber}",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = p.ffIgn,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimaryDark
                                            )
                                            Text(
                                                text = "UID: ${p.ffUid}",
                                                fontSize = 11.sp,
                                                color = TextSecondaryMuted
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Prize: ₹$estTotal",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = if (estTotal > 0) SuccessGreen else TextSecondaryMuted
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = rankMap[p.id] ?: "",
                                        onValueChange = { rankMap[p.id] = it.filter { c -> c.isDigit() } },
                                        label = { Text("Rank (1, 2, 3...)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("rank_input_${p.id}")
                                    )

                                    OutlinedTextField(
                                        value = killsMap[p.id] ?: "",
                                        onValueChange = { killsMap[p.id] = it.filter { c -> c.isDigit() } },
                                        label = { Text("Kills") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("kills_input_${p.id}")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
