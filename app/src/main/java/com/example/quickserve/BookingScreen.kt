package com.example.quickserve

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val OrangePrimary = Color(0xFFFF7622)
private val OrangeLight = Color(0xFFFFF0E8)
private val TextDark = Color(0xFF1A1A2E)
private val TextGray = Color(0xFF6B7280)
private val LightGray = Color(0xFFF0F2F5)
private val White = Color.White
private val Background = Color(0xFFFAFAFA)
private val BlackText = Color(0xFF000000)

private data class BookingServiceDetail(
    val id: Int,
    val name: String,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val priceRange: String
)

private data class BookingProviderDetail(
    val id: Int,
    val name: String,
    val rating: Float,
    val reviews: Int,
    val experience: String,
    val pricePerHour: Int,
    val isAvailable: Boolean = true,
    val isTopRated: Boolean = false
)

private val bookingSampleProviders = listOf(
    Provider(1, "Joseph Carl", "AC Specialist", 4.9f, 2100, 45, isTopRated = true),
    Provider(2, "Ava Luna", "AC Specialist", 5.0f, 980, 50),
    Provider(3, "Liam Asher", "HVAC Engineer", 4.8f, 760, 55),
    Provider(4, "Lucas Ezra", "AC Specialist", 4.7f, 430, 40),
    Provider(5, "Mia Carter", "Electrician", 4.9f, 1200, 60),
    Provider(6, "Noah Blake", "Plumber", 4.6f, 890, 35)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun BookingScreen(
    navController: NavController,
    serviceId: String?,
    providerId: String?
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance().reference
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var serviceDetail by remember { mutableStateOf<BookingServiceDetail?>(null) }
    var providerDetail by remember { mutableStateOf<BookingProviderDetail?>(null) }
    var userAddress by remember { mutableStateOf("") }
    var userFullAddress by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var problemDescription by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showMapDialog by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }

    var userFullName by remember { mutableStateOf("") }
    var userPhone by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    val currentUser = auth.currentUser

    // Location permission state
    var locationPermissionGranted by remember { mutableStateOf(false) }
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    // Map state
    val singapore = LatLng(23.8103, 90.4125) // Dhaka center
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(singapore, 12f)
    }
    var mapProperties by remember { mutableStateOf(MapProperties(isMyLocationEnabled = true)) }
    var mapUiSettings by remember { mutableStateOf(MapUiSettings(zoomControlsEnabled = true)) }

    // Check permission
    LaunchedEffect(Unit) {
        locationPermissionGranted = LocationUtils.hasLocationPermission(context)
        if (!locationPermissionGranted) {
            locationPermission.launchPermissionRequest()
        } else {
            // Get current location
            val location = LocationUtils.getCurrentLocation(context)
            location?.let {
                selectedLocation = it
                cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 15f)
                // Get address from location
                LocationUtils.getAddressFromLocation(context, it.latitude, it.longitude) { address ->
                    userAddress = address
                }
            }
        }
    }

    // Watch permission result
    LaunchedEffect(locationPermission.status) {
        locationPermissionGranted = locationPermission.status.isGranted
        if (locationPermissionGranted) {
            val location = LocationUtils.getCurrentLocation(context)
            location?.let {
                selectedLocation = it
                cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 15f)
            }
        }
    }

    val isWorkerId = providerId?.toIntOrNull() == null && !providerId.isNullOrBlank()

    fun loadWorkerDetails(workerId: String, onResult: (Worker) -> Unit) {
        val workersRef = database.child(Constants.WORKERS).child(workerId)
        workersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val worker = Worker(
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
                        isTopRated = snapshot.child("isTopRated").getValue(Boolean::class.java) ?: false
                    )
                    onResult(worker)
                } else {
                    Toast.makeText(context, "Worker not found", Toast.LENGTH_SHORT).show()
                    navController.navigateUp()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load worker: ${error.message}", Toast.LENGTH_SHORT).show()
                navController.navigateUp()
            }
        })
    }

    fun getBookingServiceDetail(categoryId: Int): BookingServiceDetail {
        val category = sampleCategories.find { it.id == categoryId }
        return BookingServiceDetail(
            id = categoryId,
            name = category?.name ?: "Service",
            color = category?.color ?: OrangePrimary,
            icon = category?.icon ?: Icons.Filled.Build,
            priceRange = when (categoryId) {
                1 -> "$35 - $60 per hour"
                2 -> "$50 - $80 per hour"
                3 -> "$45 - $70 per hour"
                4 -> "$25 - $45 per hour"
                5 -> "$40 - $65 per hour"
                6, 7 -> "$30 - $60 per hour"
                8 -> "$50 - $90 per visit"
                else -> "$30 - $70 per hour"
            }
        )
    }

    LaunchedEffect(Unit) {
        if (currentUser != null) {
            userEmail = currentUser.email ?: ""
            val userRef = database.child(Constants.USERS).child(currentUser.uid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        userFullName = snapshot.child("fullName").getValue(String::class.java) ?: ""
                        userPhone = snapshot.child("phone").getValue(String::class.java) ?: ""
                    }
                    val serviceIdInt = serviceId?.toIntOrNull() ?: 0
                    serviceDetail = getBookingServiceDetail(serviceIdInt)
                    if (isWorkerId && providerId != null) {
                        loadWorkerDetails(providerId) { worker ->
                            providerDetail = BookingProviderDetail(
                                id = 0,
                                name = worker.fullName,
                                rating = worker.rating,
                                reviews = worker.totalReviews,
                                experience = worker.experience,
                                pricePerHour = worker.chargePerHour,
                                isAvailable = worker.status == Constants.WORKER_AVAILABLE,
                                isTopRated = worker.isTopRated
                            )
                            isLoading = false
                        }
                    } else {
                        val providerIdInt = providerId?.toIntOrNull() ?: 0
                        val sampleProvider = bookingSampleProviders.find { it.id == providerIdInt }
                        if (sampleProvider != null) {
                            providerDetail = BookingProviderDetail(
                                id = sampleProvider.id,
                                name = sampleProvider.name,
                                rating = sampleProvider.rating,
                                reviews = sampleProvider.reviews,
                                experience = sampleProvider.role,
                                pricePerHour = sampleProvider.pricePerHour,
                                isAvailable = true,
                                isTopRated = sampleProvider.isTopRated
                            )
                        }
                        isLoading = false
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    isLoading = false
                    Toast.makeText(context, "Error loading data: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            isLoading = false
            Toast.makeText(context, "Please login to continue", Toast.LENGTH_SHORT).show()
            navController.navigate("login")
        }
    }

    fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return format.format(date)
    }

    fun confirmBooking() {
        when {
            userAddress.isBlank() -> {
                Toast.makeText(context, "Please select your location from map", Toast.LENGTH_SHORT).show()
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
                val bookingId = UUID.randomUUID().toString()
                val currentTime = System.currentTimeMillis()
                val bookingData = FirestoreBooking(
                    bookingId = bookingId,
                    userId = currentUser?.uid ?: "",
                    userName = userFullName,
                    userEmail = userEmail,
                    userPhone = userPhone,
                    serviceId = serviceId ?: "",
                    serviceName = serviceDetail?.name ?: "",
                    providerId = providerId ?: "",
                    providerName = providerDetail?.name ?: "",
                    providerPrice = providerDetail?.pricePerHour ?: 0,
                    address = userAddress,
                    fullAddress = "${userAddress}\nLat: ${selectedLocation?.latitude ?: ""}, Lng: ${selectedLocation?.longitude ?: ""}",
                    date = selectedDate,
                    time = selectedTime,
                    problemDescription = problemDescription,
                    status = Constants.BOOKING_PENDING,
                    createdAt = currentTime,
                    updatedAt = currentTime
                )
                firestore.collection(Constants.FIRESTORE_BOOKINGS)
                    .document(bookingId)
                    .set(bookingData)
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

    val datePickerState = rememberDatePickerState()

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = OrangePrimary)
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "Confirm Booking", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = White)
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = OrangePrimary)
                )
            },
            containerColor = Background
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Selected Service
                item {
                    BookingSectionCard(title = "Selected Service", icon = Icons.Filled.Build) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = serviceDetail?.name ?: "Service", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                Text(text = serviceDetail?.priceRange ?: "", fontSize = 13.sp, color = TextGray)
                            }
                            if (serviceDetail != null) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(serviceDetail!!.color.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = serviceDetail!!.icon, contentDescription = null, tint = serviceDetail!!.color, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }

                // Selected Provider
                item {
                    BookingSectionCard(title = "Selected Provider", icon = Icons.Filled.Person) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = providerDetail?.name ?: "Provider", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                                    Text(text = " ${providerDetail?.rating ?: 0}", fontSize = 12.sp, color = TextGray)
                                    Text(text = " • ${providerDetail?.experience ?: ""}", fontSize = 12.sp, color = TextGray)
                                }
                                Text(text = "$${providerDetail?.pricePerHour ?: 0}/hour", fontSize = 13.sp, color = OrangePrimary, fontWeight = FontWeight.Medium)
                            }
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(24.dp))
                        }
                    }
                }

                // Service Address with Map Button
                item {
                    BookingSectionCard(title = "Service Address", icon = Icons.Filled.LocationOn) {
                        Column {
                            OutlinedTextField(
                                value = userAddress,
                                onValueChange = { userAddress = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Street Address", color = TextGray) },
                                placeholder = { Text("House #, Road #, Area") },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = false,
                                textStyle = LocalTextStyle.current.copy(color = BlackText),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangePrimary,
                                    unfocusedBorderColor = LightGray,
                                    focusedTextColor = BlackText,
                                    unfocusedTextColor = BlackText
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Map Button
                            Button(
                                onClick = { showMapDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeLight),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Map, contentDescription = null, tint = OrangePrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select Location on Map", color = OrangePrimary, fontWeight = FontWeight.Medium)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = userFullAddress,
                                onValueChange = { userFullAddress = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Full Address (City, District)", color = TextGray) },
                                placeholder = { Text("City, District, Post Code") },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = false,
                                textStyle = LocalTextStyle.current.copy(color = BlackText),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangePrimary,
                                    unfocusedBorderColor = LightGray,
                                    focusedTextColor = BlackText,
                                    unfocusedTextColor = BlackText
                                )
                            )
                        }
                    }
                }

                // Date Selection
                item {
                    BookingSectionCard(title = "Select Date", icon = Icons.Filled.DateRange) {
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = LightGray)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = if (selectedDate.isBlank()) "Choose a date" else selectedDate, color = if (selectedDate.isBlank()) TextGray else BlackText, fontSize = 14.sp)
                                }
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = TextGray)
                            }
                        }
                    }
                }

                // Time Selection
                item {
                    BookingSectionCard(title = "Select Time", icon = Icons.Filled.Schedule) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val timeSlots = listOf("9:00 AM", "10:00 AM", "11:00 AM", "12:00 PM", "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM")
                            items(timeSlots.size) { index ->
                                val time = timeSlots[index]
                                FilterChip(
                                    selected = selectedTime == time,
                                    onClick = { selectedTime = time },
                                    label = { Text(time, fontSize = 13.sp, color = if (selectedTime == time) White else BlackText) },
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
                    BookingSectionCard(title = "Problem Description", icon = Icons.Filled.Description) {
                        OutlinedTextField(
                            value = problemDescription,
                            onValueChange = { problemDescription = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Describe your problem", color = TextGray) },
                            placeholder = {
                                Text(
                                    "Please describe the issue in detail...\ne.g., Leaking pipe, AC not cooling, etc.",
                                    fontSize = 13.sp, color = TextGray
                                )
                            },
                            minLines = 4,
                            maxLines = 6,
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
                }

                // Price Summary
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Price Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Service Charge", fontSize = 14.sp, color = TextGray)
                                Text("$${providerDetail?.pricePerHour ?: 0}/hour", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Service Fee", fontSize = 14.sp, color = TextGray)
                                Text("Free", fontSize = 14.sp, color = OrangePrimary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = LightGray, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Estimate", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                Text("$${providerDetail?.pricePerHour ?: 0}/hour + tax", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OrangePrimary)
                            }
                        }
                    }
                }
            }
        }

        // Bottom Button
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 16.dp), contentAlignment = Alignment.BottomCenter) {
            Button(
                onClick = { confirmBooking() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, disabledContainerColor = OrangePrimary.copy(alpha = 0.6f)),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Confirm Booking", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Location Picker Dialog
    if (showMapDialog) {
        AlertDialog(
            onDismissRequest = { showMapDialog = false },
            title = { Text("Select Your Location", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OrangePrimary) },
            text = {
                Box(
                    modifier = Modifier
                        .height(400.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = mapProperties,
                        uiSettings = mapUiSettings,
                        onMapClick = { latLng ->
                            selectedLocation = latLng
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
                            LocationUtils.getAddressFromLocation(context, latLng.latitude, latLng.longitude) { address ->
                                userAddress = address
                            }
                        }
                    ) {
                        selectedLocation?.let {
                            Marker(
                                state = MarkerState(position = it),
                                title = "Selected Location",
                                snippet = "Your service address"
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showMapDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirm Location")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMapDialog = false }) {
                    Text("Cancel", color = TextGray)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Icon(icon, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            }
            content()
        }
    }
}