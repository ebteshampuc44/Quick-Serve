package com.example.quickserve

import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
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
private val GreenSuccess = Color(0xFF4CAF50)
private val RedError = Color(0xFFF44336)
private val DeleteRed = Color(0xFFD32F2F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminApplicationsScreen(navController: NavController) {
    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance().reference
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()

    var selectedMainTab by remember { mutableIntStateOf(0) } // 0: Worker Applications, 1: Bookings
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Pending, 1: Approved, 2: Rejected for workers

    // Worker Applications States
    var pendingApplications by remember { mutableStateOf<List<WorkerApplication>>(emptyList()) }
    var approvedApplications by remember { mutableStateOf<List<WorkerApplication>>(emptyList()) }
    var rejectedApplications by remember { mutableStateOf<List<WorkerApplication>>(emptyList()) }

    // Bookings States
    var pendingBookings by remember { mutableStateOf<List<FirestoreBooking>>(emptyList()) }
    var confirmedBookings by remember { mutableStateOf<List<FirestoreBooking>>(emptyList()) }
    var completedBookings by remember { mutableStateOf<List<FirestoreBooking>>(emptyList()) }
    var cancelledBookings by remember { mutableStateOf<List<FirestoreBooking>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRejectApprovedDialog by remember { mutableStateOf(false) } // নতুন ডায়ালগ approved থেকে reject করার জন্য
    var selectedApplication by remember { mutableStateOf<WorkerApplication?>(null) }
    var rejectReason by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    // Load Worker Applications
    LaunchedEffect(Unit) {
        val applicationsRef = database.child(Constants.WORKER_APPLICATIONS)

        applicationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pendingList = mutableListOf<WorkerApplication>()
                val approvedList = mutableListOf<WorkerApplication>()
                val rejectedList = mutableListOf<WorkerApplication>()

                snapshot.children.forEach { child ->
                    val application = WorkerApplication(
                        id = child.key ?: "",
                        userId = child.child("userId").getValue(String::class.java) ?: "",
                        userEmail = child.child("userEmail").getValue(String::class.java) ?: "",
                        fullName = child.child("fullName").getValue(String::class.java) ?: "",
                        phoneNumber = child.child("phoneNumber").getValue(String::class.java) ?: "",
                        serviceType = child.child("serviceType").getValue(String::class.java) ?: "",
                        experience = child.child("experience").getValue(String::class.java) ?: "",
                        location = child.child("location").getValue(String::class.java) ?: "",
                        chargePerHour = child.child("chargePerHour").getValue(Int::class.java) ?: 0,
                        status = child.child("status").getValue(String::class.java) ?: Constants.STATUS_PENDING,
                        appliedAt = child.child("appliedAt").getValue(Long::class.java) ?: 0,
                        approvedAt = child.child("approvedAt").getValue(Long::class.java) ?: 0,
                        workerId = child.child("workerId").getValue(String::class.java) ?: "",
                        profileImageBase64 = child.child("profileImageBase64").getValue(String::class.java) ?: ""
                    )

                    when (application.status) {
                        Constants.STATUS_PENDING -> pendingList.add(application)
                        Constants.STATUS_APPROVED -> approvedList.add(application)
                        Constants.STATUS_REJECTED -> rejectedList.add(application)
                    }
                }

                pendingApplications = pendingList.sortedByDescending { it.appliedAt }
                approvedApplications = approvedList.sortedByDescending { it.approvedAt }
                rejectedApplications = rejectedList.sortedByDescending { it.appliedAt }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Load Bookings from Firestore
    LaunchedEffect(Unit) {
        firestore.collection(Constants.FIRESTORE_BOOKINGS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(context, "Failed to load bookings: ${error.message}", Toast.LENGTH_SHORT).show()
                    isLoading = false
                    return@addSnapshotListener
                }

                val pending = mutableListOf<FirestoreBooking>()
                val confirmed = mutableListOf<FirestoreBooking>()
                val completed = mutableListOf<FirestoreBooking>()
                val cancelled = mutableListOf<FirestoreBooking>()

                snapshot?.documents?.forEach { doc ->
                    val booking = doc.toObject(FirestoreBooking::class.java)?.copy(bookingId = doc.id)
                    booking?.let {
                        when (it.status) {
                            Constants.BOOKING_PENDING -> pending.add(it)
                            Constants.BOOKING_CONFIRMED -> confirmed.add(it)
                            Constants.BOOKING_COMPLETED -> completed.add(it)
                            Constants.BOOKING_CANCELLED -> cancelled.add(it)
                        }
                    }
                }

                pendingBookings = pending.sortedByDescending { it.createdAt }
                confirmedBookings = confirmed.sortedByDescending { it.createdAt }
                completedBookings = completed.sortedByDescending { it.createdAt }
                cancelledBookings = cancelled.sortedByDescending { it.createdAt }
                isLoading = false
            }
    }

    // Confirm Booking and Send Notification
    fun confirmBooking(booking: FirestoreBooking) {
        scope.launch {
            try {
                firestore.collection(Constants.FIRESTORE_BOOKINGS)
                    .document(booking.bookingId)
                    .update(
                        "status", Constants.BOOKING_CONFIRMED,
                        "updatedAt", System.currentTimeMillis()
                    )
                    .await()

                val notification = NotificationData(
                    id = UUID.randomUUID().toString(),
                    userId = booking.userId,
                    title = "✅ Booking Confirmed!",
                    message = "Your booking for ${booking.serviceName} on ${booking.date} at ${booking.time} has been confirmed. Service provider: ${booking.providerName}",
                    bookingId = booking.bookingId,
                    bookingStatus = Constants.BOOKING_CONFIRMED,
                    isRead = false,
                    createdAt = System.currentTimeMillis()
                )

                firestore.collection(Constants.FIRESTORE_NOTIFICATIONS)
                    .document(notification.id)
                    .set(notification)
                    .await()

                Toast.makeText(context, "Booking confirmed! Notification sent to customer.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to confirm: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Cancel Booking and Send Notification
    fun cancelBooking(booking: FirestoreBooking) {
        scope.launch {
            try {
                firestore.collection(Constants.FIRESTORE_BOOKINGS)
                    .document(booking.bookingId)
                    .update(
                        "status", Constants.BOOKING_CANCELLED,
                        "updatedAt", System.currentTimeMillis()
                    )
                    .await()

                val notification = NotificationData(
                    id = UUID.randomUUID().toString(),
                    userId = booking.userId,
                    title = "❌ Booking Cancelled",
                    message = "Your booking for ${booking.serviceName} on ${booking.date} at ${booking.time} has been cancelled.",
                    bookingId = booking.bookingId,
                    bookingStatus = Constants.BOOKING_CANCELLED,
                    isRead = false,
                    createdAt = System.currentTimeMillis()
                )

                firestore.collection(Constants.FIRESTORE_NOTIFICATIONS)
                    .document(notification.id)
                    .set(notification)
                    .await()

                Toast.makeText(context, "Booking cancelled! Notification sent to customer.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to cancel: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun approveApplication(application: WorkerApplication) {
        if (isProcessing) return
        isProcessing = true

        scope.launch {
            try {
                val workerId = database.child(Constants.WORKERS).push().key ?: return@launch

                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val adminRef = database.child("admins").child(currentUser.uid)
                    adminRef.setValue(true).await()
                }

                val workerData = mapOf(
                    "workerId" to workerId,
                    "userId" to application.userId,
                    "fullName" to application.fullName,
                    "email" to application.userEmail,
                    "phone" to application.phoneNumber,
                    "serviceType" to application.serviceType,
                    "experience" to application.experience,
                    "location" to application.location,
                    "chargePerHour" to application.chargePerHour,
                    "rating" to 0f,
                    "totalReviews" to 0,
                    "status" to Constants.WORKER_AVAILABLE,
                    "joinedAt" to System.currentTimeMillis(),
                    "isTopRated" to false,
                    "profileImageBase64" to (application.profileImageBase64 ?: "")
                )

                val updates = mapOf<String, Any>(
                    "${Constants.WORKERS}/$workerId" to workerData,
                    "${Constants.WORKER_APPLICATIONS}/${application.id}/status" to Constants.STATUS_APPROVED,
                    "${Constants.WORKER_APPLICATIONS}/${application.id}/workerId" to workerId,
                    "${Constants.WORKER_APPLICATIONS}/${application.id}/approvedAt" to System.currentTimeMillis()
                )

                database.updateChildren(updates).await()

                Toast.makeText(
                    context,
                    "${application.fullName} approved as ${application.serviceType}!",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to approve: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isProcessing = false
            }
        }
    }

    // Reject Application from Pending
    fun rejectApplication(application: WorkerApplication) {
        if (isProcessing) return
        isProcessing = true

        scope.launch {
            try {
                val updates = mapOf<String, Any>(
                    "${Constants.WORKER_APPLICATIONS}/${application.id}/status" to Constants.STATUS_REJECTED
                )

                database.updateChildren(updates).await()

                if (application.workerId.isNotEmpty()) {
                    database.child(Constants.WORKERS).child(application.workerId).removeValue().await()
                }

                Toast.makeText(context, "Application rejected", Toast.LENGTH_SHORT).show()
                showRejectDialog = false
                rejectReason = ""
                selectedApplication = null
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to reject: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isProcessing = false
            }
        }
    }

    // NEW: Reject Approved Application (Remove from workers)
    fun rejectApprovedApplication(application: WorkerApplication) {
        if (isProcessing) return
        isProcessing = true

        scope.launch {
            try {
                // Update application status to rejected
                val updates = mapOf<String, Any>(
                    "${Constants.WORKER_APPLICATIONS}/${application.id}/status" to Constants.STATUS_REJECTED
                )
                database.updateChildren(updates).await()

                // Remove worker from workers node (main app will no longer show this worker)
                if (application.workerId.isNotEmpty()) {
                    database.child(Constants.WORKERS).child(application.workerId).removeValue().await()
                }

                Toast.makeText(
                    context,
                    "${application.fullName} has been rejected and removed from the system!",
                    Toast.LENGTH_LONG
                ).show()

                showRejectApprovedDialog = false
                selectedApplication = null
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to reject: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isProcessing = false
            }
        }
    }

    // Permanent Delete Function (only for rejected items)
    fun permanentDeleteApplication(application: WorkerApplication) {
        if (isProcessing) return
        isProcessing = true

        scope.launch {
            try {
                database.child(Constants.WORKER_APPLICATIONS).child(application.id).removeValue().await()

                if (application.workerId.isNotEmpty()) {
                    database.child(Constants.WORKERS).child(application.workerId).removeValue().await()
                }

                Toast.makeText(
                    context,
                    "${application.fullName}'s application permanently deleted!",
                    Toast.LENGTH_LONG
                ).show()

                showDeleteDialog = false
                selectedApplication = null
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isProcessing = false
            }
        }
    }

    fun logout() {
        auth.signOut()
        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
        navController.navigate("login") {
            popUpTo("adminApplications") { inclusive = true }
        }
    }

    fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return "N/A"
        val date = Date(timestamp)
        val format = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return format.format(date)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Admin Panel",
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
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Outlined.Logout, contentDescription = "Logout", tint = White)
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
        ) {
            TabRow(
                selectedTabIndex = selectedMainTab,
                containerColor = White,
                contentColor = OrangePrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedMainTab]),
                        color = OrangePrimary,
                        height = 3.dp
                    )
                }
            ) {
                listOf("Worker Applications", "Bookings Management")
                    .forEachIndexed { index, title ->
                        Tab(
                            selected = selectedMainTab == index,
                            onClick = { selectedMainTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedMainTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedMainTab == index) OrangePrimary else TextGray
                                )
                            }
                        )
                    }
            }

            when (selectedMainTab) {
                0 -> WorkerApplicationsTab(
                    selectedTab = selectedTab,
                    pendingApplications = pendingApplications,
                    approvedApplications = approvedApplications,
                    rejectedApplications = rejectedApplications,
                    isLoading = isLoading,
                    onTabSelected = { selectedTab = it },
                    onApprove = { approveApplication(it) },
                    onReject = { application ->
                        selectedApplication = application
                        showRejectDialog = true
                    },
                    onRejectApproved = { application ->  // নতুন: approved থেকে reject
                        selectedApplication = application
                        showRejectApprovedDialog = true
                    },
                    onPermanentDelete = { application ->
                        selectedApplication = application
                        showDeleteDialog = true
                    },
                    isProcessing = isProcessing,
                    formatDate = { formatDate(it) }
                )
                1 -> BookingsManagementTab(
                    pendingBookings = pendingBookings,
                    confirmedBookings = confirmedBookings,
                    completedBookings = completedBookings,
                    cancelledBookings = cancelledBookings,
                    isLoading = isLoading,
                    onConfirm = { confirmBooking(it) },
                    onCancel = { cancelBooking(it) },
                    formatDate = { formatDate(it) }
                )
            }
        }
    }

    // Reject Dialog (for Pending)
    if (showRejectDialog && selectedApplication != null) {
        AlertDialog(
            onDismissRequest = {
                showRejectDialog = false
                rejectReason = ""
                selectedApplication = null
            },
            title = {
                Text(
                    text = "Reject Application",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = RedError
                )
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to reject ${selectedApplication?.fullName}'s application?",
                        fontSize = 14.sp,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Reason (optional)", color = TextGray) },
                        placeholder = { Text("Enter rejection reason...") },
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedError,
                            unfocusedBorderColor = LightGray
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedApplication?.let { rejectApplication(it) } },
                    colors = ButtonDefaults.buttonColors(containerColor = RedError),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = White)
                    } else {
                        Text("Yes, Reject", color = White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRejectDialog = false
                    rejectReason = ""
                    selectedApplication = null
                }) {
                    Text("Cancel", color = TextGray)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // NEW: Reject Approved Dialog (Remove from system)
    if (showRejectApprovedDialog && selectedApplication != null) {
        AlertDialog(
            onDismissRequest = {
                showRejectApprovedDialog = false
                selectedApplication = null
            },
            title = {
                Text(
                    text = "Reject Approved Application",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = RedError
                )
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to reject ${selectedApplication?.fullName}'s approved application?",
                        fontSize = 14.sp,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ This will:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = RedError
                    )
                    Text(
                        text = "• Remove the worker from the system",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                    Text(
                        text = "• The worker will no longer appear in HomeScreen",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                    Text(
                        text = "• Move this application to Rejected tab",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedApplication?.let { rejectApprovedApplication(it) } },
                    colors = ButtonDefaults.buttonColors(containerColor = RedError),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = White)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Yes, Reject & Remove", color = White)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRejectApprovedDialog = false
                    selectedApplication = null
                }) {
                    Text("Cancel", color = TextGray)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Permanent Delete Dialog (for Rejected items)
    if (showDeleteDialog && selectedApplication != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                selectedApplication = null
            },
            title = {
                Text(
                    text = "Permanently Delete",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeleteRed
                )
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to permanently delete ${selectedApplication?.fullName}'s application?",
                        fontSize = 14.sp,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ This action cannot be undone!",
                        fontSize = 12.sp,
                        color = RedError,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedApplication?.let { permanentDeleteApplication(it) } },
                    colors = ButtonDefaults.buttonColors(containerColor = DeleteRed),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = White)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Permanently Delete", color = White)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    selectedApplication = null
                }) {
                    Text("Cancel", color = TextGray)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Logout",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangePrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to logout from Admin Panel?",
                    fontSize = 14.sp,
                    color = TextGray
                )
            },
            confirmButton = {
                Button(
                    onClick = { logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Yes, Logout", color = White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextGray)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun WorkerApplicationsTab(
    selectedTab: Int,
    pendingApplications: List<WorkerApplication>,
    approvedApplications: List<WorkerApplication>,
    rejectedApplications: List<WorkerApplication>,
    isLoading: Boolean,
    onTabSelected: (Int) -> Unit,
    onApprove: (WorkerApplication) -> Unit,
    onReject: (WorkerApplication) -> Unit,
    onRejectApproved: (WorkerApplication) -> Unit,  // নতুন প্যারামিটার
    onPermanentDelete: (WorkerApplication) -> Unit,
    isProcessing: Boolean,
    formatDate: (Long) -> String
) {
    Column {
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
            listOf("Pending (${pendingApplications.size})", "Approved (${approvedApplications.size})", "Rejected (${rejectedApplications.size})")
                .forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { onTabSelected(index) },
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

        val currentApplications = when (selectedTab) {
            0 -> pendingApplications
            1 -> approvedApplications
            else -> rejectedApplications
        }

        if (isLoading && currentApplications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else if (currentApplications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.PersonOutline, contentDescription = null, tint = TextGray, modifier = Modifier.size(64.dp))
                    Text(text = "No applications", fontSize = 16.sp, color = TextGray, modifier = Modifier.padding(top = 8.dp))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(currentApplications) { application ->
                    ApplicationCard(
                        application = application,
                        onApprove = { onApprove(application) },
                        onReject = { onReject(application) },
                        onRejectApproved = { onRejectApproved(application) },  // নতুন কলব্যাক
                        onPermanentDelete = { onPermanentDelete(application) },
                        isPending = selectedTab == 0,
                        isApproved = selectedTab == 1,
                        isRejected = selectedTab == 2,
                        isProcessing = isProcessing,
                        formatDate = formatDate
                    )
                }
            }
        }
    }
}

@Composable
fun ApplicationCard(
    application: WorkerApplication,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onRejectApproved: () -> Unit,  // নতুন প্যারামিটার
    onPermanentDelete: () -> Unit,
    isPending: Boolean,
    isApproved: Boolean,
    isRejected: Boolean,
    isProcessing: Boolean,
    formatDate: (Long) -> String
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
            // Header with profile image
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Image
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(OrangeLight),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!application.profileImageBase64.isNullOrEmpty()) {
                            val imageBitmap = decodeBase64ToImageBitmap(application.profileImageBase64)
                            if (imageBitmap != null) {
                                Image(
                                    bitmap = imageBitmap,
                                    contentDescription = "Profile",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        } else {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = application.fullName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = application.userEmail,
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    }
                }

                StatusBadge(status = application.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details
            DetailRow(icon = Icons.Filled.Build, label = "Service Type", value = application.serviceType)
            DetailRow(icon = Icons.Filled.Work, label = "Experience", value = application.experience)
            DetailRow(icon = Icons.Filled.Phone, label = "Phone", value = application.phoneNumber)
            DetailRow(icon = Icons.Filled.LocationOn, label = "Location", value = application.location)
            DetailRow(icon = Icons.Filled.AttachMoney, label = "Charge", value = "৳${application.chargePerHour}/hour")
            DetailRow(icon = Icons.Filled.DateRange, label = "Applied On", value = formatDate(application.appliedAt))

            if (application.status == Constants.STATUS_APPROVED && application.approvedAt > 0) {
                DetailRow(icon = Icons.Filled.CheckCircle, label = "Approved On", value = formatDate(application.approvedAt))
            }

            // Action Buttons
            Spacer(modifier = Modifier.height(16.dp))

            when {
                isPending -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onApprove,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isProcessing
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = White)
                            } else {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Approve", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        OutlinedButton(
                            onClick = onReject,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedError),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isProcessing
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reject", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                isApproved -> {
                    // আপপ্রুভড আইটেমের জন্য Reject & Remove বাটন
                    Button(
                        onClick = onRejectApproved,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RedError),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = White)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reject & Remove from System", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
                isRejected -> {
                    Button(
                        onClick = onPermanentDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DeleteRed),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = White)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Permanently Delete", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookingsManagementTab(
    pendingBookings: List<FirestoreBooking>,
    confirmedBookings: List<FirestoreBooking>,
    completedBookings: List<FirestoreBooking>,
    cancelledBookings: List<FirestoreBooking>,
    isLoading: Boolean,
    onConfirm: (FirestoreBooking) -> Unit,
    onCancel: (FirestoreBooking) -> Unit,
    formatDate: (Long) -> String
) {
    var selectedBookingTab by remember { mutableIntStateOf(0) }

    Column {
        TabRow(
            selectedTabIndex = selectedBookingTab,
            containerColor = White,
            contentColor = OrangePrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedBookingTab]),
                    color = OrangePrimary,
                    height = 3.dp
                )
            }
        ) {
            listOf("Pending (${pendingBookings.size})", "Confirmed (${confirmedBookings.size})", "Completed (${completedBookings.size})", "Cancelled (${cancelledBookings.size})")
                .forEachIndexed { index, title ->
                    Tab(
                        selected = selectedBookingTab == index,
                        onClick = { selectedBookingTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedBookingTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedBookingTab == index) OrangePrimary else TextGray
                            )
                        }
                    )
                }
        }

        val currentBookings = when (selectedBookingTab) {
            0 -> pendingBookings
            1 -> confirmedBookings
            2 -> completedBookings
            else -> cancelledBookings
        }

        if (isLoading && currentBookings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else if (currentBookings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Bookmark, contentDescription = null, tint = TextGray, modifier = Modifier.size(64.dp))
                    Text(text = "No bookings", fontSize = 16.sp, color = TextGray, modifier = Modifier.padding(top = 8.dp))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(currentBookings) { booking ->
                    BookingAdminCard(
                        booking = booking,
                        isPending = selectedBookingTab == 0,
                        isConfirmed = selectedBookingTab == 1,
                        onConfirm = { onConfirm(booking) },
                        onCancel = { onCancel(booking) },
                        formatDate = formatDate
                    )
                }
            }
        }
    }
}

