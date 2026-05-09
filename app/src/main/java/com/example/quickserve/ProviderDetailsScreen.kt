package com.example.quickserve

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// Sample providers for ProviderDetailsScreen (copied from HomeScreen)
private val providerDetailsSampleProviders = listOf(
    Provider(1, "Joseph Carl", "AC Specialist", 4.9f, 2100, 45, isTopRated = true),
    Provider(2, "Ava Luna", "AC Specialist", 5.0f, 980, 50),
    Provider(3, "Liam Asher", "HVAC Engineer", 4.8f, 760, 55),
    Provider(4, "Lucas Ezra", "AC Specialist", 4.7f, 430, 40),
    Provider(5, "Mia Carter", "Electrician", 4.9f, 1200, 60),
    Provider(6, "Noah Blake", "Plumber", 4.6f, 890, 35)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDetailsScreen(providerId: String, navController: NavController) {
    val id = providerId.toIntOrNull() ?: 0

    // Find provider from sample data
    val provider = providerDetailsSampleProviders.find { it.id == id }

    if (provider == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Provider not found")
        }
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
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF7622)
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFFAFAFA)),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item {
                ProviderHeaderSection(provider)
            }

            item {
                ProviderStatsSection(provider)
            }

            item {
                ProviderAboutSection(provider)
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        // Navigate to booking screen with provider details
                        // Find service ID based on provider role
                        val serviceCategory = sampleCategories.find { it.name == provider.role }
                        val serviceId = serviceCategory?.id ?: 1

                        navController.navigate("booking/$serviceId/${provider.id}")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7622))
                ) {
                    Icon(Icons.Filled.Bookmark, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Book Now - $${provider.pricePerHour}/hour", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ProviderHeaderSection(provider: Provider) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFF0E8), Color(0xFFFAFAFA))
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF7622)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = provider.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A2E)
            )
            Text(
                text = provider.role,
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Rating Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(5) { index ->
                    Icon(
                        if (index < provider.rating.toInt()) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${provider.rating} (${provider.reviews} reviews)",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ProviderStatsSection(provider: Provider) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatItem(label = "Rating", value = "${provider.rating} ⭐", modifier = Modifier.weight(1f))
        StatItem(label = "Reviews", value = "${provider.reviews}", modifier = Modifier.weight(1f))
        StatItem(label = "Price", value = "$${provider.pricePerHour}/hr", modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 11.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
        }
    }
}

@Composable
fun ProviderAboutSection(provider: Provider) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = "About",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Professional service provider with years of experience in the field. Committed to providing high-quality work and ensuring customer satisfaction. Available for immediate service.",
            fontSize = 14.sp,
            color = Color.Gray,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Services Offered",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1A1A2E)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Service features based on provider role
        val features = when {
            provider.role.contains("Plumber", ignoreCase = true) -> listOf(
                "• Pipe repair and installation",
                "• Leak detection and fixing",
                "• Drain cleaning",
                "• Water heater installation"
            )
            provider.role.contains("Electrician", ignoreCase = true) -> listOf(
                "• Wiring and rewiring",
                "• Circuit breaker repair",
                "• Lighting installation",
                "• Electrical safety inspection"
            )
            provider.role.contains("AC", ignoreCase = true) -> listOf(
                "• AC repair and maintenance",
                "• Gas refilling",
                "• Installation service",
                "• Annual maintenance contract"
            )
            else -> listOf(
                "• Professional service",
                "• Quality work guaranteed",
                "• Timely service",
                "• Customer satisfaction"
            )
        }

        features.forEach { feature ->
            Text(
                text = feature,
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}
