package com.saavn.music.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.saavn.music.data.auth.AuthService
import com.saavn.music.data.model.UserProfile
import com.saavn.music.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AuthMode {
    SIGN_IN,
    REGISTER,
    VERIFY_EMAIL
}

@Composable
fun LoginDialog(
    userProfile: UserProfile?,
    authService: AuthService? = null,
    onGoogleSignInClick: () -> Unit = {},
    onQuickSignIn: (String, String) -> Unit = { _, _ -> },
    onUpdateUsername: (String) -> Unit = {},
    onSignOutClick: () -> Unit,
    onDismissRequest: () -> Unit
) {
    var editingUsername by remember(userProfile) {
        mutableStateOf(userProfile?.displayName ?: "")
    }
    var showEditNameField by remember { mutableStateOf(false) }

    // Auth mode state
    var authMode by remember {
        mutableStateOf(
            if (authService?.isUserLoggedIn() == true && !authService.isEmailVerified()) {
                AuthMode.VERIFY_EMAIL
            } else {
                AuthMode.SIGN_IN
            }
        )
    }

    var displayNameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf(authService?.getCurrentUser()?.email ?: "") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var resendCooldown by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()

    // Resend cooldown timer effect
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000L)
            resendCooldown -= 1
        }
    }

    val isUserFullyVerified = userProfile != null && (authService?.isEmailVerified() != false)

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(NeonCyan.copy(alpha = 0.6f), NeonPurple.copy(alpha = 0.3f))
                    ),
                    RoundedCornerShape(24.dp)
                ),
            color = DarkSurfaceGlass
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NeonPurple.copy(alpha = 0.15f),
                                DarkBackground.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                // Close Icon
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isUserFullyVerified && userProfile != null) {
                        // LOGGED IN & VERIFIED PROFILE VIEW
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .border(2.dp, NeonCyan, CircleShape)
                        ) {
                            if (!userProfile.photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = userProfile.photoUrl,
                                    contentDescription = userProfile.displayName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Avatar",
                                    tint = NeonCyan,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = userProfile.displayName.ifBlank { "ISAI User" },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = userProfile.email,
                            fontSize = 13.sp,
                            color = NeonCyan,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Edit Username Section
                        if (showEditNameField) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = editingUsername,
                                    onValueChange = { editingUsername = it },
                                    label = { Text("Edit Display Name", color = TextMuted) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (editingUsername.isNotBlank()) {
                                            onUpdateUsername(editingUsername.trim())
                                            showEditNameField = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            TextButton(
                                onClick = { showEditNameField = true }
                            ) {
                                Text(
                                    text = "✏️ Edit Profile Name",
                                    color = NeonCyan,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        Spacer(modifier = Modifier.height(16.dp))

                        // Logout Button
                        OutlinedButton(
                            onClick = onSignOutClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFFF5252)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFFF5252)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Sign Out",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sign Out",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                    } else {
                        // UNAUTHENTICATED OR UNVERIFIED FLOW
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.15f))
                                .border(1.dp, NeonCyan.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (authMode == AuthMode.VERIFY_EMAIL) Icons.Default.MailOutline else Icons.Default.Person,
                                contentDescription = "Auth Icon",
                                tint = NeonCyan,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Title & Tabs
                        if (authMode == AuthMode.VERIFY_EMAIL) {
                            Text(
                                text = "Verify Your Email",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "A verification link was sent to ${emailInput.ifBlank { authService?.getCurrentUser()?.email ?: "your email" }}",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                FilterChip(
                                    selected = authMode == AuthMode.SIGN_IN,
                                    onClick = {
                                        authMode = AuthMode.SIGN_IN
                                        errorMessage = null
                                        statusMessage = null
                                    },
                                    label = { Text("Sign In", fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonCyan,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                FilterChip(
                                    selected = authMode == AuthMode.REGISTER,
                                    onClick = {
                                        authMode = AuthMode.REGISTER
                                        errorMessage = null
                                        statusMessage = null
                                    },
                                    label = { Text("Create Account", fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonCyan,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Status Banner
                        statusMessage?.let { msg ->
                            Surface(
                                color = NeonLime.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, NeonLime),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = msg,
                                    color = NeonLime,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(10.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Error Banner
                        errorMessage?.let { err ->
                            Surface(
                                color = Color(0xFFFF5252).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFFF5252)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = "⚠️ $err",
                                    color = Color(0xFFFF5252),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(10.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // MODE 1: SIGN IN
                        if (authMode == AuthMode.SIGN_IN) {
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Email Address", color = TextMuted) },
                                leadingIcon = { Icon(Icons.Default.MailOutline, contentDescription = null, tint = NeonCyan) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = GlassBorderSubtle,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Password", color = TextMuted) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NeonCyan) },
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (isPasswordVisible) "Hide Password" else "Show Password",
                                            tint = TextSecondary
                                        )
                                    }
                                },
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = GlassBorderSubtle,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

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
                                        val res = authService?.loginUser(emailInput, passwordInput)
                                        if (res != null && res.isSuccess) {
                                            val verified = authService.checkEmailVerified()
                                            if (verified) {
                                                val user = res.getOrNull()
                                                onQuickSignIn(user?.displayName ?: "User", emailInput)
                                                onDismissRequest()
                                            } else {
                                                authMode = AuthMode.VERIFY_EMAIL
                                                errorMessage = "Please verify your email before logging in."
                                            }
                                        } else {
                                            errorMessage = res?.exceptionOrNull()?.message ?: "Sign in failed."
                                        }
                                        isLoading = false
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonLime)
                            ) {
                                Text(
                                    text = if (isLoading) "Signing In..." else "Sign In 🔑",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // MODE 2: REGISTER
                        if (authMode == AuthMode.REGISTER) {
                            OutlinedTextField(
                                value = displayNameInput,
                                onValueChange = { displayNameInput = it },
                                label = { Text("Full Name", color = TextMuted) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = NeonCyan) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = GlassBorderSubtle,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Email Address", color = TextMuted) },
                                leadingIcon = { Icon(Icons.Default.MailOutline, contentDescription = null, tint = NeonCyan) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = GlassBorderSubtle,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Password (min 6 chars)", color = TextMuted) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NeonCyan) },
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (isPasswordVisible) "Hide Password" else "Show Password",
                                            tint = TextSecondary
                                        )
                                    }
                                },
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = GlassBorderSubtle,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = confirmPasswordInput,
                                onValueChange = { confirmPasswordInput = it },
                                label = { Text("Confirm Password", color = TextMuted) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NeonCyan) },
                                trailingIcon = {
                                    IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (isConfirmPasswordVisible) "Hide Password" else "Show Password",
                                            tint = TextSecondary
                                        )
                                    }
                                },
                                visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = GlassBorderSubtle,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (displayNameInput.isBlank() || emailInput.isBlank() || passwordInput.isBlank()) {
                                        errorMessage = "Please fill in all required fields."
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
                                        val res = authService?.registerUser(displayNameInput, emailInput, passwordInput)
                                        if (res != null && res.isSuccess) {
                                            authMode = AuthMode.VERIFY_EMAIL
                                            statusMessage = "🎉 Account Created Successfully! A verification link was sent to $emailInput. Please check your inbox / spam folder."
                                        } else {
                                            errorMessage = res?.exceptionOrNull()?.message ?: "Registration failed."
                                        }
                                        isLoading = false
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonLime)
                            ) {
                                Text(
                                    text = if (isLoading) "Creating Account..." else "Create Account ✉️",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // MODE 3: EMAIL VERIFICATION GATE
                        if (authMode == AuthMode.VERIFY_EMAIL) {
                            Button(
                                onClick = {
                                    isLoading = true
                                    errorMessage = null
                                    statusMessage = null

                                    scope.launch {
                                        val isVerified = authService?.checkEmailVerified() ?: false
                                        if (isVerified) {
                                            val u = authService?.getCurrentUser()
                                            onQuickSignIn(u?.displayName ?: displayNameInput.ifBlank { "User" }, u?.email ?: emailInput)
                                            onDismissRequest()
                                        } else {
                                            errorMessage = "Your email is not verified yet. Please check your inbox/spam folder and click the link."
                                        }
                                        isLoading = false
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isLoading) "Checking Verification..." else "I've Verified My Email 🔄",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = {
                                    if (resendCooldown > 0) return@OutlinedButton
                                    isLoading = true
                                    errorMessage = null
                                    statusMessage = null

                                    scope.launch {
                                        val res = authService?.resendVerificationEmail()
                                        if (res != null && res.isSuccess) {
                                            statusMessage = "Verification email sent again."
                                            resendCooldown = 30
                                        } else {
                                            errorMessage = res?.exceptionOrNull()?.message ?: "Failed to resend email."
                                        }
                                        isLoading = false
                                    }
                                },
                                enabled = !isLoading && resendCooldown == 0,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, NeonLime.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonLime)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (resendCooldown > 0) "Resend Email (${resendCooldown}s)" else "Resend Verification Email",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = {
                                    authService?.logout()
                                    authMode = AuthMode.SIGN_IN
                                    errorMessage = null
                                    statusMessage = null
                                }
                            ) {
                                Text("Back to Sign In / Sign Out", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}


