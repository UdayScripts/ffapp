package com.example.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firestore.AppSettingsDto
import com.example.ui.theme.FireOrange
import com.example.ui.theme.FireOrangeDark
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAppSettingsScreen(viewModel: AppViewModel) {
    val currentSettings by viewModel.appSettings.collectAsStateWithLifecycle()

    var appName by remember(currentSettings) {
        val raw = currentSettings?.appName
        mutableStateOf(if (raw.isNullOrBlank() || raw == "FF Tournaments") "eSports Arena" else raw)
    }
    var announcement by remember(currentSettings) { mutableStateOf(currentSettings?.announcementText ?: "") }
    var supportContact by remember(currentSettings) { mutableStateOf(currentSettings?.supportContact ?: "") }
    var minDeposit by remember(currentSettings) { mutableStateOf(currentSettings?.minDeposit?.toString() ?: "10") }
    var platformFee by remember(currentSettings) { mutableStateOf(currentSettings?.platformFeePercent?.toString() ?: "10") }
    var rules by remember(currentSettings) { mutableStateOf(currentSettings?.rulesAndGuidelines ?: "") }
    var isMaintenance by remember(currentSettings) { mutableStateOf(currentSettings?.isMaintenance ?: false) }

    // Sign-Up Bonus Configuration State (Fixed or Random)
    var signUpBonusEnabled by remember(currentSettings) {
        mutableStateOf(currentSettings?.signUpBonusEnabled ?: true)
    }
    var signUpBonusMode by remember(currentSettings) {
        mutableStateOf((currentSettings?.signUpBonusMode ?: "FIXED").uppercase())
    }
    var signUpBonusFixed by remember(currentSettings) {
        mutableStateOf((currentSettings?.signUpBonusFixed ?: 10).toString())
    }
    var signUpBonusMin by remember(currentSettings) {
        mutableStateOf((currentSettings?.signUpBonusMin ?: 5).toString())
    }
    var signUpBonusMax by remember(currentSettings) {
        mutableStateOf((currentSettings?.signUpBonusMax ?: 25).toString())
    }

    var showWipeConfirmDialog by remember { mutableStateOf(false) }
    var isWiping by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Customize App Settings",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("admin_settings_back_button")
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
            // Branding & Announcement
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                border = BorderStroke(1.dp, SlateMutedBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Branding & Broadcast Banner", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimaryDark)

                    OutlinedTextField(
                        value = appName,
                        onValueChange = { appName = it },
                        label = { Text("Application Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_app_name_input")
                    )

                    OutlinedTextField(
                        value = announcement,
                        onValueChange = { announcement = it },
                        label = { Text("Live Announcement Banner Text (Home Screen)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("settings_announcement_input")
                    )
                }
            }

            // New User Sign-Up Bonus (Random or Fixed)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                border = BorderStroke(1.5.dp, if (signUpBonusEnabled) FireOrange.copy(alpha = 0.6f) else SlateMutedBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                                    .size(40.dp)
                                    .background(FireOrange.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CardGiftcard,
                                    contentDescription = null,
                                    tint = FireOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "New User Sign-Up Bonus",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "Automatically reward new players on registration",
                                    fontSize = 11.sp,
                                    color = TextSecondaryMuted
                                )
                            }
                        }

                        Switch(
                            checked = signUpBonusEnabled,
                            onCheckedChange = { signUpBonusEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = FireOrange
                            ),
                            modifier = Modifier.testTag("settings_signup_bonus_switch")
                        )
                    }

                    AnimatedVisibility(
                        visible = signUpBonusEnabled,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Mode Selector: FIXED vs RANDOM
                            Text(
                                text = "BONUS REWARD MODE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondaryMuted,
                                letterSpacing = 0.5.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val isFixed = signUpBonusMode != "RANDOM"
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isFixed) HighContrastDark else SlateSubtle,
                                    border = BorderStroke(
                                        width = if (isFixed) 1.5.dp else 1.dp,
                                        color = if (isFixed) FireOrange else SlateMutedBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { signUpBonusMode = "FIXED" }
                                        .testTag("signup_bonus_mode_fixed")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (isFixed) FireOrange else TextSecondaryMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = "Fixed Bonus",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isFixed) Color.White else TextPrimaryDark
                                            )
                                            Text(
                                                text = "Exact ₹ amount",
                                                fontSize = 10.sp,
                                                color = if (isFixed) Color(0xFF94A3B8) else TextSecondaryMuted
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (!isFixed) HighContrastDark else SlateSubtle,
                                    border = BorderStroke(
                                        width = if (!isFixed) 1.5.dp else 1.dp,
                                        color = if (!isFixed) TrophyGold else SlateMutedBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { signUpBonusMode = "RANDOM" }
                                        .testTag("signup_bonus_mode_random")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Casino,
                                            contentDescription = null,
                                            tint = if (!isFixed) TrophyGold else TextSecondaryMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = "Random Bonus",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (!isFixed) Color.White else TextPrimaryDark
                                            )
                                            Text(
                                                text = "Mystery range",
                                                fontSize = 10.sp,
                                                color = if (!isFixed) Color(0xFF94A3B8) else TextSecondaryMuted
                                            )
                                        }
                                    }
                                }
                            }

                            if (signUpBonusMode == "RANDOM") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = signUpBonusMin,
                                        onValueChange = { signUpBonusMin = it.filter { c -> c.isDigit() } },
                                        label = { Text("Min Bonus (₹)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("settings_signup_bonus_min_input")
                                    )
                                    OutlinedTextField(
                                        value = signUpBonusMax,
                                        onValueChange = { signUpBonusMax = it.filter { c -> c.isDigit() } },
                                        label = { Text("Max Bonus (₹)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("settings_signup_bonus_max_input")
                                    )
                                }

                                val minVal = signUpBonusMin.toIntOrNull() ?: 0
                                val maxVal = signUpBonusMax.toIntOrNull() ?: 0
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = TrophyGold.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, TrophyGold.copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "🎲 Mystery Reward Active: Every new player signing up will receive a random bonus between ₹${minOf(minVal, maxVal)} and ₹${maxOf(minVal, maxVal)}.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimaryDark,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            } else {
                                OutlinedTextField(
                                    value = signUpBonusFixed,
                                    onValueChange = { signUpBonusFixed = it.filter { c -> c.isDigit() } },
                                    label = { Text("Fixed Sign-Up Bonus Amount (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("settings_signup_bonus_fixed_input")
                                )

                                // Quick Preset Chips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(0, 10, 20, 50, 100).forEach { preset ->
                                        val isSelected = (signUpBonusFixed.toIntOrNull() ?: -1) == preset
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) FireOrange.copy(alpha = 0.18f) else SlateSubtle,
                                            border = BorderStroke(1.dp, if (isSelected) FireOrange else SlateMutedBorder),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { signUpBonusFixed = preset.toString() }
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "₹$preset",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) FireOrangeDark else TextPrimaryDark
                                                )
                                            }
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SuccessGreen.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "🎁 Fixed Reward Active: Every new user gets ₹${signUpBonusFixed.toIntOrNull() ?: 0} credited to their wallet upon registration.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimaryDark,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Financial & Support
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                border = BorderStroke(1.dp, SlateMutedBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Financial Limits & Contact", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimaryDark)

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = minDeposit,
                            onValueChange = { minDeposit = it.filter { c -> c.isDigit() } },
                            label = { Text("Min Deposit (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("settings_min_deposit")
                        )

                        OutlinedTextField(
                            value = platformFee,
                            onValueChange = { platformFee = it.filter { c -> c.isDigit() } },
                            label = { Text("Platform Fee %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("settings_fee_percent")
                        )
                    }

                    OutlinedTextField(
                        value = supportContact,
                        onValueChange = { supportContact = it },
                        label = { Text("Organizer Support (WhatsApp / Discord / Email)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_support_contact")
                    )
                }
            }

            // Rules & Fair Play
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                border = BorderStroke(1.dp, SlateMutedBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Rules & Tournament Fair Play", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimaryDark)

                    OutlinedTextField(
                        value = rules,
                        onValueChange = { rules = it },
                        label = { Text("Global Tournament Rules") },
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth().testTag("settings_rules_input")
                    )
                }
            }

            // Maintenance Mode
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                border = BorderStroke(1.dp, SlateMutedBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Maintenance Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimaryDark)
                        Text(
                            text = "Pause new match registrations during server maintenance",
                            fontSize = 11.sp,
                            color = TextSecondaryMuted
                        )
                    }

                    Switch(
                        checked = isMaintenance,
                        onCheckedChange = { isMaintenance = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = FireOrange, checkedTrackColor = Color(0xFFFFEDE6))
                    )
                }
            }

            // Save Settings CTA Button
            Button(
                onClick = {
                    val fixedVal = (signUpBonusFixed.toIntOrNull() ?: 10).coerceAtLeast(0)
                    val minVal = (signUpBonusMin.toIntOrNull() ?: 5).coerceAtLeast(0)
                    val maxVal = (signUpBonusMax.toIntOrNull() ?: 25).coerceAtLeast(minVal)
                    val updated = AppSettingsDto(
                        id = "main",
                        appName = appName.ifBlank { "eSports Arena" },
                        announcementText = announcement,
                        isMaintenance = isMaintenance,
                        supportContact = supportContact,
                        minDeposit = minDeposit.toIntOrNull() ?: 10,
                        platformFeePercent = platformFee.toIntOrNull() ?: 10,
                        rulesAndGuidelines = rules,
                        signUpBonusEnabled = signUpBonusEnabled,
                        signUpBonusMode = if (signUpBonusMode == "RANDOM") "RANDOM" else "FIXED",
                        signUpBonusFixed = fixedVal,
                        signUpBonusMin = minVal,
                        signUpBonusMax = maxVal
                    )
                    viewModel.updateSettings(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_settings_button")
            ) {
                Text("Save Platform Settings", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            // Danger Zone: Delete All Data
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = LiveRed.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, LiveRed.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Danger Zone: Reset Database",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = LiveRed
                    )
                    Text(
                        text = "Permanently delete all users, their match & balance history, withdrawal requests, tournaments, and notifications.",
                        fontSize = 11.sp,
                        color = TextPrimaryDark
                    )
                    OutlinedButton(
                        onClick = { showWipeConfirmDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, LiveRed),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LiveRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_delete_all_data_button")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                        Text(
                            text = "  Delete All Users & Platform Data",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isWiping) showWipeConfirmDialog = false },
            containerColor = SlateCardBg,
            title = {
                Text("Delete All Data?", fontWeight = FontWeight.Bold, color = LiveRed)
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently wipe all users, their match & balance history, withdrawal requests, tournaments, and notifications?",
                    fontSize = 13.sp,
                    color = TextPrimaryDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isWiping = true
                        viewModel.deleteAllPlatformData { _, _ ->
                            isWiping = false
                            showWipeConfirmDialog = false
                        }
                    },
                    enabled = !isWiping,
                    colors = ButtonDefaults.buttonColors(containerColor = LiveRed)
                ) {
                    Text(if (isWiping) "Deleting..." else "Delete Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showWipeConfirmDialog = false },
                    enabled = !isWiping
                ) {
                    Text("Cancel", color = TextSecondaryMuted)
                }
            }
        )
    }
}
