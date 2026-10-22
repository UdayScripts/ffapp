package com.example.ui.screens.admin

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firestore.WithdrawalRequestDto
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminWithdrawalsScreen(viewModel: AppViewModel) {
    val allWithdrawals by viewModel.allWithdrawals.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("All", "Pending", "Approved", "Rejected")

    var requestToApprove by remember { mutableStateOf<WithdrawalRequestDto?>(null) }
    var requestToReject by remember { mutableStateOf<WithdrawalRequestDto?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val pendingCount = allWithdrawals.count { it.status == "PENDING" }
    val approvedTotal = allWithdrawals.filter { it.status == "APPROVED" }.sumOf { it.amount }
    val pendingTotal = allWithdrawals.filter { it.status == "PENDING" }.sumOf { it.amount }

    val filteredWithdrawals = remember(allWithdrawals, selectedTabIndex) {
        when (selectedTabIndex) {
            1 -> allWithdrawals.filter { it.status.equals("PENDING", ignoreCase = true) }
            2 -> allWithdrawals.filter { it.status.equals("APPROVED", ignoreCase = true) }
            3 -> allWithdrawals.filter { it.status.equals("REJECTED", ignoreCase = true) }
            else -> allWithdrawals
        }
    }

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
                        modifier = Modifier.testTag("admin_withdrawals_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimaryDark
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "UPI Withdrawal Requests",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$pendingCount Pending • Total Paid: ₹$approvedTotal",
                            fontSize = 11.sp,
                            color = TextSecondaryMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (pendingCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = FireOrange.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, FireOrange),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "$pendingCount PENDING",
                                color = FireOrangeDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateLightBg)
                .padding(innerPadding)
        ) {
            // Summary Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateCardBg)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = FireOrange.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, FireOrange.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "PENDING PAYOUTS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = FireOrangeDark
                        )
                        Text(
                            text = "₹$pendingTotal ($pendingCount)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimaryDark
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = SuccessGreen.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "APPROVED & PAID",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                        Text(
                            text = "₹$approvedTotal",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimaryDark
                        )
                    }
                }
            }

            // Filter Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = SlateCardBg,
                contentColor = FireOrange,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = FireOrange,
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    val count = when (index) {
                        1 -> pendingCount
                        2 -> allWithdrawals.count { it.status == "APPROVED" }
                        3 -> allWithdrawals.count { it.status == "REJECTED" }
                        else -> allWithdrawals.size
                    }
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = "$title ($count)",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTabIndex == index) FireOrange else TextSecondaryMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.testTag("admin_wdr_tab_$index")
                    )
                }
            }

            if (filteredWithdrawals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = TextSecondaryMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No withdrawal requests in this tab",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Player UPI withdrawal requests will appear here in real-time.",
                            fontSize = 12.sp,
                            color = TextSecondaryMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredWithdrawals, key = { it.id }) { req ->
                        AdminWithdrawalCard(
                            request = req,
                            onCopyUpi = { upi ->
                                clipboardManager.setText(AnnotatedString(upi))
                                Toast.makeText(context, "UPI ID copied: $upi", Toast.LENGTH_SHORT).show()
                            },
                            onApproveClick = { requestToApprove = req },
                            onRejectClick = { requestToReject = req }
                        )
                    }
                }
            }
        }
    }

    // Approve Modal
    requestToApprove?.let { req ->
        var utrInput by remember { mutableStateOf("") }
        var noteInput by remember { mutableStateOf("Transferred via UPI") }

        AlertDialog(
            onDismissRequest = { if (!isProcessing) requestToApprove = null },
            containerColor = SlateCardBg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Approve & Confirm UPI Payout",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SlateSubtle,
                        border = BorderStroke(1.dp, SlateMutedBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Pay ₹${req.amount} to UPI ID:",
                                fontSize = 11.sp,
                                color = TextSecondaryMuted
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = req.upiId,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimaryDark
                                )
                                TextButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(req.upiId))
                                        Toast.makeText(context, "UPI Copied!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("Copy", fontSize = 12.sp, color = FireOrange, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = "Holder: ${req.accountHolderName} (${req.userName})",
                                fontSize = 11.sp,
                                color = TextSecondaryMuted
                            )
                        }
                    }

                    OutlinedTextField(
                        value = utrInput,
                        onValueChange = { utrInput = it },
                        label = { Text("UPI Reference / UTR Number (Optional)") },
                        placeholder = { Text("e.g. 429183746192") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_approve_utr_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SuccessGreen,
                            unfocusedBorderColor = SlateMutedBorder
                        )
                    )

                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Admin Remarks") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_approve_note_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SuccessGreen,
                            unfocusedBorderColor = SlateMutedBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessing = true
                        viewModel.approveWithdrawal(
                            withdrawalId = req.id,
                            adminRefId = utrInput.trim(),
                            adminNote = noteInput.trim()
                        ) { _, _ ->
                            isProcessing = false
                            requestToApprove = null
                        }
                    },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier.testTag("confirm_approve_wdr_button")
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Mark Approved & Paid", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { requestToApprove = null },
                    enabled = !isProcessing
                ) {
                    Text("Cancel", color = TextSecondaryMuted)
                }
            }
        )
    }

    // Reject & Refund Modal
    requestToReject?.let { req ->
        var reasonInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { if (!isProcessing) requestToReject = null },
            containerColor = SlateCardBg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = null,
                        tint = LiveRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reject & Refund ₹${req.amount}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Rejecting this request will automatically refund ₹${req.amount} back to ${req.userName}'s withdrawable winning balance and wallet.",
                        fontSize = 12.sp,
                        color = TextSecondaryMuted
                    )

                    OutlinedTextField(
                        value = reasonInput,
                        onValueChange = { reasonInput = it },
                        label = { Text("Reason for Rejection") },
                        placeholder = { Text("e.g. Invalid UPI ID / Name mismatch") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_reject_reason_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LiveRed,
                            unfocusedBorderColor = SlateMutedBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessing = true
                        viewModel.rejectWithdrawal(
                            withdrawalId = req.id,
                            reason = reasonInput.trim().ifBlank { "Invalid UPI details provided" }
                        ) { _, _ ->
                            isProcessing = false
                            requestToReject = null
                        }
                    },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                    modifier = Modifier.testTag("confirm_reject_wdr_button")
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Reject & Refund Player", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { requestToReject = null },
                    enabled = !isProcessing
                ) {
                    Text("Cancel", color = TextSecondaryMuted)
                }
            }
        )
    }
}

