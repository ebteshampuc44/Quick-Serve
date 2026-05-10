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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.text.TextStyle
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

@Composable
fun SignupScreen(navController: NavController) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var showVerificationDialog by remember { mutableStateOf(false) }
    var createdUserEmail by remember { mutableStateOf("") }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance().reference

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
                Spacer(modifier = Modifier.height(32.dp))

                // Logo Section - Orange Gradient
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryOrange, PrimaryDarkOrange)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Logo",
                        tint = White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "QuickServe",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryOrange,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = "Join and book trusted local services",
                    fontSize = 13.sp,
                    color = GrayText
                )

                Spacer(modifier = Modifier.height(24.dp))

                SignupServiceIcons()

                Spacer(modifier = Modifier.height(28.dp))

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
                            text = "Create Account",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Sign up to get started with QuickServe",
                            fontSize = 13.sp,
                            color = GrayText,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Full Name Field - Orange focus
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Full Name", color = GrayText) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryOrange)
                            },
                            placeholder = { Text("John Doe", color = GrayText) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            textStyle = TextStyle.Default.copy(color = BlackText),
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

                        Spacer(modifier = Modifier.height(14.dp))

                        // Email Field - Orange focus
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Email Address", color = GrayText) },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryOrange)
                            },
                            placeholder = { Text("you@example.com", color = GrayText) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            textStyle = TextStyle.Default.copy(color = BlackText),
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

                        Spacer(modifier = Modifier.height(14.dp))

                        // Phone Field - Orange focus
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Phone Number", color = GrayText) },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryOrange)
                            },
                            placeholder = { Text("+880 1XXX-XXXXXX", color = GrayText) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            textStyle = TextStyle.Default.copy(color = BlackText),
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

                        Spacer(modifier = Modifier.height(14.dp))

                        // Password Field - Orange focus
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Password", color = GrayText) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryOrange)
                            },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = GrayText
                                    )
                                }
                            },
                            visualTransformation = if (!showPassword) PasswordVisualTransformation() else VisualTransformation.None,
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            textStyle = TextStyle.Default.copy(color = BlackText),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryOrange,
                                unfocusedBorderColor = LightGray,
                                focusedContainerColor = White,
                                unfocusedContainerColor = White,
                                focusedLabelColor = PrimaryOrange,
                                focusedTextColor = BlackText,
                                unfocusedTextColor = BlackText
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Confirm Password Field - Orange focus
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Confirm Password", color = GrayText) },
                            leadingIcon = {
                                Icon(Icons.Default.LockReset, contentDescription = null, tint = PrimaryOrange)
                            },
                            trailingIcon = {
                                IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                    Icon(
                                        if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = GrayText
                                    )
                                }
                            },
                            visualTransformation = if (!showConfirmPassword) PasswordVisualTransformation() else VisualTransformation.None,
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            textStyle = TextStyle.Default.copy(color = BlackText),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryOrange,
                                unfocusedBorderColor = LightGray,
                                focusedContainerColor = White,
                                unfocusedContainerColor = White,
                                focusedLabelColor = PrimaryOrange,
                                focusedTextColor = BlackText,
                                unfocusedTextColor = BlackText
                            )
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Terms text - Orange links
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "By signing up, you agree to our ",
                                fontSize = 11.sp,
                                color = GrayText
                            )
                            Text(
                                text = "Terms",
                                fontSize = 11.sp,
                                color = PrimaryOrange,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = " and ",
                                fontSize = 11.sp,
                                color = GrayText
                            )
                            Text(
                                text = "Privacy Policy",
                                fontSize = 11.sp,
                                color = PrimaryOrange,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Sign Up Button - Orange
                        Button(
                            onClick = {
                                when {
                                    fullName.isBlank() -> {
                                        Toast.makeText(context, "Please enter your full name", Toast.LENGTH_SHORT).show()
                                    }
                                    email.isBlank() -> {
                                        Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
                                    }
                                    phone.isBlank() -> {
                                        Toast.makeText(context, "Please enter your phone number", Toast.LENGTH_SHORT).show()
                                    }
                                    password.isBlank() -> {
                                        Toast.makeText(context, "Please create a password", Toast.LENGTH_SHORT).show()
                                    }
                                    password.length < 6 -> {
                                        Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                                    }
                                    password != confirmPassword -> {
                                        Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                                    }
                                    else -> {
                                        isLoading = true

                                        auth.createUserWithEmailAndPassword(email, password)
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    val user = auth.currentUser

                                                    if (user != null) {
                                                        val userRef = database.child("users").child(user.uid)

                                                        val userData = mapOf<String, Any>(
                                                            "userId" to user.uid,
                                                            "fullName" to fullName,
                                                            "email" to email,
                                                            "phone" to phone,
                                                            "createdAt" to System.currentTimeMillis()
                                                        )

                                                        userRef.setValue(userData)
                                                            .addOnSuccessListener {
                                                                user.sendEmailVerification()
                                                                    .addOnCompleteListener { verifyTask ->
                                                                        isLoading = false
                                                                        if (verifyTask.isSuccessful) {
                                                                            createdUserEmail = email
                                                                            showVerificationDialog = true
                                                                            Toast.makeText(
                                                                                context,
                                                                                "Verification email sent to $email",
                                                                                Toast.LENGTH_LONG
                                                                            ).show()
                                                                        } else {
                                                                            Toast.makeText(
                                                                                context,
                                                                                "Failed to send verification email: ${verifyTask.exception?.message}",
                                                                                Toast.LENGTH_LONG
                                                                            ).show()
                                                                        }
                                                                    }
                                                            }
                                                            .addOnFailureListener { e ->
                                                                isLoading = false
                                                                Toast.makeText(
                                                                    context,
                                                                    "Failed to save user data: ${e.message}",
                                                                    Toast.LENGTH_LONG
                                                                ).show()
                                                            }
                                                    } else {
                                                        isLoading = false
                                                        Toast.makeText(context, "User creation failed", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    isLoading = false
                                                    Toast.makeText(
                                                        context,
                                                        task.exception?.message ?: "Sign up failed. Please try again.",
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
                                    Icons.Default.AppRegistration,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Create Account",
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

                        // Login Link - Orange
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Already have an account? ",
                                color = GrayText,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Login",
                                color = PrimaryOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.clickable {
                                    navController.navigate("login")
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Verification Dialog - Orange theme
    if (showVerificationDialog) {
        AlertDialog(
            onDismissRequest = {
                showVerificationDialog = false
                navController.navigate("login") {
                    popUpTo("signup") { inclusive = true }
                }
            },
            title = {
                Text(
                    text = "Verify Your Email",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryOrange
                )
            },
            text = {
                Column {
                    Text(
                        text = "We've sent a verification email to:",
                        fontSize = 14.sp,
                        color = GrayText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = createdUserEmail,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Please check your inbox and click the verification link to activate your account. After verification, you can login to your account.",
                        fontSize = 13.sp,
                        color = GrayText
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showVerificationDialog = false
                        navController.navigate("login") {
                            popUpTo("signup") { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = PrimaryOrange
                    )
                ) {
                    Text("Go to Login", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showVerificationDialog = false
                        navController.navigate("login") {
                            popUpTo("signup") { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = GrayText
                    )
                ) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun SignupServiceIcons() {
    val services = listOf(
        "Plumber" to Icons.Default.Plumbing,
        "Electrician" to Icons.Default.ElectricalServices,
        "Medical" to Icons.Default.HealthAndSafety,
        "Mistri" to Icons.Default.Handyman,
        "Cleaning" to Icons.Default.CleaningServices,
        "Near You" to Icons.Default.LocationOn
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            services.take(3).forEach { (title, icon) ->
                SignupServiceCircle(icon, title)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            services.drop(3).forEach { (title, icon) ->
                SignupServiceCircle(icon, title)
            }
        }
    }
}

@Composable
fun SignupServiceCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Surface(
        modifier = Modifier
            .size(70.dp)
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
                modifier = Modifier.size(26.dp)
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