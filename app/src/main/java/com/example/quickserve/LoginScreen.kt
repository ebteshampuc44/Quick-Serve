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
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.database.FirebaseDatabase

// Color palette - Orange & White Theme
private val PrimaryOrange = Color(0xFFFF6B35)      // Main Orange
private val PrimaryDarkOrange = Color(0xFFE55A2B)  // Darker Orange
private val LightOrange = Color(0xFFFFF0EB)        // Very Light Orange for backgrounds
private val LightBg = Color(0xFFFFF9F5)            // Warm light background
private val DarkText = Color(0xFF1A1A2E)
private val GrayText = Color(0xFF6B7280)
private val LightGray = Color(0xFFF0F2F5)
private val White = Color.White
private val BlackText = Color(0xFF000000)

// Admin email (no verification required)
private val ADMIN_EMAIL = "admin@quickserve.com"
private val ADMIN_PASSWORD = "admin123456"

@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showResendDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Forgot Password Dialog states
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var isResetLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance().reference

    // Function to check if user is admin
    fun isAdminUser(email: String): Boolean {
        return email == ADMIN_EMAIL
    }

    // Check if user is already logged in
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            if (isAdminUser(currentUser.email ?: "")) {
                // Admin - no verification needed
                navController.navigate("adminApplications") {
                    popUpTo("login") { inclusive = true }
                }
            } else if (currentUser.isEmailVerified) {
                // Normal user with verified email
                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
            } else {
                // Normal user but email not verified
                Toast.makeText(
                    context,
                    "Please verify your email before logging in",
                    Toast.LENGTH_LONG
                ).show()
                auth.signOut()
            }
        }
    }

    fun performLogin() {
        when {
            email.isBlank() -> {
                Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
            }
            password.isBlank() -> {
                Toast.makeText(context, "Please enter your password", Toast.LENGTH_SHORT).show()
            }
            else -> {
                isLoading = true
                errorMessage = null

                // Check for admin login first (no verification needed)
                if (email == ADMIN_EMAIL && password == ADMIN_PASSWORD) {
                    // Try to sign in with Firebase first
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                Toast.makeText(
                                    context,
                                    "Welcome Admin!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                navController.navigate("adminApplications") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } else {
                                // If Firebase doesn't have this user, create it
                                auth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { createTask ->
                                        if (createTask.isSuccessful) {
                                            // Admin account created successfully, no need to send verification email
                                            Toast.makeText(
                                                context,
                                                "Admin account created! Welcome!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            navController.navigate("adminApplications") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        } else {
                                            errorMessage = createTask.exception?.message ?: "Admin login failed"
                                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                        }
                                    }
                            }
                        }
                } else {
                    // Normal user login (requires email verification)
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
                                val exception = task.exception
                                errorMessage = when (exception) {
                                    is FirebaseAuthInvalidUserException -> "No account found with this email. Please sign up first."
                                    is FirebaseAuthInvalidCredentialsException -> "Wrong password. Please try again."
                                    else -> exception?.message ?: "Login failed. Please check your credentials."
                                }
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            }
                        }
                }
            }
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

                // Logo Section - Orange Gradient
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryOrange, PrimaryDarkOrange)
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
                    color = PrimaryOrange,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = "Book trusted local services near you",
                    fontSize = 14.sp,
                    color = GrayText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Service Icons Section - Updated with Light Orange background
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

                        // Show error message if any
                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFEBEE)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = errorMessage!!,
                                    fontSize = 12.sp,
                                    color = Color(0xFFF44336),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Email Field - Orange focus color
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Email Address", color = GrayText) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Email,
                                    contentDescription = null,
                                    tint = PrimaryOrange
                                )
                            },
                            placeholder = { Text("you@example.com", color = GrayText) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            isError = errorMessage != null,
                            textStyle = LocalTextStyle.current.copy(color = BlackText),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryOrange,
                                unfocusedBorderColor = LightGray,
                                focusedContainerColor = White,
                                unfocusedContainerColor = White,
                                focusedLabelColor = PrimaryOrange,
                                unfocusedLabelColor = GrayText,
                                focusedTextColor = BlackText,
                                unfocusedTextColor = BlackText
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Password Field - Orange focus color
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Password", color = GrayText) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = PrimaryOrange
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
                            isError = errorMessage != null,
                            textStyle = LocalTextStyle.current.copy(color = BlackText),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryOrange,
                                unfocusedBorderColor = LightGray,
                                focusedContainerColor = White,
                                unfocusedContainerColor = White,
                                focusedLabelColor = PrimaryOrange,
                                unfocusedLabelColor = GrayText,
                                focusedTextColor = BlackText,
                                unfocusedTextColor = BlackText
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Forgot Password - Orange color
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Forgot Password?",
                                color = PrimaryOrange,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    resetEmail = email
                                    showForgotPasswordDialog = true
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Login Button - Orange
                        Button(
                            onClick = { performLogin() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryOrange,
                                disabledContainerColor = PrimaryOrange.copy(alpha = 0.6f)
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

                        // Sign Up Link - Orange
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
                                color = PrimaryOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.clickable {
                                    navController.navigate("signup")
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Admin Login Hint - Orange accent
                        Divider(
                            modifier = Modifier.padding(top = 8.dp),
                            thickness = 0.5.dp,
                            color = LightGray
                        )

                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = PrimaryOrange,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Admin Login: admin@quickserve.com",
                                    fontSize = 11.sp,
                                    color = GrayText
                                )
                            }
                            Text(
                                text = "Password: admin123456 (No verification required)",
                                fontSize = 10.sp,
                                color = GrayText.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Resend Verification Dialog (only for normal users)
    if (showResendDialog) {
        AlertDialog(
            onDismissRequest = { showResendDialog = false },
            title = {
                Text(
                    text = "Email Not Verified",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryOrange
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
                        contentColor = PrimaryOrange
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
                    color = PrimaryOrange
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
                            Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryOrange)
                        },
                        placeholder = { Text("you@example.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryOrange,
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
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
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
        color = LightOrange,
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
                tint = PrimaryOrange,
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