package com.example.quickserve

import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
import com.google.firebase.firestore.FirebaseFirestore

// Data Models
data class ServiceCategory(
    val id: Int,
    val name: String,
    val icon: ImageVector,
    val color: Color
)

data class Provider(
    val id: Int,
    val name: String,
    val role: String,
    val rating: Float,
    val reviews: Int,
    val pricePerHour: Int,
    val isTopRated: Boolean = false,
    val workerId: String = "",
    val categoryId: Int = 0,
    val profileImageBase64: String = ""
)

// Sample Data
val sampleCategories = listOf(
    ServiceCategory(1, "Plumber", Icons.Filled.Plumbing, Color(0xFFFF8C42)),
    ServiceCategory(2, "Electrician", Icons.Filled.ElectricBolt, Color(0xFFFFC107)),
    ServiceCategory(3, "AC & Appliance", Icons.Filled.AcUnit, Color(0xFF42A5F5)),
    ServiceCategory(4, "Cleaning", Icons.Filled.CleaningServices, Color(0xFF66BB6A)),
    ServiceCategory(5, "Carpenter", Icons.Filled.Handyman, Color(0xFFAB7942)),
    ServiceCategory(6, "Women Salon", Icons.Filled.SelfImprovement, Color(0xFFEC407A)),
    ServiceCategory(7, "Men Salon", Icons.Filled.ContentCut, Color(0xFF7E57C2)),
    ServiceCategory(8, "Pest Control", Icons.Filled.BugReport, Color(0xFF26A69A))
)

// Colors
private val OrangePrimary = Color(0xFFFF7622)
private val OrangeLight = Color(0xFFFFF0E8)
private val OrangeMedium = Color(0xFFFFD5B8)
private val TextDark = Color(0xFF1A1A2E)
private val TextGray = Color(0xFF8A8A8A)
private val White = Color.White
private val Background = Color(0xFFFAFAFA)
private val LightGray = Color(0xFFF0F2F5)

