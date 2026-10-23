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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firestore.TournamentDto
import com.example.ui.theme.FireOrange
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import com.example.ui.theme.HighContrastDark
import com.example.ui.theme.SlateCardBg
import com.example.ui.theme.SlateLightBg
import com.example.ui.theme.SlateMutedBorder
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import com.example.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTournamentEditorScreen(viewModel: AppViewModel) {
    val existingTournament by viewModel.selectedTournament.collectAsStateWithLifecycle()
    val isNew = existingTournament == null

    var title by remember(existingTournament) { mutableStateOf(existingTournament?.title ?: "") }
    var matchType by remember(existingTournament) { mutableStateOf(existingTournament?.matchType ?: "SOLO") }
    var mapName by remember(existingTournament) { mutableStateOf(existingTournament?.map ?: "Bermuda") }
    var entryFee by remember(existingTournament) { mutableStateOf(existingTournament?.entryFee?.toString() ?: "50") }
    var prizePool by remember(existingTournament) { mutableStateOf(existingTournament?.prizePool?.toString() ?: "1800") }
    var perKill by remember(existingTournament) { mutableStateOf(existingTournament?.perKillReward?.toString() ?: "15") }
    var maxSlots by remember(existingTournament) { mutableStateOf(existingTournament?.maxSlots?.toString() ?: "48") }
    var scheduleTime by remember(existingTournament) { mutableStateOf(existingTournament?.scheduleTime ?: "Today, 09:00 PM") }
    var status by remember(existingTournament) { mutableStateOf(existingTournament?.status ?: "UPCOMING") }
    var firstPrize by remember(existingTournament) { mutableStateOf(existingTournament?.firstPrize?.toString() ?: "800") }
    var secondPrize by remember(existingTournament) { mutableStateOf(existingTournament?.secondPrize?.toString() ?: "400") }
    var thirdPrize by remember(existingTournament) { mutableStateOf(existingTournament?.thirdPrize?.toString() ?: "200") }
    var rules by remember(existingTournament) {
        mutableStateOf(
            existingTournament?.rules ?: "1. Emulators strictly forbidden.\n2. Arrive in room 10 minutes prior.\n3. Fair play only."
        )
    }

    val mapOptions = listOf("Bermuda", "Purgatory", "Kalahari", "Alpine", "NeXTerra", "Bermuda Remastered")
    val modeOptions = listOf("SOLO", "DUO", "SQUAD")
    val statusOptions = listOf("UPCOMING", "LIVE", "COMPLETED", "CANCELLED")

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var matchTimestamp by remember(existingTournament) {
        mutableStateOf(existingTournament?.timestamp ?: (System.currentTimeMillis() + 3600000))
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = matchTimestamp
    )
    val timePickerState = rememberTimePickerState(
        initialHour = 21,
        initialMinute = 0,
        is24Hour = false
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDateMillis = datePickerState.selectedDateMillis
                        showDatePicker = false
                        showTimePicker = true
                    }
                ) {
                    Text("Next: Time", color = FireOrange, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Text("Select Match Time", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cal = Calendar.getInstance()
                        selectedDateMillis?.let { cal.timeInMillis = it }
                        cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        cal.set(Calendar.MINUTE, timePickerState.minute)
                        cal.set(Calendar.SECOND, 0)
                        val finalMillis = cal.timeInMillis

                        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                        scheduleTime = sdf.format(Date(finalMillis))
                        matchTimestamp = finalMillis

                        showTimePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FireOrange)
                ) {
                    Text("Confirm Schedule Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isNew) "Create Tournament" else "Edit Tournament",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("admin_editor_back_button")
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateLightBg)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                border = BorderStroke(1.dp, SlateMutedBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Basic Tournament Info", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimaryDark)

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Match Title (e.g. Bermuda Solo Championship #105)") },
                        modifier = Modifier.fillMaxWidth().testTag("editor_title_input")
                    )

                    // Match Mode Selector with horizontal scroll
                    Text("Match Format:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondaryMuted)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        modeOptions.forEach { mode ->
                            FilterChip(
                                selected = matchType == mode,
                                onClick = {
                                    matchType = mode
                                    if (mode == "SOLO") maxSlots = "48"
                                    else if (mode == "DUO") maxSlots = "24"
                                    else if (mode == "SQUAD") maxSlots = "12"
                                },
                                label = { Text(mode) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HighContrastDark,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Map Selector with smooth horizontal scroll for small width devices
                    Text("Map (Swipe to view all):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondaryMuted)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        mapOptions.forEach { m ->
                            FilterChip(
                                selected = mapName == m,
                                onClick = { mapName = m },
                                label = { Text(m, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FireOrange,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Schedule Time with Date & Time Picker
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Schedule Date & Time:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondaryMuted)
                        OutlinedTextField(
                            value = scheduleTime,
                            onValueChange = { scheduleTime = it },
                            label = { Text("Schedule Time") },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showDatePicker = true },
                                    modifier = Modifier.testTag("pick_date_time_icon")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Pick Date & Time",
                                        tint = FireOrange
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("editor_time_input")
                        )
                        Button(
                            onClick = { showDatePicker = true },
                            colors = ButtonDefaults.buttonColors(containerColor = HighContrastDark),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("pick_schedule_datetime_button")
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = FireOrange, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pick Date & Time from Calendar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Status with smooth horizontal scroll for small width devices
                    Text("Match Status (Swipe to view all):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondaryMuted)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        statusOptions.forEach { s ->
                            FilterChip(
                                selected = status == s,
                                onClick = { status = s },
                                label = { Text(s, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // Financial & Slots Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                border = BorderStroke(1.dp, SlateMutedBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Economics & Capacity", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimaryDark)

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = entryFee,
                            onValueChange = { entryFee = it.filter { c -> c.isDigit() } },
                            label = { Text("Entry Fee (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("editor_entry_fee")
                        )
                        OutlinedTextField(
                            value = maxSlots,
                            onValueChange = { maxSlots = it.filter { c -> c.isDigit() } },
                            label = { Text("Max Slots") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("editor_max_slots")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = prizePool,
                            onValueChange = { prizePool = it.filter { c -> c.isDigit() } },
                            label = { Text("Total Prize Pool (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("editor_prize_pool")
                        )
                        OutlinedTextField(
                            value = perKill,
                            onValueChange = { perKill = it.filter { c -> c.isDigit() } },
                            label = { Text("Per Kill (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("editor_per_kill")
                        )
                    }

                    Text("Prize Breakdown:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondaryMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = firstPrize,
                            onValueChange = { firstPrize = it.filter { c -> c.isDigit() } },
                            label = { Text("1st Rank") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = secondPrize,
                            onValueChange = { secondPrize = it.filter { c -> c.isDigit() } },
                            label = { Text("2nd Rank") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = thirdPrize,
                            onValueChange = { thirdPrize = it.filter { c -> c.isDigit() } },
                            label = { Text("3rd Rank") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = rules,
                        onValueChange = { rules = it },
                        label = { Text("Rules & Guidelines") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag("editor_rules")
                    )
                }
            }

            // Save CTA
            Button(
                onClick = {
                    if (title.isBlank()) {
                        viewModel.showToast("Please enter a tournament title")
                        return@Button
                    }
                    val newEntity = TournamentDto(
                        id = existingTournament?.id ?: ("FF-T" + (100..999).random()),
                        title = title.trim(),
                        matchType = matchType,
                        map = mapName,
                        entryFee = entryFee.toIntOrNull() ?: 0,
                        prizePool = prizePool.toIntOrNull() ?: 1000,
                        perKillReward = perKill.toIntOrNull() ?: 10,
                        maxSlots = maxSlots.toIntOrNull() ?: 48,
                        joinedSlots = existingTournament?.joinedSlots ?: 0,
                        scheduleTime = scheduleTime.ifBlank { "Today, 09:00 PM" },
                        timestamp = matchTimestamp,
                        status = status,
                        roomId = existingTournament?.roomId,
                        roomPassword = existingTournament?.roomPassword,
                        roomNotice = existingTournament?.roomNotice,
                        firstPrize = firstPrize.toIntOrNull() ?: 0,
                        secondPrize = secondPrize.toIntOrNull() ?: 0,
                        thirdPrize = thirdPrize.toIntOrNull() ?: 0,
                        rules = rules
                    )
                    if (isNew) {
                        viewModel.createTournament(newEntity)
                    } else {
                        viewModel.updateTournament(newEntity)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("save_tournament_button")
            ) {
                Text(
                    text = if (isNew) "Publish New Tournament" else "Save Changes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.navigationBarsPadding().height(48.dp))
        }
    }
}
