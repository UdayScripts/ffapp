package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firestore.AppSettingsDto
import com.example.ui.theme.FireOrange
import com.example.ui.theme.HighContrastDark
import com.example.ui.theme.SlateLightBg
import com.example.ui.theme.SlateMutedBorder
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import com.example.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAppSettingsScreen(viewModel: AppViewModel) {
    val currentSettings by viewModel.appSettings.collectAsStateWithLifecycle()

    var appName by remember(currentSettings) { mutableStateOf(currentSettings?.appName ?: "FF Tournaments") }
    var announcement by remember(currentSettings) { mutableStateOf(currentSettings?.announcementText ?: "") }
    var supportContact by remember(currentSettings) { mutableStateOf(currentSettings?.supportContact ?: "") }
    var minDeposit by remember(currentSettings) { mutableStateOf(currentSettings?.minDeposit?.toString() ?: "10") }
    var platformFee by remember(currentSettings) { mutableStateOf(currentSettings?.platformFeePercent?.toString() ?: "10") }
    var rules by remember(currentSettings) { mutableStateOf(currentSettings?.rulesAndGuidelines ?: "") }
    var isMaintenance by remember(currentSettings) { mutableStateOf(currentSettings?.isMaintenance ?: false) }

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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SlateMutedBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Branding & Broadcast Banner", fontWeight = FontWeight.Bold, fontSize = 14.sp)

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

            // Financial & Support
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SlateMutedBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Financial Limits & Contact", fontWeight = FontWeight.Bold, fontSize = 14.sp)

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
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SlateMutedBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Rules & Tournament Fair Play", fontWeight = FontWeight.Bold, fontSize = 14.sp)

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
                colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        Text("Maintenance Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                    val updated = AppSettingsDto(
                        id = "main",
                        appName = appName.ifBlank { "FF Tournaments" },
                        announcementText = announcement,
                        isMaintenance = isMaintenance,
                        supportContact = supportContact,
                        minDeposit = minDeposit.toIntOrNull() ?: 10,
                        platformFeePercent = platformFee.toIntOrNull() ?: 10,
                        rulesAndGuidelines = rules
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

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
