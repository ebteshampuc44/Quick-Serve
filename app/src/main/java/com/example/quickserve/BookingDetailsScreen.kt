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
import androidx.compose.material.icons.outlined.*
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
import com.google.firebase.database.*

private val OrangePrimary = Color(0xFFFF7622)
private val OrangeLight = Color(0xFFFFF0E8)
private val TextDark = Color(0xFF1A1A2E)
private val TextGray = Color(0xFF6B7280)
private val LightGray = Color(0xFFF0F2F5)
private val White = Color.White
private val Background = Color(0xFFFAFAFA)
private val BlackText = Color(0xFF000000)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailsScreen(
    bookingId: String,
    navController: NavController
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance().reference
    var isLoading by remember { mutableStateOf(true) }
    var booking by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var ratingValue by remember { mutableStateOf(0f) }
    var reviewText by remember { mutableStateOf("") }

    // Load booking details
    LaunchedEffect(bookingId) {
        val bookingRef = database.child("bookings").child(bookingId)

        bookingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val data = mutableMapOf<String, Any?>()
                    snapshot.children.forEach { child ->
                        data[child.key!!] = child.value
                    }
                    booking = data
                } else {
                    Toast.makeText(context, "Booking not found", Toast.LENGTH_SHORT).show()
                    navController.navigateUp()
                }
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
                Toast.makeText(context, "Failed to load: ${error.message}", Toast.LENGTH_SHORT).show()
                navController.navigateUp()
            }
        })
    }

    fun cancelBooking() {
        val bookingRef = database.child("bookings").child(bookingId)
        bookingRef.child("status").setValue("cancelled")
            .addOnSuccessListener {
                Toast.makeText(context, "Booking cancelled", Toast.LENGTH_SHORT).show()
                showCancelDialog = false
                navController.navigateUp()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun submitRating() {
        if (ratingValue == 0f) {
            Toast.makeText(context, "Please select a rating", Toast.LENGTH_SHORT).show()
            return
        }

        val reviewData = mapOf(
            "bookingId" to bookingId,
            "userId" to auth.currentUser?.uid,
            "providerName" to (booking?.get("providerName") ?: ""),
            "rating" to ratingValue,
            "review" to reviewText,
            "serviceName" to (booking?.get("serviceName") ?: ""),
            "createdAt" to System.currentTimeMillis()
        )

        database.child("reviews").push().setValue(reviewData)
            .addOnSuccessListener {
                database.child("bookings").child(bookingId).child("status").setValue("completed")
                    .addOnSuccessListener {
                        Toast.makeText(context, "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
                        showRatingDialog = false
                        navController.navigateUp()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = OrangePrimary)
        }
        return
    }

    val bookingData = booking ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Booking Details",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Booking Status",
                            fontSize = 12.sp,
                            color = TextGray
                        )
                        Text(
                            text = (bookingData["status"] as? String)?.uppercase() ?: "PENDING",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = when(bookingData["status"]) {
                                "pending" -> Color(0xFFFF9800)
                                "confirmed" -> Color(0xFF4CAF50)
                                "completed" -> Color(0xFF2196F3)
                                "cancelled" -> Color(0xFFF44336)
                                else -> OrangePrimary
                            }
                        )
                    }

                    when(bookingData["status"]) {
                        "pending", "confirmed" -> {
                            Button(
                                onClick = { showCancelDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF44336)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel Booking")
                            }
                        }
                        "completed" -> {
                            Button(
                                onClick = { showRatingDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rate Service")
                            }
                        }
                    }
                }
            }

            // Service Info Card
            InfoCard(
                title = "Service Information",
                icon = Icons.Filled.Build
            ) {
                InfoRow("Service Name", bookingData["serviceName"] as? String ?: "N/A")
                InfoRow("Provider", bookingData["providerName"] as? String ?: "N/A")
                InfoRow("Price", "৳${bookingData["providerPrice"]?.toString() ?: "0"}/hour")
            }

            // Schedule Card
            InfoCard(
                title = "Schedule",
                icon = Icons.Filled.CalendarToday
            ) {
                InfoRow("Date", bookingData["date"] as? String ?: "N/A")
                InfoRow("Time", bookingData["time"] as? String ?: "N/A")
                InfoRow("Booked On", formatDate(bookingData["createdAt"] as? Long ?: 0))
            }

            // Address Card
            InfoCard(
                title = "Service Address",
                icon = Icons.Filled.LocationOn
            ) {
                InfoRow("Street Address", bookingData["address"] as? String ?: "N/A")
                val fullAddress = bookingData["fullAddress"] as? String
                if (!fullAddress.isNullOrBlank()) {
                    InfoRow("Full Address", fullAddress)
                }
            }

            // Problem Description Card
            val problem = bookingData["problemDescription"] as? String
            if (!problem.isNullOrBlank()) {
                InfoCard(
                    title = "Problem Description",
                    icon = Icons.Filled.Description
                ) {
                    Text(
                        text = problem,
                        fontSize = 14.sp,
                        color = TextDark,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Cancel Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = {
                Text(
                    text = "Cancel Booking",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangePrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to cancel this booking? This action cannot be undone.",
                    fontSize = 14.sp,
                    color = TextGray
                )
            },
            confirmButton = {
                Button(
                    onClick = { cancelBooking() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Yes, Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("No, Keep It", color = TextGray)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Rating Dialog
    if (showRatingDialog) {
        AlertDialog(
            onDismissRequest = { showRatingDialog = false },
            title = {
                Text(
                    text = "Rate Your Service",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangePrimary
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "How was your experience with",
                        fontSize = 14.sp,
                        color = TextGray
                    )
                    Text(
                        text = bookingData["providerName"] as? String ?: "",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (i in 1..5) {
                            IconButton(onClick = { ratingValue = i.toFloat() }) {
                                Icon(
                                    if (i <= ratingValue) Icons.Filled.Star else Icons.Outlined.Star,
                                    contentDescription = null,
                                    tint = if (i <= ratingValue) Color(0xFFFFC107) else TextGray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = reviewText,
                        onValueChange = { reviewText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Write a review (optional)", color = TextGray) },
                        placeholder = { Text("Share your experience...", color = TextGray) },
                        minLines = 3,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(color = BlackText),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = LightGray,
                            focusedTextColor = BlackText,
                            unfocusedTextColor = BlackText
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { submitRating() },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Submit Rating")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRatingDialog = false }) {
                    Text("Later", color = TextGray)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun InfoCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(OrangeLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextGray
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextDark,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(0.6f)
                .padding(start = 8.dp)
        )
    }
}

fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return "N/A"
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
    return format.format(date)
}