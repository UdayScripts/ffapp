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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ads.UnityAdsManager
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
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val currentSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val localAdsAccount by UnityAdsManager.accountDetails.collectAsStateWithLifecycle()

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

    // Unity Ads Account & Monetization State
    var adsEnabled by remember(currentSettings) {
        mutableStateOf(currentSettings?.adsEnabled ?: localAdsAccount.adsEnabled)
    }
    var adsAccountName by remember(currentSettings) {
        mutableStateOf(
            currentSettings?.adsAccountName?.ifBlank { localAdsAccount.accountName }
                ?: localAdsAccount.accountName
        )
    }
    var adsAccountEmail by remember(currentSettings) {
        mutableStateOf(
            currentSettings?.adsAccountEmail?.ifBlank { localAdsAccount.accountEmail }
                ?: localAdsAccount.accountEmail
        )
    }
    var adsOrganizationId by remember(currentSettings) {
        mutableStateOf(
            currentSettings?.adsOrganizationId?.ifBlank { localAdsAccount.organizationId }
                ?: localAdsAccount.organizationId
        )
    }
    var unityGameId by remember(currentSettings) {
        mutableStateOf(
            UnityAdsManager.normalizeGameId(currentSettings?.unityGameId ?: localAdsAccount.gameId)
        )
    }
    var unityPlacementId by remember(currentSettings) {
        mutableStateOf(
            UnityAdsManager.normalizePlacementId(currentSettings?.unityPlacementId ?: localAdsAccount.rewardedPlacementId)
        )
    }
    var unityInterstitialId by remember(currentSettings) {
        mutableStateOf(
            currentSettings?.unityInterstitialId?.ifBlank { localAdsAccount.interstitialPlacementId }
                ?: localAdsAccount.interstitialPlacementId
        )
    }
    var unityBannerId by remember(currentSettings) {
        mutableStateOf(
            currentSettings?.unityBannerId?.ifBlank { localAdsAccount.bannerPlacementId }
                ?: localAdsAccount.bannerPlacementId
        )
    }
    var unityTestMode by remember(currentSettings) {
        mutableStateOf(currentSettings?.unityTestMode ?: localAdsAccount.testMode)
    }
    var dailyAdLimit by remember(currentSettings) {
        mutableStateOf((currentSettings?.dailyAdLimit ?: localAdsAccount.dailyAdLimit).toString())
    }
    var adRewardAmount by remember(currentSettings) {
        mutableStateOf((currentSettings?.adRewardAmount ?: localAdsAccount.rewardPerAd).toString())
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

            // Unity Ads Configuration (Game ID: 800408446 | Rewarded Placement: esports | Test Mode: Enabled)
            Card(
                modifier = Modifier.fillMaxWidth().testTag("settings_ads_account_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (adsEnabled) Color(0xFF8B5CF6).copy(alpha = 0.7f) else SlateMutedBorder
                )
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
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.16f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF8B5CF6),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Unity Ads Configuration",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = TextPrimaryDark,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    color = when {
                                        !adsEnabled -> SlateSubtle
                                        unityTestMode -> Color(0xFFFEF3C7)
                                        else -> Color(0xFFDCFCE7)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        when {
                                            !adsEnabled -> SlateMutedBorder
                                            unityTestMode -> Color(0xFFF59E0B)
                                            else -> Color(0xFF16A34A)
                                        }
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = when {
                                            !adsEnabled -> "● ADS PAUSED"
                                            unityTestMode -> "● TEST MODE (DEV)"
                                            else -> "● LIVE ADS ACTIVE"
                                        },
                                        color = when {
                                            !adsEnabled -> TextSecondaryMuted
                                            unityTestMode -> Color(0xFFB45309)
                                            else -> Color(0xFF15803D)
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Android Game ID, Rewarded Placement ID & Development Test Mode",
                                    fontSize = 11.sp,
                                    color = TextSecondaryMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Switch(
                            checked = adsEnabled,
                            onCheckedChange = { adsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF8B5CF6)
                            ),
                            modifier = Modifier.testTag("settings_ads_enabled_switch")
                        )
                    }

                    // SDK Credentials & Placement ID
                    Text(
                        text = "UNITY ANDROID CONFIGURATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B5CF6),
                        letterSpacing = 0.5.sp
                    )

                    OutlinedTextField(
                        value = unityGameId,
                        onValueChange = { unityGameId = it.trim() },
                        label = { Text("Android Game ID (Default: 800408446)") },
                        singleLine = true,
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(unityGameId))
                                        Toast.makeText(context, "Copied Game ID: $unityGameId", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Game ID",
                                        tint = TextSecondaryMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val pasted = clipboardManager.getText()?.text?.trim()
                                        if (!pasted.isNullOrBlank()) {
                                            unityGameId = pasted
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste Game ID",
                                        tint = Color(0xFF8B5CF6),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_unity_game_id_input")
                    )

                    OutlinedTextField(
                        value = unityPlacementId,
                        onValueChange = { unityPlacementId = it.trim() },
                        label = { Text("Rewarded Placement ID (Default: esports)") },
                        singleLine = true,
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(unityPlacementId))
                                        Toast.makeText(context, "Copied Placement ID: $unityPlacementId", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Placement ID",
                                        tint = TextSecondaryMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val pasted = clipboardManager.getText()?.text?.trim()
                                        if (!pasted.isNullOrBlank()) {
                                            unityPlacementId = pasted
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste Placement ID",
                                        tint = Color(0xFF8B5CF6),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_unity_placement_id_input")
                    )

                    // Delivery Mode: Test Mode (Enabled for Development) vs Live Real Ads
                    Text(
                        text = "TEST MODE & REWARD ECONOMY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B5CF6),
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isTestEnabled = unityTestMode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isTestEnabled) Color(0xFF0F172A) else SlateCardBg,
                            border = BorderStroke(
                                width = if (isTestEnabled) 2.dp else 1.2.dp,
                                color = if (isTestEnabled) TrophyGold else SlateMutedBorder
                            ),
                            shadowElevation = if (isTestEnabled) 3.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { unityTestMode = true }
                                .testTag("settings_ads_mode_test")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = if (isTestEnabled) TrophyGold else Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isTestEnabled) "Test Mode ●" else "Test Mode",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isTestEnabled) Color.White else TextPrimaryDark,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = "Development Ads",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isTestEnabled) TrophyGold else TextSecondaryMuted,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (!isTestEnabled) Color(0xFF064E3B) else SlateCardBg,
                            border = BorderStroke(
                                width = if (!isTestEnabled) 2.dp else 1.2.dp,
                                color = if (!isTestEnabled) Color(0xFF22C55E) else SlateMutedBorder
                            ),
                            shadowElevation = if (!isTestEnabled) 3.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { unityTestMode = false }
                                .testTag("settings_ads_mode_live")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (!isTestEnabled) Color(0xFF4ADE80) else Color(0xFF16A34A),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (!isTestEnabled) "Live Real Ads ●" else "Live Real Ads",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (!isTestEnabled) Color.White else TextPrimaryDark,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = "Production Mode",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (!isTestEnabled) Color(0xFF86EFAC) else TextSecondaryMuted,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = dailyAdLimit,
                            onValueChange = { dailyAdLimit = it.filter { c -> c.isDigit() } },
                            label = { Text("Daily Ad Limit / User") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_daily_ad_limit_input")
                        )

                        OutlinedTextField(
                            value = adRewardAmount,
                            onValueChange = { input ->
                                adRewardAmount = input.filter { c -> c.isDigit() || c == '.' }
                            },
                            label = { Text("Base Ad Reward (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_ad_reward_amount_input")
                        )
                    }

                    // Active Unity Configuration Summary Banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF8B5CF6).copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "Android Game ID: ${unityGameId.ifBlank { UnityAdsManager.DEFAULT_GAME_ID }} • Rewarded Placement ID: ${unityPlacementId.ifBlank { UnityAdsManager.DEFAULT_PLACEMENT_ID }}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "SDK: ${UnityAdsManager.SDK_DEPENDENCY_NOTATION} • Test Mode: ${if (unityTestMode) "Enabled for development" else "Disabled (Live)"}",
                                fontSize = 10.sp,
                                color = TextSecondaryMuted
                            )
                        }
                    }

                    // Quick Save Ads Configuration & Reload SDK Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val resolvedGameId = UnityAdsManager.normalizeGameId(unityGameId)
                                val resolvedPlacementId = UnityAdsManager.normalizePlacementId(unityPlacementId)
                                unityGameId = resolvedGameId
                                unityPlacementId = resolvedPlacementId
                                UnityAdsManager.configure(
                                    context = context,
                                    gameId = resolvedGameId,
                                    placementId = resolvedPlacementId,
                                    interstitialPlacementId = unityInterstitialId.trim(),
                                    bannerPlacementId = unityBannerId.trim(),
                                    accountName = adsAccountName.trim().ifBlank { UnityAdsManager.DEFAULT_ACCOUNT_NAME },
                                    accountEmail = adsAccountEmail.trim().ifBlank { UnityAdsManager.DEFAULT_ACCOUNT_EMAIL },
                                    organizationId = adsOrganizationId.trim(),
                                    adsEnabled = adsEnabled,
                                    testMode = unityTestMode,
                                    dailyAdLimit = (dailyAdLimit.toIntOrNull() ?: UnityAdsManager.DEFAULT_DAILY_LIMIT).coerceIn(1, 50),
                                    rewardPerAd = (adRewardAmount.toDoubleOrNull() ?: UnityAdsManager.DEFAULT_REWARD_PER_AD).coerceIn(0.1, 100.0)
                                )
                                Toast.makeText(
                                    context,
                                    "Unity Ads SDK reloaded (Game ID: $resolvedGameId, Placement: $resolvedPlacementId)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF8B5CF6)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8B5CF6)),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("settings_reload_ads_sdk_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reload SDK", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                        }

                        Button(
                            onClick = {
                                val resolvedGameId = UnityAdsManager.normalizeGameId(unityGameId)
                                val resolvedPlacementId = UnityAdsManager.normalizePlacementId(unityPlacementId)
                                unityGameId = resolvedGameId
                                unityPlacementId = resolvedPlacementId
                                viewModel.updateAdsAccountDetails(
                                    adsEnabled = adsEnabled,
                                    adsAccountName = adsAccountName,
                                    adsAccountEmail = adsAccountEmail,
                                    adsOrganizationId = adsOrganizationId,
                                    unityGameId = resolvedGameId,
                                    unityPlacementId = resolvedPlacementId,
                                    unityInterstitialId = unityInterstitialId,
                                    unityBannerId = unityBannerId,
                                    unityTestMode = unityTestMode,
                                    dailyAdLimit = dailyAdLimit.toIntOrNull() ?: UnityAdsManager.DEFAULT_DAILY_LIMIT,
                                    adRewardAmount = adRewardAmount.toDoubleOrNull() ?: UnityAdsManager.DEFAULT_REWARD_PER_AD
                                )
                                Toast.makeText(context, "Unity Ads configuration saved & synced!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .heightIn(min = 44.dp)
                                .testTag("settings_save_ads_account_button")
                        ) {
                            Text("Save Ads Config", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
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
                        signUpBonusMax = maxVal,
                        adsEnabled = adsEnabled,
                        adsAccountName = adsAccountName.trim().ifBlank { UnityAdsManager.DEFAULT_ACCOUNT_NAME },
                        adsAccountEmail = adsAccountEmail.trim().ifBlank { UnityAdsManager.DEFAULT_ACCOUNT_EMAIL },
                        adsOrganizationId = adsOrganizationId.trim(),
                        unityGameId = UnityAdsManager.normalizeGameId(unityGameId),
                        unityPlacementId = UnityAdsManager.normalizePlacementId(unityPlacementId),
                        unityInterstitialId = unityInterstitialId.trim(),
                        unityBannerId = unityBannerId.trim(),
                        unityTestMode = unityTestMode,
                        dailyAdLimit = (dailyAdLimit.toIntOrNull() ?: UnityAdsManager.DEFAULT_DAILY_LIMIT).coerceIn(1, 50),
                        adRewardAmount = (adRewardAmount.toDoubleOrNull() ?: UnityAdsManager.DEFAULT_REWARD_PER_AD).coerceIn(0.1, 100.0)
                    )
                    viewModel.updateSettings(updated)
                    Toast.makeText(context, "Platform & Ads Account settings saved!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("save_settings_button")
            ) {
                Text("Save Platform & Ads Settings", fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
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
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                            .testTag("settings_delete_all_data_button")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                        Text(
                            text = "  Delete All Users & Platform Data",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.navigationBarsPadding().height(48.dp))
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
