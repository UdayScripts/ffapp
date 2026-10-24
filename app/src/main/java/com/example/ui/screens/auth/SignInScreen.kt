package com.example.ui.screens.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.AuthManager
import com.example.auth.AuthenticatedSession
import com.example.ui.theme.FireOrange
import com.example.ui.theme.FireOrangeDark
import com.example.ui.theme.FireOrangeLight
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
import com.example.ui.theme.TrophyGoldLight
import com.example.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(
    viewModel: AppViewModel,
    onAuthSuccess: (AuthenticatedSession) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val focusManager = LocalFocusManager.current

    // Auth State
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Sign In, 1: Register
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    // Sign In Fields
    var loginUsernameOrEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var isLoginPasswordVisible by remember { mutableStateOf(false) }

    // Register Fields
    var regUsername by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regFfIgn by remember { mutableStateOf("") }
    var regFfUid by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var isRegPasswordVisible by remember { mutableStateOf(false) }
    var isRegConfirmPasswordVisible by remember { mutableStateOf(false) }

    // Silent Auto-Sign-In on screen start
    LaunchedEffect(Unit) {
        AuthManager.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = { /* Wait for user action */ },
            scope = coroutineScope
        )
    }

    // Redesigned Forgot Password Dialog
    if (showForgotPasswordDialog) {
        var resetInput by remember { mutableStateOf(loginUsernameOrEmail) }
        var isSendingReset by remember { mutableStateOf(false) }
        var resetError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isSendingReset) showForgotPasswordDialog = false },
            shape = RoundedCornerShape(22.dp),
            containerColor = SlateCardBg,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(FireOrange.copy(alpha = 0.14f), RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = FireOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Reset Password",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Account Recovery",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FireOrange
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter your registered username or email address. We'll send a password recovery link straight to your inbox.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = TextSecondaryMuted
                    )
                    OutlinedTextField(
                        value = resetInput,
                        onValueChange = {
                            resetInput = it
                            resetError = null
                        },
                        label = { Text("Username or Email") },
                        placeholder = { Text("e.g. player_one or email@domain.com") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = FireOrange
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(13.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FireOrange,
                            unfocusedBorderColor = SlateMutedBorder,
                            focusedContainerColor = SlateSubtle.copy(alpha = 0.45f),
                            unfocusedContainerColor = SlateSubtle.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_password_input")
                    )
                    if (resetError != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = LiveRed,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = resetError ?: "",
                                color = LiveRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetInput.isBlank()) {
                            resetError = "Please enter your username or email."
                            return@Button
                        }
                        isSendingReset = true
                        resetError = null
                        coroutineScope.launch {
                            val res = viewModel.sendPasswordReset(resetInput.trim())
                            isSendingReset = false
                            res.fold(
                                onSuccess = { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    showForgotPasswordDialog = false
                                },
                                onFailure = { err ->
                                    resetError = err.message ?: "Failed to send reset email."
                                }
                            )
                        }
                    },
                    enabled = !isSendingReset,
                    shape = RoundedCornerShape(11.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FireOrange,
                        contentColor = Color.White
                    )
                ) {
                    if (isSendingReset) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Send Recovery Link", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showForgotPasswordDialog = false },
                    enabled = !isSendingReset
                ) {
                    Text("Cancel", color = TextSecondaryMuted, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateLightBg)
    ) {
        // Subtle ambient top backdrop glow
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        FireOrange.copy(alpha = 0.16f),
                        TrophyGold.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.25f),
                    radius = size.width * 0.75f
                ),
                center = Offset(size.width * 0.5f, size.height * 0.25f),
                radius = size.width * 0.75f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // =================== HERO ESPORTS ARENA BANNER ===================
                EsportsHeroBanner(selectedTab = selectedTab)

                Spacer(modifier = Modifier.height(16.dp))

                // =================== MAIN AUTHENTICATION CARD ===================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                    border = BorderStroke(1.dp, SlateMutedBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Custom Tactile Segmented Mode Switcher
                        AuthModeSegmentSwitcher(
                            selectedTab = selectedTab,
                            onSelectTab = { newTab ->
                                selectedTab = newTab
                                errorMessage = null
                            }
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Error Banner if any
                        AnimatedVisibility(
                            visible = errorMessage != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                shape = RoundedCornerShape(13.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color(0xFFFEE2E2), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = errorMessage ?: "",
                                        color = Color(0xFF991B1B),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Smooth Animated Form Content
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    (slideInHorizontally(tween(240)) { width -> width / 4 } + fadeIn(tween(240)))
                                        .togetherWith(slideOutHorizontally(tween(200)) { width -> -width / 4 } + fadeOut(tween(180)))
                                } else {
                                    (slideInHorizontally(tween(240)) { width -> -width / 4 } + fadeIn(tween(240)))
                                        .togetherWith(slideOutHorizontally(tween(200)) { width -> width / 4 } + fadeOut(tween(180)))
                                }.using(SizeTransform(clip = false))
                            },
                            label = "auth_form_transition"
                        ) { tabIndex ->
                            if (tabIndex == 0) {
                                // =================== SIGN IN FORM ===================
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(13.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "Welcome Back, Champion",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TextPrimaryDark
                                        )
                                        Text(
                                            text = "Enter your credentials to access live rooms & wallet",
                                            fontSize = 12.sp,
                                            color = TextSecondaryMuted
                                        )
                                    }

                                    EsportsAuthTextField(
                                        value = loginUsernameOrEmail,
                                        onValueChange = {
                                            loginUsernameOrEmail = it
                                            errorMessage = null
                                        },
                                        label = "Username or Email",
                                        placeholder = "e.g. shadow_ninja or user@email.com",
                                        leadingIcon = Icons.Default.Person,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Email,
                                            imeAction = ImeAction.Next
                                        ),
                                        testTag = "login_username_input"
                                    )

                                    EsportsAuthTextField(
                                        value = loginPassword,
                                        onValueChange = {
                                            loginPassword = it
                                            errorMessage = null
                                        },
                                        label = "Password",
                                        placeholder = "Enter your secret password",
                                        leadingIcon = Icons.Default.Lock,
                                        isPassword = true,
                                        isPasswordVisible = isLoginPasswordVisible,
                                        onTogglePasswordVisibility = {
                                            isLoginPasswordVisible = !isLoginPasswordVisible
                                        },
                                        passwordToggleTestTag = "toggle_login_password_visibility",
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = { focusManager.clearFocus() }
                                        ),
                                        testTag = "login_password_input"
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VerifiedUser,
                                                contentDescription = null,
                                                tint = SuccessGreen,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Encrypted Session",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextSecondaryMuted
                                            )
                                        }

                                        TextButton(
                                            onClick = { showForgotPasswordDialog = true },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.testTag("forgot_password_button")
                                        ) {
                                            Text(
                                                text = "Forgot Password?",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = FireOrange
                                            )
                                        }
                                    }

                                    // High-Impact Gradient Sign In CTA
                                    GradientActionButton(
                                        text = "Sign In to Arena",
                                        loadingText = "Signing in...",
                                        isLoading = isLoading,
                                        testTag = "signin_submit_button",
                                        onClick = {
                                            focusManager.clearFocus()
                                            val id = loginUsernameOrEmail.trim()
                                            val pass = loginPassword
                                            if (id.isBlank()) {
                                                errorMessage = "Please enter your username or email address."
                                                return@GradientActionButton
                                            }
                                            if (pass.isBlank()) {
                                                errorMessage = "Please enter your password."
                                                return@GradientActionButton
                                            }

                                            isLoading = true
                                            errorMessage = null
                                            coroutineScope.launch {
                                                val result = viewModel.loginWithUsernameOrEmail(id, pass)
                                                isLoading = false
                                                result.fold(
                                                    onSuccess = { session ->
                                                        val welcomeText = if (session.isAdmin) {
                                                            "Welcome back, Admin ${session.username}!"
                                                        } else {
                                                            "Welcome back, ${session.username.ifBlank { id }}!"
                                                        }
                                                        Toast.makeText(
                                                            context,
                                                            welcomeText,
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        onAuthSuccess(session)
                                                    },
                                                    onFailure = { err ->
                                                        val msg = err.localizedMessage ?: "Invalid username or password"
                                                        errorMessage = if (msg.contains("password", ignoreCase = true) || msg.contains("credential", ignoreCase = true)) {
                                                            "Invalid username/email or password. Please verify and try again."
                                                        } else if (msg.contains("user-not-found", ignoreCase = true)) {
                                                            "No account found with this username or email. Please register first."
                                                        } else {
                                                            msg
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    )
                                }
                            } else {
                                // =================== REGISTER FORM ===================
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "Create Player Profile",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TextPrimaryDark
                                        )
                                        Text(
                                            text = "Register your Free Fire roster profile for instant payouts",
                                            fontSize = 12.sp,
                                            color = TextSecondaryMuted
                                        )
                                    }

                                    EsportsAuthTextField(
                                        value = regUsername,
                                        onValueChange = {
                                            regUsername = it
                                            errorMessage = null
                                        },
                                        label = "Username *",
                                        placeholder = "e.g. shadow_ninja",
                                        leadingIcon = Icons.Default.Person,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Text,
                                            imeAction = ImeAction.Next
                                        ),
                                        testTag = "register_username_input"
                                    )

                                    EsportsAuthTextField(
                                        value = regEmail,
                                        onValueChange = {
                                            regEmail = it
                                            errorMessage = null
                                        },
                                        label = "Email (Optional)",
                                        placeholder = "Optional recovery email address",
                                        leadingIcon = Icons.Default.Email,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Email,
                                            imeAction = ImeAction.Next
                                        ),
                                        testTag = "register_email_input"
                                    )

                                    // Highlighted Free Fire In-Game Identity Sub-Card
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = FireOrange.copy(alpha = 0.06f)
                                        ),
                                        border = BorderStroke(1.dp, FireOrange.copy(alpha = 0.32f))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
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
                                                            .size(26.dp)
                                                            .background(
                                                                FireOrange.copy(alpha = 0.15f),
                                                                RoundedCornerShape(7.dp)
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.SportsEsports,
                                                            contentDescription = null,
                                                            tint = FireOrange,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = "FREE FIRE ROSTER ID",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black,
                                                        letterSpacing = 0.8.sp,
                                                        color = FireOrangeDark,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = TrophyGold.copy(alpha = 0.22f),
                                                    border = BorderStroke(1.dp, TrophyGold.copy(alpha = 0.4f))
                                                ) {
                                                    Text(
                                                        text = "MATCH VERIFICATION",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = FireOrangeDark,
                                                        maxLines = 1,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }

                                            EsportsAuthTextField(
                                                value = regFfIgn,
                                                onValueChange = { regFfIgn = it },
                                                label = "Free Fire IGN (In-Game Name)",
                                                placeholder = "e.g. IND_SHADOW",
                                                leadingIcon = Icons.Default.SportsEsports,
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Text,
                                                    imeAction = ImeAction.Next
                                                ),
                                                containerColor = SlateCardBg,
                                                unfocusedBorderColor = FireOrange.copy(alpha = 0.28f),
                                                testTag = "register_ign_input"
                                            )

                                            EsportsAuthTextField(
                                                value = regFfUid,
                                                onValueChange = { regFfUid = it },
                                                label = "Free Fire UID (Numeric ID)",
                                                placeholder = "e.g. 2948104921",
                                                leadingIcon = Icons.Default.Badge,
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Number,
                                                    imeAction = ImeAction.Next
                                                ),
                                                containerColor = SlateCardBg,
                                                unfocusedBorderColor = FireOrange.copy(alpha = 0.28f),
                                                testTag = "register_uid_input"
                                            )
                                        }
                                    }

                                    EsportsAuthTextField(
                                        value = regPassword,
                                        onValueChange = {
                                            regPassword = it
                                            errorMessage = null
                                        },
                                        label = "Password *",
                                        placeholder = "At least 6 characters",
                                        leadingIcon = Icons.Default.Lock,
                                        isPassword = true,
                                        isPasswordVisible = isRegPasswordVisible,
                                        onTogglePasswordVisibility = {
                                            isRegPasswordVisible = !isRegPasswordVisible
                                        },
                                        passwordToggleTestTag = "toggle_reg_password_visibility",
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Next
                                        ),
                                        testTag = "register_password_input"
                                    )

                                    EsportsAuthTextField(
                                        value = regConfirmPassword,
                                        onValueChange = {
                                            regConfirmPassword = it
                                            errorMessage = null
                                        },
                                        label = "Confirm Password *",
                                        placeholder = "Re-enter password",
                                        leadingIcon = Icons.Default.Fingerprint,
                                        isPassword = true,
                                        isPasswordVisible = isRegConfirmPasswordVisible,
                                        onTogglePasswordVisibility = {
                                            isRegConfirmPasswordVisible = !isRegConfirmPasswordVisible
                                        },
                                        passwordToggleTestTag = "toggle_reg_confirm_password_visibility",
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = { focusManager.clearFocus() }
                                        ),
                                        testTag = "register_confirm_password_input"
                                    )

                                    // Real-Time Password Strength & Match Indicator
                                    if (regPassword.isNotEmpty() || regConfirmPassword.isNotEmpty()) {
                                        PasswordSecurityStatusRow(
                                            password = regPassword,
                                            confirmPassword = regConfirmPassword
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    GradientActionButton(
                                        text = "Create Account",
                                        loadingText = "Creating Account...",
                                        isLoading = isLoading,
                                        testTag = "register_submit_button",
                                        onClick = {
                                            focusManager.clearFocus()
                                            val username = regUsername.trim()
                                            val email = regEmail.trim()
                                            val pass = regPassword
                                            val confirmPass = regConfirmPassword
                                            val ign = regFfIgn.trim()
                                            val uid = regFfUid.trim()

                                            if (username.length < 3) {
                                                errorMessage = "Username must be at least 3 characters."
                                                return@GradientActionButton
                                            }
                                            if (pass.length < 6) {
                                                errorMessage = "Password must be at least 6 characters."
                                                return@GradientActionButton
                                            }
                                            if (pass != confirmPass) {
                                                errorMessage = "Passwords do not match."
                                                return@GradientActionButton
                                            }

                                            isLoading = true
                                            errorMessage = null
                                            coroutineScope.launch {
                                                val result = viewModel.registerUser(
                                                    username = username,
                                                    email = email,
                                                    password = pass,
                                                    ffIgn = ign,
                                                    ffUid = uid
                                                )
                                                isLoading = false
                                                result.fold(
                                                    onSuccess = { session ->
                                                        Toast.makeText(
                                                            context,
                                                            "Account created! Welcome, ${session.username.ifBlank { username }}!",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        onAuthSuccess(session)
                                                    },
                                                    onFailure = { err ->
                                                        val msg = err.localizedMessage ?: "Registration failed."
                                                        errorMessage = if (msg.contains("email-already-in-use", ignoreCase = true) || msg.contains("already exists", ignoreCase = true)) {
                                                            "An account with this username or email already exists. Please choose another username or sign in."
                                                        } else {
                                                            msg
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // "OR CONTINUE WITH" Divider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = SlateMutedBorder
                            )
                            Text(
                                text = "  OR CONTINUE WITH  ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.1.sp,
                                color = TextSecondaryMuted
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = SlateMutedBorder
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Google Sign-In / Sign-Up Button
                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                AuthManager.onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = { session ->
                                        isLoading = false
                                        Toast.makeText(
                                            context,
                                            "Welcome back, ${session.username.ifBlank { "Player" }}!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onAuthSuccess(session)
                                    },
                                    onAuthError = { err ->
                                        isLoading = false
                                        errorMessage = err
                                    },
                                    scope = coroutineScope,
                                    onAuthCancelled = {
                                        isLoading = false
                                    }
                                )
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HighContrastDark,
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "G",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF4285F4)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (selectedTab == 0) "Sign in with Google" else "Sign up with Google",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Bottom Mode Toggle Prompt
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedTab = if (selectedTab == 0) 1 else 0
                                    errorMessage = null
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (selectedTab == 0) "New to eSports Arena? " else "Already have an account? ",
                                fontSize = 12.sp,
                                color = TextSecondaryMuted
                            )
                            Text(
                                text = if (selectedTab == 0) "Create Account" else "Sign In",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = FireOrange
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Security & Fair-Play Trust Strip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Zero-trust secured with Google Cloud Identity",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondaryMuted
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // UdayScripts Branding Footer
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                    border = BorderStroke(1.dp, FireOrange.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = FireOrange,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Powered by ",
                            fontSize = 11.sp,
                            color = TextSecondaryMuted
                        )
                        Text(
                            text = "UdayScripts",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = FireOrange,
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun EsportsHeroBanner(selectedTab: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    FireOrange.copy(alpha = 0.7f),
                    TrophyGold.copy(alpha = 0.45f),
                    Color(0xFF334155)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF090D16),
                            Color(0xFF171E31),
                            Color(0xFF3B1306)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(900f, 650f)
                    )
                )
        ) {
            // Decorative Esports Arena Canvas Glows & Tactical Lines
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            FireOrange.copy(alpha = 0.28f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.18f),
                        radius = size.width * 0.55f
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.18f),
                    radius = size.width * 0.55f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TrophyGold.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.12f, size.height * 0.85f),
                        radius = size.width * 0.45f
                    ),
                    center = Offset(size.width * 0.12f, size.height * 0.85f),
                    radius = size.width * 0.45f
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Live Arena Top Status Pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, FireOrange.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(SuccessGreen, CircleShape)
                        )
                        Text(
                            text = "OFFICIAL TOURNAMENT PLATFORM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.4.sp,
                            color = TrophyGoldLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Emblem & Headline Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Double-Ringed Glowing App Logo
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(78.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(FireOrange, TrophyGold)
                                ),
                                shape = RoundedCornerShape(22.dp)
                            )
                            .padding(2.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(20.dp))
                            .padding(2.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "eSports Arena Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(18.dp))
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "eSports Arena",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = (-0.3).sp
                            )
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = TrophyGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = if (selectedTab == 0) {
                                "Join Free Fire custom rooms, track live standings & withdraw winnings instantly."
                            } else {
                                "Link your Free Fire IGN & UID to compete in verified daily prize-pool matches."
                            },
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3 Feature Highlights Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroFeatureChip(
                        icon = Icons.Default.Bolt,
                        label = "Instant Payout",
                        tint = TrophyGold,
                        modifier = Modifier.weight(1f)
                    )
                    HeroFeatureChip(
                        icon = Icons.Default.SportsEsports,
                        label = "Custom Rooms",
                        tint = FireOrangeLight,
                        modifier = Modifier.weight(1f)
                    )
                    HeroFeatureChip(
                        icon = Icons.Default.Shield,
                        label = "Anti-Cheat",
                        tint = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroFeatureChip(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF1F5F9),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AuthModeSegmentSwitcher(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlateSubtle, RoundedCornerShape(16.dp))
            .border(1.dp, SlateMutedBorder, RoundedCornerShape(16.dp))
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Sign In Pill Tab
            val isSignInSelected = selectedTab == 0
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = if (isSignInSelected) {
                            Brush.horizontalGradient(listOf(FireOrange, FireOrangeDark))
                        } else {
                            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                    )
                    .clickable { onSelectTab(0) }
                    .testTag("tab_sign_in"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Login,
                        contentDescription = null,
                        tint = if (isSignInSelected) Color.White else TextSecondaryMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Sign In",
                        fontSize = 14.sp,
                        fontWeight = if (isSignInSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isSignInSelected) Color.White else TextSecondaryMuted
                    )
                }
            }

            // Register Pill Tab
            val isRegisterSelected = selectedTab == 1
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = if (isRegisterSelected) {
                            Brush.horizontalGradient(listOf(FireOrange, FireOrangeDark))
                        } else {
                            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                    )
                    .clickable { onSelectTab(1) }
                    .testTag("tab_register"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = if (isRegisterSelected) Color.White else TextSecondaryMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Register",
                        fontSize = 14.sp,
                        fontWeight = if (isRegisterSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isRegisterSelected) Color.White else TextSecondaryMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun EsportsAuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    testTag: String,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    passwordToggleTestTag: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    containerColor: Color? = null,
    unfocusedBorderColor: Color = SlateMutedBorder
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        placeholder = {
            Text(
                text = placeholder,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(FireOrange.copy(alpha = 0.12f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = FireOrange,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        trailingIcon = if (isPassword && onTogglePasswordVisibility != null) {
            {
                IconButton(
                    onClick = onTogglePasswordVisibility,
                    modifier = if (passwordToggleTestTag != null) {
                        Modifier.testTag(passwordToggleTestTag)
                    } else {
                        Modifier
                    }
                ) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle password visibility",
                        tint = TextSecondaryMuted
                    )
                }
            }
        } else {
            null
        },
        visualTransformation = if (isPassword && !isPasswordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FireOrange,
            unfocusedBorderColor = unfocusedBorderColor,
            focusedContainerColor = containerColor ?: SlateSubtle.copy(alpha = 0.45f),
            unfocusedContainerColor = containerColor ?: SlateSubtle.copy(alpha = 0.25f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

@Composable
private fun PasswordSecurityStatusRow(
    password: String,
    confirmPassword: String
) {
    val isLengthValid = password.length >= 6
    val isStrong = password.length >= 8 && password.any { it.isDigit() }
    val passwordsMatch = password.isNotEmpty() && password == confirmPassword

    val strengthLabel = when {
        password.isEmpty() -> "Enter password"
        !isLengthValid -> "Too short (min 6 chars)"
        isStrong -> "Strong password"
        else -> "Good password"
    }
    val strengthColor = when {
        !isLengthValid -> LiveRed
        isStrong -> SuccessGreen
        else -> TrophyGold
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlateSubtle, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(strengthColor, CircleShape)
            )
            Text(
                text = strengthLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = strengthColor
            )
        }

        if (confirmPassword.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (passwordsMatch) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = if (passwordsMatch) SuccessGreen else LiveRed,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = if (passwordsMatch) "Matched" else "No match",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (passwordsMatch) SuccessGreen else LiveRed
                )
            }
        }
    }
}

@Composable
private fun GradientActionButton(
    text: String,
    loadingText: String,
    isLoading: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = FireOrange.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(FireOrange, FireOrangeDark)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = loadingText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.3.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
