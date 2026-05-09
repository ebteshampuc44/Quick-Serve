// TasksScreen.kt
package com.example.quickserve

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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

private val OrangePrimary = Color(0xFFFF7622)
private val OrangeLight = Color(0xFFFFF0E8)
private val TextDark = Color(0xFF1A1A2E)
private val TextGray = Color(0xFF6B7280)
private val LightGray = Color(0xFFF0F2F5)
private val White = Color.White
private val Background = Color(0xFFFAFAFA)
private val BlackText = Color(0xFF000000)

// Task status colors
private val StatusPending = Color(0xFFFF9800)
private val StatusConfirmed = Color(0xFF4CAF50)
private val StatusCompleted = Color(0xFF2196F3)
private val StatusCancelled = Color(0xFFF44336)

data class TaskBooking(
    val bookingId: String = "",
    val serviceName: String = "",
    val providerName: String = "",
    val providerImage: String = "",
    val date: String = "",
    val time: String = "",
    val address: String = "",
    val status: String = "pending",
    val price: Int = 0,
    val problemDescription: String = "",
    val createdAt: Long = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance().reference
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Upcoming, 1: Completed, 2: Cancelled
    var bookings by remember { mutableStateOf<List<TaskBooking>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var selectedBookingId by remember { mutableStateOf<String?>(null) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var selectedBookingForRating by remember { mutableStateOf<TaskBooking?>(null) }
    var ratingValue by remember { mutableFloatStateOf(0f) }
    var reviewText by remember { mutableStateOf("") }

    // Load bookings from Realtime Database
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val bookingsRef = database.child("bookings")

            bookingsRef.orderByChild("userId").equalTo(currentUser.uid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val bookingList = mutableListOf<TaskBooking>()
                        snapshot.children.forEach { child ->
                            val booking = TaskBooking(
                                bookingId = child.key ?: "",
                                serviceName = child.child("serviceName").getValue(String::class.java) ?: "",
                                providerName = child.child("providerName").getValue(String::class.java) ?: "",
                                date = child.child("date").getValue(String::class.java) ?: "",
                                time = child.child("time").getValue(String::class.java) ?: "",
                                address = child.child("address").getValue(String::class.java) ?: "",
                                status = child.child("status").getValue(String::class.java) ?: "pending",
                                price = child.child("providerPrice").getValue(Int::class.java) ?: 0,
                                problemDescription = child.child("problemDescription").getValue(String::class.java) ?: "",
                                createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0
                            )
                            bookingList.add(booking)
                        }
                        // Sort by createdAt descending
                        bookings = bookingList.sortedByDescending { it.createdAt }
                        isLoading = false
                    }

                    override fun onCancelled(error: DatabaseError) {
                        isLoading = false
                        Toast.makeText(context, "Failed to load bookings: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            isLoading = false
        }
    }

    // Filter bookings based on selected tab
    val filteredBookings = when (selectedTab) {
        0 -> bookings.filter { it.status == "pending" || it.status == "confirmed" }
        1 -> bookings.filter { it.status == "completed" }
        2 -> bookings.filter { it.status == "cancelled" }
        else -> bookings
    }

    fun cancelBooking(bookingId: String) {
        val bookingRef = database.child("bookings").child(bookingId)
        bookingRef.child("status").setValue("cancelled")
            .addOnSuccessListener {
                Toast.makeText(context, "Booking cancelled successfully", Toast.LENGTH_SHORT).show()
                showCancelDialog = false
                selectedBookingId = null
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to cancel: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun submitRating(booking: TaskBooking) {
        if (ratingValue == 0f) {
            Toast.makeText(context, "Please select a rating", Toast.LENGTH_SHORT).show()
            return
        }

        val reviewData = mapOf(
            "bookingId" to booking.bookingId,
            "userId" to auth.currentUser?.uid,
            "providerName" to booking.providerName,
            "rating" to ratingValue,
            "review" to reviewText,
            "serviceName" to booking.serviceName,
            "createdAt" to System.currentTimeMillis()
        )

        database.child("reviews").push().setValue(reviewData)
            .addOnSuccessListener {
                // Update booking status to completed with review
                database.child("bookings").child(booking.bookingId).child("status").setValue("completed")
                    .addOnSuccessListener {
                        Toast.makeText(context, "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
                        showRatingDialog = false
                        selectedBookingForRating = null
                        ratingValue = 0f
                        reviewText = ""
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to submit review: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Tasks",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OrangePrimary
                ),
                actions = {
                    IconButton(onClick = { navController.navigate("bookingHistory") }) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = White)
                    }
                }
            )
        },
        containerColor = Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = White,
                contentColor = OrangePrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = OrangePrimary,
                        height = 3.dp
                    )
                }
            ) {
                listOf("Upcoming (${bookings.filter { it.status == "pending" || it.status == "confirmed" }.size})",
                    "Completed (${bookings.filter { it.status == "completed" }.size})",
                    "Cancelled (${bookings.filter { it.status == "cancelled" }.size})")
                    .forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) OrangePrimary else TextGray
                                )
                            }
                        )
                    }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = OrangePrimary)
                }
            } else if (filteredBookings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = when (selectedTab) {
                                0 -> "No upcoming tasks"
                                1 -> "No completed tasks"
                                else -> "No cancelled tasks"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextGray
                        )
                        val taskTypeText = when (selectedTab) {
                            0 -> "upcoming"
                            1 -> "completed"
                            else -> "cancelled"
                        }
                        Text(
                            text = "Your $taskTypeText tasks will appear here",
                            fontSize = 13.sp,
                            color = TextGray
                        )
                        if (selectedTab == 0) {
                            Button(
                                onClick = { navController.navigate("allServices") },
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Book a Service")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredBookings) { booking ->
                        TaskCard(
                            booking = booking,
                            onCancelClick = {
                                selectedBookingId = booking.bookingId
                                showCancelDialog = true
                            },
                            onRateClick = {
                                selectedBookingForRating = booking
                                showRatingDialog = true
                            },
                            onViewDetails = {
                                navController.navigate("bookingDetails/${booking.bookingId}")
                            }
                        )
                    }
                }
            }
        }
    }

    // Cancel Confirmation Dialog
    if (showCancelDialog && selectedBookingId != null) {
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
                    onClick = { selectedBookingId?.let { cancelBooking(it) } },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Yes, Cancel", color = White)
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
    if (showRatingDialog && selectedBookingForRating != null) {
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
                        text = selectedBookingForRating?.providerName ?: "",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Star Rating
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (i in 1..5) {
                            IconButton(onClick = { ratingValue = i.toFloat() }) {
                                Icon(
                                    imageVector = if (i <= ratingValue) Icons.Filled.Star else Icons.Outlined.Star,
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
                    onClick = { selectedBookingForRating?.let { submitRating(it) } },
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
fun TaskCard(
    booking: TaskBooking,
    onCancelClick: () -> Unit,
    onRateClick: () -> Unit,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row with Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = booking.serviceName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                StatusChip(status = booking.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Provider Info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(OrangeLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = booking.providerName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDark
                    )
                    Text(
                        text = "Service Provider",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Date and Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = booking.date,
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = booking.time,
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Address
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = booking.address,
                    fontSize = 12.sp,
                    color = TextGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Problem Description if available
            if (booking.problemDescription.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val descriptionText = if (booking.problemDescription.length > 60) {
                        booking.problemDescription.take(60) + "..."
                    } else {
                        booking.problemDescription
                    }
                    Text(
                        text = descriptionText,
                        fontSize = 11.sp,
                        color = TextGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = LightGray, thickness = 0.5.dp)

            Spacer(modifier = Modifier.height(12.dp))

            // Price and Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Amount",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                    Text(
                        text = "৳${booking.price}/hour",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (booking.status) {
                        "pending", "confirmed" -> {
                            Button(
                                onClick = onCancelClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFFF44336)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel", fontSize = 12.sp)
                            }
                        }
                        "completed" -> {
                            Button(
                                onClick = onRateClick,
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Rate", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onViewDetails,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangeLight,
                            contentColor = OrangePrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Details", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (color, bgColor, displayText) = when (status.lowercase()) {
        "pending" -> Triple(StatusPending, StatusPending.copy(alpha = 0.12f), "Pending")
        "confirmed" -> Triple(StatusConfirmed, StatusConfirmed.copy(alpha = 0.12f), "Confirmed")
        "completed" -> Triple(StatusCompleted, StatusCompleted.copy(alpha = 0.12f), "Completed")
        "cancelled" -> Triple(StatusCancelled, StatusCancelled.copy(alpha = 0.12f), "Cancelled")
        else -> Triple(TextGray, LightGray, status)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = displayText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}