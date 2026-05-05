package com.example.quickserve

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

private val OrangePrimary = Color(0xFFFF7622)
private val OrangeLight = Color(0xFFFFF0E8)
private val TextDark = Color(0xFF1A1A2E)
private val TextGray = Color(0xFF6B7280)
private val LightGray = Color(0xFFF0F2F5)
private val White = Color.White
private val Background = Color(0xFFFAFAFA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    navController: NavController,
    serviceId: String?,
    providerId: String?
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance().reference

    // States
    var isLoading by remember { mutableStateOf(true) }
    var serviceDetail by remember { mutableStateOf<ServiceDetail?>(null) }
    var providerDetail by remember { mutableStateOf<ServiceProvider?>(null) }
    var userAddress by remember { mutableStateOf("") }
    var userFullAddress by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var problemDescription by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Get current user info
    var userFullName by remember { mutableStateOf("") }
    var userPhone by remember { mutableStateOf("") }

    // Load data
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            try {
                // Get user data from Realtime Database
                val userRef = database.child("users").child(currentUser.uid)

                userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            userFullName = snapshot.child("fullName").getValue(String::class.java) ?: ""
                            userPhone = snapshot.child("phone").getValue(String::class.java) ?: ""
                        }

                        // Get service details
                        val serviceIdInt = serviceId?.toIntOrNull() ?: 0
                        serviceDetail = getServiceDetail(serviceIdInt)

                        // Get provider details
                        val providerIdInt = providerId?.toIntOrNull() ?: 0
                        providerDetail = getProvidersForCategory(serviceIdInt).find { it.id == providerIdInt }

                        isLoading = false
                    }

                    override fun onCancelled(error: DatabaseError) {
                        isLoading = false
                        Toast.makeText(context, "Error loading data: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    // Date and Time picker states
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    // Format date
    fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return format.format(date)
    }

    // Format time
    fun formatTime(hour: Int, minute: Int): String {
        val format = SimpleDateFormat("h:mm a", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        return format.format(calendar.time)
    }

    // Confirm booking
    fun confirmBooking() {
        when {
            userAddress.isBlank() -> {
                Toast.makeText(context, "Please enter your address", Toast.LENGTH_SHORT).show()
            }
            selectedDate.isBlank() -> {
                Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
            }
            selectedTime.isBlank() -> {
                Toast.makeText(context, "Please select a time", Toast.LENGTH_SHORT).show()
            }
            problemDescription.isBlank() -> {
                Toast.makeText(context, "Please describe your problem", Toast.LENGTH_SHORT).show()
            }
            else -> {
                isSubmitting = true

                // Save booking to Realtime Database
                val bookingData = mapOf(
                    "userId" to (auth.currentUser?.uid ?: ""),
                    "userName" to userFullName,
                    "userPhone" to userPhone,
                    "serviceId" to (serviceId ?: ""),
                    "serviceName" to (serviceDetail?.name ?: ""),
                    "providerId" to (providerId ?: ""),
                    "providerName" to (providerDetail?.name ?: ""),
                    "providerPrice" to (providerDetail?.pricePerHour ?: 0),
                    "address" to userAddress,
                    "fullAddress" to userFullAddress,
                    "date" to selectedDate,
                    "time" to selectedTime,
                    "problemDescription" to problemDescription,
                    "status" to "pending",
                    "createdAt" to System.currentTimeMillis()
                )

                val bookingsRef = database.child("bookings").push()

                bookingsRef.setValue(bookingData)
                    .addOnSuccessListener {
                        isSubmitting = false
                        Toast.makeText(context, "Booking confirmed successfully!", Toast.LENGTH_LONG).show()
                        navController.navigate("home") {
                            popUpTo("booking") { inclusive = true }
                        }
                    }
                    .addOnFailureListener { e ->
                        isSubmitting = false
                        Toast.makeText(context, "Failed to book: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = OrangePrimary)
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Confirm Booking",
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Selected Service
                item {
                    BookingSectionCard(
                        title = "Selected Service",
                        icon = Icons.Filled.Build
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = serviceDetail?.name ?: "Service",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = serviceDetail?.priceRange ?: "",
                                    fontSize = 13.sp,
                                    color = TextGray
                                )
                            }
                            if (serviceDetail != null) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(serviceDetail!!.color.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = serviceDetail!!.icon,
                                        contentDescription = null,
                                        tint = serviceDetail!!.color,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Selected Provider
                item {
                    BookingSectionCard(
                        title = "Selected Provider",
                        icon = Icons.Filled.Person
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = providerDetail?.name ?: "Provider",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFC107),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = " ${providerDetail?.rating ?: 0}",
                                        fontSize = 12.sp,
                                        color = TextGray
                                    )
                                    Text(
                                        text = " • ${providerDetail?.experience ?: ""}",
                                        fontSize = 12.sp,
                                        color = TextGray
                                    )
                                }
                                Text(
                                    text = "$${providerDetail?.pricePerHour ?: 0}/hour",
                                    fontSize = 13.sp,
                                    color = OrangePrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(
                                Icons.Filled.Verified,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // User Address
                item {
                    BookingSectionCard(
                        title = "Service Address",
                        icon = Icons.Filled.LocationOn
                    ) {
                        Column {
                            OutlinedTextField(
                                value = userAddress,
                                onValueChange = { userAddress = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Street Address", color = TextGray) },
                                placeholder = { Text("House #, Road #, Area") },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangePrimary,
                                    unfocusedBorderColor = LightGray
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = userFullAddress,
                                onValueChange = { userFullAddress = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Full Address (City, District)", color = TextGray) },
                                placeholder = { Text("City, District, Post Code") },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangePrimary,
                                    unfocusedBorderColor = LightGray
                                )
                            )
                        }
                    }
                }

                // Date Selection
                item {
                    BookingSectionCard(
                        title = "Select Date",
                        icon = Icons.Filled.DateRange
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = LightGray)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.CalendarToday,
                                        contentDescription = null,
                                        tint = OrangePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (selectedDate.isBlank()) "Choose a date" else selectedDate,
                                        color = if (selectedDate.isBlank()) TextGray else TextDark,
                                        fontSize = 14.sp
                                    )
                                }
                                Icon(
                                    Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                    tint = TextGray
                                )
                            }
                        }
                    }
                }

                // Time Selection
                item {
                    BookingSectionCard(
                        title = "Select Time",
                        icon = Icons.Filled.Schedule
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val timeSlots = listOf("9:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
                                "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM")
                            items(timeSlots.size) { index ->
                                val time = timeSlots[index]
                                FilterChip(
                                    selected = selectedTime == time,
                                    onClick = { selectedTime = time },
                                    label = { Text(time, fontSize = 13.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = OrangePrimary,
                                        selectedLabelColor = White,
                                        disabledContainerColor = LightGray,
                                        disabledLabelColor = TextGray
                                    )
                                )
                            }
                        }
                    }
                }

                // Problem Description
                item {
                    BookingSectionCard(
                        title = "Problem Description",
                        icon = Icons.Filled.Description
                    ) {
                        OutlinedTextField(
                            value = problemDescription,
                            onValueChange = { problemDescription = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Describe your problem", color = TextGray) },
                            placeholder = {
                                Text(
                                    "Please describe the issue in detail...\n" +
                                            "e.g., Leaking pipe, AC not cooling, etc.",
                                    fontSize = 13.sp
                                )
                            },
                            minLines = 4,
                            maxLines = 6,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangePrimary,
                                unfocusedBorderColor = LightGray
                            )
                        )
                    }
                }

                // Price Summary
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Price Summary",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Service Charge", fontSize = 14.sp, color = TextGray)
                                Text(
                                    "$${providerDetail?.pricePerHour ?: 0}/hour",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextDark
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Service Fee", fontSize = 14.sp, color = TextGray)
                                Text("Free", fontSize = 14.sp, color = OrangePrimary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = LightGray, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Total Estimate",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    "$${providerDetail?.pricePerHour ?: 0}/hour + tax",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OrangePrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = { confirmBooking() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangePrimary,
                    disabledContainerColor = OrangePrimary.copy(alpha = 0.6f)
                ),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Confirm Booking",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = formatDate(it)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = OrangePrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextGray)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun BookingSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
            }
            content()
        }
    }
}