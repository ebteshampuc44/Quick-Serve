package com.example.quickserve

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// Service Details Data Model
data class ServiceDetail(
    val id: Int,
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val description: String,
    val priceRange: String,
    val features: List<String>,
    val providers: List<ServiceProvider>
)

data class ServiceProvider(
    val id: Int,
    val name: String,
    val image: String,
    val rating: Float,
    val reviews: Int,
    val experience: String,
    val pricePerHour: Int,
    val isAvailable: Boolean = true,
    val isTopRated: Boolean = false
)

// Sample Service Providers
val plumberProviders = listOf(
    ServiceProvider(1, "John Smith", "", 4.9f, 1250, "8 years", 45, true, true),
    ServiceProvider(2, "Mike Johnson", "", 4.8f, 890, "6 years", 40, true, false),
    ServiceProvider(3, "David Wilson", "", 4.7f, 560, "5 years", 38, true, false),
    ServiceProvider(4, "Robert Brown", "", 4.6f, 430, "4 years", 35, false, false)
)

val electricianProviders = listOf(
    ServiceProvider(5, "James Anderson", "", 4.9f, 1100, "10 years", 60, true, true),
    ServiceProvider(6, "Michael Martinez", "", 4.8f, 950, "8 years", 55, true, false),
    ServiceProvider(7, "William Taylor", "", 4.7f, 780, "6 years", 50, true, false),
    ServiceProvider(8, "Thomas Thomas", "", 4.5f, 320, "3 years", 45, false, false)
)

val cleaningProviders = listOf(
    ServiceProvider(9, "Sarah Wilson", "", 4.9f, 2100, "7 years", 35, true, true),
    ServiceProvider(10, "Emma Davis", "", 4.8f, 1450, "5 years", 30, true, false),
    ServiceProvider(11, "Lisa Miller", "", 4.7f, 980, "4 years", 28, true, false),
    ServiceProvider(12, "Karen Garcia", "", 4.6f, 670, "3 years", 25, false, false)
)

val acProviders = listOf(
    ServiceProvider(13, "Joseph Carl", "", 4.9f, 2100, "9 years", 55, true, true),
    ServiceProvider(14, "Ava Luna", "", 5.0f, 980, "7 years", 50, true, false),
    ServiceProvider(15, "Liam Asher", "", 4.8f, 760, "6 years", 48, true, false)
)

val carpenterProviders = listOf(
    ServiceProvider(16, "Chris Evans", "", 4.8f, 890, "8 years", 42, true, true),
    ServiceProvider(17, "Paul Walker", "", 4.7f, 670, "5 years", 38, true, false)
)

val salonProviders = listOf(
    ServiceProvider(18, "Maria Garcia", "", 4.9f, 1560, "6 years", 30, true, true),
    ServiceProvider(19, "Sophia Lee", "", 4.8f, 1100, "4 years", 25, true, false)
)

val pestControlProviders = listOf(
    ServiceProvider(20, "Alex Turner", "", 4.7f, 540, "5 years", 40, true, true),
    ServiceProvider(21, "Ryan Cooper", "", 4.6f, 380, "3 years", 35, true, false)
)

// Get providers based on category
fun getProvidersForCategory(categoryId: Int): List<ServiceProvider> {
    return when (categoryId) {
        1 -> plumberProviders      // Plumber
        2 -> electricianProviders  // Electrician
        3 -> acProviders           // AC & Appliance
        4 -> cleaningProviders     // Cleaning
        5 -> carpenterProviders    // Carpenter
        6 -> salonProviders        // Women Salon
        7 -> salonProviders        // Men Salon
        8 -> pestControlProviders  // Pest Control
        else -> emptyList()
    }
}

