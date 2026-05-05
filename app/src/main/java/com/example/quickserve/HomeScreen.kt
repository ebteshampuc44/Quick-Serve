package com.example.quickserve

import android.graphics.BitmapFactory
import android.util.Base64
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

// ─── Data Models ─────────────────────────────────────────────────────────────

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
    val isTopRated: Boolean = false
)

// ─── Sample Data ──────────────────────────────────────────────────────────────

val sampleCategories = listOf(
    ServiceCategory(1, "Plumber",          Icons.Filled.Plumbing,    Color(0xFFFF8C42)),
    ServiceCategory(2, "Electrician",      Icons.Filled.ElectricBolt, Color(0xFFFFC107)),
    ServiceCategory(3, "AC & Appliance",   Icons.Filled.AcUnit,       Color(0xFF42A5F5)),
    ServiceCategory(4, "Cleaning",         Icons.Filled.CleaningServices, Color(0xFF66BB6A)),
    ServiceCategory(5, "Carpenter",        Icons.Filled.Handyman,     Color(0xFFAB7942)),
    ServiceCategory(6, "Women Salon",      Icons.Filled.SelfImprovement, Color(0xFFEC407A)),
    ServiceCategory(7, "Men Salon",        Icons.Filled.ContentCut,   Color(0xFF7E57C2)),
    ServiceCategory(8, "Pest Control",     Icons.Filled.BugReport,    Color(0xFF26A69A))
)

val sampleProviders = listOf(
    Provider(1, "Joseph Carl",    "AC Specialist",    4.9f, 2100, 45, isTopRated = true),
    Provider(2, "Ava Luna",       "AC Specialist",    5.0f, 980,  50),
    Provider(3, "Liam Asher",     "HVAC Engineer",    4.8f, 760,  55),
    Provider(4, "Lucas Ezra",     "AC Specialist",    4.7f, 430,  40),
    Provider(5, "Mia Carter",     "Electrician",      4.9f, 1200, 60),
    Provider(6, "Noah Blake",     "Plumber",          4.6f, 890,  35)
)

// ─── Colors ───────────────────────────────────────────────────────────────────

private val OrangePrimary  = Color(0xFFFF7622)
private val OrangeLight    = Color(0xFFFFF0E8)
private val OrangeMedium   = Color(0xFFFFD5B8)
private val TextDark       = Color(0xFF1A1A2E)
private val TextGray       = Color(0xFF8A8A8A)
private val White          = Color.White
private val Background     = Color(0xFFFAFAFA)
private val LightGray      = Color(0xFFF0F2F5)

// Helper function to decode Base64 to ImageBitmap
fun decodeBase64ToImageBitmap(base64: String): androidx.compose.ui.graphics.ImageBitmap? {
    return try {
        val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        bitmap?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

// ─── HomeScreen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("User") }
    var profileImageBase64 by remember { mutableStateOf<String?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showApplyDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance().reference

    // Get current user data from Realtime Database
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userRef = database.child("users").child(currentUser.uid)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val name = snapshot.child("fullName").getValue(String::class.java)
                        if (!name.isNullOrEmpty()) {
                            userName = name.split(" ")[0]
                        } else {
                            userName = currentUser.email?.split("@")?.get(0) ?: "User"
                        }

                        // Load profile image from Base64
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
                navController   = navController,
                searchQuery     = searchQuery,
                onSearchChange  = { searchQuery = it },
                userName        = userName,
                profileImageBase64 = profileImageBase64,
                onLogoutClick   = { showLogoutDialog = true },
                onApplyClick    = { showApplyDialog = true },
                modifier        = Modifier.padding(padding)
            )
            1 -> PlaceholderTab("Services", padding, navController)
            2 -> PlaceholderTab("Tasks / Bookings", padding, navController)
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
                        text = "You can apply to become a worker and offer your services to customers.",
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

// ─── Home Tab Content ─────────────────────────────────────────────────────────

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
    modifier: Modifier = Modifier
) {
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
                onApplyClick = onApplyClick
            )
        }

        item {
            Spacer(Modifier.height(16.dp))
            SearchBarSection(
                query    = searchQuery,
                onChange = onSearchChange,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        item {
            Spacer(Modifier.height(24.dp))
            SectionHeader(
                title    = "Category",
                onSeeAll = {
                    navController.navigate("allServices")
                },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(12.dp))
            CategoryGrid(
                categories  = sampleCategories,
                onCategoryClick = { cat ->
                    navController.navigate("serviceDetails/${cat.id}")
                }
            )
        }

        item {
            Spacer(Modifier.height(24.dp))
            SectionHeader(
                title    = "Specialty services",
                onSeeAll = {
                    navController.navigate("allProviders")
                },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(12.dp))
        }

        items(sampleProviders) { provider ->
            ProviderCard(
                provider = provider,
                onClick  = { navController.navigate("providerDetails/${provider.id}") },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        item {
            Spacer(Modifier.height(16.dp))
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
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Header (Updated with Profile Image) ─────────────────────────────────────

@Composable
fun HomeHeader(
    userName: String,
    profileImageBase64: String?,
    onLogoutClick: () -> Unit,
    onApplyClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(OrangeLight, Background))
            )
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(
            modifier            = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment   = Alignment.CenterVertically
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

                // Profile Image - Shows user photo if available, otherwise default icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(OrangeMedium)
                        .clickable {
                            // Navigate to profile when clicked
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (!profileImageBase64.isNullOrEmpty()) {
                        val imageBitmap = decodeBase64ToImageBitmap(profileImageBase64)
                        if (imageBitmap != null) {
                            androidx.compose.foundation.Image(
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
                text       = "All your services",
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold,
                color      = TextDark
            )
            Text(
                text       = "in one place",
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold,
                color      = TextDark
            )
        }
    }
}

// ─── Search Bar ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarSection(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value          = query,
        onValueChange  = onChange,
        modifier       = modifier.fillMaxWidth(),
        placeholder    = { Text("Search for services...", color = TextGray) },
        leadingIcon    = {
            Icon(Icons.Filled.Search, contentDescription = null, tint = TextGray)
        },
        trailingIcon   = {
            Row {
                Icon(Icons.Filled.Mic, contentDescription = "Voice", tint = TextGray,
                    modifier = Modifier.padding(end = 8.dp))
                Icon(Icons.Filled.Tune, contentDescription = "Filter", tint = TextGray,
                    modifier = Modifier.padding(end = 12.dp))
            }
        },
        shape          = RoundedCornerShape(14.dp),
        colors         = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = OrangePrimary,
            unfocusedBorderColor = Color(0xFFE8E8E8),
            focusedContainerColor   = White,
            unfocusedContainerColor = White
        ),
        singleLine = true
    )
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, onSeeAll: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Text(
            text     = "See all",
            fontSize = 14.sp,
            color    = OrangePrimary,
            modifier = Modifier.clickable { onSeeAll() }
        )
    }
}

// ─── Category Grid ────────────────────────────────────────────────────────────

@Composable
fun CategoryGrid(
    categories: List<ServiceCategory>,
    onCategoryClick: (ServiceCategory) -> Unit
) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(categories) { cat ->
            CategoryChip(cat, onClick = { onCategoryClick(cat) })
        }
    }
}

