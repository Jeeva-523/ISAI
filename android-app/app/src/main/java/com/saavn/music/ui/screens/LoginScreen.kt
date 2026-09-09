package com.saavn.music.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saavn.music.data.auth.AuthService
import com.saavn.music.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AndroidAuthMode {
    SIGN_IN,
    REGISTER,
    FORGOT_PASSWORD,
    VERIFY_EMAIL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit = {},
    onLoginSuccess: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val authService = remember { AuthService.getInstance(context) }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var authMode by remember {
        mutableStateOf(
            if (authService.isUserLoggedIn() && !authService.isEmailVerified()) {
                AndroidAuthMode.VERIFY_EMAIL
            } else {
                AndroidAuthMode.SIGN_IN
            }
        )
    }

    var displayNameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf(authService.getCurrentUser()?.email ?: "") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var resendCooldown by remember { mutableIntStateOf(0) }

    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000L)
            resendCooldown -= 1
        }
    }

    val primaryGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4))
    )

    Scaffold(
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF140D26),
                            DarkBackground
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Subtle ambient lighting elements
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-80).dp, y = (-80).dp)
                    .background(Color(0xFF8B5CF6).copy(alpha = 0.12f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 80.dp, y = 80.dp)
                    .background(Color(0xFF06B6D4).copy(alpha = 0.10f), CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 1. ISAI Music Brand Header
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(primaryGradient)
                        .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "ISAI Logo",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ISAI ",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Music",
                        color = Color(0xFF06B6D4),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                }

                Text(
                    text = when (authMode) {
                        AndroidAuthMode.SIGN_IN -> "Welcome back! Ready for unlimited music?"
                        AndroidAuthMode.REGISTER -> "Create your account to sync favorites & playlists"
                        AndroidAuthMode.FORGOT_PASSWORD -> "Enter your email to receive a password reset link"
                        AndroidAuthMode.VERIFY_EMAIL -> "Verify your email address to continue"
                    },
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                // Main Glassmorphism Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkSurfaceGlass
                    ),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Alerts
                        if (statusMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = statusMessage ?: "",
                                        color = Color(0xFF34D399),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        if (errorMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error",
                                        tint = Color(0xFFF87171),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = errorMessage ?: "",
                                        color = Color(0xFFF87171),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        if (authMode == AndroidAuthMode.SIGN_IN || authMode == AndroidAuthMode.REGISTER) {
                            // Segmented Switcher
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Color.White.copy(alpha = 0.05f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (authMode == AndroidAuthMode.SIGN_IN) primaryGradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                        )
                                        .clickable {
                                            authMode = AndroidAuthMode.SIGN_IN
                                            errorMessage = null
                                            statusMessage = null
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Sign In",
                                        color = if (authMode == AndroidAuthMode.SIGN_IN) Color.White else TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (authMode == AndroidAuthMode.REGISTER) primaryGradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                        )
                                        .clickable {
                                            authMode = AndroidAuthMode.REGISTER
                                            errorMessage = null
                                            statusMessage = null
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Create Account",
                                        color = if (authMode == AndroidAuthMode.REGISTER) Color.White else TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                        }

                        // Forms
                        when (authMode) {
                            AndroidAuthMode.SIGN_IN -> {
                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text("Email Address") },
                                    leadingIcon = {
                                        Icon(Icons.Default.MailOutline, contentDescription = null, tint = TextMuted)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF06B6D4),
                                        unfocusedBorderColor = DarkBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = Color(0xFF06B6D4),
                                        unfocusedLabelColor = TextSecondary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it },
                                    label = { Text("Password") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                            Icon(
                                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null,
                                                tint = TextMuted
                                            )
                                        }
                                    },
                                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF06B6D4),
                                        unfocusedBorderColor = DarkBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = Color(0xFF06B6D4),
                                        unfocusedLabelColor = TextSecondary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "Forgot password?",
                                        color = Color(0xFF06B6D4),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            authMode = AndroidAuthMode.FORGOT_PASSWORD
                                            errorMessage = null
                                            statusMessage = null
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        if (emailInput.isBlank() || passwordInput.isBlank()) {
                                            errorMessage = "Please enter both email and password."
                                            return@Button
                                        }
                                        isLoading = true
                                        errorMessage = null
                                        statusMessage = null
                                        scope.launch {
                                            val res = authService.loginUser(emailInput, passwordInput)
                                            isLoading = false
                                            res.onSuccess { user ->
                                                if (user.isEmailVerified) {
                                                    onLoginSuccess(user.displayName ?: emailInput.substringBefore("@"), user.email ?: emailInput)
                                                } else {
                                                    authMode = AndroidAuthMode.VERIFY_EMAIL
                                                    errorMessage = "Email not verified. Please check your inbox."
                                                }
                                            }.onFailure { err ->
                                                errorMessage = err.message ?: "Sign in failed."
                                            }
                                        }
                                    },
                                    enabled = !isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(primaryGradient),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                text = "Sign In",
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            AndroidAuthMode.REGISTER -> {
                                OutlinedTextField(
                                    value = displayNameInput,
                                    onValueChange = { displayNameInput = it },
                                    label = { Text("Your Name") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = FocusDirection.Down.let { ImeAction.Next }),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF06B6D4),
                                        unfocusedBorderColor = DarkBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text("Email Address") },
                                    leadingIcon = {
                                        Icon(Icons.Default.MailOutline, contentDescription = null, tint = TextMuted)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF06B6D4),
                                        unfocusedBorderColor = DarkBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it },
                                    label = { Text("Password (min 6 chars)") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                            Icon(
                                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null,
                                                tint = TextMuted
                                            )
                                        }
                                    },
                                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Next
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF06B6D4),
                                        unfocusedBorderColor = DarkBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = confirmPasswordInput,
                                    onValueChange = { confirmPasswordInput = it },
                                    label = { Text("Confirm Password") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                            Icon(
                                                imageVector = if (isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null,
                                                tint = TextMuted
                                            )
                                        }
                                    },
                                    visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF06B6D4),
                                        unfocusedBorderColor = DarkBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                Button(
                                    onClick = {
                                        if (displayNameInput.isBlank() || emailInput.isBlank() || passwordInput.isBlank()) {
                                            errorMessage = "Please fill in all fields."
                                            return@Button
                                        }
                                        if (passwordInput.length < 6) {
                                            errorMessage = "Password must be at least 6 characters."
                                            return@Button
                                        }
                                        if (passwordInput != confirmPasswordInput) {
                                            errorMessage = "Passwords do not match."
                                            return@Button
                                        }
                                        isLoading = true
                                        errorMessage = null
                                        statusMessage = null
                                        scope.launch {
                                            val res = authService.registerUser(displayNameInput, emailInput, passwordInput)
                                            isLoading = false
                                            res.onSuccess {
                                                authMode = AndroidAuthMode.VERIFY_EMAIL
                                                statusMessage = "Verification link sent to $emailInput"
                                                resendCooldown = 30
                                            }.onFailure { err ->
                                                errorMessage = err.message ?: "Registration failed."
                                            }
                                        }
                                    },
                                    enabled = !isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(primaryGradient),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                text = "Create Account",
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            AndroidAuthMode.FORGOT_PASSWORD -> {
                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text("Account Email") },
                                    leadingIcon = {
                                        Icon(Icons.Default.MailOutline, contentDescription = null, tint = TextMuted)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF06B6D4),
                                        unfocusedBorderColor = DarkBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                Button(
                                    onClick = {
                                        if (emailInput.isBlank()) {
                                            errorMessage = "Please enter your email."
                                            return@Button
                                        }
                                        isLoading = true
                                        errorMessage = null
                                        statusMessage = null
                                        scope.launch {
                                            val res = authService.sendPasswordResetEmail(emailInput)
                                            isLoading = false
                                            res.onSuccess { _ ->
                                                statusMessage = "Password reset link sent to $emailInput"
                                            }.onFailure { err ->
                                                errorMessage = err.message ?: "Failed to send reset link."
                                            }
                                        }
                                    },
                                    enabled = !isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(primaryGradient),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                text = "Send Reset Link",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                TextButton(
                                    onClick = {
                                        authMode = AndroidAuthMode.SIGN_IN
                                        errorMessage = null
                                        statusMessage = null
                                    }
                                ) {
                                    Text("← Back to Sign In", color = TextSecondary, fontSize = 13.sp)
                                }
                            }

                            AndroidAuthMode.VERIFY_EMAIL -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF06B6D4).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MailOutline,
                                            contentDescription = null,
                                            tint = Color(0xFF06B6D4),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "Check Your Inbox",
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = "We sent a link to $emailInput. Please click it to verify.",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                                    )

                                    Button(
                                        onClick = {
                                            isLoading = true
                                            scope.launch {
                                                val verified = authService.checkEmailVerified()
                                                isLoading = false
                                                if (verified) {
                                                    onLoginSuccess(
                                                        displayNameInput.ifBlank { "ISAI Listener" },
                                                        emailInput
                                                    )
                                                } else {
                                                    errorMessage = "Email not verified yet. Please click the link and retry."
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Text("I Have Verified My Email", fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedButton(
                                        onClick = {
                                            if (resendCooldown > 0) return@OutlinedButton
                                            isLoading = true
                                            scope.launch {
                                                val res = authService.resendVerificationEmail()
                                                isLoading = false
                                                res.onSuccess {
                                                    statusMessage = "Verification email sent again."
                                                    resendCooldown = 30
                                                }.onFailure { err ->
                                                    errorMessage = err.message ?: "Failed to resend email."
                                                }
                                            }
                                        },
                                        enabled = resendCooldown == 0 && !isLoading,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text(
                                            if (resendCooldown > 0) "Resend in ${resendCooldown}s" else "Resend Verification Email",
                                            fontSize = 12.sp
                                        )
                                    }

                                    TextButton(
                                        onClick = {
                                            authMode = AndroidAuthMode.SIGN_IN
                                            errorMessage = null
                                            statusMessage = null
                                        }
                                    ) {
                                        Text("Back to Sign In", color = TextSecondary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // 8. Continue as Guest Option
                        if (authMode != AndroidAuthMode.VERIFY_EMAIL) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Want to explore first? Continue as Guest →",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable {
                                        onLoginSuccess("Guest Listener", "guest@isaimusic.com")
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer branding
                Text(
                    text = "Protected by Firebase Security • High-Fidelity Audio",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "powered By Jeeva",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
