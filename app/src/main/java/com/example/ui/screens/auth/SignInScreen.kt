package com.example.ui.screens.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.AuthManager
import com.example.auth.AuthenticatedSession
import com.example.ui.theme.FireOrange
import com.example.ui.theme.FireOrangeDark
import com.example.ui.theme.HighContrastDark
import com.example.ui.theme.SlateCardBg
import com.example.ui.theme.SlateLightBg
import com.example.ui.theme.SlateMutedBorder
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
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

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        var resetInput by remember { mutableStateOf(loginUsernameOrEmail) }
        var isSendingReset by remember { mutableStateOf(false) }
        var resetError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isSendingReset) showForgotPasswordDialog = false },
            title = {
                Text(
                    text = "Reset Password",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter your username or email address. We will send password reset instructions to your registered email.",
                        fontSize = 13.sp,
                        color = TextSecondaryMuted
                    )
                    OutlinedTextField(
                        value = resetInput,
                        onValueChange = {
                            resetInput = it
                            resetError = null
                        },
                        label = { Text("Username or Email") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_password_input")
                    )
                    if (resetError != null) {
                        Text(
                            text = resetError ?: "",
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp
                        )
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
                    colors = ButtonDefaults.buttonColors(containerColor = FireOrange)
                ) {
                    if (isSendingReset) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Send Reset Link")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showForgotPasswordDialog = false },
                    enabled = !isSendingReset
                ) {
                    Text("Cancel", color = TextSecondaryMuted)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateLightBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // App Emblem / Custom eSports Arena Logo
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "eSports Arena Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(22.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "OFFICIAL TOURNAMENT PLATFORM",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                color = FireOrangeDark
            )

            Text(
                text = "eSports Arena",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimaryDark
            )

            Text(
                text = "Join Free Fire custom room matches, verify your gameplay, and withdraw match winnings instantly.",
                fontSize = 12.sp,
                color = TextSecondaryMuted,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main Auth Card with TabRow
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                border = BorderStroke(1.dp, SlateMutedBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Navigation Tabs: Sign In / Register
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = SlateCardBg,
                        contentColor = FireOrange,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = FireOrange,
                                height = 3.dp
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                errorMessage = null
                            },
                            text = {
                                Text(
                                    text = "Sign In",
                                    fontSize = 15.sp,
                                    fontWeight = if (selectedTab == 0) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (selectedTab == 0) FireOrange else TextSecondaryMuted
                                )
                            },
                            modifier = Modifier.testTag("tab_sign_in")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                errorMessage = null
                            },
                            text = {
                                Text(
                                    text = "Register",
                                    fontSize = 15.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (selectedTab == 1) FireOrange else TextSecondaryMuted
                                )
                            },
                            modifier = Modifier.testTag("tab_register")
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Error display if any
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
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFF991B1B),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (selectedTab == 0) {
                        // =================== SIGN IN FORM ===================
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = loginUsernameOrEmail,
                                onValueChange = {
                                    loginUsernameOrEmail = it
                                    errorMessage = null
                                },
                                label = { Text("Username or Email") },
                                placeholder = { Text("e.g. player_one or user@email.com") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = FireOrange
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FireOrange,
                                    unfocusedBorderColor = SlateMutedBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_username_input")
                            )

                            OutlinedTextField(
                                value = loginPassword,
                                onValueChange = {
                                    loginPassword = it
                                    errorMessage = null
                                },
                                label = { Text("Password") },
                                placeholder = { Text("Enter your password") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = FireOrange
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { isLoginPasswordVisible = !isLoginPasswordVisible },
                                        modifier = Modifier.testTag("toggle_login_password_visibility")
                                    ) {
                                        Icon(
                                            imageVector = if (isLoginPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle password visibility",
                                            tint = TextSecondaryMuted
                                        )
                                    }
                                },
                                visualTransformation = if (isLoginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FireOrange,
                                    unfocusedBorderColor = SlateMutedBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_password_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { showForgotPasswordDialog = true },
                                    modifier = Modifier.testTag("forgot_password_button")
                                ) {
                                    Text(
                                        text = "Forgot Password?",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = FireOrange
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    val id = loginUsernameOrEmail.trim()
                                    val pass = loginPassword
                                    if (id.isBlank()) {
                                        errorMessage = "Please enter your username or email address."
                                        return@Button
                                    }
                                    if (pass.isBlank()) {
                                        errorMessage = "Please enter your password."
                                        return@Button
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
                                },
                                enabled = !isLoading,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FireOrange,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("signin_submit_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Signing in...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("Sign In", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    } else {
                        // =================== REGISTER FORM ===================
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = regUsername,
                                onValueChange = {
                                    regUsername = it
                                    errorMessage = null
                                },
                                label = { Text("Username *") },
                                placeholder = { Text("e.g. shadow_ninja") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = FireOrange
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FireOrange,
                                    unfocusedBorderColor = SlateMutedBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_username_input")
                            )

                            OutlinedTextField(
                                value = regEmail,
                                onValueChange = {
                                    regEmail = it
                                    errorMessage = null
                                },
                                label = { Text("Email (Optional)") },
                                placeholder = { Text("Optional recovery email") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = FireOrange
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FireOrange,
                                    unfocusedBorderColor = SlateMutedBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_email_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = regFfIgn,
                                    onValueChange = { regFfIgn = it },
                                    label = { Text("Free Fire IGN") },
                                    placeholder = { Text("In-game name") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.SportsEsports,
                                            contentDescription = null,
                                            tint = FireOrange
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = FireOrange,
                                        unfocusedBorderColor = SlateMutedBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("register_ign_input")
                                )

                                OutlinedTextField(
                                    value = regFfUid,
                                    onValueChange = { regFfUid = it },
                                    label = { Text("FF UID") },
                                    placeholder = { Text("Game UID") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Badge,
                                            contentDescription = null,
                                            tint = FireOrange
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = FireOrange,
                                        unfocusedBorderColor = SlateMutedBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("register_uid_input")
                                )
                            }

                            OutlinedTextField(
                                value = regPassword,
                                onValueChange = {
                                    regPassword = it
                                    errorMessage = null
                                },
                                label = { Text("Password *") },
                                placeholder = { Text("At least 6 characters") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = FireOrange
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { isRegPasswordVisible = !isRegPasswordVisible },
                                        modifier = Modifier.testTag("toggle_reg_password_visibility")
                                    ) {
                                        Icon(
                                            imageVector = if (isRegPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle password visibility",
                                            tint = TextSecondaryMuted
                                        )
                                    }
                                },
                                visualTransformation = if (isRegPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FireOrange,
                                    unfocusedBorderColor = SlateMutedBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_password_input")
                            )

                            OutlinedTextField(
                                value = regConfirmPassword,
                                onValueChange = {
                                    regConfirmPassword = it
                                    errorMessage = null
                                },
                                label = { Text("Confirm Password *") },
                                placeholder = { Text("Re-enter password") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = FireOrange
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { isRegConfirmPasswordVisible = !isRegConfirmPasswordVisible },
                                        modifier = Modifier.testTag("toggle_reg_confirm_password_visibility")
                                    ) {
                                        Icon(
                                            imageVector = if (isRegConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle password visibility",
                                            tint = TextSecondaryMuted
                                        )
                                    }
                                },
                                visualTransformation = if (isRegConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FireOrange,
                                    unfocusedBorderColor = SlateMutedBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_confirm_password_input")
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
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
                                        return@Button
                                    }
                                    if (pass.length < 6) {
                                        errorMessage = "Password must be at least 6 characters."
                                        return@Button
                                    }
                                    if (pass != confirmPass) {
                                        errorMessage = "Passwords do not match."
                                        return@Button
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
                                },
                                enabled = !isLoading,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FireOrange,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("register_submit_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Creating Account...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("Create Account", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // "OR" Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = SlateMutedBorder
                        )
                        Text(
                            text = "  OR  ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryMuted
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = SlateMutedBorder
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

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
                                    Toast.makeText(context, "Welcome back, ${session.username.ifBlank { "Player" }}!", Toast.LENGTH_SHORT).show()
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
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HighContrastDark,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
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
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Trust badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = TextSecondaryMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Zero-trust secured with Google Cloud Identity",
                    fontSize = 11.sp,
                    color = TextSecondaryMuted
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

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
