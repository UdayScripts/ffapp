package com.example.ui.screens.player

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firestore.SupportMessageDto
import com.example.ui.components.ChatImageHelper
import com.example.ui.theme.FireOrange
import com.example.ui.theme.FireOrangeDark
import com.example.ui.theme.HighContrastDark
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
fun SupportChatScreen(viewModel: AppViewModel) {
    val messages by viewModel.userSupportMessages.collectAsStateWithLifecycle()
    val thread by viewModel.userSupportThread.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)

    var messageInput by remember { mutableStateOf("") }
    var attachedImageBase64 by remember { mutableStateOf<String?>(null) }
    var isEncodingPhoto by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var fullScreenImageBase64 by remember { mutableStateOf<String?>(null) }

    BackHandler {
        viewModel.navigateBack()
    }

    // Zero-permission Android Photo Picker
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
                        err.message ?: "Could not attach selected photo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Mark unread messages as read and scroll to bottom when new messages arrive or keyboard opens
    LaunchedEffect(messages.size, imeBottomPx > 0) {
        viewModel.markCurrentUserSupportRead()
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size) // +1 for welcome header item
        }
    }

    val canSend = !isSending && !isEncodingPhoto &&
            (messageInput.isNotBlank() || !attachedImageBase64.isNullOrBlank())
    val sendScale by animateFloatAsState(
        targetValue = if (canSend) 1.0f else 0.92f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "send_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateLightBg)
    ) {
        // Pinned Top Bar (Always visible below status bar, never panned off-screen)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SlateCardBg,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("support_chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimaryDark
                        )
                    }

                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    Brush.linearGradient(listOf(FireOrangeDark, FireOrange)),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(SlateCardBg, CircleShape)
                                .padding(2.dp)
                                .background(SuccessGreen, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Admin Support Desk",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Support",
                                tint = FireOrange,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Online • Instant Screenshot & Ticket Help",
                            fontSize = 11.sp,
                            color = SuccessGreen,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val statusLabel = thread?.status ?: "OPEN"
                Surface(
                    color = if (statusLabel == "RESOLVED") SuccessGreen.copy(alpha = 0.14f) else FireOrange.copy(alpha = 0.14f),
                    border = BorderStroke(
                        1.dp,
                        if (statusLabel == "RESOLVED") SuccessGreen.copy(alpha = 0.4f) else FireOrange.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (statusLabel == "RESOLVED") "✓ RESOLVED" else "● ACTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (statusLabel == "RESOLVED") SuccessGreen else FireOrangeDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Scrollable Message List filling exact space between Top Bar and Bottom Composer
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(SlateLightBg),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Encrypted Support Banner Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = HighContrastDark,
                    border = BorderStroke(1.dp, FireOrange.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(FireOrange.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = FireOrange,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Official Priority Support Desk",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }

                            Surface(
                                color = SuccessGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "24/7 LIVE",
                                    color = SuccessGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Hi ${currentUser?.username ?: "Player"}! Messages and screenshots sent here go directly to the tournament organizers. Tap the photo icon below to attach payment receipts or match results.",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                SupportChatBubble(
                    message = msg,
                    isOwnMessage = !msg.isAdminSender,
                    onImageClick = { imgBase64 ->
                        fullScreenImageBase64 = imgBase64
                    }
                )
            }
        }

        // Bottom Composer Bar - Uses union(navigationBars, ime) so there is zero gap above the keyboard
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SlateCardBg,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, SlateMutedBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            ) {
                // Quick Topic Chips
                val quickPrompts = listOf(
                    "💸 Withdrawal Delay",
                    "💳 Deposit Screenshot",
                    "🔑 Room ID & Pass",
                    "🏆 Match Prize Help",
                    "🎁 Sign-Up Bonus"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickPrompts.forEach { chip ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SlateSubtle,
                            border = BorderStroke(1.dp, SlateMutedBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    messageInput = "Hi Admin Support, I need assistance with ${chip.substringAfter(" ")}: "
                                }
                        ) {
                            Text(
                                text = chip,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryDark,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Attached Photo Preview Bar with smooth animation
                AnimatedVisibility(
                    visible = attachedImageBase64 != null || isEncodingPhoto,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = SlateSubtle,
                        border = BorderStroke(1.dp, FireOrange.copy(alpha = 0.6f))
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
                                        modifier = Modifier.size(36.dp),
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
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { fullScreenImageBase64 = attachedImageBase64 }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isEncodingPhoto) "Optimizing screenshot..." else "Screenshot Ready to Send",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                    Text(
                                        text = "Tap image to preview or add a caption below",
                                        fontSize = 10.sp,
                                        color = TextSecondaryMuted
                                    )
                                }
                            }

                            IconButton(
                                onClick = { attachedImageBase64 = null },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("remove_attached_photo_button")
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

                // Redesigned Floating Pill Input Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attach Photo Button
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !isEncodingPhoto && !isSending) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("attach_photo_button"),
                        color = FireOrange.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, FireOrange.copy(alpha = 0.45f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Attach Photo",
                                tint = FireOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = {
                            Text(
                                text = "Write a message or attach screenshot...",
                                fontSize = 13.sp,
                                color = TextSecondaryMuted
                            )
                        },
                        maxLines = 4,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FireOrange,
                            unfocusedBorderColor = SlateMutedBorder,
                            focusedContainerColor = SlateSubtle,
                            unfocusedContainerColor = SlateSubtle
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("support_message_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .scale(sendScale)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = canSend) {
                                val textToSend = messageInput.trim()
                                val imgToSend = attachedImageBase64
                                isSending = true
                                viewModel.sendUserSupportMessage(
                                    messageText = textToSend,
                                    imageBase64 = imgToSend
                                ) { success, _ ->
                                    isSending = false
                                    if (success) {
                                        messageInput = ""
                                        attachedImageBase64 = null
                                    }
                                }
                            }
                            .testTag("send_support_message_button"),
                        color = if (canSend) FireOrange else SlateSubtle,
                        border = BorderStroke(1.dp, if (canSend) FireOrangeDark else SlateMutedBorder)
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
                                    contentDescription = "Send Message",
                                    tint = if (canSend) Color.White else TextSecondaryMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
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

@Composable
fun SupportChatBubble(
    message: SupportMessageDto,
    isOwnMessage: Boolean,
    onImageClick: (String) -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a · MMM d", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) {
        timeFormat.format(Date(message.timestamp))
    }

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val isAdmin = message.isAdminSender

    AnimatedVisibility(
        visible = appeared,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                scaleIn(initialScale = 0.94f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            // Left Avatar for Incoming Message
            if (!isOwnMessage) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (isAdmin) {
                                Brush.linearGradient(listOf(FireOrangeDark, FireOrange))
                            } else {
                                Brush.linearGradient(listOf(HighContrastDark, Color(0xFF334155)))
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAdmin) Icons.Default.SupportAgent else Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Redesigned Modern Chat Bubble
            Surface(
                modifier = Modifier.widthIn(min = 120.dp, max = 290.dp),
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isOwnMessage) 18.dp else 4.dp,
                    bottomEnd = if (isOwnMessage) 4.dp else 18.dp
                ),
                color = if (isOwnMessage) HighContrastDark else SlateCardBg,
                border = BorderStroke(
                    width = 1.dp,
                    color = when {
                        isAdmin -> FireOrange.copy(alpha = 0.65f)
                        isOwnMessage -> Color(0xFF334155)
                        else -> SlateMutedBorder
                    }
                ),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    // Sender Header with Role Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isAdmin) "Admin Support" else message.senderName.ifBlank { "Player" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isAdmin) FireOrange else if (isOwnMessage) Color(0xFF38BDF8) else TextPrimaryDark
                            )
                            if (isAdmin) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Official Admin",
                                    tint = TrophyGold,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            color = if (isAdmin) FireOrange.copy(alpha = 0.2f) else SlateSubtle.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (isAdmin) "OFFICIAL" else if (isOwnMessage) "YOU" else "PLAYER",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isAdmin) FireOrange else if (isOwnMessage) Color(0xFFCBD5E1) else TextSecondaryMuted,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // Attached Image if present
                    if (!message.imageBase64.isNullOrBlank()) {
                        val bitmap = remember(message.imageBase64) {
                            ChatImageHelper.decodeBase64ToImageBitmap(message.imageBase64)
                        }
                        if (bitmap != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 130.dp, max = 230.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onImageClick(message.imageBase64) }
                            ) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "Chat photo attachment",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Surface(
                                    color = Color.Black.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ZoomIn,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Full View",
                                            fontSize = 9.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Message Text if present
                    if (message.messageText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = message.messageText,
                            fontSize = 13.sp,
                            color = if (isOwnMessage) Color.White else TextPrimaryDark,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    // Timestamp & Delivery Checkmark Footer
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = formattedTime,
                            fontSize = 9.sp,
                            color = if (isOwnMessage) Color(0xFF94A3B8) else TextSecondaryMuted
                        )
                        if (isOwnMessage) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Delivered",
                                tint = SuccessGreen,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // Right Avatar for Outgoing Message
            if (isOwnMessage) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            if (isAdmin) {
                                Brush.linearGradient(listOf(FireOrangeDark, FireOrange))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF38BDF8)))
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (message.senderName.take(1).ifBlank { "Y" }).uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun FullScreenChatPhotoDialog(
    imageBase64: String,
    onDismiss: () -> Unit
) {
    val bitmap = remember(imageBase64) {
        ChatImageHelper.decodeBase64ToImageBitmap(imageBase64)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Full screen photo preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close preview",
                    tint = Color.White
                )
            }
        }
    }
}
