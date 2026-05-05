package com.example.quickserve

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

// Color palette
private val PrimaryBlue = Color(0xFF1E88E5)
private val PrimaryDark = Color(0xFF1565C0)
private val LightBlue = Color(0xFFE3F2FD)
private val LightBg = Color(0xFFF5F9FF)
private val Orange = Color(0xFFFF6B35)
private val DarkText = Color(0xFF1A1A2E)
private val GrayText = Color(0xFF6B7280)
private val LightGray = Color(0xFFF0F2F5)
private val White = Color.White
private val BlackText = Color(0xFF000000)

@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showResendDialog by remember { mutableStateOf(false) }

    // Forgot Password Dialog states
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var isResetLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    // Check if user is already logged in and verified
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        } else if (currentUser != null && !currentUser.isEmailVerified) {
            Toast.makeText(
                context,
                "Please verify your email before logging in",
                Toast.LENGTH_LONG
            ).show()
            auth.signOut()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = LightBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(LightBg, White),
                        startY = 0f,
                        endY = 0.6f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                // Logo Section
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryBlue, PrimaryDark)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Logo",
                        tint = White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "QuickServe",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = "Book trusted local services near you",
                    fontSize = 14.sp,
                    color = GrayText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Service Icons Section
                ServiceIconSection()

                Spacer(modifier = Modifier.height(32.dp))

                // Login Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Welcome Back!",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Login to continue to your account",
                            fontSize = 14.sp,
                            color = GrayText,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Email Address", color = GrayText) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Email,
                                    contentDescription = null,
                                    tint = PrimaryBlue
                                )
                            },
                            placeholder = { Text("you@example.com", color = GrayText) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            textStyle = LocalTextStyle.current.copy(color = BlackText),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = LightGray,
                                focusedContainerColor = White,
                                unfocusedContainerColor = White,
                                focusedLabelColor = PrimaryBlue,
                                unfocusedLabelColor = GrayText,
                                focusedTextColor = BlackText,
                                unfocusedTextColor = BlackText
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Password", color = GrayText) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = PrimaryBlue
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showPassword) "Hide password" else "Show password",
                                        tint = GrayText
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            textStyle = LocalTextStyle.current.copy(color = BlackText),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = LightGray,
                                focusedContainerColor = White,
                                unfocusedContainerColor = White,
                                focusedLabelColor = PrimaryBlue,
                                unfocusedLabelColor = GrayText,
                                focusedTextColor = BlackText,
                                unfocusedTextColor = BlackText
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Forgot Password
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Forgot Password?",
                                color = Orange,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    resetEmail = email
                                    showForgotPasswordDialog = true
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Login Button
                        Button(
                            onClick = {
                                when {
                                    email.isBlank() -> {
                                        Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
                                    }
                                    password.isBlank() -> {
                                        Toast.makeText(context, "Please enter your password", Toast.LENGTH_SHORT).show()
                                    }
                                    else -> {
                                        isLoading = true
                                        auth.signInWithEmailAndPassword(email, password)
                                            .addOnCompleteListener { task ->
                                                isLoading = false
                                                if (task.isSuccessful) {
                                                    val user = auth.currentUser
                                                    if (user?.isEmailVerified == true) {
                                                        Toast.makeText(
                                                            context,
                                                            "Welcome ${user.email?.split("@")?.get(0) ?: "back"}!",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        navController.navigate("home") {
                                                            popUpTo("login") { inclusive = true }
                                                        }
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            "Please verify your email address before logging in. Check your inbox for verification link.",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                        auth.signOut()
                                                        showResendDialog = true
                                                    }
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        task.exception?.message ?: "Login failed. Please check your credentials.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryBlue,
                                disabledContainerColor = PrimaryBlue.copy(alpha = 0.6f)
                            ),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Login",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Divider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                thickness = 1.dp,
                                color = LightGray
                            )
                            Text(
                                text = " OR ",
                                fontSize = 12.sp,
                                color = GrayText,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                thickness = 1.dp,
                                color = LightGray
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Sign Up Link
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Don't have an account? ",
                                color = GrayText,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Sign Up",
                                color = Orange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.clickable {
                                    navController.navigate("signup")
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Resend Verification Dialog
    if (showResendDialog) {
        AlertDialog(
            onDismissRequest = { showResendDialog = false },
            title = {
                Text(
                    text = "Email Not Verified",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Orange
                )
            },
            text = {
                Column {
                    Text(
                        text = "Please verify your email address before logging in.",
                        fontSize = 14.sp,
                        color = GrayText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Didn't receive the verification email?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkText
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResendDialog = false
                        auth.currentUser?.sendEmailVerification()
                            ?.addOnSuccessListener {
                                Toast.makeText(
                                    context,
                                    "Verification email resent to $email",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            ?.addOnFailureListener {
                                Toast.makeText(
                                    context,
                                    "Failed to resend: ${it.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = PrimaryBlue
                    )
                ) {
                    Text("Resend Email", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResendDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = GrayText
                    )
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showForgotPasswordDialog = false
                resetEmail = ""
            },
            title = {
                Text(
                    text = "Reset Password",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Orange
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your email address and we'll send you a password reset link.",
                        fontSize = 14.sp,
                        color = GrayText
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email Address", color = GrayText) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryBlue)
                        },
                        placeholder = { Text("you@example.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = LightGray
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "⚠️ Check your inbox (and spam folder) for the reset link. The link will expire in 1 hour.",
                        fontSize = 11.sp,
                        color = GrayText,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            resetEmail.isBlank() -> {
                                Toast.makeText(context, "Please enter your email address", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                isResetLoading = true
                                auth.sendPasswordResetEmail(resetEmail)
                                    .addOnSuccessListener {
                                        isResetLoading = false
                                        showForgotPasswordDialog = false
                                        Toast.makeText(
                                            context,
                                            "✅ Password reset link sent to ${resetEmail}\n\nPlease check your email and follow the instructions to create a new password.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        resetEmail = ""
                                    }
                                    .addOnFailureListener { e ->
                                        isResetLoading = false
                                        Toast.makeText(
                                            context,
                                            "❌ Failed: ${e.message}\n\nPlease check if this email is registered with QuickServe.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            }
                        }
                    },
                    enabled = !isResetLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isResetLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Send Reset Link", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showForgotPasswordDialog = false
                        resetEmail = ""
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = GrayText)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun ServiceIconSection() {
    val services = listOf(
        Triple(Icons.Default.Plumbing, "Plumber", "Fix pipes & leaks"),
        Triple(Icons.Default.ElectricalServices, "Electrician", "Wiring & repairs"),
        Triple(Icons.Default.HealthAndSafety, "Medical", "Health services"),
        Triple(Icons.Default.Handyman, "Mistri", "Home repairs"),
        Triple(Icons.Default.CleaningServices, "Cleaning", "House cleaning"),
        Triple(Icons.Default.LocationOn, "Near You", "Local services")
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            services.take(3).forEach { (icon, title, description) ->
                ServiceCircle(
                    icon = icon,
                    title = title,
                    description = description
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            services.drop(3).forEach { (icon, title, description) ->
                ServiceCircle(
                    icon = icon,
                    title = title,
                    description = description
                )
            }
        }
    }
}

@Composable
fun ServiceCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape),
        color = LightBlue,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = PrimaryBlue,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = DarkText,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
