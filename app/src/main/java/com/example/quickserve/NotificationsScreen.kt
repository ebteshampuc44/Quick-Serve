package com.example.quickserve

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var notifications by remember { mutableStateOf<List<NotificationData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var unreadCount by remember { mutableStateOf(0) }

    // Load notifications
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            firestore.collection(Constants.FIRESTORE_NOTIFICATIONS)
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        isLoading = false
                        return@addSnapshotListener
                    }

                    val notificationList = mutableListOf<NotificationData>()
                    var unread = 0

                    snapshot?.documents?.forEach { doc ->
                        val notification = doc.toObject(NotificationData::class.java)?.copy(id = doc.id)
                        notification?.let {
                            notificationList.add(it)
                            if (!it.isRead) unread++
                        }
                    }

                    notifications = notificationList
                    unreadCount = unread
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    // Mark notification as read
    fun markAsRead(notification: NotificationData) {
        scope.launch {
            try {
                firestore.collection(Constants.FIRESTORE_NOTIFICATIONS)
                    .document(notification.id)
                    .update("isRead", true)
                    .await()
            } catch (e: Exception) {
                // Error handling
            }
        }
    }

    // Mark all as read
    fun markAllAsRead() {
        scope.launch {
            try {
                notifications.filter { !it.isRead }.forEach { notification ->
                    firestore.collection(Constants.FIRESTORE_NOTIFICATIONS)
                        .document(notification.id)
                        .update("isRead", true)
                        .await()
                }
            } catch (e: Exception) {
                // Error handling
            }
        }
    }

    fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val now = Date()
        val diff = now.time - timestamp

        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000} minutes ago"
            diff < 86400000 -> "${diff / 3600000} hours ago"
            else -> {
                val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                format.format(date)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notifications",
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
                    if (unreadCount > 0) {
                        TextButton(onClick = { markAllAsRead() }) {
                            Text("Mark all read", color = White, fontSize = 12.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OrangePrimary
                )
            )
        },
        containerColor = Background
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.NotificationsNone,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No notifications",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextGray
                    )
                    Text(
                        text = "You'll see notifications here when your booking status changes",
                        fontSize = 13.sp,
                        color = TextGray,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications) { notification ->
                    NotificationCard(
                        notification = notification,
                        onClick = {
                            markAsRead(notification)
                            if (notification.bookingId.isNotEmpty()) {
                                navController.navigate("bookingDetails/${notification.bookingId}")
                            }
                        },
                        formatDate = { formatDate(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationData,
    onClick: () -> Unit,
    formatDate: (Long) -> String
) {
    val statusColor = when (notification.bookingStatus) {
        Constants.BOOKING_CONFIRMED -> Color(0xFF4CAF50)
        Constants.BOOKING_CANCELLED -> Color(0xFFF44336)
        Constants.BOOKING_COMPLETED -> Color(0xFF2196F3)
        else -> OrangePrimary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) White else OrangeLight
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (notification.bookingStatus) {
                        Constants.BOOKING_CONFIRMED -> Icons.Filled.CheckCircle
                        Constants.BOOKING_CANCELLED -> Icons.Filled.Cancel
                        else -> Icons.Filled.Notifications
                    },
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = formatDate(notification.createdAt),
                        fontSize = 10.sp,
                        color = TextGray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    fontSize = 12.sp,
                    color = TextGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!notification.isRead) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(OrangePrimary)
                    )
                }
            }
        }
    }
}