@Composable
fun CategoryChip(category: ServiceCategory, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(category.color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = category.icon,
                contentDescription = category.name,
                tint               = category.color,
                modifier           = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text      = category.name,
            fontSize  = 11.sp,
            color     = TextDark,
            maxLines  = 2,
            overflow  = TextOverflow.Ellipsis,
            lineHeight = 14.sp
        )
    }
}

// ─── Provider Card ────────────────────────────────────────────────────────────

@Composable
fun ProviderCard(provider: Provider, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(OrangeLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, contentDescription = null,
                    tint = OrangePrimary, modifier = Modifier.size(32.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (provider.isTopRated) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(OrangeLight)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("⭐ Top Service", fontSize = 9.sp, color = OrangePrimary)
                    }
                    Spacer(Modifier.height(2.dp))
                }
                Text(provider.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                Text(provider.role, fontSize = 12.sp, color = TextGray)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null,
                        tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                    Text(" ${provider.rating}  (${provider.reviews})",
                        fontSize = 12.sp, color = TextGray)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("$${provider.pricePerHour}/hr",
                    fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallIconBtn(Icons.Filled.Phone)
                    SmallIconBtn(Icons.Filled.Message)
                }
            }
        }
    }
}

@Composable
fun SmallIconBtn(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(OrangeLight),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(16.dp))
    }
}

// ─── Bottom Navigation ────────────────────────────────────────────────────────

@Composable
fun BottomNavBar(selectedTab: Int, navController: NavController, onTabSelected: (Int) -> Unit) {
    NavigationBar(
        containerColor = White,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple(Icons.Filled.Home,       Icons.Outlined.Home,        "Home"),
            Triple(Icons.Filled.Build,      Icons.Outlined.Build,       "Services"),
            Triple(Icons.Filled.ShoppingBag,Icons.Outlined.ShoppingBag, "Tasks"),
            Triple(Icons.Filled.Person,     Icons.Outlined.Person,      "Profile")
        )
        items.forEachIndexed { index, (filledIcon, outlinedIcon, label) ->
            val selected = selectedTab == index
            NavigationBarItem(
                selected = selected,
                onClick  = { onTabSelected(index) },
                icon     = {
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
                label    = { Text(label, fontSize = 10.sp,
                    color = if (selected) OrangePrimary else TextGray) },
                colors   = NavigationBarItemDefaults.colors(
                    indicatorColor = OrangeLight
                )
            )
        }
    }
}

// ─── Placeholder Tabs ─────────────────────────────────────────────────────────

@Composable
fun PlaceholderTab(name: String, padding: PaddingValues, navController: NavController) {
    Box(
        modifier          = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment  = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextGray)
            Spacer(modifier = Modifier.height(16.dp))
            if (name == "Services") {
                Button(
                    onClick = { navController.navigate("allServices") },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Text("Browse All Services")
                }
            }
            if (name == "Tasks / Bookings") {
                Button(
                    onClick = { navController.navigate("bookingHistory") },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Text("View Bookings")
                }
            }
        }
    }
}