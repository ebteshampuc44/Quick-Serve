package com.example.quickserve

import android.Manifest
import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, padding: PaddingValues) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance().reference
    val scope = rememberCoroutineScope()

    var userData by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isUpdating by remember { mutableStateOf(false) }
    var profileImageBase64 by remember { mutableStateOf<String?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditEmailDialog by remember { mutableStateOf(false) }
    var showEditPhoneDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    var newName by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }

    val currentUser = auth.currentUser

    // Function to convert URI to Base64
    fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()

            // Compress bitmap
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()

            // Convert to Base64
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Function to upload profile image to Realtime Database as Base64
    fun uploadProfileImage(imageUri: Uri) {
        scope.launch {
            val user = currentUser ?: return@launch
            isUploadingImage = true
            try {
                val base64Image = uriToBase64(imageUri)
                if (base64Image != null) {
                    val userId = user.uid
                    val userRef = database.child(Constants.USERS).child(userId)
                    userRef.child("profileImageBase64").setValue(base64Image).await()

                    profileImageBase64 = base64Image
                    Toast.makeText(context, "Profile image updated!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to process image", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isUploadingImage = false
            }
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            uploadProfileImage(it)
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            imagePickerLauncher.launch("image/*")
        } else {
            Toast.makeText(context, "Storage permission required", Toast.LENGTH_SHORT).show()
        }
    }

    // Load user data
    LaunchedEffect(Unit) {
        currentUser?.let { user ->
            val userId = user.uid
            val userRef = database.child(Constants.USERS).child(userId)

            userRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val data = mutableMapOf<String, Any?>()
                        snapshot.children.forEach { child ->
                            data[child.key!!] = child.value
                        }
                        userData = data
                        profileImageBase64 = data["profileImageBase64"] as? String
                        newName = data["fullName"] as? String ?: user.displayName ?: ""
                        newPhone = data["phone"] as? String ?: ""
                        newEmail = user.email ?: ""
                    } else {
                        val initialData = mapOf<String, Any>(
                            "userId" to userId,
                            "fullName" to (user.displayName ?: ""),
                            "email" to (user.email ?: ""),
                            "phone" to "",
                            "createdAt" to System.currentTimeMillis()
                        )
                        userRef.setValue(initialData)
                        userData = initialData
                        newName = initialData["fullName"] as? String ?: ""
                        newPhone = initialData["phone"] as? String ?: ""
                        newEmail = user.email ?: ""
                    }
                    isLoading = false
                }

                override fun onCancelled(error: DatabaseError) {
                    isLoading = false
                    Toast.makeText(context, "Failed to load: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } ?: run {
            isLoading = false
        }
    }

    // Helper to decode Base64 to ImageBitmap
    fun decodeBase64ToImageBitmap(base64: String): androidx.compose.ui.graphics.ImageBitmap? {
        return try {
            val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    fun updateName() {
        scope.launch {
            if (newName.isBlank()) {
                Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val user = currentUser ?: return@launch
            isUpdating = true
            try {
                val userId = user.uid
                val userRef = database.child(Constants.USERS).child(userId)
                userRef.child("fullName").setValue(newName).await()

                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build()
                user.updateProfile(profileUpdates).await()

                userData = userData?.toMutableMap()?.apply { put("fullName", newName) }
                Toast.makeText(context, "Name updated!", Toast.LENGTH_SHORT).show()
                showEditNameDialog = false
            } catch (e: Exception) {
                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isUpdating = false
            }
        }
    }

    fun updatePhone() {
        scope.launch {
            val user = currentUser ?: return@launch
            isUpdating = true
            try {
                val userId = user.uid
                val userRef = database.child(Constants.USERS).child(userId)
                userRef.child("phone").setValue(newPhone).await()

                userData = userData?.toMutableMap()?.apply { put("phone", newPhone) }
                Toast.makeText(context, "Phone updated!", Toast.LENGTH_SHORT).show()
                showEditPhoneDialog = false
            } catch (e: Exception) {
                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isUpdating = false
            }
        }
    }

    fun updateEmail() {
        scope.launch {
            val user = currentUser ?: return@launch
            if (newEmail.isBlank() || newEmail == user.email) {
                Toast.makeText(context, "Enter a different email", Toast.LENGTH_SHORT).show()
                return@launch
            }
            isUpdating = true
            try {
                val credential = EmailAuthProvider.getCredential(user.email ?: "", currentPassword)
                user.reauthenticate(credential).await()
                user.verifyBeforeUpdateEmail(newEmail).await()

                val userId = user.uid
                val userRef = database.child(Constants.USERS).child(userId)
                userRef.child("email").setValue(newEmail).await()

                Toast.makeText(context, "Verification sent to $newEmail", Toast.LENGTH_LONG).show()
                showEditEmailDialog = false
                currentPassword = ""
            } catch (e: Exception) {
                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isUpdating = false
            }
        }
    }

    fun changePassword() {
        scope.launch {
            val user = currentUser ?: return@launch
            when {
                currentPassword.isBlank() -> {
                    Toast.makeText(context, "Enter current password", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                newPassword.length < 6 -> {
                    Toast.makeText(context, "Password must be 6+ characters", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                newPassword != confirmPassword -> {
                    Toast.makeText(context, "Passwords don't match", Toast.LENGTH_SHORT).show()
                    return@launch
                }
            }

            isUpdating = true
            try {
                val credential = EmailAuthProvider.getCredential(user.email ?: "", currentPassword)
                user.reauthenticate(credential).await()
                user.updatePassword(newPassword).await()

                Toast.makeText(context, "Password changed!", Toast.LENGTH_SHORT).show()
                showChangePasswordDialog = false
                currentPassword = ""
                newPassword = ""
                confirmPassword = ""
            } catch (e: Exception) {
                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isUpdating = false
            }
        }
    }

    // ======================== UI SECTION ========================

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = OrangePrimary)
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "My Profile",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = OrangePrimary
                    )
                )
            },
            containerColor = Background,
            contentWindowInsets = WindowInsets(
                left = 0.dp,
                top = 0.dp,
                right = 0.dp,
                bottom = 80.dp
            )
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Profile Image Section - Clickable
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(OrangeLight)
                        .clickable {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
                            } else {
                                permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploadingImage) {
                        CircularProgressIndicator(color = OrangePrimary)
                    } else if (!profileImageBase64.isNullOrEmpty()) {
                        val imageBitmap = decodeBase64ToImageBitmap(profileImageBase64!!)
                        if (imageBitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = imageBitmap,
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = "Profile",
                                tint = OrangePrimary,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    } else {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Profile",
                            tint = OrangePrimary,
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }

                Text(
                    text = "Tap to change photo",
                    fontSize = 12.sp,
                    color = TextGray,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = currentUser?.email?.split("@")?.get(0) ?: "User",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Profile Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Personal Information",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        ProfileInfoRow(
                            label = "Full Name",
                            value = newName.ifEmpty { "Not set" },
                            icon = Icons.Filled.Person,
                            onEditClick = { showEditNameDialog = true }
                        )

                        Divider(color = LightGray, thickness = 0.5.dp)

                        ProfileInfoRow(
                            label = "Email",
                            value = newEmail,
                            icon = Icons.Filled.Email,
                            onEditClick = { showEditEmailDialog = true }
                        )

                        Divider(color = LightGray, thickness = 0.5.dp)

                        ProfileInfoRow(
                            label = "Phone Number",
                            value = newPhone.ifEmpty { "Not provided" },
                            icon = Icons.Filled.Phone,
                            onEditClick = { showEditPhoneDialog = true }
                        )

                        Divider(color = LightGray, thickness = 0.5.dp)

                        ProfileInfoRow(
                            label = "Member Since",
                            value = userData?.get("createdAt")?.let {
                                val date = Date(it as Long)
                                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
                            } ?: "Just Joined",
                            icon = Icons.Filled.DateRange,
                            showEditButton = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Security Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Security",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showChangePasswordDialog = true }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(OrangeLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = OrangePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Change Password",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextDark
                                )
                            }
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = TextGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // ======================== DIALOGS ========================

    // Edit Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Name", color = OrangePrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(color = BlackText),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        focusedTextColor = BlackText,
                        unfocusedTextColor = BlackText
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = { updateName() },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = White)
                    } else {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = TextGray)
                }
            }
        )
    }

    // Edit Phone Dialog
    if (showEditPhoneDialog) {
        AlertDialog(
            onDismissRequest = { showEditPhoneDialog = false },
            title = { Text("Edit Phone", color = OrangePrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPhone,
                    onValueChange = { newPhone = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("+880 1XXX-XXXXXX") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(color = BlackText),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        focusedTextColor = BlackText,
                        unfocusedTextColor = BlackText
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = { updatePhone() },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = White)
                    } else {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPhoneDialog = false }) {
                    Text("Cancel", color = TextGray)
                }
            }
        )
    }

    // Edit Email Dialog
    if (showEditEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEditEmailDialog = false },
            title = { Text("Edit Email", color = OrangePrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Enter your current password to change email",
                        fontSize = 12.sp,
                        color = TextGray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("New Email") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(color = BlackText),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            focusedTextColor = BlackText,
                            unfocusedTextColor = BlackText
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Current Password") },
                        visualTransformation = if (showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                                Icon(
                                    if (showCurrentPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(color = BlackText),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            focusedTextColor = BlackText,
                            unfocusedTextColor = BlackText
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { updateEmail() },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = White)
                    } else {
                        Text("Update Email")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEditEmailDialog = false
                    currentPassword = ""
                }) {
                    Text("Cancel", color = TextGray)
                }
            }
        )
    }

    // Change Password Dialog
    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Change Password", color = OrangePrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Current Password") },
                        visualTransformation = if (showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                                Icon(
                                    if (showCurrentPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(color = BlackText),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            focusedTextColor = BlackText,
                            unfocusedTextColor = BlackText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showNewPassword = !showNewPassword }) {
                                Icon(
                                    if (showNewPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(color = BlackText),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            focusedTextColor = BlackText,
                            unfocusedTextColor = BlackText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(color = BlackText),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            focusedTextColor = BlackText,
                            unfocusedTextColor = BlackText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { changePassword() },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = White)
                    } else {
                        Text("Change Password")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showChangePasswordDialog = false
                    currentPassword = ""
                    newPassword = ""
                    confirmPassword = ""
                }) {
                    Text("Cancel", color = TextGray)
                }
            }
        )
    }
}

@Composable
fun ProfileInfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onEditClick: () -> Unit = {},
    showEditButton: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(OrangeLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = TextGray
                )
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDark,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }

        if (showEditButton) {
            TextButton(onClick = onEditClick) {
                Text("Edit", color = OrangePrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}