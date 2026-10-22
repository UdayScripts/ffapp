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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
        .sumOf { it.amount }
    val maxFromLifetimeWinnings = ((currentUser?.totalWinnings ?: 0) - activeWithdrawnSum).coerceAtLeast(0)
    val withdrawableWinnings = minOf(
        currentUser?.effectiveWinningBalance ?: 0,
        maxFromLifetimeWinnings
    ).coerceAtLeast(0)
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
                                text = "₹${currentUser?.walletBalance ?: 0}",
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
                                        text = "₹$totalDeposited",
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
                                        text = "₹$totalWon",
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
                                        text = "₹$withdrawableWinnings",
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
                            TransactionItemCard(txn)
                        }
                    }
                } else {
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
            onDismiss = { showWithdrawDialog = false },
            onSubmitWithdrawal = { amount, upiId, holderName, onResult ->
                viewModel.requestWithdrawal(amount, upiId, holderName) { success, msg ->
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
fun TransactionItemCard(txn: TransactionDto) {
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
        else -> Triple(Color(0xFF64748B).copy(alpha = 0.15f), Color(0xFF64748B), txn.type)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCardBg),
        border = BorderStroke(1.dp, SlateMutedBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                    text = "${if (isPositive) "+" else ""}₹${txn.amount}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isPositive) SuccessGreen else Color(0xFFEF4444),
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "Bal: ₹${txn.balanceAfter}",
                    fontSize = 11.sp,
                    color = TextSecondaryMuted,
                    maxLines = 1,
                    softWrap = false
                )
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
    currentBalance: Int = 0,
    initialAmount: Int = 100,
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
    var verificationStatusMessage by remember { mutableStateOf<String?>(null) }
    var isStatusError by remember { mutableStateOf(false) }

    // Verified Success Receipt Details
    var confirmedTxnId by remember { mutableStateOf("") }
    var confirmedAmount by remember { mutableStateOf(0) }
    var confirmedBankTxnId by remember { mutableStateOf<String?>(null) }

    val quickAmounts = listOf(50, 100, 200, 500, 1000)

    val parsedAmount = amountInput.toIntOrNull() ?: 0
    val projectedBalance = currentBalance + parsedAmount

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
                .background(Color.Black.copy(alpha = 0.70f))
                .systemBarsPadding()
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            val isCompactDialog = maxWidth < 360.dp
            val contentHorizontalPadding = if (isCompactDialog) 14.dp else 18.dp
            val qrContainerSize = (maxWidth * 0.40f).coerceIn(120.dp, 155.dp)

            androidx.compose.animation.AnimatedVisibility(
                visible = sheetVisible,
                enter = androidx.compose.animation.slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                    )
                ) + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut()
            ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight - 16.dp)
                    .clickable(enabled = false) {}
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = SlateCardBg,
                border = BorderStroke(1.dp, SlateMutedBorder),
                shadowElevation = 20.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight - 16.dp)
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
                                        .padding(horizontal = contentHorizontalPadding, vertical = 12.dp),
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
                                                .background(Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
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
                                                color = TextPrimaryDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy",
                                                tint = Color(0xFF475569),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }

                                // Paytm VPA / UPI ID Card
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFFFF7ED),
                                    border = BorderStroke(1.dp, FireOrange.copy(alpha = 0.4f)),
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
                                                text = "UPI VPA (TAP TO COPY)",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = FireOrangeDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = PaytmPaymentService.DEFAULT_UPI_VPA,
                                                fontSize = if (isCompactDialog) 11.sp else 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimaryDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(PaytmPaymentService.DEFAULT_UPI_VPA))
                                                Toast.makeText(context, "Copied UPI ID!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.heightIn(min = 32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Copy",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
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
                                            color = TextSecondaryMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
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
                                            text = "Scan QR with UPI app, pay ₹$parsedAmount, then tap Verify Payment Status below.",
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
                                        .padding(horizontal = contentHorizontalPadding, vertical = 10.dp),
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
                                            text = "Updated Balance: ₹${currentBalance + confirmedAmount}",
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
                                            Text("Payment VPA", fontSize = 11.sp, color = TextSecondaryMuted)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = PaytmPaymentService.DEFAULT_UPI_VPA,
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
                                        .padding(horizontal = contentHorizontalPadding, vertical = 12.dp)
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
    withdrawableWinnings: Int,
    totalBalance: Int,
    defaultName: String,
    onDismiss: () -> Unit,
    onSubmitWithdrawal: (amount: Int, upiId: String, holderName: String, onResult: (Boolean, String) -> Unit) -> Unit
) {
    var amountInput by remember { mutableStateOf(if (withdrawableWinnings >= 50) "50" else if (withdrawableWinnings >= 20) "$withdrawableWinnings" else "") }
    var upiIdInput by remember { mutableStateOf("") }
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
                .background(Color.Black.copy(alpha = 0.70f))
                .systemBarsPadding()
                .clickable(enabled = !isSubmitting, onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            val isCompactDialog = maxWidth < 360.dp
            val contentHorizontalPadding = if (isCompactDialog) 14.dp else 18.dp

            androidx.compose.animation.AnimatedVisibility(
                visible = sheetVisible,
                enter = androidx.compose.animation.slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                    )
                ) + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut()
            ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = SlateCardBg,
                border = BorderStroke(1.dp, SlateMutedBorder),
                shadowElevation = 20.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
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
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = contentHorizontalPadding, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        if (withdrawableWinnings >= 20) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(20, 50, 100).filter { it <= withdrawableWinnings }.forEach { preset ->
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

                        // UPI ID Field
                        OutlinedTextField(
                            value = upiIdInput,
                            onValueChange = {
                                upiIdInput = it
                                errorMessage = null
                            },
                            label = { Text("Your UPI ID") },
                            placeholder = { Text("e.g. yourname@okaxis or 9876543210@paytm") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdraw_upi_id_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SuccessGreen,
                                unfocusedBorderColor = SlateMutedBorder
                            )
                        )

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
                                focusedBorderColor = SuccessGreen,
                                unfocusedBorderColor = SlateMutedBorder
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

                        Button(
                            onClick = {
                                when {
                                    withdrawableWinnings < 20 -> {
                                        errorMessage = "You need at least ₹20 in withdrawable tournament winnings to request a payout."
                                    }
                                    parsedAmount < 20 -> {
                                        errorMessage = "Minimum withdrawal amount is ₹20."
                                    }
                                    parsedAmount > withdrawableWinnings -> {
                                        errorMessage = "You can only withdraw from your winning balance (Max: ₹$withdrawableWinnings)."
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
                                            holderNameInput.trim()
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
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
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
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
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