@Composable
fun BookingAdminCard(
    booking: FirestoreBooking,
    isPending: Boolean,
    isConfirmed: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    formatDate: (Long) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                StatusBadgeBooking(status = booking.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            DetailRow(icon = Icons.Filled.Person, label = "Customer", value = booking.userName)
            DetailRow(icon = Icons.Filled.Phone, label = "Phone", value = booking.userPhone)
            DetailRow(icon = Icons.Filled.Email, label = "Email", value = booking.userEmail)
            DetailRow(icon = Icons.Filled.Build, label = "Provider", value = booking.providerName)
            DetailRow(icon = Icons.Filled.DateRange, label = "Date & Time", value = "${booking.date} at ${booking.time}")
            DetailRow(icon = Icons.Filled.LocationOn, label = "Address", value = booking.address)
            DetailRow(icon = Icons.Filled.AttachMoney, label = "Price", value = "৳${booking.providerPrice}/hour")

            if (booking.problemDescription.isNotBlank()) {
                DetailRow(icon = Icons.Filled.Description, label = "Problem", value = booking.problemDescription)
            }

            DetailRow(icon = Icons.Filled.DateRange, label = "Booked On", value = formatDate(booking.createdAt))

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isPending -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Confirm & Notify", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedError),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel", fontSize = 12.sp)
                        }
                    }
                }
                isConfirmed -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirmed - Notification sent to customer", fontSize = 12.sp, color = Color(0xFF2196F3))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadgeBooking(status: String) {
    val (color, bgColor, displayText) = when (status) {
        Constants.BOOKING_PENDING -> Triple(Color(0xFFFF9800), Color(0xFFFFF3E0), "PENDING")
        Constants.BOOKING_CONFIRMED -> Triple(Color(0xFF4CAF50), Color(0xFFE8F5E9), "CONFIRMED")
        Constants.BOOKING_COMPLETED -> Triple(Color(0xFF2196F3), Color(0xFFE3F2FD), "COMPLETED")
        Constants.BOOKING_CANCELLED -> Triple(Color(0xFFF44336), Color(0xFFFFEBEE), "CANCELLED")
        else -> Triple(TextGray, LightGray, status.uppercase())
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
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

@Composable
fun StatusBadge(status: String) {
    val (color, bgColor, displayText) = when (status) {
        Constants.STATUS_PENDING -> Triple(Color(0xFFFF9800), Color(0xFFFFF3E0), "PENDING")
        Constants.STATUS_APPROVED -> Triple(GreenSuccess, Color(0xFFE8F5E9), "APPROVED")
        Constants.STATUS_REJECTED -> Triple(RedError, Color(0xFFFFEBEE), "REJECTED")
        else -> Triple(TextGray, LightGray, status.uppercase())
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = displayText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = OrangePrimary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label:",
            fontSize = 12.sp,
            color = TextGray,
            modifier = Modifier.width(85.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextDark,
            modifier = Modifier.weight(1f)
        )
    }
}