// Helper function to get category ID
fun getCategoryId(serviceType: String): Int {
    return when (serviceType) {
        "Plumber" -> 1
        "Electrician" -> 2
        "AC & Appliance" -> 3
        "Cleaning" -> 4
        "Carpenter" -> 5
        "Women Salon" -> 6
        "Men Salon" -> 7
        "Pest Control" -> 8
        else -> 0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("User") }
    var profileImageBase64 by remember { mutableStateOf<String?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showApplyDialog by remember { mutableStateOf(false) }
    val isAdmin by remember { derivedStateOf {
        FirebaseAuth.getInstance().currentUser?.email == "admin@quickserve.com"
    } }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance().reference

    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userRef = database.child(Constants.USERS).child(currentUser.uid)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val name = snapshot.child("fullName").getValue(String::class.java)
                        if (!name.isNullOrEmpty()) {
                            userName = name.split(" ")[0]
                        } else {
                            userName = currentUser.email?.split("@")?.get(0) ?: "User"
                        }
                        val imageBase64 = snapshot.child("profileImageBase64").getValue(String::class.java)
                        profileImageBase64 = imageBase64
                    } else {
                        userName = currentUser.email?.split("@")?.get(0) ?: "User"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    userName = currentUser.email?.split("@")?.get(0) ?: "User"
                }
            })
        }
    }

    Scaffold(
        containerColor = Background,
        bottomBar = { BottomNavBar(selectedTab, navController) { selectedTab = it } }
    ) { padding ->
        when (selectedTab) {
            0 -> HomeTab(
                navController = navController,
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                userName = userName,
                profileImageBase64 = profileImageBase64,
                onLogoutClick = { showLogoutDialog = true },
                onApplyClick = { showApplyDialog = true },
                isAdmin = isAdmin,
                modifier = Modifier.padding(padding)
            )
            1 -> ServicesScreen(navController = navController)
            2 -> TasksScreen(navController = navController)
            3 -> ProfileScreen(navController, padding)
        }
    }

    // Logout Confirmation Dialog
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
                    text = "Are you sure you want to logout?",
                    fontSize = 14.sp,
                    color = TextGray
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        auth.signOut()
                        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = OrangePrimary
                    )
                ) {
                    Text("Yes, Logout", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = TextGray
                    )
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Apply as Worker Dialog
    if (showApplyDialog) {
        AlertDialog(
            onDismissRequest = { showApplyDialog = false },
            title = {
                Text(
                    text = "Become a Service Provider",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangePrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Want to join our team as a service provider?",
                        fontSize = 14.sp,
                        color = TextDark,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select a category from below to apply for that specific service",
                        fontSize = 13.sp,
                        color = TextGray
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showApplyDialog = false
                        navController.navigate("applyWorker")
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = OrangePrimary
                    )
                ) {
                    Text("Apply Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showApplyDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = TextGray
                    )
                ) {
                    Text("Later")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTab(
    navController: NavController,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    userName: String,
    profileImageBase64: String?,
    onLogoutClick: () -> Unit,
    onApplyClick: () -> Unit,
    isAdmin: Boolean,
    modifier: Modifier = Modifier
) {
    var dynamicProviders by remember { mutableStateOf<Map<String, List<Provider>>>(emptyMap()) }
    var isLoadingProviders by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance().reference

    // Load approved workers from Firebase Realtime Database
    LaunchedEffect(Unit) {
        val workersRef = database.child(Constants.WORKERS)

        workersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val providerMap = mutableMapOf<String, MutableList<Provider>>()
                var idCounter = 100

                // Initialize map for each category
                Constants.SERVICE_CATEGORIES.forEach { categoryName ->
                    providerMap[categoryName] = mutableListOf()
                }

                snapshot.children.forEach { child ->
                    val status = child.child("status").getValue(String::class.java) ?: ""

                    // Only show approved workers that are available
                    if (status == Constants.WORKER_AVAILABLE) {
                        val workerId = child.key ?: return@forEach
                        val fullName = child.child("fullName").getValue(String::class.java) ?: ""
                        val serviceType = child.child("serviceType").getValue(String::class.java) ?: ""
                        val rating = child.child("rating").getValue(Float::class.java) ?: 0f
                        val totalReviews = child.child("totalReviews").getValue(Int::class.java) ?: 0
                        val chargePerHour = child.child("chargePerHour").getValue(Int::class.java) ?: 0
                        val isTopRated = child.child("isTopRated").getValue(Boolean::class.java) ?: false
                        val workerImage = child.child("profileImageBase64").getValue(String::class.java) ?: ""

                        val provider = Provider(
                            id = idCounter++,
                            name = fullName,
                            role = serviceType,
                            rating = rating,
                            reviews = totalReviews,
                            pricePerHour = chargePerHour,
                            isTopRated = isTopRated,
                            workerId = workerId,
                            categoryId = getCategoryId(serviceType),
                            profileImageBase64 = workerImage
                        )

                        // Add to category-specific list
                        if (providerMap.containsKey(serviceType)) {
                            providerMap[serviceType]?.add(provider)
                        }
                    }
                }

                dynamicProviders = providerMap
                isLoadingProviders = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoadingProviders = false
                Toast.makeText(context, "Failed to load providers: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    if (isLoadingProviders) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = OrangePrimary)
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Background),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            HomeHeader(
                userName = userName,
                profileImageBase64 = profileImageBase64,
                onLogoutClick = onLogoutClick,
                onApplyClick = onApplyClick,
                isAdmin = isAdmin,
                navController = navController
            )
        }

        item {
            Spacer(Modifier.height(16.dp))
            SearchBarSection(
                query = searchQuery,
                onChange = onSearchChange,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // Show only categories that have providers
        items(Constants.SERVICE_CATEGORIES) { categoryName ->
            val categoryProviders = dynamicProviders[categoryName] ?: emptyList()

            // Get category color and icon
            val category = sampleCategories.find { it.name == categoryName }

            if (categoryProviders.isNotEmpty() && category != null) {
                Column(
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    SectionHeader(
                        title = categoryName,
                        onSeeAll = {
                            navController.navigate("serviceDetails/${category.id}")
                        },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(categoryProviders.take(5)) { provider ->
                            ProviderCardSmall(
                                provider = provider,
                                onClick = {
                                    navController.navigate("workerProviderDetails/${provider.workerId}/${provider.id}")
                                }
                            )
                        }
                    }
                }
            }
        }

        // Become a Provider Banner
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clickable { onApplyClick() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = OrangeLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(OrangePrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Work,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Become a Service Provider",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "Join our team and grow your business",
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = OrangePrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ProviderCardSmall(
    provider: Provider,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Image with Circle Shape
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(OrangeLight),
                contentAlignment = Alignment.Center
            ) {
                if (!provider.profileImageBase64.isNullOrEmpty()) {
                    val imageBitmap = decodeBase64ToImageBitmap(provider.profileImageBase64)
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
                            tint = OrangePrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = provider.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = " ${provider.rating}",
                    fontSize = 11.sp,
                    color = TextGray
                )
                Text(
                    text = " (${provider.reviews})",
                    fontSize = 10.sp,
                    color = TextGray
                )
            }

            Text(
                text = "৳${provider.pricePerHour}/hr",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = OrangePrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun HomeHeader(
    userName: String,
    profileImageBase64: String?,
    onLogoutClick: () -> Unit,
    onApplyClick: () -> Unit,
    isAdmin: Boolean,
    navController: NavController
) {
    var unreadNotificationCount by remember { mutableStateOf(0) }
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    // Load unread notification count
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            firestore.collection(Constants.FIRESTORE_NOTIFICATIONS)
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("isRead", false)
                .addSnapshotListener { snapshot, _ ->
                    unreadNotificationCount = snapshot?.size() ?: 0
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(OrangeLight, Background))
            )
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OrangePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.GridView, contentDescription = null,
                        tint = White, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Welcome", fontSize = 12.sp, color = TextGray)
                    Text(
                        text = userName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Notification Button with Badge
                Box {
                    IconButton(onClick = { navController.navigate("notifications") }) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = OrangePrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Badge for unread notifications
                    if (unreadNotificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .offset(x = 18.dp, y = (-4).dp)
                                .clip(CircleShape)
                                .background(Color.Red),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unreadNotificationCount > 99) "99+" else unreadNotificationCount.toString(),
                                fontSize = 9.sp,
                                color = White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Admin button - only visible for admin users
                if (isAdmin) {
                    IconButton(onClick = { navController.navigate("adminApplications") }) {
                        Icon(
                            Icons.Outlined.AdminPanelSettings,
                            contentDescription = "Admin Panel",
                            tint = OrangePrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                IconButton(onClick = onApplyClick) {
                    Icon(
                        Icons.Outlined.Work,
                        contentDescription = "Apply as Worker",
                        tint = OrangePrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(onClick = onLogoutClick) {
                    Icon(
                        Icons.Outlined.Logout,
                        contentDescription = "Logout",
                        tint = OrangePrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(OrangeMedium),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profileImageBase64.isNullOrEmpty()) {
                        val imageBitmap = decodeBase64ToImageBitmap(profileImageBase64!!)
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "Profile Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = "Avatar",
                                tint = OrangePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Avatar",
                            tint = OrangePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(top = 72.dp)) {
            Text(
                text = "All your services",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = "in one place",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarSection(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search for services...", color = TextGray) },
        textStyle = LocalTextStyle.current.copy(
            color = Color.Black  // This makes the typed text black
        ),
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null, tint = TextGray)
        },
        trailingIcon = {
            Row {
                Icon(Icons.Filled.Mic, contentDescription = "Voice", tint = TextGray,
                    modifier = Modifier.padding(end = 8.dp))
                Icon(Icons.Filled.Tune, contentDescription = "Filter", tint = TextGray,
                    modifier = Modifier.padding(end = 12.dp))
            }
        },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OrangePrimary,
            unfocusedBorderColor = Color(0xFFE8E8E8),
            focusedContainerColor = White,
            unfocusedContainerColor = White,
<<<<<<< HEAD
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
=======
<<<<<<< HEAD
            focusedTextColor = Color.Black,      // When text field is focused
            unfocusedTextColor = Color.Black     // When text field is unfocused
=======
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
>>>>>>> de2d189 (update project)
>>>>>>> 9d1af91a54fbe5428f2be8f0871f6412ab6fc07d
        ),
        singleLine = true
    )
}

@Composable
fun SectionHeader(title: String, onSeeAll: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Text(
            text = "See all",
            fontSize = 14.sp,
            color = OrangePrimary,
            modifier = Modifier.clickable { onSeeAll() }
        )
    }
}

@Composable
fun BottomNavBar(selectedTab: Int, navController: NavController, onTabSelected: (Int) -> Unit) {
    NavigationBar(
        containerColor = White,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple(Icons.Filled.Home, Icons.Outlined.Home, "Home"),
            Triple(Icons.Filled.Build, Icons.Outlined.Build, "Services"),
            Triple(Icons.Filled.ShoppingBag, Icons.Outlined.ShoppingBag, "Tasks"),
            Triple(Icons.Filled.Person, Icons.Outlined.Person, "Profile")
        )
        items.forEachIndexed { index, (filledIcon, outlinedIcon, label) ->
            val selected = selectedTab == index
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(index) },
                icon = {
                    if (index == 2 && selected) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(OrangePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(filledIcon, contentDescription = label,
                                tint = White, modifier = Modifier.size(24.dp))
                        }
                    } else {
                        Icon(
                            if (selected) filledIcon else outlinedIcon,
                            contentDescription = label,
                            tint = if (selected) OrangePrimary else TextGray
                        )
                    }
                },
                label = { Text(label, fontSize = 10.sp,
                    color = if (selected) OrangePrimary else TextGray) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = OrangeLight
                )
            )
        }
    }
}