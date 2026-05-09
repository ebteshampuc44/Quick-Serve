package com.example.quickserve

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.quickserve.ui.theme.QuickServeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuickServeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val startDestination = when {
        currentUser != null && currentUser.email == "admin@quickserve.com" -> "adminApplications"
        currentUser != null && currentUser.isEmailVerified -> "home"
        else -> "login"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth Screens
        composable("login") {
            LoginScreen(navController)
        }
        composable("signup") {
            SignupScreen(navController)
        }

        // Home Screen
        composable("home") {
            HomeScreen(navController)
        }

        // Service Details Screen (Category wise)
        composable("serviceDetails/{categoryId}") { backStackEntry ->
            ServiceDetailsScreen(
                categoryId = backStackEntry.arguments?.getString("categoryId") ?: "0",
                navController = navController
            )
        }

        // Provider Details Screen (for sample providers - backward compatibility)
        composable("providerDetails/{providerId}") { backStackEntry ->
            ProviderDetailsScreen(
                providerId = backStackEntry.arguments?.getString("providerId") ?: "0",
                navController = navController
            )
        }

        // Worker Provider Details Screen (for approved workers from Firebase)
        composable("workerProviderDetails/{workerId}/{providerId}") { backStackEntry ->
            WorkerProviderDetailsScreen(
                workerId = backStackEntry.arguments?.getString("workerId") ?: "",
                providerId = backStackEntry.arguments?.getString("providerId") ?: "0",
                navController = navController
            )
        }

        // Booking Screen (supports both sample provider ID and worker ID)
        composable("booking/{serviceId}/{providerId}") { backStackEntry ->
            BookingScreen(
                navController = navController,
                serviceId = backStackEntry.arguments?.getString("serviceId"),
                providerId = backStackEntry.arguments?.getString("providerId")
            )
        }

        // Apply Worker Screen
        composable("applyWorker") {
            ApplyWorkerScreen(navController = navController)
        }

        // All Services Screen
        composable("allServices") {
            AllServicesScreen(navController)
        }

        // All Providers Screen
        composable("allProviders") {
            AllProvidersScreen(navController)
        }

        // Booking History Screen
        composable("bookingHistory") {
            BookingHistoryScreen(navController)
        }

        // Booking Details Screen
        composable("bookingDetails/{bookingId}") { backStackEntry ->
            BookingDetailsScreen(
                bookingId = backStackEntry.arguments?.getString("bookingId") ?: "",
                navController = navController
            )
        }

        // Admin Applications Screen
        composable("adminApplications") {
            AdminApplicationsScreen(navController)
        }

        // Notifications Screen
        composable("notifications") {
            NotificationsScreen(navController)
        }
    }
}

// Colors
private val OrangePrimary = Color(0xFFFF7622)
private val OrangeLight = Color(0xFFFFF0E8)
private val TextDark = Color(0xFF1A1A2E)
private val TextGray = Color(0xFF8A8A8A)
private val White = Color.White
private val Background = Color(0xFFFAFAFA)
private val LightGray = Color(0xFFF0F2F5)

// All Services Screen
@Composable
fun AllServicesScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp)
    ) {
        Text(
            text = "All Services",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sampleCategories.size) { index ->
                val category = sampleCategories[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate("serviceDetails/${category.id}")
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = White)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(category.color.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                category.icon,
                                contentDescription = null,
                                tint = category.color,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = category.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Text(
                                text = "Professional services available",
                                fontSize = 12.sp,
                                color = TextGray
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextGray
                        )
                    }
                }
            }
        }
    }
}

