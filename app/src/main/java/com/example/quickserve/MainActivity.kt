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

    // Check if user is already logged in and verified
    val startDestination = if (auth.currentUser != null && auth.currentUser?.isEmailVerified == true) "home" else "login"

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

        // Service Details Screen
        composable("serviceDetails/{categoryId}") { backStackEntry ->
            ServiceDetailsScreen(
                categoryId = backStackEntry.arguments?.getString("categoryId") ?: "0",
                navController = navController
            )
        }

        // Provider Details Screen
        composable("providerDetails/{providerId}") { backStackEntry ->
            ProviderDetailsScreen(
                providerId = backStackEntry.arguments?.getString("providerId") ?: "0",
                navController = navController
            )
        }

        // Booking Screen
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
    }
}

// Colors
private val OrangePrimary  = Color(0xFFFF7622)
private val OrangeLight    = Color(0xFFFFF0E8)
private val TextDark       = Color(0xFF1A1A2E)
private val TextGray       = Color(0xFF8A8A8A)
private val White          = Color.White
private val Background     = Color(0xFFFAFAFA)
private val LightGray      = Color(0xFFF0F2F5)

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

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sampleProviders.size) { index ->
                val provider = sampleProviders[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate("providerDetails/${provider.id}")
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
        }
    }
}

// Booking History Screen (Using Realtime Database)
@Composable
fun BookingHistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance().reference
    var bookings by remember { mutableStateOf<List<Map<String, Any>>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val bookingsRef = database.child("bookings")

            bookingsRef.orderByChild("userId").equalTo(currentUser.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val bookingList = mutableListOf<Map<String, Any>>()
                        snapshot.children.forEach { child ->
                            val data = mutableMapOf<String, Any>()
                            child.children.forEach { field ->
                                data[field.key!!] = field.value ?: ""
                            }
                            // Add the booking ID as well
                            data["bookingId"] = child.key ?: ""
                            bookingList.add(data)
                        }
                        // Sort by createdAt descending
                        bookings = bookingList.sortedByDescending { it["createdAt"] as? Long ?: 0 }
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
        } else if (bookings.isNullOrEmpty()) {
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
                items(bookings!!.size) { index ->
                    val booking = bookings!![index]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                    text = booking["serviceName"] as? String ?: "Service",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when(booking["status"]) {
                                        "pending" -> Color(0xFFFFF3E0)
                                        "confirmed" -> Color(0xFFE8F5E9)
                                        "completed" -> Color(0xFFE3F2FD)
                                        "cancelled" -> Color(0xFFFFEBEE)
                                        else -> LightGray
                                    }
                                ) {
                                    Text(
                                        text = booking["status"] as? String ?: "pending",
                                        fontSize = 11.sp,
                                        color = when(booking["status"]) {
                                            "pending" -> Color(0xFFFF9800)
                                            "confirmed" -> Color(0xFF4CAF50)
                                            "completed" -> Color(0xFF2196F3)
                                            "cancelled" -> Color(0xFFF44336)
                                            else -> TextGray
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Provider: ${booking["providerName"]}",
                                fontSize = 13.sp,
                                color = TextGray
                            )
                            Text(
                                text = "Date: ${booking["date"]} at ${booking["time"]}",
                                fontSize = 12.sp,
                                color = TextGray
                            )
                            Text(
                                text = "Address: ${booking["address"]}",
                                fontSize = 12.sp,
                                color = TextGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Show problem description if available
                            val problem = booking["problemDescription"] as? String
                            if (!problem.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Issue: ${problem.take(50)}${if (problem.length > 50) "..." else ""}",
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