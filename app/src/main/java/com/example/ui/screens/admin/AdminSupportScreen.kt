package com.example.ui.screens.admin

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firestore.SupportThreadDto
import com.example.ui.components.ChatImageHelper
import com.example.ui.components.HighContrastCard
import com.example.ui.screens.player.FullScreenChatPhotoDialog
import com.example.ui.screens.player.SupportChatBubble
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

@Composable
fun AdminSupportInboxScreen(viewModel: AppViewModel) {
    val allThreads by viewModel.allSupportThreads.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("ALL") }

    BackHandler {
        viewModel.navigateBack()
    }

    val unreadCount = allThreads.count { it.unreadByAdmin > 0 }
    val openCount = allThreads.count { it.status == "OPEN" }
    val resolvedCount = allThreads.count { it.status == "RESOLVED" }

    val filteredThreads = remember(allThreads, selectedFilter) {
        when (selectedFilter) {
            "UNREAD" -> allThreads.filter { it.unreadByAdmin > 0 }
            "OPEN" -> allThreads.filter { it.status == "OPEN" }
            "RESOLVED" -> allThreads.filter { it.status == "RESOLVED" }
            else -> allThreads
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
                        modifier = Modifier.testTag("admin_support_inbox_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimaryDark
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Player Support Inbox",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Real-Time User Chat & Photo Support",
                            fontSize = 11.sp,
                            color = TextSecondaryMuted
                        )
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
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Metrics Header
            item {
                HighContrastCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricItem("TOTAL CHATS", "${allThreads.size}", Icons.Default.Forum, Color.White)
                        MetricItem("UNREAD", "$unreadCount", Icons.Default.MarkChatUnread, LiveRed)
                        MetricItem("OPEN", "$openCount", Icons.Default.SupportAgent, FireOrange)
                        MetricItem("RESOLVED", "$resolvedCount", Icons.Default.CheckCircle, SuccessGreen)
                    }
                }
            }

            // Filter Row
            item {
                val filters = listOf("ALL", "UNREAD", "OPEN", "RESOLVED")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { f ->
                        val selected = selectedFilter == f
                        FilterChip(
                            selected = selected,
                            onClick = { selectedFilter = f },
                            label = {
                                Text(
                                    text = f,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HighContrastDark,
                                selectedLabelColor = FireOrange,
                                containerColor = SlateCardBg,
                                labelColor = TextSecondaryMuted
                            )
                        )
                    }
                }
            }

            if (filteredThreads.isEmpty()) {
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
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = null,
                                tint = FireOrange,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Support Chats Yet",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "When players send messages or screenshots to Support, their threads will appear here live.",
                                fontSize = 12.sp,
                                color = TextSecondaryMuted,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(filteredThreads, key = { it.userId }) { thread ->
                    SupportThreadItemCard(
                        thread = thread,
                        onClick = { viewModel.openAdminSupportThread(thread.userId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportThreadItemCard(
    thread: SupportThreadDto,
    onClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a · MMM d", Locale.getDefault()) }
    val timeStr = remember(thread.lastMessageTimestamp) {
        timeFormat.format(Date(thread.lastMessageTimestamp))
    }

    val hasUnread = thread.unreadByAdmin > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("support_thread_${thread.userId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCardBg),
        border = BorderStroke(
            width = if (hasUnread) 1.5.dp else 1.dp,
            color = if (hasUnread) FireOrange else SlateMutedBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (hasUnread) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if (hasUnread) FireOrange else HighContrastDark,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (thread.userName.take(1).ifBlank { "P" }).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = thread.userName.ifBlank { "Player" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (thread.userFfIgn.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${thread.userFfIgn})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FireOrangeDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        val metaLine = buildString {
                            if (thread.userFfUid.isNotBlank()) append("UID: ${thread.userFfUid}")
                            if (thread.userEmail.isNotBlank()) {
                                if (isNotEmpty()) append(" · ")
                                append(thread.userEmail)
                            }
                        }
                        if (metaLine.isNotBlank()) {
                            Text(
                                text = metaLine,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondaryMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (hasUnread) {
                            Surface(
                                color = LiveRed,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${thread.unreadByAdmin} NEW",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Surface(
                            color = if (thread.status == "RESOLVED") SuccessGreen.copy(alpha = 0.14f) else TrophyGold.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = thread.status,
                                color = if (thread.status == "RESOLVED") SuccessGreen else Color(0xFFB45309),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timeStr,
                        fontSize = 10.sp,
                        color = TextSecondaryMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = if (hasUnread) FireOrange.copy(alpha = 0.14f) else SlateSubtle,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = (if (thread.lastSenderIsAdmin) "Support: " else "") + thread.lastMessage.ifBlank { "No messages" },
                        fontSize = 12.sp,
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
                        color = TextPrimaryDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reply →",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FireOrangeDark
                    )
                }
            }
        }
    }
}

@Composable
fun AdminSupportChatScreen(viewModel: AppViewModel) {
    val targetUserId by viewModel.selectedAdminSupportUserId.collectAsStateWithLifecycle()
    val thread by viewModel.selectedAdminSupportThread.collectAsStateWithLifecycle()
    val messages by viewModel.selectedAdminSupportMessages.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var replyInput by remember { mutableStateOf("") }
    var attachedImageBase64 by remember { mutableStateOf<String?>(null) }
    var isEncodingPhoto by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var fullScreenImageBase64 by remember { mutableStateOf<String?>(null) }

    BackHandler {
        viewModel.navigateBack()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isEncodingPhoto = true
            coroutineScope.launch {
                val res = ChatImageHelper.encodeUriToBase64(context, uri)
                isEncodingPhoto = false
                res.onSuccess { base64 ->
                    attachedImageBase64 = base64
                }.onFailure { err ->
                    Toast.makeText(
                        context,
                        err.message ?: "Failed to process selected image",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    LaunchedEffect(messages.size, targetUserId) {
        val uid = targetUserId
        if (!uid.isNullOrBlank()) {
            viewModel.markAdminSupportRead(uid)
        }
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SlateCardBg,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = { viewModel.navigateBack() },
                            modifier = Modifier.testTag("admin_support_chat_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimaryDark
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = thread?.userName?.ifBlank { "Player Support Chat" } ?: "Player Support Chat",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val subInfo = buildString {
                                if (!thread?.userFfIgn.isNullOrBlank()) append("IGN: ${thread?.userFfIgn}")
                                if (!thread?.userFfUid.isNullOrBlank()) {
                                    if (isNotEmpty()) append(" · ")
                                    append("UID: ${thread?.userFfUid}")
                                }
                            }.ifBlank { thread?.userEmail ?: "" }

                            Text(
                                text = subInfo.ifBlank { "User ID: ${targetUserId ?: ""}" },
                                fontSize = 11.sp,
                                color = TextSecondaryMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val currentStatus = thread?.status ?: "OPEN"
                    val nextStatus = if (currentStatus == "RESOLVED") "OPEN" else "RESOLVED"
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                targetUserId?.let { uid ->
                                    viewModel.updateSupportTicketStatus(uid, nextStatus)
                                }
                            }
                            .testTag("toggle_ticket_status_button"),
                        color = if (currentStatus == "RESOLVED") SuccessGreen.copy(alpha = 0.15f) else HighContrastDark
                    ) {
                        Text(
                            text = if (currentStatus == "RESOLVED") "✓ Resolved (Reopen)" else "Mark Resolved",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentStatus == "RESOLVED") SuccessGreen else FireOrange,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = SlateCardBg,
                shadowElevation = 10.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Quick Canned Replies for Admin Support Team
                    val cannedReplies = listOf(
                        "✅ Issue Resolved!",
                        "💸 Withdrawal Approved & Paid",
                        "🔍 Checking your screenshot",
                        "📸 Please share payment screenshot"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cannedReplies.forEach { reply ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = SlateSubtle,
                                border = BorderStroke(1.dp, SlateMutedBorder),
                                modifier = Modifier.clickable {
                                    replyInput = reply
                                }
                            ) {
                                Text(
                                    text = reply,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Attached Photo Preview Bar
                    if (attachedImageBase64 != null || isEncodingPhoto) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = SlateSubtle,
                            border = BorderStroke(1.dp, FireOrange.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isEncodingPhoto) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(34.dp),
                                            color = FireOrange,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        val previewBitmap = remember(attachedImageBase64) {
                                            ChatImageHelper.decodeBase64ToImageBitmap(attachedImageBase64)
                                        }
                                        if (previewBitmap != null) {
                                            Image(
                                                bitmap = previewBitmap,
                                                contentDescription = "Selected photo preview",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { fullScreenImageBase64 = attachedImageBase64 }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isEncodingPhoto) "Compressing photo..." else "Photo attached to reply",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                }

                                IconButton(
                                    onClick = { attachedImageBase64 = null },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove photo",
                                        tint = TextSecondaryMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Input Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !isEncodingPhoto && !isSending) {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("admin_attach_photo_button"),
                            color = FireOrange.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, FireOrange.copy(alpha = 0.4f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Attach Photo",
                                    tint = FireOrangeDark,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = replyInput,
                            onValueChange = { replyInput = it },
                            placeholder = {
                                Text(
                                    text = "Reply as Admin Support...",
                                    fontSize = 13.sp,
                                    color = TextSecondaryMuted
                                )
                            },
                            maxLines = 4,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FireOrange,
                                unfocusedBorderColor = SlateMutedBorder,
                                focusedContainerColor = SlateSubtle,
                                unfocusedContainerColor = SlateSubtle
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("admin_support_reply_input")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        val uid = targetUserId ?: ""
                        val canSend = uid.isNotBlank() && !isSending && !isEncodingPhoto &&
                                (replyInput.isNotBlank() || !attachedImageBase64.isNullOrBlank())

                        Surface(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = canSend) {
                                    val textToSend = replyInput.trim()
                                    val imgToSend = attachedImageBase64
                                    isSending = true
                                    viewModel.sendAdminSupportMessage(
                                        targetUserId = uid,
                                        messageText = textToSend,
                                        imageBase64 = imgToSend
                                    ) { success, _ ->
                                        isSending = false
                                        if (success) {
                                            replyInput = ""
                                            attachedImageBase64 = null
                                        }
                                    }
                                }
                                .testTag("admin_send_support_reply_button"),
                            color = if (canSend) FireOrange else SlateSubtle
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isSending) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send Reply",
                                        tint = if (canSend) Color.White else Color(0xFF94A3B8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(SlateLightBg)
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                SupportChatBubble(
                    message = msg,
                    isOwnMessage = msg.isAdminSender,
                    onImageClick = { imgBase64 ->
                        fullScreenImageBase64 = imgBase64
                    }
                )
            }
        }
    }

    if (fullScreenImageBase64 != null) {
        FullScreenChatPhotoDialog(
            imageBase64 = fullScreenImageBase64!!,
            onDismiss = { fullScreenImageBase64 = null }
        )
    }
}
