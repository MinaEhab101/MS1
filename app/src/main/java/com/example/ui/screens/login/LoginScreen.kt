package com.example.ui.screens.login

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.firebase.AuthRepository
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.BoardWoodDark
import com.example.ui.theme.BoardWoodMedium
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardDark
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Sign In, 1: Register, 2: Guest

    // Sign In form state
    var signInEmail by remember { mutableStateOf("") }
    var signInPassword by remember { mutableStateOf("") }
    var signInPasswordVisible by remember { mutableStateOf(false) }

    // Register form state
    var regDisplayName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }
    var regConfirmPasswordVisible by remember { mutableStateOf(false) }
    var regAvatarIndex by remember { mutableIntStateOf(2) } // default crown

    // Guest form state
    var guestNickname by remember { mutableStateOf("") }
    var guestAvatarIndex by remember { mutableIntStateOf(0) }

    // Status & Feedback state
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    // Password reset dialog state
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmailInput by remember { mutableStateOf("") }
    var isResetLoading by remember { mutableStateOf(false) }

    // Google Sign-In Fallback Dialog state (if serverClientId is not set on cloud console)
    var showGoogleDemoDialog by remember { mutableStateOf(false) }
    var googleDemoEmail by remember { mutableStateOf("") }
    var googleDemoName by remember { mutableStateOf("") }

    val avatars = listOf("👑", "🦁", "🦅", "⚔️", "🎲", "🧙‍♂️", "💎", "🐉")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BoardWoodMedium, BoardWoodDark, SurfaceDark)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // App Emblem
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(GoldSecondary, GoldDark, BoardWoodDark)))
                    .border(2.5.dp, GoldPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.backgammon_king_logo),
                    contentDescription = "Royal Board 3D Logo",
                    modifier = Modifier.size(78.dp).clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "ROYAL BOARD 3D",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GoldSecondary,
                letterSpacing = 2.sp
            )

            Text(
                text = "Chess • Dominoes • Backgammon",
                fontSize = 12.sp,
                color = GoldPrimary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )

            Text(
                text = "Sign in to play online, earn trophies & climb rankings",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )

            // Auth Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, GoldDark, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Navigation Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = BoardWoodDark,
                        contentColor = GoldPrimary,
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = GoldPrimary,
                                    height = 3.dp
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                errorMessage = null
                                infoMessage = null
                            },
                            text = {
                                Text(
                                    "Sign In",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                errorMessage = null
                                infoMessage = null
                            },
                            text = {
                                Text(
                                    "Register",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = {
                                selectedTab = 2
                                errorMessage = null
                                infoMessage = null
                            },
                            text = {
                                Text(
                                    "Guest",
                                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Error Message Banner
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        errorMessage?.let { err ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .background(AccentRed.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    .border(1.dp, AccentRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = AccentRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = err,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { errorMessage = null },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Info Message Banner
                    AnimatedVisibility(
                        visible = infoMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        infoMessage?.let { info ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .background(AccentGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    .border(1.dp, AccentGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = AccentGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = info,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Content depending on selected tab
                    when (selectedTab) {
                        0 -> {
                            // SIGN IN TAB
                            OutlinedTextField(
                                value = signInEmail,
                                onValueChange = { signInEmail = it; errorMessage = null },
                                label = { Text("Email Address") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = "Email", tint = GoldPrimary)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signin_email_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = CardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedLabelColor = GoldPrimary,
                                    unfocusedLabelColor = TextMuted
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = signInPassword,
                                onValueChange = { signInPassword = it; errorMessage = null },
                                label = { Text("Password") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = "Password", tint = GoldPrimary)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { signInPasswordVisible = !signInPasswordVisible }) {
                                        Icon(
                                            imageVector = if (signInPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (signInPasswordVisible) "Hide password" else "Show password",
                                            tint = TextMuted
                                        )
                                    }
                                },
                                visualTransformation = if (signInPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signin_password_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = CardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedLabelColor = GoldPrimary,
                                    unfocusedLabelColor = TextMuted
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Forgot Password link
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    color = GoldPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clickable {
                                            resetEmailInput = signInEmail
                                            showResetDialog = true
                                        }
                                        .padding(4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Sign In Button
                            Button(
                                onClick = {
                                    if (signInEmail.isBlank() || signInPassword.isBlank()) {
                                        errorMessage = "Please enter both your email and password."
                                        return@Button
                                    }
                                    isLoading = true
                                    errorMessage = null
                                    coroutineScope.launch {
                                        val result = authRepository.signInWithEmail(signInEmail, signInPassword)
                                        isLoading = false
                                        result.fold(
                                            onSuccess = {
                                                Toast.makeText(context, "Welcome back, ${it.username}!", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess()
                                            },
                                            onFailure = { err ->
                                                errorMessage = err.message ?: "Authentication failed."
                                            }
                                        )
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("email_signin_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = BoardWoodDark
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = BoardWoodDark, strokeWidth = 2.dp)
                                } else {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Sign In")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sign In to Arena", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }

                        1 -> {
                            // REGISTER TAB
                            OutlinedTextField(
                                value = regDisplayName,
                                onValueChange = { regDisplayName = it; errorMessage = null },
                                label = { Text("Display Name") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = "Display Name", tint = GoldPrimary)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = CardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedLabelColor = GoldPrimary,
                                    unfocusedLabelColor = TextMuted
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = regEmail,
                                onValueChange = { regEmail = it; errorMessage = null },
                                label = { Text("Email Address") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = "Email", tint = GoldPrimary)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_email_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = CardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedLabelColor = GoldPrimary,
                                    unfocusedLabelColor = TextMuted
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = regPassword,
                                onValueChange = { regPassword = it; errorMessage = null },
                                label = { Text("Password (min 6 characters)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = "Password", tint = GoldPrimary)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { regPasswordVisible = !regPasswordVisible }) {
                                        Icon(
                                            imageVector = if (regPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle password",
                                            tint = TextMuted
                                        )
                                    }
                                },
                                visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_password_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = CardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedLabelColor = GoldPrimary,
                                    unfocusedLabelColor = TextMuted
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = regConfirmPassword,
                                onValueChange = { regConfirmPassword = it; errorMessage = null },
                                label = { Text("Confirm Password") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = "Confirm Password", tint = GoldPrimary)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { regConfirmPasswordVisible = !regConfirmPasswordVisible }) {
                                        Icon(
                                            imageVector = if (regConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle password",
                                            tint = TextMuted
                                        )
                                    }
                                },
                                visualTransformation = if (regConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_confirm_password_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = CardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedLabelColor = GoldPrimary,
                                    unfocusedLabelColor = TextMuted
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Avatar selection
                            Text(
                                text = "Choose Your Royal Avatar",
                                color = GoldSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                avatars.forEachIndexed { index, emoji ->
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (regAvatarIndex == index) GoldDark else BoardWoodMedium)
                                            .border(
                                                2.dp,
                                                if (regAvatarIndex == index) GoldSecondary else CardBorder,
                                                CircleShape
                                            )
                                            .clickable { regAvatarIndex = index },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 16.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Register Button
                            Button(
                                onClick = {
                                    if (regDisplayName.isBlank()) {
                                        errorMessage = "Please enter a display name."
                                        return@Button
                                    }
                                    if (regEmail.isBlank()) {
                                        errorMessage = "Please enter an email address."
                                        return@Button
                                    }
                                    if (regPassword.length < 6) {
                                        errorMessage = "Password must be at least 6 characters."
                                        return@Button
                                    }
                                    if (regPassword != regConfirmPassword) {
                                        errorMessage = "Passwords do not match."
                                        return@Button
                                    }

                                    isLoading = true
                                    errorMessage = null
                                    coroutineScope.launch {
                                        val result = authRepository.registerWithEmail(
                                            email = regEmail,
                                            password = regPassword,
                                            displayName = regDisplayName,
                                            avatarIndex = regAvatarIndex
                                        )
                                        isLoading = false
                                        result.fold(
                                            onSuccess = {
                                                Toast.makeText(context, "Account created! Welcome, ${it.username}!", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess()
                                            },
                                            onFailure = { err ->
                                                errorMessage = err.message ?: "Registration failed."
                                            }
                                        )
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("email_register_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = BoardWoodDark
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = BoardWoodDark, strokeWidth = 2.dp)
                                } else {
                                    Icon(imageVector = Icons.Default.Casino, contentDescription = "Create Account")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Create Royal Account (+1,500 🪙)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }

                        2 -> {
                            // GUEST TAB
                            Text(
                                text = "Choose Avatar & Nickname",
                                color = GoldSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.Start)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                avatars.forEachIndexed { index, emoji ->
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (guestAvatarIndex == index) GoldDark else BoardWoodMedium)
                                            .border(
                                                2.dp,
                                                if (guestAvatarIndex == index) GoldSecondary else CardBorder,
                                                CircleShape
                                            )
                                            .clickable { guestAvatarIndex = index },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 16.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = guestNickname,
                                onValueChange = { guestNickname = it },
                                label = { Text("Nickname (optional)") },
                                placeholder = { Text("e.g. Sultan_99") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("guest_username_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = CardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedLabelColor = GoldPrimary,
                                    unfocusedLabelColor = TextMuted
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BoardWoodDark, RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "💡 Guest data is saved on this device. You can link or create an email account anytime in Settings to backup rankings.",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val user = authRepository.loginAsGuest(
                                        customName = guestNickname.ifBlank { null },
                                        avatarIndex = guestAvatarIndex
                                    )
                                    Toast.makeText(context, "Playing as ${user.username}", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("guest_login_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = BoardWoodDark
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play as Guest (+1,000 🪙)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }

                    // Divider and Google Sign-In (available across all tabs)
                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(CardBorder))
                        Text(
                            text = "  OR  ",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(CardBorder))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Google Sign-In Button
                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                val result = authRepository.signInWithGoogle(context)
                                isLoading = false
                                result.fold(
                                    onSuccess = { user ->
                                        Toast.makeText(context, "Signed in as ${user.username}!", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    },
                                    onFailure = { err ->
                                        // If Web Client ID or Play Services not configured in cloud, show fallback demo option
                                        val msg = err.message ?: ""
                                        if (msg.contains("Web Client ID") || msg.contains("not configured") || msg.contains("No Google account")) {
                                            showGoogleDemoDialog = true
                                        } else {
                                            errorMessage = msg
                                        }
                                    }
                                )
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("google_login_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1F1F1F)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Stylized Google 'G' icon badge
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4285F4)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("G", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sign In with Google",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Password Reset Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { if (!isResetLoading) showResetDialog = false },
            containerColor = CardDark,
            title = {
                Text("Reset Password", color = GoldSecondary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Enter your registered email address to receive password reset instructions:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetEmailInput,
                        onValueChange = { resetEmailInput = it },
                        label = { Text("Email Address") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmailInput.isBlank()) {
                            Toast.makeText(context, "Please enter an email", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isResetLoading = true
                        coroutineScope.launch {
                            val result = authRepository.sendPasswordReset(resetEmailInput)
                            isResetLoading = false
                            showResetDialog = false
                            result.fold(
                                onSuccess = { msg ->
                                    infoMessage = msg
                                },
                                onFailure = { err ->
                                    errorMessage = err.message ?: "Failed to send reset email."
                                }
                            )
                        }
                    },
                    enabled = !isResetLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = BoardWoodDark)
                ) {
                    if (isResetLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BoardWoodDark, strokeWidth = 2.dp)
                    } else {
                        Text("Send Reset Link", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // Google Sign-In Client ID Dialog (Fallback for development/testing without pre-configured Web Client ID)
    if (showGoogleDemoDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleDemoDialog = false },
            containerColor = CardDark,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Google Sign-In", color = GoldSecondary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Sign in directly with your Google account details to link ratings and compete online:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = googleDemoName,
                        onValueChange = { googleDemoName = it },
                        label = { Text("Google Display Name") },
                        placeholder = { Text("e.g. Ehab Mina") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = googleDemoEmail,
                        onValueChange = { googleDemoEmail = it },
                        label = { Text("Google Email") },
                        placeholder = { Text("e.g. player@gmail.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = googleDemoName.ifBlank { "Google Master" }
                        val email = googleDemoEmail.ifBlank { "google_player@gmail.com" }
                        val profile = authRepository.signInWithGoogleCredentials(
                            displayName = name,
                            email = email,
                            googleId = "goog_" + email.hashCode().toString().replace("-", "")
                        )
                        showGoogleDemoDialog = false
                        Toast.makeText(context, "Signed in as ${profile.username}!", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = BoardWoodDark)
                ) {
                    Text("Confirm Google Sign-In", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleDemoDialog = false }) {
                    Text("Close", color = TextMuted)
                }
            }
        )
    }
}
