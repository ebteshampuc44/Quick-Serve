package com.example.quickserve

import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

private val OrangePrimary = Color(0xFFFF7622)
private val OrangeLight = Color(0xFFFFF0E8)
private val TextDark = Color(0xFF1A1A2E)
private val TextGray = Color(0xFF6B7280)
private val White = Color.White
private val Background = Color(0xFFFAFAFA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerProviderDetailsScreen(
    workerId: String,
    providerId: String,
    navController: NavController
) {
    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance().reference
    var worker by remember { mutableStateOf<Worker?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Find the service category based on worker's service type
    val serviceCategory = sampleCategories.find {
        it.name.equals(worker?.serviceType, ignoreCase = true)
    } ?: sampleCategories.first()

    LaunchedEffect(workerId) {
        val workerRef = database.child(Constants.WORKERS).child(workerId)

        workerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    worker = Worker(
                        workerId = snapshot.key ?: "",
                        userId = snapshot.child("userId").getValue(String::class.java) ?: "",
                        fullName = snapshot.child("fullName").getValue(String::class.java) ?: "",
                        email = snapshot.child("email").getValue(String::class.java) ?: "",
                        phone = snapshot.child("phone").getValue(String::class.java) ?: "",
                        serviceType = snapshot.child("serviceType").getValue(String::class.java) ?: "",
                        experience = snapshot.child("experience").getValue(String::class.java) ?: "",
                        location = snapshot.child("location").getValue(String::class.java) ?: "",
                        chargePerHour = snapshot.child("chargePerHour").getValue(Int::class.java) ?: 0,
                        rating = snapshot.child("rating").getValue(Float::class.java) ?: 0f,
                        totalReviews = snapshot.child("totalReviews").getValue(Int::class.java) ?: 0,
                        status = snapshot.child("status").getValue(String::class.java) ?: Constants.WORKER_AVAILABLE,
                        joinedAt = snapshot.child("joinedAt").getValue(Long::class.java) ?: System.currentTimeMillis(),
                        isTopRated = snapshot.child("isTopRated").getValue(Boolean::class.java) ?: false,
                        profileImageBase64 = snapshot.child("profileImageBase64").getValue(String::class.java) ?: ""
                    )
                }
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
                Toast.makeText(context, "Failed to load provider: ${error.message}", Toast.LENGTH_SHORT).show()
                navController.navigateUp()
            }
        })
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

    val currentWorker = worker ?: run {
        Toast.makeText(context, "Provider not found", Toast.LENGTH_SHORT).show()
        navController.navigateUp()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Provider Profile",
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
            // Header Section with Profile Image
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(OrangeLight, Background)
                            )
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Profile Image
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(OrangePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!currentWorker.profileImageBase64.isNullOrEmpty()) {
                                val imageBitmap = decodeBase64ToImageBitmap(currentWorker.profileImageBase64)
                                if (imageBitmap != null) {
                                    Image(
                                        bitmap = imageBitmap,
                                        contentDescription = "Provider Image",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = White,
                                        modifier = Modifier.size(60.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = White,
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = currentWorker.fullName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = currentWorker.serviceType,
                            fontSize = 16.sp,
                            color = TextGray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Rating Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(5) { index ->
                                Icon(
                                    if (index < currentWorker.rating.toInt()) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${currentWorker.rating} (${currentWorker.totalReviews} reviews)",
                                fontSize = 13.sp,
                                color = TextGray
                            )
                        }
                    }
                }
            }

            // Stats Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Experience",
                        value = currentWorker.experience,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Charge",
                        value = "৳${currentWorker.chargePerHour}/hr",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Status",
                        value = currentWorker.status,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // About Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "About",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Professional ${currentWorker.serviceType} with ${currentWorker.experience} of experience. Serving in ${currentWorker.location} area. Committed to providing quality service and customer satisfaction.",
                            fontSize = 14.sp,
                            color = TextGray,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Contact Info Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Contact Information",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Phone,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = currentWorker.phone,
                                fontSize = 14.sp,
                                color = TextDark
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Email,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = currentWorker.email,
                                fontSize = 14.sp,
                                color = TextDark
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = currentWorker.location,
                                fontSize = 14.sp,
                                color = TextDark
                            )
                        }
                    }
                }
            }

            // Book Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        // Navigate to booking screen with worker details
                        // Find service ID based on service type
                        val serviceId = sampleCategories.find {
                            it.name.equals(currentWorker.serviceType, ignoreCase = true)
                        }?.id ?: 0

                        navController.navigate("booking/$serviceId/${currentWorker.workerId}")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Icon(Icons.Filled.Bookmark, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Book Now - ৳${currentWorker.chargePerHour}/hour",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = TextGray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }
    }
}