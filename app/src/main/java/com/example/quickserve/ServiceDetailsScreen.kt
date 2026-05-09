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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

private val OrangePrimary = Color(0xFFFF7622)
private val TextDark = Color(0xFF1A1A2E)
private val TextGray = Color(0xFF6B7280)
private val White = Color.White

// Service Details Data Model
data class ServiceDetail(
    val id: Int,
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val description: String,
    val priceRange: String,
    val features: List<String>
)

// Get service details based on category
fun getServiceDetailOnly(categoryId: Int): ServiceDetail {
    val category = sampleCategories.find { it.id == categoryId }

    return when (categoryId) {
        1 -> ServiceDetail(
            id = 1,
            name = "Plumber",
            icon = Icons.Filled.Plumbing,
            color = Color(0xFFFF8C42),
            description = "Professional plumbing services for all your home needs. From fixing leaks to installing new pipes, our expert plumbers ensure quality workmanship.",
            priceRange = "$35 - $60 per hour",
            features = listOf(
                "✅ 24/7 Emergency Service",
                "✅ Licensed & Insured",
                "✅ Free Inspection",
                "✅ Quality Parts Guarantee",
                "✅ Same Day Service"
            )
        )
        2 -> ServiceDetail(
            id = 2,
            name = "Electrician",
            icon = Icons.Filled.ElectricBolt,
            color = Color(0xFFFFC107),
            description = "Expert electrical services for residential and commercial properties. We handle everything from wiring to panel upgrades.",
            priceRange = "$50 - $80 per hour",
            features = listOf(
                "✅ Licensed Electricians",
                "✅ 24/7 Emergency Service",
                "✅ Free Safety Check",
                "✅ Warranty on Work",
                "✅ Upfront Pricing"
            )
        )
        3 -> ServiceDetail(
            id = 3,
            name = "AC & Appliance",
            icon = Icons.Filled.AcUnit,
            color = Color(0xFF42A5F5),
            description = "Complete AC repair, maintenance, and installation services. Also repair all major home appliances.",
            priceRange = "$45 - $70 per hour",
            features = listOf(
                "✅ Same Day Service",
                "✅ Certified Technicians",
                "✅ Free Diagnostic",
                "✅ 30-Day Warranty",
                "✅ Preventive Maintenance"
            )
        )
        4 -> ServiceDetail(
            id = 4,
            name = "Cleaning",
            icon = Icons.Filled.CleaningServices,
            color = Color(0xFF66BB6A),
            description = "Professional home and office cleaning services. Deep cleaning, regular maintenance, and move-in/out cleaning.",
            priceRange = "$25 - $45 per hour",
            features = listOf(
                "✅ Eco-Friendly Products",
                "✅ Trained Staff",
                "✅ Flexible Scheduling",
                "✅ 100% Satisfaction",
                "✅ Bonded & Insured"
            )
        )
        5 -> ServiceDetail(
            id = 5,
            name = "Carpenter",
            icon = Icons.Filled.Handyman,
            color = Color(0xFFAB7942),
            description = "Custom carpentry, furniture repair, and home improvement services. Expert craftsmanship for all woodworking needs.",
            priceRange = "$40 - $65 per hour",
            features = listOf(
                "✅ Custom Furniture",
                "✅ Cabinet Installation",
                "✅ Door & Window Repair",
                "✅ Free Quote",
                "✅ Quality Materials"
            )
        )
        6 -> ServiceDetail(
            id = 6,
            name = "Women Salon",
            icon = Icons.Filled.SelfImprovement,
            color = Color(0xFFEC407A),
            description = "Premium salon services including haircut, styling, makeup, and spa treatments at your doorstep.",
            priceRange = "$30 - $60 per hour",
            features = listOf(
                "✅ Professional Stylists",
                "✅ Premium Products",
                "✅ Home Service",
                "✅ Bridal Packages",
                "✅ Skin & Hair Care"
            )
        )
        7 -> ServiceDetail(
            id = 7,
            name = "Men Salon",
            icon = Icons.Filled.ContentCut,
            color = Color(0xFF7E57C2),
            description = "Professional grooming services for men including haircut, beard styling, and facial treatments.",
            priceRange = "$20 - $45 per hour",
            features = listOf(
                "✅ Expert Barbers",
                "✅ Beard Styling",
                "✅ Hair Coloring",
                "✅ Facial Treatments",
                "✅ Home Service"
            )
        )
        8 -> ServiceDetail(
            id = 8,
            name = "Pest Control",
            icon = Icons.Filled.BugReport,
            color = Color(0xFF26A69A),
            description = "Effective pest control services for residential and commercial properties. Safe and eco-friendly treatments.",
            priceRange = "$50 - $90 per visit",
            features = listOf(
                "✅ Free Inspection",
                "✅ Eco-Friendly Products",
                "✅ Guaranteed Results",
                "✅ Monthly Plans",
                "✅ Emergency Service"
            )
        )
        else -> ServiceDetail(
            id = 0,
            name = category?.name ?: "Service",
            icon = category?.icon ?: Icons.Filled.Build,
            color = category?.color ?: Color.Gray,
            description = "Professional service at your doorstep",
            priceRange = "$30 - $70 per hour",
            features = listOf(
                "✅ Professional Service",
                "✅ Best Price Guarantee",
                "✅ Same Day Service",
                "✅ Satisfaction Guaranteed"
            )
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
    val serviceDetail = getServiceDetailOnly(id)
    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance().reference

    var categoryProviders by remember { mutableStateOf<List<Provider>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load providers for this specific category from Firebase
    LaunchedEffect(categoryId) {
        val workersRef = database.child(Constants.WORKERS)

        workersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val providerList = mutableListOf<Provider>()
                var idCounter = 1000

                snapshot.children.forEach { child ->
                    val status = child.child("status").getValue(String::class.java) ?: ""

                    if (status == Constants.WORKER_AVAILABLE) {
                        val serviceType = child.child("serviceType").getValue(String::class.java) ?: ""
                        val category = sampleCategories.find { it.name.equals(serviceType, ignoreCase = true) }

                        // Only show providers matching this category
                        if (category?.id == id) {
                            val workerId = child.key ?: ""
                            val fullName = child.child("fullName").getValue(String::class.java) ?: ""
                            val rating = child.child("rating").getValue(Float::class.java) ?: 0f
                            val totalReviews = child.child("totalReviews").getValue(Int::class.java) ?: 0
                            val chargePerHour = child.child("chargePerHour").getValue(Int::class.java) ?: 0
                            val isTopRated = child.child("isTopRated").getValue(Boolean::class.java) ?: false

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
                                    categoryId = id
                                )
                            )
                        }
                    }
                }

                // Sort by rating (higher first)
                categoryProviders = providerList.sortedByDescending { it.rating }
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
                Toast.makeText(context, "Failed to load providers: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

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
                            text = "${categoryProviders.size} providers",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }

            // Show loading or providers list
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = serviceDetail.color)
                    }
                }
            } else if (categoryProviders.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.PersonOutline,
                                contentDescription = null,
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No providers available yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF6B7280)
                            )
                            Text(
                                text = "Check back later for service providers",
                                fontSize = 13.sp,
                                color = Color(0xFF9E9E9E)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { navController.navigate("applyWorker") },
                                colors = ButtonDefaults.buttonColors(containerColor = serviceDetail.color),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Become a Provider")
                            }
                        }
                    }
                }
            } else {
                items(categoryProviders) { provider ->
                    ProviderCardForCategory(
                        provider = provider,
                        categoryColor = serviceDetail.color,
                        onBookClick = {
                            navController.navigate("booking/${serviceDetail.id}/${provider.workerId}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProviderCardForCategory(
    provider: Provider,
    categoryColor: Color,
    onBookClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
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
                    tint = OrangePrimary,
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
                            Text("⭐ Top", fontSize = 9.sp, color = OrangePrimary)
                        }
                    }
                }

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
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Available Now",
                    fontSize = 11.sp,
                    color = Color(0xFF4CAF50)
                )
            }

            // Price & Book Button
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${provider.pricePerHour}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = categoryColor
                )
                Text(
                    text = "/hour",
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280)
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onBookClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = categoryColor,
                        disabledContainerColor = Color(0xFFE0E0E0)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(width = 80.dp, height = 32.dp)
                ) {
                    Text("Book", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}