// Get service details based on category
fun getServiceDetail(categoryId: Int): ServiceDetail {
    val category = sampleCategories.find { it.id == categoryId }
    val providers = getProvidersForCategory(categoryId)

    return when (categoryId) {
        1 -> ServiceDetail(
            id = 1,
            name = "Plumber",
            icon = Icons.Filled.Plumbing,
            color = Color(0xFFFF8C42),
            description = "Professional plumbing services for all your home needs. From fixing leaks to installing new pipes, our expert plumbers ensure quality workmanship.",
            priceRange = "\$35 - \$60 per hour",
            features = listOf(
                "✅ 24/7 Emergency Service",
                "✅ Licensed & Insured",
                "✅ Free Inspection",
                "✅ Quality Parts Guarantee",
                "✅ Same Day Service"
            ),
            providers = providers
        )
        2 -> ServiceDetail(
            id = 2,
            name = "Electrician",
            icon = Icons.Filled.ElectricBolt,
            color = Color(0xFFFFC107),
            description = "Expert electrical services for residential and commercial properties. We handle everything from wiring to panel upgrades.",
            priceRange = "\$50 - \$80 per hour",
            features = listOf(
                "✅ Licensed Electricians",
                "✅ 24/7 Emergency Service",
                "✅ Free Safety Check",
                "✅ Warranty on Work",
                "✅ Upfront Pricing"
            ),
            providers = providers
        )
        3 -> ServiceDetail(
            id = 3,
            name = "AC & Appliance",
            icon = Icons.Filled.AcUnit,
            color = Color(0xFF42A5F5),
            description = "Complete AC repair, maintenance, and installation services. Also repair all major home appliances.",
            priceRange = "\$45 - \$70 per hour",
            features = listOf(
                "✅ Same Day Service",
                "✅ Certified Technicians",
                "✅ Free Diagnostic",
                "✅ 30-Day Warranty",
                "✅ Preventive Maintenance"
            ),
            providers = providers
        )
        4 -> ServiceDetail(
            id = 4,
            name = "Cleaning",
            icon = Icons.Filled.CleaningServices,
            color = Color(0xFF66BB6A),
            description = "Professional home and office cleaning services. Deep cleaning, regular maintenance, and move-in/out cleaning.",
            priceRange = "\$25 - \$45 per hour",
            features = listOf(
                "✅ Eco-Friendly Products",
                "✅ Trained Staff",
                "✅ Flexible Scheduling",
                "✅ 100% Satisfaction",
                "✅ Bonded & Insured"
            ),
            providers = providers
        )
        5 -> ServiceDetail(
            id = 5,
            name = "Carpenter",
            icon = Icons.Filled.Handyman,
            color = Color(0xFFAB7942),
            description = "Custom carpentry, furniture repair, and home improvement services. Expert craftsmanship for all woodworking needs.",
            priceRange = "\$40 - \$65 per hour",
            features = listOf(
                "✅ Custom Furniture",
                "✅ Cabinet Installation",
                "✅ Door & Window Repair",
                "✅ Free Quote",
                "✅ Quality Materials"
            ),
            providers = providers
        )
        6 -> ServiceDetail(
            id = 6,
            name = "Women Salon",
            icon = Icons.Filled.SelfImprovement,
            color = Color(0xFFEC407A),
            description = "Premium salon services including haircut, styling, makeup, and spa treatments at your doorstep.",
            priceRange = "\$30 - \$60 per hour",
            features = listOf(
                "✅ Professional Stylists",
                "✅ Premium Products",
                "✅ Home Service",
                "✅ Bridal Packages",
                "✅ Skin & Hair Care"
            ),
            providers = providers
        )
        7 -> ServiceDetail(
            id = 7,
            name = "Men Salon",
            icon = Icons.Filled.ContentCut,
            color = Color(0xFF7E57C2),
            description = "Professional grooming services for men including haircut, beard styling, and facial treatments.",
            priceRange = "\$20 - \$45 per hour",
            features = listOf(
                "✅ Expert Barbers",
                "✅ Beard Styling",
                "✅ Hair Coloring",
                "✅ Facial Treatments",
                "✅ Home Service"
            ),
            providers = providers
        )
        8 -> ServiceDetail(
            id = 8,
            name = "Pest Control",
            icon = Icons.Filled.BugReport,
            color = Color(0xFF26A69A),
            description = "Effective pest control services for residential and commercial properties. Safe and eco-friendly treatments.",
            priceRange = "\$50 - \$90 per visit",
            features = listOf(
                "✅ Free Inspection",
                "✅ Eco-Friendly Products",
                "✅ Guaranteed Results",
                "✅ Monthly Plans",
                "✅ Emergency Service"
            ),
            providers = providers
        )
        else -> ServiceDetail(
            id = 0,
            name = category?.name ?: "Service",
            icon = category?.icon ?: Icons.Filled.Build,
            color = category?.color ?: Color.Gray,
            description = "Professional service at your doorstep",
            priceRange = "\$30 - \$70 per hour",
            features = listOf(
                "✅ Professional Service",
                "✅ Best Price Guarantee",
                "✅ Same Day Service",
                "✅ Satisfaction Guaranteed"
            ),
            providers = providers
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailsScreen(
    categoryId: String,
    navController: NavController
) {
    val id = categoryId.toIntOrNull() ?: 0
    val serviceDetail = getServiceDetail(id)
    var selectedProvider by remember { mutableStateOf<ServiceProvider?>(null) }
    var showBookingDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Available time slots
    val timeSlots = listOf("9:00 AM", "10:00 AM", "11:00 AM", "12:00 PM", "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = serviceDetail.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = serviceDetail.color
                ),
                actions = {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }
                }
            )
        },
        containerColor = Color(0xFFFAFAFA)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Hero Section
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(serviceDetail.color.copy(alpha = 0.15f), Color.White)
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Service Icon
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(serviceDetail.color.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = serviceDetail.icon,
                                contentDescription = serviceDetail.name,
                                tint = serviceDetail.color,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = serviceDetail.name,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = serviceDetail.color
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Rating Summary
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(5) { index ->
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = if (index < 4) Color(0xFFFFC107) else Color(0xFFE0E0E0),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "4.8 (2,500+ reviews)",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Description Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "About this service",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A2E)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = serviceDetail.description,
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280),
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Price Range Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Price Range",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A2E)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.AttachMoney,
                                contentDescription = null,
                                tint = serviceDetail.color,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = serviceDetail.priceRange,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = serviceDetail.color
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Prices vary based on experience and service complexity",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }

            // Features Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "What we offer",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A2E)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        serviceDetail.features.forEach { feature ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = serviceDetail.color,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = feature,
                                    fontSize = 14.sp,
                                    color = Color(0xFF374151)
                                )
                            }
                        }
                    }
                }
            }

            // Available Providers Section
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Available Providers",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A2E)
                        )
                        Text(
                            text = "${serviceDetail.providers.size} providers",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Providers List
            items(serviceDetail.providers) { provider ->
                ProviderDetailCard(
                    provider = provider,
                    onSelect = {
                        selectedProvider = provider
                        showBookingDialog = true
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }

    // Booking Dialog
    if (showBookingDialog && selectedProvider != null) {
        AlertDialog(
            onDismissRequest = { showBookingDialog = false },
            title = {
                Column {
                    Text(
                        text = "Book Service",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = serviceDetail.color
                    )
                    Text(
                        text = selectedProvider?.name ?: "",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "${serviceDetail.name} Service",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A2E)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Price Display
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Price per hour",
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280)
                        )
                        Text(
                            text = "\$${selectedProvider?.pricePerHour}/hr",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = serviceDetail.color
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Date Picker
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = { selectedDate = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Select Date") },
                        placeholder = { Text("DD/MM/YYYY") },
                        leadingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Time Slots
                    Text(
                        text = "Select Time",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF374151)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(timeSlots) { time ->
                            FilterChip(
                                selected = selectedTime == time,
                                onClick = { selectedTime = time },
                                label = { Text(time, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = serviceDetail.color,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Total Estimate
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(serviceDetail.color.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Estimated Total",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF374151)
                        )
                        Text(
                            text = "\$${selectedProvider?.pricePerHour}/hr + tax",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = serviceDetail.color
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedDate.isBlank()) {
                            Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
                        } else if (selectedTime.isBlank()) {
                            Toast.makeText(context, "Please select a time", Toast.LENGTH_SHORT).show()
                        } else {
                            showBookingDialog = false
                            Toast.makeText(
                                context,
                                "Booking confirmed! ${selectedProvider?.name} will arrive at $selectedTime on $selectedDate",
                                Toast.LENGTH_LONG
                            ).show()
                            // Navigate to bookings or home
                            navController.navigate("home") {
                                popUpTo("serviceDetails/$categoryId") { inclusive = true }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = serviceDetail.color
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirm Booking", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBookingDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Gray
                    )
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun ProviderDetailCard(
    provider: ServiceProvider,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFF0E8)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = Color(0xFFFF7622),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = provider.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1A1A2E)
                    )
                    if (provider.isTopRated) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFFF0E8))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("⭐ Top", fontSize = 9.sp, color = Color(0xFFFF7622))
                        }
                    }
                }

                Text(
                    text = provider.experience + " experience",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = " ${provider.rating}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF374151)
                        )
                        Text(
                            text = " (${provider.reviews})",
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280)
                        )
                    }

                    if (!provider.isAvailable) {
                        Text(
                            text = "• Not Available",
                            fontSize = 11.sp,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }

            // Price & Book Button
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "\$${provider.pricePerHour}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFFFF7622)
                )
                Text(
                    text = "/hour",
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280)
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onSelect,
                    enabled = provider.isAvailable,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF7622),
                        disabledContainerColor = Color(0xFFE0E0E0)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(width = 80.dp, height = 32.dp)
                ) {
                    Text(
                        text = if (provider.isAvailable) "Book" else "Unavailable",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}