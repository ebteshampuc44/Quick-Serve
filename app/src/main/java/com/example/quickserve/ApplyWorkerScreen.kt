package com.example.quickserve

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val OrangePrimary = Color(0xFFFF7622)
private val OrangeLight = Color(0xFFFFF0E8)
private val TextDark = Color(0xFF1A1A2E)
private val TextGray = Color(0xFF6B7280)
private val LightGray = Color(0xFFF0F2F5)
private val White = Color.White
private val Background = Color(0xFFFAFAFA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyWorkerScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    // Form states
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedServiceType by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var chargePerHour by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Service types list
    val serviceTypes = listOf(
        "Plumber",
        "Electrician",
        "AC & Appliance",
        "Cleaning",
        "Carpenter",
        "Women Salon",
        "Men Salon",
        "Pest Control"
    )

    // Experience options
    val experienceOptions = listOf(
        "1 year",
        "2 years",
        "3 years",
        "4 years",
        "5 years",
        "6+ years"
    )

    // Get current user email
    val currentUser = auth.currentUser
    val userEmail = currentUser?.email ?: ""

    fun submitApplication() {
        when {
            fullName.isBlank() -> {
                Toast.makeText(context, "Please enter your full name", Toast.LENGTH_SHORT).show()
            }
            phoneNumber.isBlank() -> {
                Toast.makeText(context, "Please enter your phone number", Toast.LENGTH_SHORT).show()
            }
            selectedServiceType.isBlank() -> {
                Toast.makeText(context, "Please select a service type", Toast.LENGTH_SHORT).show()
            }
            experience.isBlank() -> {
                Toast.makeText(context, "Please select your experience", Toast.LENGTH_SHORT).show()
            }
            location.isBlank() -> {
                Toast.makeText(context, "Please enter your location", Toast.LENGTH_SHORT).show()
            }
            chargePerHour.isBlank() -> {
                Toast.makeText(context, "Please enter your charge per hour", Toast.LENGTH_SHORT).show()
            }
            chargePerHour.toIntOrNull() == null -> {
                Toast.makeText(context, "Please enter a valid charge amount", Toast.LENGTH_SHORT).show()
            }
            else -> {
                isLoading = true

                val applicationData = hashMapOf(
                    "userId" to currentUser?.uid,
                    "userEmail" to userEmail,
                    "fullName" to fullName,
                    "phoneNumber" to phoneNumber,
                    "serviceType" to selectedServiceType,
                    "experience" to experience,
                    "location" to location,
                    "chargePerHour" to chargePerHour.toInt(),
                    "status" to "pending",
                    "appliedAt" to System.currentTimeMillis()
                )

                db.collection("worker_applications")
                    .add(applicationData)
                    .addOnSuccessListener {
                        isLoading = false
                        showSuccessDialog = true
                    }
                    .addOnFailureListener { e ->
                        isLoading = false
                        Toast.makeText(context, "Failed to apply: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Apply as Worker",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OrangePrimary
                )
            )
        },
        containerColor = Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Background, White)
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
                // Header Icon
                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(OrangeLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Work,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Join Our Team",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangePrimary
                )

                Text(
                    text = "Become a service provider and grow your business",
                    fontSize = 13.sp,
                    color = TextGray,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Application Form Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Worker Application",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )

                        Text(
                            text = "Please fill out the form below to apply",
                            fontSize = 13.sp,
                            color = TextGray
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Full Name Field
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Full Name", color = TextGray) },
                            leadingIcon = {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = OrangePrimary)
                            },
                            placeholder = { Text("Enter your full name") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangePrimary,
                                unfocusedBorderColor = LightGray
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Phone Number Field
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Phone Number", color = TextGray) },
                            leadingIcon = {
                                Icon(Icons.Filled.Phone, contentDescription = null, tint = OrangePrimary)
                            },
                            placeholder = { Text("+880 1XXX-XXXXXX") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangePrimary,
                                unfocusedBorderColor = LightGray
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Service Type Dropdown
                        var expandedService by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = expandedService,
                            onExpandedChange = { expandedService = it }
                        ) {
                            OutlinedTextField(
                                value = selectedServiceType,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                label = { Text("Service Type", color = TextGray) },
                                leadingIcon = {
                                    Icon(Icons.Filled.Build, contentDescription = null, tint = OrangePrimary)
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedService) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangePrimary,
                                    unfocusedBorderColor = LightGray
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expandedService,
                                onDismissRequest = { expandedService = false }
                            ) {
                                serviceTypes.forEach { service ->
                                    DropdownMenuItem(
                                        text = { Text(service) },
                                        onClick = {
                                            selectedServiceType = service
                                            expandedService = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Experience Dropdown
                        var expandedExp by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = expandedExp,
                            onExpandedChange = { expandedExp = it }
                        ) {
                            OutlinedTextField(
                                value = experience,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                label = { Text("Experience", color = TextGray) },
                                leadingIcon = {
                                    Icon(Icons.Filled.Work, contentDescription = null, tint = OrangePrimary)
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedExp) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangePrimary,
                                    unfocusedBorderColor = LightGray
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expandedExp,
                                onDismissRequest = { expandedExp = false }
                            ) {
                                experienceOptions.forEach { exp ->
                                    DropdownMenuItem(
                                        text = { Text(exp) },
                                        onClick = {
                                            experience = exp
                                            expandedExp = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Location Field
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Location", color = TextGray) },
                            leadingIcon = {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = OrangePrimary)
                            },
                            placeholder = { Text("City, Area") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangePrimary,
                                unfocusedBorderColor = LightGray
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Charge per hour Field
                        OutlinedTextField(
                            value = chargePerHour,
                            onValueChange = { chargePerHour = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Charge per hour (BDT)", color = TextGray) },
                            leadingIcon = {
                                Icon(Icons.Filled.AttachMoney, contentDescription = null, tint = OrangePrimary)
                            },
                            placeholder = { Text("e.g., 500") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangePrimary,
                                unfocusedBorderColor = LightGray
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Apply Button
                        Button(
                            onClick = { submitApplication() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrangePrimary,
                                disabledContainerColor = OrangePrimary.copy(alpha = 0.6f)
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
                                Icon(Icons.Filled.Send, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Submit Application",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                navController.navigateUp()
            },
            title = {
                Text(
                    text = "Application Submitted!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangePrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Your application has been submitted successfully.",
                        fontSize = 14.sp,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "We will review your application and contact you soon.",
                        fontSize = 13.sp,
                        color = TextGray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        navController.navigateUp()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangePrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}