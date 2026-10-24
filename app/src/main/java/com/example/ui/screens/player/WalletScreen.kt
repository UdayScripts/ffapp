package com.example.ui.screens.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.R
import android.app.Activity
import androidx.compose.material.icons.filled.PlayCircle
import com.example.data.ads.UnityAdsManager
import com.example.data.payment.DepositReceiptData
import com.example.data.payment.DepositReceiptManager
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.firestore.TransactionDto
import com.example.data.firestore.WithdrawalRequestDto
import com.example.data.firestore.formatCurrency
import com.example.data.payment.PaytmPaymentService
import com.example.data.payment.PaytmTxnStatus
import com.example.ui.components.HighContrastCard
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
import com.example.ui.theme.ThemePreferenceState
import com.example.ui.theme.TrophyGold
import com.example.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(viewModel: AppViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val transactions by viewModel.userTransactions.collectAsStateWithLifecycle()
    val userWithdrawals by viewModel.userWithdrawals.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()

    var showDepositDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var depositInitialAmount by remember { mutableStateOf(100) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedLedgerTab by remember { mutableStateOf(0) } // 0 = Transactions, 1 = Withdrawals

    val filterOptions = listOf("ALL", "DEPOSIT", "ENTRY_FEE", "PRIZE_WIN", "WITHDRAWAL")

    val filteredList = transactions.filter {
        if (selectedFilter == "ALL") true
        else if (selectedFilter == "WITHDRAWAL") it.type.startsWith("WITHDRAWAL", ignoreCase = true)
        else it.type.equals(selectedFilter, ignoreCase = true)
    }

    val totalDeposited = transactions
        .filter { it.type == "DEPOSIT" && it.amount > 0 }
        .sumOf { it.amount }

    val totalWon = transactions
        .filter { it.type == "PRIZE_WIN" }
        .sumOf { it.amount }

    val activeWithdrawnSum = userWithdrawals
        .filter { it.status.equals("PENDING", ignoreCase = true) || it.status.equals("APPROVED", ignoreCase = true) }
        .sumOf { it.amount.toDouble() }
    val maxFromLifetimeWinnings = ((currentUser?.totalWinnings ?: 0.0) - activeWithdrawnSum).coerceAtLeast(0.0)
    val withdrawableWinnings = minOf(
        currentUser?.effectiveWinningBalance ?: 0.0,
        maxFromLifetimeWinnings
    ).coerceAtLeast(0.0)
    val pendingWithdrawalCount = userWithdrawals.count { it.status == "PENDING" }

    Scaffold(
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
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("wallet_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimaryDark
                        )
                    }
                    Text(
                        text = "Wallet & UPI Withdrawals",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateLightBg)
                .padding(innerPadding)
        ) {
            val isCompactScreen = maxWidth < 360.dp

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isCompactScreen) 12.dp else 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // High Contrast Balance Card
                item {
                    HighContrastCard {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AVAILABLE BALANCE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8),
                                    letterSpacing = 0.8.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = SuccessGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        color = SuccessGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "₹${currentUser?.walletBalance.formatCurrency()}",
                                fontSize = if (isCompactScreen) 30.sp else 36.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Stats: Total Deposited vs Total Won
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "TOTAL TOP-UP",
                                        fontSize = 9.sp,
                                        color = Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "₹${totalDeposited.formatCurrency()}",
                                        fontSize = if (isCompactScreen) 14.sp else 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "TOTAL WON",
                                        fontSize = 9.sp,
                                        color = Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "₹${totalWon.formatCurrency()}",
                                        fontSize = if (isCompactScreen) 14.sp else 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TrophyGold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "MATCHES",
                                        fontSize = 9.sp,
                                        color = Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${currentUser?.matchesPlayed ?: 0}",
                                        fontSize = if (isCompactScreen) 14.sp else 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Withdrawable Winning Balance Highlight
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = TrophyGold.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, TrophyGold.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = TrophyGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = "WITHDRAWABLE WINNINGS",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TrophyGold
                                            )
                                            Text(
                                                text = "Only winning amount can be withdrawn via UPI",
                                                fontSize = 9.sp,
                                                color = Color(0xFFCBD5E1)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "₹${withdrawableWinnings.formatCurrency()}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TrophyGold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action Buttons: Top-Up QR & Withdraw UPI
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        depositInitialAmount = 100
                                        showDepositDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("top_up_wallet_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode2,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Top-Up QR",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isCompactScreen) 12.sp else 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Button(
                                    onClick = { showWithdrawDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("withdraw_upi_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Withdraw UPI",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = if (isCompactScreen) 12.sp else 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Watch Ads & Earn Balance Card (Unity Rewarded Ads - Real Ads Mode)
                if (appSettings?.adsEnabled != false) {
                    item {
                        val activity = LocalContext.current as? Activity
                        val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
                        val maxDailyAds = (appSettings?.dailyAdLimit ?: UnityAdsManager.DEFAULT_DAILY_LIMIT).coerceIn(1, 20)
                        val todayWatchedCount = currentUser?.getTodayWatchedAds(todayDateStr) ?: 0
                        val remainingAds = (maxDailyAds - todayWatchedCount).coerceAtLeast(0)
                        var isAdShowing by remember { mutableStateOf(false) }
                        var rewardEarnedModalAmount by remember { mutableStateOf<Double?>(null) }
                        var adErrorMsg by remember { mutableStateOf<String?>(null) }

                        LaunchedEffect(
                            activity,
                            appSettings?.adsEnabled,
                            appSettings?.adsAccountName,
                            appSettings?.adsAccountEmail,
                            appSettings?.adsOrganizationId,
                            appSettings?.unityGameId,
                            appSettings?.unityPlacementId,
                            appSettings?.unityInterstitialId,
                            appSettings?.unityBannerId,
                            appSettings?.unityTestMode,
                            appSettings?.dailyAdLimit,
                            appSettings?.adRewardAmount
                        ) {
                            if (activity != null) {
                                UnityAdsManager.configure(
                                    context = activity,
                                    gameId = appSettings?.unityGameId ?: UnityAdsManager.DEFAULT_GAME_ID,
                                    placementId = appSettings?.unityPlacementId ?: UnityAdsManager.DEFAULT_PLACEMENT_ID,
                                    interstitialPlacementId = appSettings?.unityInterstitialId ?: UnityAdsManager.DEFAULT_INTERSTITIAL_ID,
                                    bannerPlacementId = appSettings?.unityBannerId ?: UnityAdsManager.DEFAULT_BANNER_ID,
                                    accountName = appSettings?.adsAccountName ?: UnityAdsManager.DEFAULT_ACCOUNT_NAME,
                                    accountEmail = appSettings?.adsAccountEmail ?: UnityAdsManager.DEFAULT_ACCOUNT_EMAIL,
                                    organizationId = appSettings?.adsOrganizationId ?: UnityAdsManager.DEFAULT_ORGANIZATION_ID,
                                    adsEnabled = appSettings?.adsEnabled ?: true,
                                    testMode = appSettings?.unityTestMode ?: UnityAdsManager.DEFAULT_TEST_MODE,
                                    dailyAdLimit = maxDailyAds,
                                    rewardPerAd = appSettings?.adRewardAmount ?: UnityAdsManager.DEFAULT_REWARD_PER_AD
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SlateCardBg,
                            border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.45f)),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("watch_ads_reward_card")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF2E1065).copy(alpha = 0.5f),
                                                SlateCardBg
                                            )
                                        )
                                    )
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(Color(0xFF8B5CF6), Color(0xFFD946EF))
                                                    ),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayCircle,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "WATCH ADS & EARN",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White,
                                                    letterSpacing = 0.5.sp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = Color(0xFFD946EF).copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "FREE CASH 🎁",
                                                        color = Color(0xFFF472B6),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Earn ₹1 – ₹5 random deposit balance per video",
                                                fontSize = 11.sp,
                                                color = TextSecondaryMuted
                                            )
                                        }
                                    }
                                }

                                // Progress Dots & Count
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "DAILY ALLOWANCE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF94A3B8)
                                        )
                                        Text(
                                            text = "$todayWatchedCount / $maxDailyAds Completed Today",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (todayWatchedCount >= maxDailyAds) TrophyGold else Color.White
                                        )
                                    }

                                    // Segment Indicators (up to 7 dots for clean layout)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val dotsToShow = maxDailyAds.coerceIn(1, 7)
                                        for (i in 1..dotsToShow) {
                                            val isDone = i <= todayWatchedCount
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 22.dp, height = 10.dp)
                                                    .background(
                                                        color = if (isDone) SuccessGreen else Color(0xFF334155),
                                                        shape = RoundedCornerShape(3.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isDone) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(8.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (adErrorMsg != null) {
                                    Text(
                                        text = adErrorMsg!!,
                                        fontSize = 11.sp,
                                        color = Color(0xFFEF4444),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                // Action Button or Limit Reached Notice
                                if (todayWatchedCount >= maxDailyAds) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color(0xFF0F172A),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFF334155))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.HourglassEmpty,
                                                contentDescription = null,
                                                tint = TrophyGold,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "Daily Limit Reached ($todayWatchedCount/$maxDailyAds Watched)",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TrophyGold
                                                )
                                                Text(
                                                    text = "Come back tomorrow after 12:00 AM midnight for $maxDailyAds more rewarded video ads!",
                                                    fontSize = 10.sp,
                                                    color = TextSecondaryMuted
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            if (isAdShowing || activity == null) return@Button
                                            isAdShowing = true
                                            adErrorMsg = null

                                            UnityAdsManager.showRewardedAd(
                                                activity = activity,
                                                placementId = appSettings?.unityPlacementId ?: UnityAdsManager.DEFAULT_PLACEMENT_ID,
                                                onRewardEarned = { randomReward ->
                                                    isAdShowing = false
                                                    viewModel.claimAdReward(randomReward) { success, msg, claimedAmt ->
                                                        if (success) {
                                                            rewardEarnedModalAmount = claimedAmt
                                                        } else {
                                                            adErrorMsg = msg
                                                        }
                                                    }
                                                },
                                                onAdDismissed = {
                                                    isAdShowing = false
                                                },
                                                onError = { err ->
                                                    isAdShowing = false
                                                    adErrorMsg = err
                                                }
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF8B5CF6)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = !isAdShowing,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("watch_reward_ad_button")
                                    ) {
                                        if (isAdShowing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Loading Video Ad...",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.PlayCircle,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Watch Ad • Win ₹1 - ₹5 ($remainingAds Left Today)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Celebration Modal for Ad Reward
                        if (rewardEarnedModalAmount != null) {
                            AdRewardCelebrationDialog(
                                rewardAmount = rewardEarnedModalAmount!!,
                                remainingAdsToday = remainingAds,
                                onDismiss = { rewardEarnedModalAmount = null }
                            )
                        }
                    }
                }

                // Quick Deposit Presets
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                        border = BorderStroke(1.dp, SlateMutedBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Quick Instant Top-Up (Paytm QR Code)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(50, 100, 200, 500).forEach { amount ->
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                depositInitialAmount = amount
                                                showDepositDialog = true
                                            },
                                        color = SlateSubtle,
                                        border = BorderStroke(1.dp, SlateMutedBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "+₹$amount",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = if (isCompactScreen) 12.sp else 13.sp,
                                                color = FireOrangeDark,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            Text(
                                                text = "Add",
                                                fontSize = 10.sp,
                                                color = TextSecondaryMuted,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Ledger & Withdrawals Section Tabs
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                        border = BorderStroke(1.dp, SlateMutedBorder)
                    ) {
                        TabRow(
                            selectedTabIndex = selectedLedgerTab,
                            containerColor = SlateCardBg,
                            contentColor = FireOrange,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedLedgerTab]),
                                    color = FireOrange,
                                    height = 3.dp
                                )
                            }
                        ) {
                            Tab(
                                selected = selectedLedgerTab == 0,
                                onClick = { selectedLedgerTab = 0 },
                                text = {
                                    Text(
                                        text = "Transactions (${transactions.size})",
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedLedgerTab == 0) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedLedgerTab == 0) FireOrange else TextSecondaryMuted
                                    )
                                },
                                modifier = Modifier.testTag("wallet_tab_transactions")
                            )
                            Tab(
                                selected = selectedLedgerTab == 1,
                                onClick = { selectedLedgerTab = 1 },
                                text = {
                                    Text(
                                        text = if (pendingWithdrawalCount > 0) "Withdrawals (${userWithdrawals.size} • $pendingWithdrawalCount Pending)" else "Withdrawals (${userWithdrawals.size})",
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedLedgerTab == 1) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedLedgerTab == 1) FireOrange else TextSecondaryMuted
                                    )
                                },
                                modifier = Modifier.testTag("wallet_tab_withdrawals")
                            )
                        }
                    }
                }

                if (selectedLedgerTab == 0) {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(filterOptions) { filter ->
                                val isSelected = selectedFilter == filter
                                val label = when (filter) {
                                    "ALL" -> "All"
                                    "DEPOSIT" -> "Deposits"
                                    "ENTRY_FEE" -> "Entry Fees"
                                    "PRIZE_WIN" -> "Winnings"
                                    "WITHDRAWAL" -> "Withdrawals"
                                    else -> filter
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFilter = filter },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
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
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }

                    if (filteredList.isEmpty()) {
                        item {
                            Text(
                                text = "No transactions found in this category.",
                                fontSize = 12.sp,
                                color = TextSecondaryMuted
                            )
                        }
                    } else {
                        items(filteredList) { txn ->
                            TransactionItemCard(
                                txn = txn,
                                userName = currentUser?.username ?: "Player",
                                userEmail = currentUser?.email ?: ""
                            )
                        }
                    }
                } else {
                    val savedUpi = currentUser?.savedUpiId.orEmpty()
                    if (savedUpi.isNotBlank()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SlateCardBg,
                                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("saved_upi_summary_card")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(SuccessGreen.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.BookmarkAdded,
                                                contentDescription = null,
                                                tint = SuccessGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "SAVED WITHDRAWAL UPI",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SuccessGreen
                                            )
                                            Text(
                                                text = savedUpi,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimaryDark,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    TextButton(
                                        onClick = { showWithdrawDialog = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("withdraw_with_saved_upi_button")
                                    ) {
                                        Text(
                                            text = "Withdraw Now",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SuccessGreen
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (userWithdrawals.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
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
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = TextSecondaryMuted,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No UPI Withdrawal Requests Yet",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                    Text(
                                        text = "Win tournaments and tap 'Withdraw UPI' above to transfer your winnings directly to your UPI ID.",
                                        fontSize = 11.sp,
                                        color = TextSecondaryMuted,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(userWithdrawals, key = { it.id }) { wdr ->
                            UserWithdrawalTrackingCard(wdr)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // Real Paytm UPI Deposit Modal
    if (showDepositDialog) {
        DepositModalDialog(
            currentBalance = currentUser?.walletBalance ?: 0,
            initialAmount = depositInitialAmount,
            userName = currentUser?.username ?: "Player",
            userEmail = currentUser?.email ?: "",
            onDismiss = { showDepositDialog = false },
            onVerifyPayment = { orderId, amount ->
                viewModel.checkAndVerifyPaytmDeposit(orderId, amount)
            }
        )
    }

    // UPI Winning Withdrawal Request Modal
    if (showWithdrawDialog) {
        UpiWithdrawalModalDialog(
            withdrawableWinnings = withdrawableWinnings,
            totalBalance = currentUser?.walletBalance ?: 0,
            defaultName = currentUser?.username ?: "",
            savedUpiId = currentUser?.savedUpiId ?: "",
            onDismiss = { showWithdrawDialog = false },
            onSubmitWithdrawal = { amount, upiId, holderName, saveUpi, onResult ->
                viewModel.requestWithdrawal(amount, upiId, holderName, saveUpiId = saveUpi) { success, msg ->
                    onResult(success, msg)
                    if (success) {
                        selectedLedgerTab = 1 // Switch to Withdrawals tab so user immediately tracks status
                    }
                }
            }
        )
    }
}

@Composable
fun TransactionItemCard(
    txn: TransactionDto,
    userName: String = "Player",
    userEmail: String = ""
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isDownloadingReceipt by remember { mutableStateOf(false) }
    var receiptDownloaded by remember { mutableStateOf(false) }

    val isPositive = txn.amount > 0
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val formattedDate = dateFormat.format(Date(txn.timestamp))

    val (badgeBg, badgeColor, typeLabel) = when (txn.type) {
        "DEPOSIT" -> Triple(SuccessGreen.copy(alpha = 0.15f), SuccessGreen, "DEPOSIT")
        "PRIZE_WIN" -> Triple(TrophyGold.copy(alpha = 0.15f), TrophyGold, "WINNING")
        "ENTRY_FEE" -> Triple(Color(0xFFEF4444).copy(alpha = 0.15f), Color(0xFFEF4444), "MATCH ENTRY")
        "WITHDRAWAL" -> Triple(FireOrange.copy(alpha = 0.15f), FireOrangeDark, "UPI WITHDRAWAL")
        "WITHDRAWAL_REFUND" -> Triple(SuccessGreen.copy(alpha = 0.15f), SuccessGreen, "WDR REFUND")
        "SIGNUP_BONUS" -> Triple(TrophyGold.copy(alpha = 0.18f), FireOrangeDark, "SIGN-UP BONUS")
        "REWARD_AD" -> Triple(Color(0xFF8B5CF6).copy(alpha = 0.2f), Color(0xFFA855F7), "AD REWARD 🎬")
        else -> Triple(Color(0xFF64748B).copy(alpha = 0.15f), Color(0xFF64748B), txn.type)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCardBg),
        border = BorderStroke(1.dp, SlateMutedBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = badgeBg,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = typeLabel,
                                color = badgeColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ref: ${txn.referenceId}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondaryMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = txn.description,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = TextSecondaryMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (isPositive) "+" else ""}₹${txn.amount.formatCurrency()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isPositive) SuccessGreen else Color(0xFFEF4444),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "Bal: ₹${txn.balanceAfter.formatCurrency()}",
                        fontSize = 11.sp,
                        color = TextSecondaryMuted,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Official Receipt Action for Deposit transactions
            if (txn.type == "DEPOSIT") {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = SlateMutedBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (receiptDownloaded) "Receipt saved" else "Official Payment Receipt",
                            fontSize = 11.sp,
                            color = if (receiptDownloaded) SuccessGreen else TextSecondaryMuted,
                            fontWeight = if (receiptDownloaded) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val receiptData = DepositReceiptData(
                                        orderId = txn.referenceId,
                                        txnId = txn.referenceId,
                                        amount = txn.amount,
                                        updatedBalance = txn.balanceAfter,
                                        paymentMethod = "Paytm UPI Gateway",
                                        userName = userName,
                                        userEmail = userEmail,
                                        timestamp = txn.timestamp
                                    )
                                    DepositReceiptManager.shareReceipt(context, receiptData)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("share_txn_receipt_${txn.referenceId}"),
                            border = BorderStroke(1.dp, SlateMutedBorder),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = TextPrimaryDark,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Share",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryDark
                            )
                        }

                        Button(
                            onClick = {
                                if (isDownloadingReceipt) return@Button
                                coroutineScope.launch {
                                    isDownloadingReceipt = true
                                    val receiptData = DepositReceiptData(
                                        orderId = txn.referenceId,
                                        txnId = txn.referenceId,
                                        amount = txn.amount,
                                        updatedBalance = txn.balanceAfter,
                                        paymentMethod = "Paytm UPI Gateway",
                                        userName = userName,
                                        userEmail = userEmail,
                                        timestamp = txn.timestamp
                                    )
                                    val uri = DepositReceiptManager.saveReceiptToDevice(context, receiptData).getOrNull()
                                    isDownloadingReceipt = false
                                    if (uri != null) {
                                        receiptDownloaded = true
                                        Toast.makeText(context, "Receipt saved to Downloads / Pictures folder!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Could not save receipt", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("download_txn_receipt_${txn.referenceId}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (receiptDownloaded) SuccessGreen else HighContrastDark,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(
                                imageVector = if (receiptDownloaded) Icons.Default.Check else Icons.Default.Download,
                                contentDescription = "Download Receipt",
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (receiptDownloaded) "Saved" else "Receipt",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class DepositFlowStep {
    SELECT_AMOUNT,
    PAYMENT_GATEWAY,
    VERIFIED_SUCCESS
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DepositModalDialog(
    currentBalance: Number = 0,
    initialAmount: Int = 100,
    userName: String = "Player",
    userEmail: String = "",
    onDismiss: () -> Unit,
    onVerifyPayment: suspend (orderId: String, amount: Int) -> PaytmTxnStatus
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(DepositFlowStep.SELECT_AMOUNT) }
    var amountInput by remember { mutableStateOf(if (initialAmount > 0) initialAmount.toString() else "100") }
    var currentOrderId by remember { mutableStateOf("") }
    var currentUpiDeeplink by remember { mutableStateOf("") }
    var currentQrCodeUrl by remember { mutableStateOf("") }

    var isVerifying by remember { mutableStateOf(false) }
    var isDownloadingQr by remember { mutableStateOf(false) }
    var isDownloadingReceipt by remember { mutableStateOf(false) }
    var receiptDownloaded by remember { mutableStateOf(false) }
    var verificationStatusMessage by remember { mutableStateOf<String?>(null) }
    var isStatusError by remember { mutableStateOf(false) }

    // Verified Success Receipt Details
    var confirmedTxnId by remember { mutableStateOf("") }
    var confirmedAmount by remember { mutableStateOf(0) }
    var confirmedBankTxnId by remember { mutableStateOf<String?>(null) }

    // Celebratory Lottie Animation for Deposit Success
    val lottieComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.deposit_success))
    val lottieProgress by animateLottieCompositionAsState(
        composition = lottieComposition,
        iterations = LottieConstants.IterateForever,
        isPlaying = currentStep == DepositFlowStep.VERIFIED_SUCCESS,
        speed = 1.05f
    )

    val quickAmounts = listOf(50, 100, 200, 500, 1000)

    val parsedAmount = amountInput.toIntOrNull() ?: 0
    val projectedBalance = currentBalance.toDouble() + parsedAmount

    fun verifyStatus(silent: Boolean = false) {
        if (currentOrderId.isBlank() || isVerifying) return
        coroutineScope.launch {
            isVerifying = true
            if (!silent) {
                verificationStatusMessage = "Querying Paytm Payment Gateway for Order #$currentOrderId..."
                isStatusError = false
            }

            try {
                val status = onVerifyPayment(currentOrderId, parsedAmount)
                if (status.isSuccess) {
                    confirmedTxnId = status.txnId ?: ("PTM" + (100000..999999).random())
                    confirmedAmount = status.txnAmount?.toInt() ?: parsedAmount
                    confirmedBankTxnId = status.bankTxnId
                    currentStep = DepositFlowStep.VERIFIED_SUCCESS
                } else if (status.isPending) {
                    isStatusError = false
                    verificationStatusMessage = "⏳ Payment pending. If you just authorized in your UPI app, please wait 5-10 seconds and tap 'Verify Payment Status' again."
                } else {
                    isStatusError = true
                    verificationStatusMessage = status.respMsg.ifBlank { "Status: ${status.status}. Complete the QR payment in your UPI app and retry." }
                }
            } catch (e: Exception) {
                isStatusError = true
                verificationStatusMessage = "Connection error: ${e.message ?: "Could not reach Paytm gateway"}"
            } finally {
                isVerifying = false
            }
        }
    }

    var sheetVisible by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { sheetVisible = true }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .systemBarsPadding()
                .imePadding()
                .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 52.dp)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            val isCompactDialog = maxWidth < 360.dp
            val contentHorizontalPadding = if (isCompactDialog) 14.dp else 18.dp
            val qrContainerSize = (maxWidth * 0.38f).coerceIn(115.dp, 148.dp)
            val sheetMaxHeight = maxHeight * 0.84f

            androidx.compose.animation.AnimatedVisibility(
                visible = sheetVisible,
                enter = androidx.compose.animation.slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                    )
                ) + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it / 2 }) + androidx.compose.animation.fadeOut()
            ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = sheetMaxHeight)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(24.dp),
                color = SlateCardBg,
                border = BorderStroke(1.5.dp, FireOrange.copy(alpha = 0.6f)),
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = sheetMaxHeight)
                ) {
                    when (currentStep) {
                        DepositFlowStep.SELECT_AMOUNT -> {
                            // Header (Fixed) with Bottom Sheet Drag Handle
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(HighContrastDark, Color(0xFF1E293B))
                                        )
                                    )
                                    .padding(horizontal = contentHorizontalPadding, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(44.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color.White.copy(alpha = 0.35f))
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(if (isCompactDialog) 36.dp else 42.dp)
                                                .background(TrophyGold.copy(alpha = 0.2f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalanceWallet,
                                                contentDescription = null,
                                                tint = TrophyGold,
                                                modifier = Modifier.size(if (isCompactDialog) 20.dp else 24.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Instant Wallet Top-Up",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = if (isCompactDialog) 14.sp else 16.sp,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(SuccessGreen, CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Paytm QR Gateway • 0% Fee",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF94A3B8),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    IconButton(
                                        onClick = onDismiss,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .testTag("deposit_close_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Content Body (Scrollable)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = contentHorizontalPadding, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Balance Impact Projection Card
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = SlateSubtle,
                                    border = BorderStroke(1.dp, SlateMutedBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Current Balance",
                                                fontSize = 10.sp,
                                                color = TextSecondaryMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "₹$currentBalance",
                                                fontSize = if (isCompactDialog) 14.sp else 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimaryDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = FireOrange,
                                            modifier = Modifier
                                                .padding(horizontal = 6.dp)
                                                .size(16.dp)
                                        )

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = "New Balance (+₹$parsedAmount)",
                                                fontSize = 10.sp,
                                                color = SuccessGreen,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "₹$projectedBalance",
                                                fontSize = if (isCompactDialog) 15.sp else 17.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = SuccessGreen,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Amount Entry & Quick Chips
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "ENTER TOP-UP AMOUNT (₹)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondaryMuted,
                                        letterSpacing = 0.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    OutlinedTextField(
                                        value = amountInput,
                                        onValueChange = { amountInput = it.filter { char -> char.isDigit() }.take(5) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.CurrencyRupee,
                                                contentDescription = null,
                                                tint = FireOrange,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            if (amountInput.isNotBlank()) {
                                                IconButton(onClick = { amountInput = "" }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Clear",
                                                        tint = TextSecondaryMuted,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("deposit_amount_input")
                                    )

                                    // Quick Amount Chips - Responsive sizing so +₹1000 never wraps or clips
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        quickAmounts.forEach { amt ->
                                            val isSelected = parsedAmount == amt
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) FireOrange.copy(alpha = 0.15f) else SlateSubtle,
                                                border = BorderStroke(1.dp, if (isSelected) FireOrange else SlateMutedBorder),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { amountInput = amt.toString() }
                                            ) {
                                                Text(
                                                    text = "+₹$amt",
                                                    fontSize = if (isCompactDialog) 10.sp else 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) FireOrangeDark else TextPrimaryDark,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier
                                                        .padding(vertical = 8.dp, horizontal = 2.dp)
                                                        .fillMaxWidth(),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }

                                // Single Deposit Method: Paytm UPI QR Code
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "DEPOSIT METHOD",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondaryMuted,
                                        letterSpacing = 0.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp)),
                                        shape = RoundedCornerShape(14.dp),
                                        color = FireOrange.copy(alpha = 0.12f),
                                        border = BorderStroke(1.5.dp, FireOrange)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(if (isCompactDialog) 38.dp else 42.dp)
                                                    .background(FireOrange.copy(alpha = 0.15f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.QrCode2,
                                                    contentDescription = "Paytm QR Code",
                                                    tint = FireOrange,
                                                    modifier = Modifier.size(if (isCompactDialog) 22.dp else 24.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = "Paytm UPI QR Code",
                                                        fontSize = if (isCompactDialog) 13.sp else 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimaryDark,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.align(Alignment.CenterVertically)
                                                    )
                                                    Surface(
                                                        color = FireOrange,
                                                        shape = RoundedCornerShape(4.dp),
                                                        modifier = Modifier.align(Alignment.CenterVertically)
                                                    ) {
                                                        Text(
                                                            text = "QR ONLY",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = Color.White,
                                                            maxLines = 1,
                                                            softWrap = false,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = "Instant dynamic QR code • Scan with Paytm, PhonePe, GPay, BHIM or any UPI app",
                                                    fontSize = 11.sp,
                                                    color = FireOrangeDark,
                                                    lineHeight = 14.sp
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .background(FireOrange, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Security Banner
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF0FDF4),
                                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "256-Bit SSL Encrypted • Verified via Paytm Payment Gateway",
                                            fontSize = 10.sp,
                                            color = Color(0xFF166534),
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = 13.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            // Actions (Fixed Bottom Bar)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = SlateCardBg,
                                border = BorderStroke(1.dp, SlateMutedBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = contentHorizontalPadding,
                                            vertical = 14.dp
                                        ),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = onDismiss,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, SlateMutedBorder),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .weight(0.9f)
                                            .heightIn(min = 48.dp)
                                    ) {
                                        Text(
                                            text = "Cancel",
                                            color = TextPrimaryDark,
                                            fontSize = if (isCompactDialog) 12.sp else 13.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (parsedAmount > 0) {
                                                val orderId = PaytmPaymentService.generateOrderId()
                                                val deeplink = PaytmPaymentService.buildUpiDeeplink(orderId, parsedAmount)
                                                val qrUrl = PaytmPaymentService.buildQrCodeUrl(deeplink)

                                                currentOrderId = orderId
                                                currentUpiDeeplink = deeplink
                                                currentQrCodeUrl = qrUrl
                                                verificationStatusMessage = "Scan the QR code with any UPI app to complete payment."
                                                isStatusError = false
                                                currentStep = DepositFlowStep.PAYMENT_GATEWAY
                                            }
                                        },
                                        enabled = parsedAmount > 0,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = FireOrange,
                                            disabledContainerColor = Color(0xFFCBD5E1)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .weight(1.9f)
                                            .heightIn(min = 48.dp)
                                            .testTag("confirm_deposit_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode2,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Generate QR • ₹${if (parsedAmount > 0) parsedAmount else 0}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = if (isCompactDialog) 12.sp else 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        DepositFlowStep.PAYMENT_GATEWAY -> {
                            // Paytm QR Code Payment Screen (Header fixed at top)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(HighContrastDark, Color(0xFF0F172A))
                                        )
                                    )
                                    .padding(horizontal = contentHorizontalPadding, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        IconButton(
                                            onClick = { currentStep = DepositFlowStep.SELECT_AMOUNT },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Paytm QR Code Payment",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = if (isCompactDialog) 14.sp else 15.sp,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Scan QR Code with any UPI App to Pay",
                                                fontSize = 10.sp,
                                                color = Color(0xFF94A3B8),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    IconButton(
                                        onClick = onDismiss,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Middle Scrollable Area (Amount, UPI ID, QR Code, Quick instructions)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = contentHorizontalPadding, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Amount & Order ID Badge
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, SlateMutedBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "TOTAL AMOUNT TO PAY",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondaryMuted,
                                            letterSpacing = 0.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "₹$parsedAmount",
                                            fontSize = if (isCompactDialog) 22.sp else 26.sp,
                                            fontWeight = FontWeight.Black,
                                            color = FireOrangeDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .background(if (ThemePreferenceState.isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                                .clickable {
                                                    clipboardManager.setText(AnnotatedString(currentOrderId))
                                                    Toast.makeText(context, "Copied Order ID: $currentOrderId", Toast.LENGTH_SHORT).show()
                                                }
                                        ) {
                                            Text(
                                                text = "Order ID: $currentOrderId",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (ThemePreferenceState.isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy",
                                                tint = if (ThemePreferenceState.isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }

                                // Scan & Pay QR Code
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.5.dp, FireOrange.copy(alpha = 0.5f)),
                                    shadowElevation = 2.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "SCAN & PAY VIA ANY UPI APP",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FireOrangeDark,
                                            letterSpacing = 0.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        Box(
                                            modifier = Modifier
                                                .size(qrContainerSize)
                                                .background(Color.White, RoundedCornerShape(8.dp))
                                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = currentQrCodeUrl,
                                                contentDescription = "Paytm UPI QR Code",
                                                modifier = Modifier
                                                    .size(qrContainerSize - 10.dp)
                                                    .testTag("deposit_qr_code_image")
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "GPay • PhonePe • Paytm • BHIM",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF475569),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Download QR Code Option (replaces showing UPI ID)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (ThemePreferenceState.isDarkMode) Color(0xFF0F172A) else Color(0xFFFFF7ED),
                                    border = BorderStroke(1.dp, FireOrange.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "SAVE QR TO GALLERY",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = FireOrangeDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Download QR & scan from any UPI app",
                                                fontSize = if (isCompactDialog) 11.sp else 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (ThemePreferenceState.isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = {
                                                if (!isDownloadingQr) {
                                                    coroutineScope.launch {
                                                        isDownloadingQr = true
                                                        val result = PaytmPaymentService.downloadQrCodeToGallery(
                                                            context = context,
                                                            qrCodeUrl = currentQrCodeUrl,
                                                            orderId = currentOrderId,
                                                            amount = parsedAmount
                                                        )
                                                        isDownloadingQr = false
                                                        result.onSuccess { msg ->
                                                            isStatusError = false
                                                            verificationStatusMessage = "✅ $msg"
                                                            Toast.makeText(context, "QR Code downloaded to Gallery!", Toast.LENGTH_SHORT).show()
                                                        }.onFailure { err ->
                                                            isStatusError = true
                                                            verificationStatusMessage = "Could not save QR: ${err.message ?: "Try again"}"
                                                            Toast.makeText(context, "Failed to download QR", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            },
                                            enabled = !isDownloadingQr,
                                            colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .heightIn(min = 36.dp)
                                                .testTag("download_qr_button")
                                        ) {
                                            if (isDownloadingQr) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(12.dp),
                                                    color = Color.White,
                                                    strokeWidth = 1.8.dp
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Saving...",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Download,
                                                    contentDescription = "Download QR",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Download QR",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }

                                // Quick Instructions Badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, SlateMutedBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = FireOrange,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Download or scan the QR code in your UPI app, pay ₹$parsedAmount, then tap Verify Payment Status below.",
                                            fontSize = 10.sp,
                                            color = Color(0xFF475569),
                                            lineHeight = 14.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            // Pinned Bottom Action Bar with "Verify Payment Status" - ALWAYS VISIBLE, NEVER HIDDEN!
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = SlateCardBg,
                                border = BorderStroke(1.dp, SlateMutedBorder),
                                shadowElevation = 10.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = contentHorizontalPadding,
                                            vertical = 14.dp
                                        ),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Status / Error feedback message
                                    if (!verificationStatusMessage.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isStatusError) Color(0xFFFEF2F2) else Color(0xFFF1F5F9),
                                            border = BorderStroke(1.dp, if (isStatusError) Color(0xFFFECACA) else Color(0xFFCBD5E1)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = verificationStatusMessage ?: "",
                                                fontSize = 11.sp,
                                                color = if (isStatusError) LiveRed else Color(0xFF334155),
                                                lineHeight = 15.sp,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    // Pinned Prominent Verification Button
                                    Button(
                                        onClick = { verifyStatus(silent = false) },
                                        enabled = !isVerifying,
                                        colors = ButtonDefaults.buttonColors(containerColor = HighContrastDark),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, FireOrange),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("verify_payment_button")
                                    ) {
                                        if (isVerifying) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = FireOrange,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Verifying via Paytm Gateway...",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Sync,
                                                contentDescription = null,
                                                tint = FireOrange,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Verify Payment Status",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        DepositFlowStep.VERIFIED_SUCCESS -> {
                            // Verified Success Screen (Scrollable middle, pinned bottom button)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = contentHorizontalPadding, vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Lottie Celebration Animation for Deposit Success
                                Box(
                                    modifier = Modifier
                                        .size(if (isCompactDialog) 120.dp else 140.dp)
                                        .testTag("deposit_success_lottie_container"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (lottieComposition != null) {
                                        LottieAnimation(
                                            composition = lottieComposition,
                                            progress = { lottieProgress },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .background(SuccessGreen.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Success",
                                                tint = SuccessGreen,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Payment Verified & Credited!",
                                        fontSize = if (isCompactDialog) 16.sp else 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Paytm Gateway confirmed your transaction.",
                                        fontSize = 12.sp,
                                        color = TextSecondaryMuted,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFF0FDF4),
                                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "AMOUNT ADDED",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF166534)
                                        )
                                        Text(
                                            text = "+₹$confirmedAmount",
                                            fontSize = if (isCompactDialog) 24.sp else 28.sp,
                                            fontWeight = FontWeight.Black,
                                            color = SuccessGreen,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Updated Balance: ₹${(currentBalance.toDouble() + confirmedAmount).formatCurrency()}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Receipt Breakdown
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, SlateMutedBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Order ID", fontSize = 11.sp, color = TextSecondaryMuted)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = currentOrderId,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.End,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Paytm Txn ID", fontSize = 11.sp, color = TextSecondaryMuted)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = confirmedTxnId,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = FireOrangeDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.End,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Payment Method", fontSize = 11.sp, color = TextSecondaryMuted)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Paytm QR Code",
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.End,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Status", fontSize = 11.sp, color = TextSecondaryMuted)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "TXN_SUCCESS (Code 01)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SuccessGreen,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.End,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                // Receipt Actions: Download & Share Receipt Card
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SlateCardBg,
                                    border = BorderStroke(1.dp, SlateMutedBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ReceiptLong,
                                                contentDescription = null,
                                                tint = FireOrange,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Official Deposit Receipt",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimaryDark
                                            )
                                            if (receiptDownloaded) {
                                                Spacer(modifier = Modifier.weight(1f))
                                                Surface(
                                                    color = SuccessGreen.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "Saved ✓",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SuccessGreen,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = "Download a signed payment voucher receipt to your device Downloads storage or share with your records.",
                                            fontSize = 11.sp,
                                            color = TextSecondaryMuted,
                                            lineHeight = 15.sp
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    if (isDownloadingReceipt) return@Button
                                                    coroutineScope.launch {
                                                        isDownloadingReceipt = true
                                                        val receiptData = DepositReceiptData(
                                                            orderId = currentOrderId,
                                                            txnId = confirmedTxnId,
                                                            bankTxnId = confirmedBankTxnId,
                                                            amount = confirmedAmount,
                                                            updatedBalance = currentBalance.toDouble() + confirmedAmount,
                                                            paymentMethod = "Paytm QR & UPI Gateway",
                                                            userName = userName,
                                                            userEmail = userEmail
                                                        )
                                                        val uri = DepositReceiptManager.saveReceiptToDevice(context, receiptData).getOrNull()
                                                        isDownloadingReceipt = false
                                                        if (uri != null) {
                                                            receiptDownloaded = true
                                                            Toast.makeText(context, "Receipt saved to Downloads / Pictures folder!", Toast.LENGTH_LONG).show()
                                                        } else {
                                                            Toast.makeText(context, "Unable to save receipt. Please try sharing instead.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (receiptDownloaded) SuccessGreen else HighContrastDark,
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                enabled = !isDownloadingReceipt,
                                                modifier = Modifier
                                                    .weight(1.2f)
                                                    .heightIn(min = 44.dp)
                                                    .testTag("download_deposit_receipt_button")
                                            ) {
                                                if (isDownloadingReceipt) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(16.dp),
                                                        strokeWidth = 2.dp,
                                                        color = Color.White
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Saving...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                } else {
                                                    Icon(
                                                        imageVector = if (receiptDownloaded) Icons.Default.Check else Icons.Default.Download,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (receiptDownloaded) "Downloaded" else "Download Receipt",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val receiptData = DepositReceiptData(
                                                            orderId = currentOrderId,
                                                            txnId = confirmedTxnId,
                                                            bankTxnId = confirmedBankTxnId,
                                                            amount = confirmedAmount,
                                                            updatedBalance = currentBalance.toDouble() + confirmedAmount,
                                                            paymentMethod = "Paytm QR & UPI Gateway",
                                                            userName = userName,
                                                            userEmail = userEmail
                                                        )
                                                        DepositReceiptManager.shareReceipt(context, receiptData)
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, SlateMutedBorder),
                                                modifier = Modifier
                                                    .weight(0.8f)
                                                    .heightIn(min = 44.dp)
                                                    .testTag("share_deposit_receipt_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = "Share Receipt",
                                                    tint = TextPrimaryDark,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Share",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimaryDark,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Done Button (Pinned at Bottom)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = SlateCardBg,
                                border = BorderStroke(1.dp, SlateMutedBorder)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = contentHorizontalPadding,
                                            vertical = 14.dp
                                        )
                                ) {
                                    Button(
                                        onClick = onDismiss,
                                        colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("deposit_done_button")
                                    ) {
                                        Text(
                                            text = "Done • View Updated Wallet",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
fun UserWithdrawalTrackingCard(request: WithdrawalRequestDto) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(request.timestamp) { sdf.format(Date(request.timestamp)) }

    val (statusColor, statusBg, statusLabel, statusIcon) = when (request.status.uppercase()) {
        "APPROVED" -> listOf(SuccessGreen, Color(0xFFF0FDF4), "APPROVED & PAID", Icons.Default.CheckCircle)
        "REJECTED" -> listOf(LiveRed, Color(0xFFFEF2F2), "REJECTED & REFUNDED", Icons.Default.Cancel)
        else -> listOf(FireOrangeDark, Color(0xFFFFF7ED), "PENDING ADMIN REVIEW", Icons.Default.HourglassEmpty)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_withdrawal_card_${request.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCardBg),
        border = BorderStroke(1.dp, SlateMutedBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusBg as Color,
                        border = BorderStroke(1.dp, (statusColor as Color).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = statusIcon as androidx.compose.ui.graphics.vector.ImageVector,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = statusLabel as String,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }
                }

                Text(
                    text = "₹${request.amount}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimaryDark
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "UPI ID: ${request.upiId}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimaryDark
                    )
                    if (request.accountHolderName.isNotBlank()) {
                        Text(
                            text = "Account Holder: ${request.accountHolderName}",
                            fontSize = 11.sp,
                            color = TextSecondaryMuted
                        )
                    }
                    Text(
                        text = "Req ID: ${request.id} • $formattedDate",
                        fontSize = 10.sp,
                        color = TextSecondaryMuted
                    )
                }
            }

            if (request.adminRefId.isNotBlank() || request.adminNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = SlateMutedBorder)
                Spacer(modifier = Modifier.height(6.dp))
                if (request.adminRefId.isNotBlank()) {
                    Text(
                        text = "UTR / Reference ID: ${request.adminRefId}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = SuccessGreen
                    )
                }
                if (request.adminNote.isNotBlank()) {
                    Text(
                        text = "Admin Note: ${request.adminNote}",
                        fontSize = 11.sp,
                        color = if (request.status == "REJECTED") LiveRed else TextSecondaryMuted
                    )
                }
            }
        }
    }
}

@Composable
fun UpiWithdrawalModalDialog(
    withdrawableWinnings: Number,
    totalBalance: Number,
    defaultName: String,
    savedUpiId: String = "",
    onDismiss: () -> Unit,
    onSubmitWithdrawal: (amount: Int, upiId: String, holderName: String, saveUpi: Boolean, onResult: (Boolean, String) -> Unit) -> Unit
) {
    var amountInput by remember { mutableStateOf(if (withdrawableWinnings.toDouble() >= 50) "50" else if (withdrawableWinnings.toDouble() >= 20) "${withdrawableWinnings.formatCurrency()}" else "") }
    var upiIdInput by remember { mutableStateOf(savedUpiId.ifBlank { "" }) }
    var saveUpiForFuture by remember { mutableStateOf(true) }
    var holderNameInput by remember { mutableStateOf(defaultName) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val parsedAmount = amountInput.toIntOrNull() ?: 0

    var sheetVisible by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { sheetVisible = true }

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .systemBarsPadding()
                .imePadding()
                .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 52.dp)
                .clickable(enabled = !isSubmitting, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            val isCompactDialog = maxWidth < 360.dp
            val contentHorizontalPadding = if (isCompactDialog) 14.dp else 18.dp
            val sheetMaxHeight = maxHeight * 0.84f

            androidx.compose.animation.AnimatedVisibility(
                visible = sheetVisible,
                enter = androidx.compose.animation.slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                    )
                ) + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it / 2 }) + androidx.compose.animation.fadeOut()
            ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = sheetMaxHeight)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(24.dp),
                color = SlateCardBg,
                border = BorderStroke(1.5.dp, SuccessGreen.copy(alpha = 0.6f)),
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = sheetMaxHeight)
                ) {
                    // Header with Drag Handle
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(HighContrastDark, Color(0xFF1E293B))
                                )
                            )
                            .padding(horizontal = contentHorizontalPadding, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.35f))
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(SuccessGreen.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Withdraw Winnings via UPI",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Direct payout to Paytm, PhonePe, GPay or any UPI",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { if (!isSubmitting) onDismiss() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = contentHorizontalPadding, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Balance Breakdown Banner
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SuccessGreen.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "WITHDRAWABLE WINNING BALANCE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
                                    Text(
                                        text = "Total Wallet: ₹$totalBalance • Only match winnings are withdrawable",
                                        fontSize = 10.sp,
                                        color = TextSecondaryMuted
                                    )
                                }
                                Text(
                                    text = "₹$withdrawableWinnings",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SuccessGreen
                                )
                            }
                        }

                        // Amount Field
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = {
                                amountInput = it.filter { ch -> ch.isDigit() }
                                errorMessage = null
                            },
                            label = { Text("Withdrawal Amount (₹) • Min ₹20") },
                            placeholder = { Text("Enter winning amount to withdraw") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdraw_amount_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SuccessGreen,
                                unfocusedBorderColor = SlateMutedBorder
                            )
                        )

                        // Quick Amount Chips
                        if (withdrawableWinnings.toDouble() >= 20) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(20, 50, 100).filter { it.toDouble() <= withdrawableWinnings.toDouble() }.forEach { preset ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SlateSubtle,
                                        border = BorderStroke(1.dp, SlateMutedBorder),
                                        modifier = Modifier
                                            .clickable {
                                                amountInput = preset.toString()
                                                errorMessage = null
                                            }
                                    ) {
                                        Text(
                                            text = "₹$preset",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = TrophyGold.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, TrophyGold),
                                    modifier = Modifier
                                        .clickable {
                                            amountInput = withdrawableWinnings.toString()
                                            errorMessage = null
                                        }
                                ) {
                                    Text(
                                        text = "MAX (₹$withdrawableWinnings)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FireOrangeDark,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Saved UPI Quick-Fill Card (if user has a saved UPI ID)
                        if (savedUpiId.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SuccessGreen.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("saved_upi_badge")
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
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(SuccessGreen.copy(alpha = 0.2f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.BookmarkAdded,
                                                contentDescription = null,
                                                tint = SuccessGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "SAVED UPI ID",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SuccessGreen
                                            )
                                            Text(
                                                text = savedUpiId,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimaryDark,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    if (upiIdInput.trim() != savedUpiId.trim()) {
                                        TextButton(
                                            onClick = {
                                                upiIdInput = savedUpiId
                                                errorMessage = null
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("use_saved_upi_button")
                                        ) {
                                            Text(
                                                text = "Use Saved",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SuccessGreen
                                            )
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = SuccessGreen.copy(alpha = 0.25f),
                                            modifier = Modifier.testTag("saved_upi_active_chip")
                                        ) {
                                            Text(
                                                text = "✓ Auto-filled",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SuccessGreen,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // UPI ID Field
                        OutlinedTextField(
                            value = upiIdInput,
                            onValueChange = {
                                upiIdInput = it
                                errorMessage = null
                            },
                            label = { Text("Your UPI ID") },
                            placeholder = { Text("e.g. yourname@okaxis or 9876543210@paytm") },
                            trailingIcon = {
                                if (savedUpiId.isNotBlank() && upiIdInput.trim() == savedUpiId.trim()) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Saved UPI active",
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdraw_upi_id_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark,
                                focusedBorderColor = SuccessGreen,
                                unfocusedBorderColor = SlateMutedBorder,
                                focusedLabelColor = SuccessGreen,
                                unfocusedLabelColor = TextSecondaryMuted,
                                focusedPlaceholderColor = TextSecondaryMuted,
                                unfocusedPlaceholderColor = TextSecondaryMuted
                            )
                        )

                        // Save UPI ID for future withdrawals toggle
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SlateSubtle.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, SlateMutedBorder.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { saveUpiForFuture = !saveUpiForFuture }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = saveUpiForFuture,
                                    onCheckedChange = { saveUpiForFuture = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = SuccessGreen,
                                        checkmarkColor = Color.White,
                                        uncheckedColor = SlateMutedBorder
                                    ),
                                    modifier = Modifier.testTag("save_upi_checkbox")
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Save UPI ID for future withdrawals",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimaryDark
                                    )
                                    Text(
                                        text = if (savedUpiId.isNotBlank() && upiIdInput.trim() != savedUpiId.trim() && upiIdInput.isNotBlank()) {
                                            "Will update your saved UPI to this new address"
                                        } else {
                                            "Auto-fills next time for 1-tap instant payouts"
                                        },
                                        fontSize = 10.sp,
                                        color = TextSecondaryMuted
                                    )
                                }
                            }
                        }

                        // Account Holder Name Field
                        OutlinedTextField(
                            value = holderNameInput,
                            onValueChange = {
                                holderNameInput = it
                                errorMessage = null
                            },
                            label = { Text("Account Holder Name") },
                            placeholder = { Text("Name registered on UPI") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdraw_holder_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark,
                                focusedBorderColor = SuccessGreen,
                                unfocusedBorderColor = SlateMutedBorder,
                                focusedLabelColor = SuccessGreen,
                                unfocusedLabelColor = TextSecondaryMuted,
                                focusedPlaceholderColor = TextSecondaryMuted,
                                unfocusedPlaceholderColor = TextSecondaryMuted
                            )
                        )

                        if (!errorMessage.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    fontSize = 11.sp,
                                    color = LiveRed,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    // Pinned Bottom Action Bar for UPI Withdrawal Button (Always 100% visible)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SlateCardBg,
                        border = BorderStroke(1.dp, SlateMutedBorder),
                        shadowElevation = 12.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = contentHorizontalPadding,
                                    vertical = 14.dp
                                )
                        ) {
                            Button(
                                onClick = {
                                    when {
                                        withdrawableWinnings.toDouble() < 20 -> {
                                            errorMessage = "You need at least ₹20 in withdrawable tournament winnings to request a payout."
                                        }
                                        parsedAmount < 20 -> {
                                            errorMessage = "Minimum withdrawal amount is ₹20."
                                        }
                                        parsedAmount.toDouble() > withdrawableWinnings.toDouble() -> {
                                            errorMessage = "You can only withdraw from your winning balance (Max: ₹${withdrawableWinnings.formatCurrency()})."
                                        }
                                        !upiIdInput.trim().contains("@") || upiIdInput.trim().length < 5 -> {
                                            errorMessage = "Please enter a valid UPI ID (e.g., player@upi)."
                                        }
                                        holderNameInput.trim().isBlank() -> {
                                            errorMessage = "Please enter the account holder name."
                                        }
                                        else -> {
                                            isSubmitting = true
                                            errorMessage = null
                                            onSubmitWithdrawal(
                                                parsedAmount,
                                                upiIdInput.trim(),
                                                holderNameInput.trim(),
                                                saveUpiForFuture
                                            ) { success, msg ->
                                                isSubmitting = false
                                                if (success) {
                                                    onDismiss()
                                                } else {
                                                    errorMessage = msg
                                                }
                                            }
                                        }
                                    }
                                },
                                enabled = !isSubmitting,
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("submit_withdrawal_button")
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Submitting Request...", color = Color.White, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Request UPI Withdrawal • ₹${if (parsedAmount > 0) parsedAmount else 0}",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
fun AdRewardCelebrationDialog(
    rewardAmount: Double,
    remainingAdsToday: Int,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCardBg),
            border = BorderStroke(1.5.dp, Color(0xFF8B5CF6))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF8B5CF6), Color(0xFF6366F1).copy(alpha = 0.2f))
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Reward",
                        tint = TrophyGold,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "REWARD CREDITED! 🎉",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimaryDark,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Thanks for watching! Deposit balance updated.",
                        fontSize = 12.sp,
                        color = TextSecondaryMuted,
                        textAlign = TextAlign.Center
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2E1065).copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "RANDOM REWARD EARNED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC084FC)
                        )
                        Text(
                            text = "+₹${rewardAmount.formatCurrency()}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = TrophyGold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (remainingAdsToday > 0)
                                "You can watch $remainingAdsToday more ad${if (remainingAdsToday > 1) "s" else ""} today!"
                            else
                                "You completed all 5 ads for today! Resets at midnight.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("claim_ad_reward_dismiss_button")
                ) {
                    Text(
                        text = "Claim & Continue",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