@Composable
private fun AdminWithdrawalCard(
    request: WithdrawalRequestDto,
    onCopyUpi: (String) -> Unit,
    onApproveClick: () -> Unit,
    onRejectClick: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val dateStr = remember(request.timestamp) { sdf.format(Date(request.timestamp)) }

    val (statusColor, statusBg, statusLabel) = when (request.status.uppercase()) {
        "APPROVED" -> Triple(SuccessGreen, SuccessGreen.copy(alpha = 0.14f), "APPROVED & PAID")
        "REJECTED" -> Triple(LiveRed, LiveRed.copy(alpha = 0.14f), "REJECTED & REFUNDED")
        else -> Triple(FireOrangeDark, FireOrange.copy(alpha = 0.14f), "PENDING REVIEW")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_wdr_card_${request.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCardBg),
        border = BorderStroke(
            width = if (request.status == "PENDING") 1.5.dp else 1.dp,
            color = if (request.status == "PENDING") FireOrange.copy(alpha = 0.6f) else SlateMutedBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: ID + Date + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = request.id,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondaryMuted
                    )
                    Text(
                        text = dateStr,
                        fontSize = 10.sp,
                        color = TextSecondaryMuted
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg,
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Amount & UPI Info Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = SlateSubtle,
                border = BorderStroke(1.dp, SlateMutedBorder)
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
                            text = "UPI ID (Winning Withdrawal)",
                            fontSize = 10.sp,
                            color = TextSecondaryMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = request.upiId,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SlateCardBg,
                                border = BorderStroke(1.dp, SlateMutedBorder),
                                modifier = Modifier
                                    .clickable { onCopyUpi(request.upiId) }
                                    .testTag("copy_upi_${request.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy UPI",
                                        tint = FireOrange,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Copy", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = FireOrange)
                                }
                            }
                        }
                        if (request.accountHolderName.isNotBlank()) {
                            Text(
                                text = "A/C Holder: ${request.accountHolderName}",
                                fontSize = 11.sp,
                                color = TextSecondaryMuted
                            )
                        }
                    }

                    Text(
                        text = "₹${request.amount}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Player Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(HighContrastDark, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = TrophyGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${request.userName} ${if (request.userFfIgn.isNotBlank()) "(${request.userFfIgn})" else ""}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = buildString {
                                if (request.userFfUid.isNotBlank()) append("FF UID: ${request.userFfUid} • ")
                                append(request.userEmail)
                            },
                            fontSize = 10.sp,
                            color = TextSecondaryMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (request.adminRefId.isNotBlank() || request.adminNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = SlateMutedBorder)
                Spacer(modifier = Modifier.height(6.dp))
                if (request.adminRefId.isNotBlank()) {
                    Text(
                        text = "UTR / Ref ID: ${request.adminRefId}",
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

            // Action Buttons for PENDING requests
            if (request.status.equals("PENDING", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRejectClick,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, LiveRed),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LiveRed),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("reject_wdr_${request.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reject & Refund", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onApproveClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("approve_wdr_${request.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Accept & Pay", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