// All Providers Screen
@Composable
fun AllProvidersScreen(navController: NavController) {
    val database = FirebaseDatabase.getInstance().reference
    var dynamicProviders by remember { mutableStateOf<List<Provider>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    // Load approved workers from Firebase
    LaunchedEffect(Unit) {
        val workersRef = database.child(Constants.WORKERS)

        workersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val providerList = mutableListOf<Provider>()
                var idCounter = 100

                snapshot.children.forEach { child ->
                    val status = child.child("status").getValue(String::class.java) ?: ""

                    if (status == Constants.WORKER_AVAILABLE) {
                        val workerId = child.key ?: ""
                        val fullName = child.child("fullName").getValue(String::class.java) ?: ""
                        val serviceType = child.child("serviceType").getValue(String::class.java) ?: ""
                        val rating = child.child("rating").getValue(Float::class.java) ?: 0f
                        val totalReviews = child.child("totalReviews").getValue(Int::class.java) ?: 0
                        val chargePerHour = child.child("chargePerHour").getValue(Int::class.java) ?: 0
                        val isTopRated = child.child("isTopRated").getValue(Boolean::class.java) ?: false

                        // Find category ID
                        val category = sampleCategories.find { it.name.equals(serviceType, ignoreCase = true) }
                        val categoryId = category?.id ?: 0

                        providerList.add(
                            Provider(
                                id = idCounter++,
                                name = fullName,
                                role = serviceType,
                                rating = rating,
                                reviews = totalReviews,
                                pricePerHour = chargePerHour,
                                isTopRated = isTopRated,
                                workerId = workerId,
                                categoryId = categoryId
                            )
                        )
                    }
                }

                dynamicProviders = providerList
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
                Toast.makeText(context, "Failed to load providers: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp)
    ) {
        Text(
            text = "All Providers",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Show approved workers first
                if (dynamicProviders.isNotEmpty()) {
                    item {
                        Text(
                            text = "Approved Service Providers",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(dynamicProviders) { provider ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate("workerProviderDetails/${provider.workerId}/${provider.id}")
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = White)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(OrangeLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = OrangePrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = provider.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    Text(
                                        text = provider.role,
                                        fontSize = 12.sp,
                                        color = TextGray
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFC107),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = " ${provider.rating}",
                                            fontSize = 12.sp,
                                            color = TextGray
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "$${provider.pricePerHour}/hr",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OrangePrimary
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.PersonOutline,
                                    contentDescription = null,
                                    tint = TextGray,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No approved providers yet",
                                    fontSize = 16.sp,
                                    color = TextGray
                                )
                                Text(
                                    text = "Check back later",
                                    fontSize = 14.sp,
                                    color = TextGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Booking History Screen
@Composable
fun BookingHistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    var bookings by remember { mutableStateOf<List<FirestoreBooking>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            firestore.collection(Constants.FIRESTORE_BOOKINGS)
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        isLoading = false
                        Toast.makeText(context, "Failed to load bookings: ${error.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    val bookingList = mutableListOf<FirestoreBooking>()
                    snapshot?.documents?.forEach { doc ->
                        val booking = doc.toObject(FirestoreBooking::class.java)?.copy(bookingId = doc.id)
                        booking?.let { bookingList.add(it) }
                    }
                    bookings = bookingList
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    fun formatDate(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
        return format.format(date)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp)
    ) {
        Text(
            text = "Booking History",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else if (bookings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No bookings yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextGray
                    )
                    Text(
                        text = "Your bookings will appear here",
                        fontSize = 14.sp,
                        color = TextGray
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(bookings) { booking ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate("bookingDetails/${booking.bookingId}")
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = White)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = booking.serviceName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when(booking.status) {
                                        Constants.BOOKING_PENDING -> Color(0xFFFFF3E0)
                                        Constants.BOOKING_CONFIRMED -> Color(0xFFE8F5E9)
                                        Constants.BOOKING_COMPLETED -> Color(0xFFE3F2FD)
                                        Constants.BOOKING_CANCELLED -> Color(0xFFFFEBEE)
                                        else -> LightGray
                                    }
                                ) {
                                    Text(
                                        text = when(booking.status) {
                                            Constants.BOOKING_PENDING -> "PENDING"
                                            Constants.BOOKING_CONFIRMED -> "CONFIRMED"
                                            Constants.BOOKING_COMPLETED -> "COMPLETED"
                                            Constants.BOOKING_CANCELLED -> "CANCELLED"
                                            else -> booking.status.uppercase()
                                        },
                                        fontSize = 11.sp,
                                        color = when(booking.status) {
                                            Constants.BOOKING_PENDING -> Color(0xFFFF9800)
                                            Constants.BOOKING_CONFIRMED -> Color(0xFF4CAF50)
                                            Constants.BOOKING_COMPLETED -> Color(0xFF2196F3)
                                            Constants.BOOKING_CANCELLED -> Color(0xFFF44336)
                                            else -> TextGray
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Provider: ${booking.providerName}",
                                fontSize = 13.sp,
                                color = TextGray
                            )
                            Text(
                                text = "Date: ${booking.date} at ${booking.time}",
                                fontSize = 12.sp,
                                color = TextGray
                            )
                            Text(
                                text = "Address: ${booking.address}",
                                fontSize = 12.sp,
                                color = TextGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (booking.problemDescription.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Issue: ${booking.problemDescription.take(50)}${if (booking.problemDescription.length > 50) "..." else ""}",
                                    fontSize = 11.sp,
                                    color = TextGray,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}