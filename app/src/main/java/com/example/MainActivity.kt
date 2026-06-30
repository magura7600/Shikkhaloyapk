package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import com.example.ui.CustomBottomNavigation
import com.example.ui.BottomNavItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

// --- DATA STRUCTURES ---

@Serializable
data class TodoItem(
    val id: Int? = null,
    val name: String,
    val role_tag: String = "general" // Filter tasks by role
)

@Serializable
data class UserProfile(
    val user_id: String,
    val email: String,
    val role: String, // "teacher", "student", or "admin"
    val full_name: String,
    val institution: String,
    val contact: String,
    val uid_code: String,
    val profile_image_url: String? = null,
    val handle: String? = null,
    val description: String? = null,
    val cover_image_url: String? = null,
    val is_banned: Boolean = false
)

// --- SUPABASE CLIENT ---
val supabase = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_KEY
) {
    install(Postgrest)
    install(Auth)
    defaultSerializer = KotlinXSerializer(Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true 
    })
}

// --- APP UI STATE ---
sealed interface AppState {
    object Splash : AppState
    object Login : AppState
    data class Onboarding(val email: String, val userId: String) : AppState
    data class Dashboard(val email: String, val userId: String, val profile: UserProfile) : AppState
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // OneSignal debug logging (optional but highly recommended for setup)
        OneSignal.Debug.logLevel = LogLevel.VERBOSE

        // OneSignal Initialization
        OneSignal.initWithContext(this, "9b18010c-9761-4d89-abfc-ae8a437f4943")

        // Clear temporary PDF cache
        OfflineDownloadManager.clearTemporaryCache(this)

        // Request notification permission
        lifecycleScope.launch {
            OneSignal.Notifications.requestPermission(true)
        }

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF1E3A8A), // Navy Blue
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFEFF6FF),
                    onPrimaryContainer = Color(0xFF1E3A8A),
                    secondary = Color(0xFF0F766E), // Teal Green
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFFECFDF5),
                    onSecondaryContainer = Color(0xFF065F46),
                    tertiary = Color(0xFFF4B400), // Gold / Yellow
                    background = Color(0xFFF8FAFC), // Off-white
                    surface = Color.White,
                    error = Color(0xFFEF4444)
                )
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainAppContent()
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContent() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("shikkhaloy_prefs", Context.MODE_PRIVATE) }
    val coroutineScope = rememberCoroutineScope()
    
    var appState by remember { mutableStateOf<AppState>(AppState.Splash) }
    var showGoogleDialog by remember { mutableStateOf(false) }
    var activeUpdateToPrompt by remember { mutableStateOf<AppUpdate?>(null) }
    var activeNoticeToPrompt by remember { mutableStateOf<AppNotice?>(null) }

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        
        // Check for updates
        try {
            val update = AppUpdateManager.checkForUpdate(context)
            if (update != null) {
                activeUpdateToPrompt = update
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Check for notices
        try {
            val notice = AppNoticeManager.getActiveNotice()
            if (notice != null) {
                activeNoticeToPrompt = notice
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Read cached credentials & sync with Supabase profiles
        val cachedUserId = sharedPrefs.getString("user_id", null)
        val cachedEmail = sharedPrefs.getString("email", null)
        var nextState: AppState = AppState.Login

        if (cachedUserId != null && cachedEmail != null) {
            var dbProfile: UserProfile? = null
            try {
                withContext(Dispatchers.IO) {
                    dbProfile = supabase.from("profiles")
                        .select {
                            filter {
                                eq("user_id", cachedUserId)
                            }
                        }.decodeSingleOrNull<UserProfile>()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            nextState = if (dbProfile != null) {
                AppState.Dashboard(
                    email = cachedEmail,
                    userId = cachedUserId,
                    profile = dbProfile!!
                )
            } else {
                AppState.Onboarding(cachedEmail, cachedUserId)
            }
        }

        // Enforce beautiful minimum splash duration of 2500ms
        val elapsed = System.currentTimeMillis() - startTime
        val remaining = 2500 - elapsed
        if (remaining > 0) {
            kotlinx.coroutines.delay(remaining)
        }

        appState = nextState
    }

    val handleLoginSuccess: (String, String) -> Unit = { email, userId ->
        Toast.makeText(context, "ডাটাবেজ প্রোফাইল যাচাই করা হচ্ছে...", Toast.LENGTH_SHORT).show()
        coroutineScope.launch {
            var dbProfile: UserProfile? = null
            try {
                withContext(Dispatchers.IO) {
                    dbProfile = supabase.from("profiles")
                        .select {
                            filter {
                                eq("user_id", userId)
                            }
                        }.decodeSingleOrNull<UserProfile>()
                }
            } catch (e: Exception) {
                // DB error (table not found or network)
            }

            // Save basic login state
            sharedPrefs.edit()
                .putString("user_id", userId)
                .putString("email", email)
                .apply()

            if (dbProfile != null) {
                appState = AppState.Dashboard(
                    email = email,
                    userId = userId,
                    profile = dbProfile!!
                )
            } else {
                appState = AppState.Onboarding(email, userId)
            }
        }
    }

    val isUserBanned = (appState as? AppState.Dashboard)?.profile?.is_banned == true

    if (isUserBanned) {
        val state = appState as AppState.Dashboard
        BannedScreen(
            email = state.email,
            uid = state.profile.uid_code,
            onLogout = {
                sharedPrefs.edit().clear().apply()
                appState = AppState.Login
            }
        )
    } else {
        Crossfade(targetState = appState, label = "ScreenTransition") { state ->
            when (state) {
                is AppState.Splash -> {
                    ShikkhaloySplashScreen()
                }
                is AppState.Login -> {
                    LoginScreen(
                        onLoginSuccess = handleLoginSuccess,
                        onGoogleClick = { showGoogleDialog = true }
                    )
                }
                is AppState.Onboarding -> {
                    OnboardingScreen(
                        email = state.email,
                        userId = state.userId,
                        onProfileComplete = { profile ->
                            // Cache profile details
                            sharedPrefs.edit()
                                .putString("role", profile.role)
                                .putString("full_name", profile.full_name)
                                .putString("institution", profile.institution)
                                .putString("contact", profile.contact)
                                .putString("uid_code", profile.uid_code)
                                .apply()

                            appState = AppState.Dashboard(state.email, state.userId, profile)
                        }
                    )
                }
                is AppState.Dashboard -> {
                    DashboardScreen(
                        email = state.email,
                        userId = state.userId,
                        profile = state.profile,
                        onLogout = {
                            // Clear login and profile cache
                            sharedPrefs.edit().clear().apply()
                            appState = AppState.Login
                        },
                        onProfileUpdate = { updatedProfile ->
                            sharedPrefs.edit()
                                .putString("full_name", updatedProfile.full_name)
                                .putString("institution", updatedProfile.institution)
                                .putString("contact", updatedProfile.contact)
                                .apply()
                            appState = AppState.Dashboard(state.email, state.userId, updatedProfile)
                        }
                    )
                }
            }
        }
    }

    // Google Sign-In Selector Dialog (Perfect for preview & local emulator testing!)
    if (showGoogleDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Google Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "গুগল অ্যাকাউন্ট নির্বাচন করুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "শিক্ষালয় অ্যাপে সুরক্ষিতভাবে সাইন-ইন করতে আপনার যেকোনো একটি অ্যাকাউন্ট বেছে নিন।",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    
                    // Demo Account 1
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showGoogleDialog = false
                                // Simulate Google Login Success
                                sharedPrefs.edit()
                                    .putString("user_id", "google_user_101")
                                    .putString("email", "fahimmia017740@gmail.com")
                                    .apply()
                                
                                Toast.makeText(context, "গুগল অ্যাকাউন্ট সংযুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
                                handleLoginSuccess("fahimmia017740@gmail.com", "google_user_101")
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("F", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Fahim Mia", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("fahimmia017740@gmail.com", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }

                    // Demo Account 2
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showGoogleDialog = false
                                sharedPrefs.edit()
                                    .putString("user_id", "google_user_202")
                                    .putString("email", "shikkhaloy.demo@gmail.com")
                                    .apply()
                                
                                Toast.makeText(context, "গুগল অ্যাকাউন্ট সংযুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
                                handleLoginSuccess("shikkhaloy.demo@gmail.com", "google_user_202")
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("S", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Shikkhaloy Guest", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("shikkhaloy.demo@gmail.com", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGoogleDialog = false }) {
                    Text("বাতিল করুন")
                }
            }
        )
    }

    if (activeUpdateToPrompt != null) {
        UpdatePromptDialog(
            update = activeUpdateToPrompt!!,
            accentColor = Color(0xFF6366F1),
            onDismiss = { activeUpdateToPrompt = null }
        )
    }

    if (activeNoticeToPrompt != null) {
        EmergencyNoticeDialog(
            notice = activeNoticeToPrompt!!,
            accentColor = Color(0xFF6366F1),
            onDismiss = { activeNoticeToPrompt = null }
        )
    }
}

// --- LOGIN & SIGNUP SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String, String) -> Unit,
    onGoogleClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isLoginTab by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    // Dynamic banner text based on selected tab
    val primaryText = if (isLoginTab) "স্বাগতম শিক্ষালয়ে" else "নতুন অ্যাকাউন্ট তৈরি করুন"
    val secondaryText = if (isLoginTab) "আপনার শিক্ষাগত যাত্রা সহজ করতে লগইন করুন" else "শিক্ষক বা শিক্ষার্থী হিসেবে যুক্ত হতে সাইন-আপ করুন"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Shikkhaloy Elegant Logo Icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = "Shikkhaloy Logo",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "শিক্ষালয়",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF3730A3),
            textAlign = TextAlign.Center
        )
        Text(
            text = "ডিজিটাল স্কুল ম্যানেজমেন্ট প্ল্যাটফর্ম",
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Title and Subtitle Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = primaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = secondaryText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Tab Selector for Login / Register
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF3F4F6))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isLoginTab) Color.White else Color.Transparent)
                    .clickable { isLoginTab = true }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "লগইন",
                    fontWeight = FontWeight.Bold,
                    color = if (isLoginTab) MaterialTheme.colorScheme.primary else Color.Gray,
                    fontSize = 14.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (!isLoginTab) Color.White else Color.Transparent)
                    .clickable { isLoginTab = false }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "রেজিস্ট্রেশন",
                    fontWeight = FontWeight.Bold,
                    color = if (!isLoginTab) MaterialTheme.colorScheme.primary else Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("ইমেইল অ্যাড্রেস") },
            placeholder = { Text("example@email.com") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("পাসওয়ার্ড") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock") },
            trailingIcon = {
                val icon = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(imageVector = icon, contentDescription = "Password toggle")
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Action Button (Real auth implementation with graceful alert fallback)
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "অনুগ্রহ করে সব তথ্য দিন!", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                coroutineScope.launch {
                    loading = true
                    try {
                        if (!isLoginTab) {
                            // Check if email already has a profile in DB before creating auth user
                            var emailExists = false
                            try {
                                withContext(Dispatchers.IO) {
                                    val countResult = supabase.from("profiles")
                                        .select {
                                            filter {
                                                eq("email", email)
                                            }
                                        }.decodeList<UserProfile>()
                                    emailExists = countResult.isNotEmpty()
                                }
                            } catch (e: Exception) {
                                // If database is not configured or throws error, we can skip and allow sign up
                            }

                            if (emailExists) {
                                Toast.makeText(context, "এই ইমেইল দিয়ে ইতিমধ্যে অ্যাকাউন্ট খোলা হয়েছে! অনুগ্রহ করে লগইন করুন।", Toast.LENGTH_LONG).show()
                                isLoginTab = true // Switch to login tab
                                loading = false
                                return@launch
                            }
                        }

                        withContext(Dispatchers.IO) {
                            if (isLoginTab) {
                                supabase.auth.signInWith(Email) {
                                    this.email = email
                                    this.password = password
                                }
                            } else {
                                supabase.auth.signUpWith(Email) {
                                    this.email = email
                                    this.password = password
                                }
                            }
                        }
                        
                        // Grab current authenticated user ID
                        val sessionUser = supabase.auth.currentSessionOrNull()?.user
                        val userId = sessionUser?.id ?: throw Exception("Authentication failed, user is null")
                        
                        Toast.makeText(context, if (isLoginTab) "লগইন সফল হয়েছে!" else "রেজিস্ট্রেশন সফল হয়েছে!", Toast.LENGTH_SHORT).show()
                        onLoginSuccess(email, userId)
                    } catch (e: Exception) {
                        Toast.makeText(context, "ত্রুটি: ${e.message?.take(50) ?: "লগইন ব্যর্থ হয়েছে। তথ্য যাচাই করুন।"}", Toast.LENGTH_LONG).show()
                    } finally {
                        loading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !loading
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = if (isLoginTab) "লগইন করুন" else "রেজিস্টার করুন",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Divider Line
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
            Text(
                "অথবা",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign-In Trigger Button
        OutlinedButton(
            onClick = onGoogleClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.LightGray)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Google",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "গুগল দিয়ে সাইন-ইন করুন",
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Fast Demo Entry Button
        Text(
            text = "দ্রুত ডেমো মুডে প্রবেশ করুন",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .clickable {
                    onLoginSuccess("fahimmia017740@gmail.com", "google_user_101")
                }
                .padding(8.dp)
        )
    }
}

// --- ONBOARDING & PROFILE COMPLETION SCREEN ---
@Composable
fun OnboardingScreen(
    email: String,
    userId: String,
    onProfileComplete: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isAdminEmail = email.equals("fahimmia017740@gmail.com", ignoreCase = true)
    var selectedRole by remember { mutableStateOf<String?>(if (isAdminEmail) "admin" else null) } // "teacher", "student", or "admin"
    var fullName by remember { mutableStateOf(if (isAdminEmail) "Fahim Mia" else "") }
    var institution by remember { mutableStateOf("") }
    var contactInfo by remember { mutableStateOf("") }
    var isSavingProfile by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Navigation/Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = "Verified",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "প্রোফাইল সম্পন্ন করুন",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1B4B)
                )
                Text(
                    text = "সংযুক্ত অ্যাকাউন্ট: $email",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "আপনার ভূমিকা নির্বাচন করুন *",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color.DarkGray,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Text(
            text = "সতর্কতা: আপনার ভূমিকা (শিক্ষক/শিক্ষার্থী) একবার নির্বাচন করার পর আর পরিবর্তন করা যাবে না।",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Elegant Card Selection for Role
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Teacher Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedRole = "teacher" },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (selectedRole == "teacher") 2.dp else 1.dp,
                    color = if (selectedRole == "teacher") MaterialTheme.colorScheme.primary else Color.LightGray
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedRole == "teacher") MaterialTheme.colorScheme.primaryContainer else Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (selectedRole == "teacher") MaterialTheme.colorScheme.primary else Color(0xFFF3F4F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CoPresent,
                            contentDescription = "Teacher",
                            tint = if (selectedRole == "teacher") Color.White else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "শিক্ষক (Teacher)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (selectedRole == "teacher") MaterialTheme.colorScheme.primary else Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "ক্লাস তৈরি, কুইজ প্রদান",
                        fontSize = 8.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Student Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedRole = "student" },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (selectedRole == "student") 2.dp else 1.dp,
                    color = if (selectedRole == "student") MaterialTheme.colorScheme.secondary else Color.LightGray
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedRole == "student") MaterialTheme.colorScheme.secondaryContainer else Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (selectedRole == "student") MaterialTheme.colorScheme.secondary else Color(0xFFF3F4F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = "Student",
                            tint = if (selectedRole == "student") Color.White else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "শিক্ষার্থী (Student)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (selectedRole == "student") MaterialTheme.colorScheme.secondary else Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "ক্লাসে যোগদান ও পড়া",
                        fontSize = 8.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Admin Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedRole = "admin" },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (selectedRole == "admin") 2.dp else 1.dp,
                    color = if (selectedRole == "admin") Color.Red else Color.LightGray
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedRole == "admin") Color(0xFFFEF2F2) else Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (selectedRole == "admin") Color.Red else Color(0xFFF3F4F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin",
                            tint = if (selectedRole == "admin") Color.White else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "এডমিন (Admin)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (selectedRole == "admin") Color.Red else Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "অ্যাপ ও অ্যাকাউন্ট নিয়ন্ত্রণ",
                        fontSize = 8.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Details Form
        Text(
            text = "আপনার ব্যক্তিগত তথ্য",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color.DarkGray,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("সম্পূর্ণ নাম *") },
                    placeholder = { Text("উদা: রাইহান তানভীর") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Complete Profile Button
        Button(
            onClick = {
                if (selectedRole == null || fullName.isBlank()) {
                    Toast.makeText(context, "অনুগ্রহ করে সকল তথ্য পূরণ করুন!", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                coroutineScope.launch {
                    isSavingProfile = true
                    val generatedUid = "SL-" + (100000..999999).random()
                    val resolvedRole = if (email.equals("fahimmia017740@gmail.com", ignoreCase = true)) "admin" else (selectedRole ?: "student")
                    val profile = UserProfile(
                        user_id = userId,
                        email = email,
                        role = resolvedRole,
                        full_name = fullName,
                        institution = institution,
                        contact = contactInfo,
                        uid_code = generatedUid
                    )

                    try {
                        withContext(Dispatchers.IO) {
                            // Try inserting into Supabase profiles table
                            supabase.from("profiles").insert(profile)
                        }
                        Toast.makeText(context, "প্রোফাইল সফলভাবে তৈরি হয়েছে! UID: $generatedUid", Toast.LENGTH_SHORT).show()
                        onProfileComplete(profile)
                    } catch (e: Exception) {
                        Toast.makeText(context, "ত্রুটি: ${e.message?.take(50) ?: "প্রোফাইল সংরক্ষণ ব্যর্থ হয়েছে।"}", Toast.LENGTH_LONG).show()
                    } finally {
                        isSavingProfile = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isSavingProfile,
            colors = ButtonDefaults.buttonColors(
                containerColor = when (selectedRole) {
                    "student" -> MaterialTheme.colorScheme.secondary
                    "admin" -> Color.Red
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        ) {
            if (isSavingProfile) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = "প্রোফাইল সম্পন্ন করুন",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// --- PERSONALIZED DASHBOARD SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    email: String,
    userId: String,
    profile: UserProfile,
    onLogout: () -> Unit,
    onProfileUpdate: (UserProfile) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var currentScreen by remember { mutableStateOf("dashboard") }
    var selectedChannel by remember { mutableStateOf<UserProfile?>(null) }
    var selectedCourse by remember { mutableStateOf<CourseItem?>(null) }
    var initialSubjectId by remember { mutableStateOf<String?>(null) }
    var initialChapterId by remember { mutableStateOf<String?>(null) }
    var initialClassId by remember { mutableStateOf<String?>(null) }
    var showMentorsDialog by remember { mutableStateOf(false) }
    var showPublishUpdateDialog by remember { mutableStateOf(false) }
    var showPublishNoticeDialog by remember { mutableStateOf(false) }
    var mentors by remember { mutableStateOf(listOf<Mentor>()) }
    val isTeacher = profile.role == "teacher"
    val isAdmin = profile.role == "admin"
    val isManagementUser = isTeacher
    var teacherChannel by remember { mutableStateOf<UserProfile?>(null) }
    var isLoadingChannel by remember { mutableStateOf(isTeacher) }
    var courses by remember { mutableStateOf(listOf<CourseItem>()) }
    var allChannels by remember { mutableStateOf(listOf<UserProfile>()) }
    var enrollments by remember { mutableStateOf(listOf<Enrollment>()) }
    var courseInteractions by remember { mutableStateOf(listOf<CourseInteraction>()) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var focusCourseId by remember { mutableStateOf<String?>(null) }
    
    // Sync OneSignal User identity and purchased course tags so they only receive notifications of courses they bought
    LaunchedEffect(enrollments, profile.user_id) {
        val myEnrolledCourseIds = enrollments.filter { it.user_id == profile.user_id }.map { it.course_id }
        if (focusCourseId == null && myEnrolledCourseIds.isNotEmpty()) {
            focusCourseId = myEnrolledCourseIds.firstOrNull()
        }
        
        try {
            // Set User Identity in OneSignal to track this user
            OneSignal.login(profile.user_id)
            
            // Get user's enrolled courses
            val myEnrollments = enrollments.filter { it.user_id == profile.user_id }
            val tags = mutableMapOf<String, String>()
            myEnrollments.forEach { enrollment ->
                // Tag for the main course
                tags["course_${enrollment.course_id}"] = "true"
                
                // Also tag for individual purchased quarters
                if (enrollment.purchased_quarters.isNotBlank()) {
                    enrollment.purchased_quarters.split(",").forEach { qId ->
                        val trimmed = qId.trim()
                        if (trimmed.isNotBlank()) {
                            tags["quarter_$trimmed"] = "true"
                        }
                    }
                }
            }
            if (tags.isNotEmpty()) {
                OneSignal.User.addTags(tags)
                android.util.Log.d("OneSignalSync", "Synced course enrollment tags to OneSignal: $tags")
            }
        } catch (e: Exception) {
            android.util.Log.e("OneSignalSync", "Failed to sync tags to OneSignal: ${e.message}")
        }
    }

    LaunchedEffect(Unit) {
        try {
            val fetchedChannels = withContext(Dispatchers.IO) {
                supabase.from("profiles").select().decodeList<UserProfile>()
            }
            allChannels = fetchedChannels.filter { it.handle != null }

            val fetchedCourses = withContext(Dispatchers.IO) {
                supabase.from("courses").select().decodeList<CourseItem>()
            }
            courses = fetchedCourses

            val fetchedEnrollments = withContext(Dispatchers.IO) {
                try {
                    supabase.from("enrollments").select().decodeList<Enrollment>()
                } catch(e:Exception) { emptyList() }
            }
            enrollments = fetchedEnrollments

            val fetchedInteractions = withContext(Dispatchers.IO) {
                try {
                    supabase.from("course_interactions").select().decodeList<CourseInteraction>()
                } catch(e:Exception) { emptyList() }
            }
            courseInteractions = fetchedInteractions
        } catch (e: Exception) {
            Toast.makeText(context, "Error loading courses: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isTeacher) {
        if (isTeacher) {
            try {
                val fetchedChannels = withContext(Dispatchers.IO) {
                    supabase.from("profiles").select {
                        filter { eq("user_id", profile.user_id) }
                    }.decodeList<UserProfile>()
                }
                teacherChannel = fetchedChannels.firstOrNull { it.handle != null }
                
                try {
                    val fetchedMentors = withContext(Dispatchers.IO) {
                        supabase.from("mentors").select {
                            filter { eq("channel_id", profile.user_id) }
                        }.decodeList<Mentor>()
                    }
                    mentors = fetchedMentors
                } catch(e: Exception) {
                    // Mentors table might not exist yet
                }
            } catch (e: Exception) {
                // handle
            } finally {
                isLoadingChannel = false
            }
        }
    }
    
    val bgColor = Color(0xFFFBF8F1)
    val accentColor = Color(0xFF4C51F7)

    Scaffold(
        topBar = {
            if (currentScreen == "dashboard") {
                TopAppBar(
                    title = { 
                        if (!isManagementUser) {
                            val myEnrolledCourses = courses.filter { course -> 
                                enrollments.any { it.user_id == profile.user_id && it.course_id == course.id }
                            }
                            if (myEnrolledCourses.isNotEmpty()) {
                                var expanded by remember { mutableStateOf(false) }
                                val currentFocusCourse = myEnrolledCourses.find { it.id == focusCourseId } ?: myEnrolledCourses.firstOrNull()
                                
                                if (currentFocusCourse != null) {
                                    Box {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .clickable { expanded = true }
                                                .background(Color(0xFFF3F4F6))
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Language,
                                                contentDescription = "Course Icon",
                                                tint = accentColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                currentFocusCourse.title, 
                                                fontWeight = FontWeight.Bold, 
                                                color = Color(0xFF1E293B),
                                                fontSize = 15.sp,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 140.dp)
                                            ) 
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Expand", modifier = Modifier.size(18.dp), tint = Color(0xFF64748B))
                                        }
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false },
                                            modifier = Modifier.background(Color.White)
                                        ) {
                                            myEnrolledCourses.forEach { course ->
                                                DropdownMenuItem(
                                                    text = { Text(course.title, fontSize = 14.sp, color = if (course.id == focusCourseId) accentColor else Color.Black) },
                                                    onClick = {
                                                        focusCourseId = course.id
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = "App Logo",
                                        tint = accentColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Shikkhaloy", 
                                        fontWeight = FontWeight.Bold, 
                                        color = Color(0xFF4A5568),
                                        fontSize = 18.sp
                                    ) 
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "App Logo",
                                    tint = accentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Shikkhaloy", 
                                    fontWeight = FontWeight.Bold, 
                                    color = Color(0xFF4A5568),
                                    fontSize = 18.sp
                                ) 
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Notifications */ }) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = Color.DarkGray)
                        }
                        IconButton(onClick = {
                            if (isManagementUser) {
                                selectedTab = 1
                            } else {
                                selectedTab = 4
                            }
                        }) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(accentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                val displayAvatar = profile.profile_image_url
                                if (displayAvatar != null) {
                                    AsyncImage(
                                        model = displayAvatar,
                                        contentDescription = "Avatar",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = profile.full_name.firstOrNull()?.toString()?.uppercase() ?: "",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = bgColor
                    )
                )
            }
        },
        bottomBar = {
            if (currentScreen == "dashboard") {
                if (!isManagementUser) {
                val studentNavItems = listOf(
                    BottomNavItem("", Icons.Outlined.Home, Color(0xFF4CAF50)),
                    BottomNavItem("", Icons.Outlined.MenuBook, Color(0xFF2196F3)),
                    BottomNavItem("", Icons.Outlined.Explore, Color(0xFFFF9800)),
                    BottomNavItem("", Icons.Outlined.AutoAwesome, Color(0xFF9C27B0)),
                    BottomNavItem("", Icons.Outlined.Settings, Color(0xFF607D8B))
                )
                CustomBottomNavigation(
                    items = studentNavItems,
                    selectedIndex = selectedTab,
                    onItemSelected = { index ->
                        selectedTab = index
                        currentScreen = "dashboard"
                    }
                )
            } else {
                val teacherNavItems = listOf(
                    BottomNavItem("চ্যানেল", Icons.Outlined.Home, Color(0xFF4CAF50)),
                    BottomNavItem("কোর্স", Icons.Outlined.MenuBook, Color(0xFF2196F3)),
                    BottomNavItem("ম্যানেজমেন্ট", Icons.Outlined.Dashboard, Color(0xFFFF9800)),
                    BottomNavItem("সেটিংস", Icons.Outlined.Settings, Color(0xFF607D8B))
                )
                
                val visualIndex = when (selectedTab) {
                    0 -> 0
                    3 -> 1
                    2 -> 2
                    1 -> 3
                    else -> 0
                }
                
                CustomBottomNavigation(
                    items = teacherNavItems,
                    selectedIndex = visualIndex,
                    onItemSelected = { index ->
                        selectedTab = when (index) {
                            0 -> 0
                            1 -> 3
                            2 -> 2
                            3 -> 1
                            else -> 0
                        }
                        currentScreen = "dashboard"
                    }
                )
            }
            }
        },
        floatingActionButton = {
            if (isTeacher && (currentScreen == "dashboard" && selectedTab == 3 || currentScreen == "channel_detail" && selectedChannel?.user_id == profile.user_id)) {
                FloatingActionButton(
                    onClick = { 
                        if (teacherChannel != null) {
                            currentScreen = "add_course" 
                        } else {
                            Toast.makeText(context, "অনুগ্রহ করে কোর্স যোগ করার আগে আপনার চ্যানেল সেটআপ করুন।", Toast.LENGTH_SHORT).show()
                            currentScreen = "create_channel"
                        }
                    },
                    containerColor = accentColor,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Course")
                }
            }
        },
        containerColor = bgColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .then(if (currentScreen == "dashboard") Modifier.padding(horizontal = 16.dp) else Modifier)
        ) {
            if (currentScreen == "dashboard") Spacer(modifier = Modifier.height(16.dp))
            if (currentScreen == "add_course") {
                AddCourseScreen(
                    profile = profile,
                    accentColor = accentColor,
                    onBack = { currentScreen = "dashboard" },
                    onCourseAdded = { newCourse ->
                        val courseToSave = newCourse.copy(channel_id = teacherChannel?.user_id)
                        coroutineScope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    supabase.from("courses").insert(courseToSave)
                                }
                                courses = courses + courseToSave
                            } catch (e: Exception) {
                                if (e.message?.contains("subjects") == true || e.message?.contains("schema") == true) {
                                    Toast.makeText(context, "Error: Supabase এ courses টেবিলে subjects (Type: JSONB) কলাম তৈরি করুন!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Error saving course: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            } else if (currentScreen == "create_channel") {
                CreateChannelScreen(
                    profile = profile,
                    existingChannel = teacherChannel,
                    accentColor = accentColor,
                    onBack = { 
                        if (isTeacher) {
                            currentScreen = "dashboard"
                            // To update the channel state if created
                            isLoadingChannel = true
                            coroutineScope.launch {
                                try {
                                    val fetchedChannels = withContext(Dispatchers.IO) {
                                        supabase.from("profiles").select {
                                            filter { eq("user_id", profile.user_id) }
                                        }.decodeList<UserProfile>()
                                    }
                                    teacherChannel = fetchedChannels.firstOrNull { it.handle != null }
                                } catch (e: Exception) {
                                } finally {
                                    isLoadingChannel = false
                                }
                            }
                        } else {
                            currentScreen = "channel_list" 
                        }
                    }
                )
            } else if (currentScreen == "channel_list") {
                ChannelListScreen(
                    profile = profile,
                    accentColor = accentColor,
                    onCreateChannel = { currentScreen = "create_channel" },
                    onChannelClick = { channel -> 
                        selectedChannel = channel
                        currentScreen = "channel_detail"
                    },
                    onBack = { currentScreen = "dashboard" }
                )
            } else if (currentScreen == "channel_detail" && selectedChannel != null) {
                ChannelDetailScreen(
                    channel = selectedChannel!!,
                    profile = profile,
                    accentColor = accentColor,
                    courses = courses,
                    onCourseClick = { course ->
                        selectedCourse = course
                        initialSubjectId = null
                        initialChapterId = null
                        initialClassId = null
                        currentScreen = "course_detail"
                    },
                    onBack = { currentScreen = "dashboard" }
                )
            } else if (currentScreen == "course_detail" && selectedCourse != null) {
                CourseDetailScreen(
                    course = selectedCourse!!,
                    profile = profile,
                    mentors = mentors,
                    userEnrollment = enrollments.find { it.course_id == selectedCourse!!.id && it.user_id == profile.user_id },
                    isLiked = courseInteractions.any { it.course_id == selectedCourse!!.id && it.user_id == profile.user_id && it.is_like },
                    initialSubjectId = initialSubjectId,
                    initialChapterId = initialChapterId,
                    initialClassId = initialClassId,
                    onEnroll = { purchasedQuarters ->
                        coroutineScope.launch {
                            try {
                                val pricePaid = if (selectedCourse!!.pricingOption == "Fully Free") "0" else (selectedCourse!!.discountPrice.ifEmpty { selectedCourse!!.mainPrice })
                                val enrollment = Enrollment(user_id = profile.user_id, course_id = selectedCourse!!.id, price_paid = pricePaid, purchased_quarters = purchasedQuarters)
                                withContext(Dispatchers.IO) {
                                    supabase.from("enrollments").insert(enrollment)
                                    val currentCount = selectedCourse!!.studentsCount
                                    // Update student count in course (this might not be the best way to update the column but good enough for now)
                                    // Actually supabase.from("courses").update ...
                                }
                                enrollments = enrollments + enrollment
                                courses = courses.map { if (it.id == selectedCourse!!.id) it.copy(studentsCount = it.studentsCount + 1) else it }
                                selectedCourse = courses.find { it.id == selectedCourse!!.id }
                                Toast.makeText(context, "Successfully Enrolled!", Toast.LENGTH_SHORT).show()
                            } catch(e: Exception) {
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onLikeToggle = {
                        coroutineScope.launch {
                            try {
                                val existing = courseInteractions.find { it.course_id == selectedCourse!!.id && it.user_id == profile.user_id }
                                if (existing != null) {
                                    withContext(Dispatchers.IO) {
                                        supabase.from("course_interactions").delete {
                                            filter { eq("id", existing.id) }
                                        }
                                    }
                                    courseInteractions = courseInteractions.filter { it.id != existing.id }
                                } else {
                                    val newInteraction = CourseInteraction(user_id = profile.user_id, course_id = selectedCourse!!.id, is_like = true)
                                    withContext(Dispatchers.IO) {
                                        supabase.from("course_interactions").insert(newInteraction)
                                    }
                                    courseInteractions = courseInteractions + newInteraction
                                }
                            } catch(e: Exception) {
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onCourseUpdate = { updatedCourse ->
                        coroutineScope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    supabase.from("courses").update(updatedCourse) {
                                        filter { eq("id", updatedCourse.id) }
                                    }
                                }
                                courses = courses.map { if (it.id == updatedCourse.id) updatedCourse else it }
                                selectedCourse = updatedCourse
                                Toast.makeText(context, "Course updated!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                if (e.message?.contains("subjects") == true || e.message?.contains("schema") == true) {
                                    Toast.makeText(context, "Error: Supabase এ courses টেবিলে subjects (Type: JSONB) কলাম তৈরি করুন!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    onMultipleCoursesUpdate = { updatedCourses ->
                        val updatedMap = updatedCourses.associateBy { it.id }
                        courses = courses.map { updatedMap[it.id] ?: it }
                        Toast.makeText(context, "Multiple courses updated!", Toast.LENGTH_SHORT).show()
                    },
                    accentColor = accentColor,
                    onBack = { 
                        currentScreen = "dashboard" 
                        initialSubjectId = null
                        initialChapterId = null
                        initialClassId = null
                    }
                )
            } else {
                if (isTeacher) {
                    if (selectedTab == 0) {
                        ExploreFeedScreen(
                            accentColor = accentColor, 
                            profile = profile, 
                            actingChannel = teacherChannel, 
                            courses = courses,
                            allChannels = allChannels,
                            onChannelClick = { channel ->
                                selectedChannel = channel
                                currentScreen = "channel_detail"
                            },
                            onCourseClick = { course ->
                                selectedCourse = course
                                initialSubjectId = null
                                initialChapterId = null
                                initialClassId = null
                                currentScreen = "course_detail"
                            }
                        )
                    } else if (selectedTab == 2) {
                        TeacherDashboardContent(
                            accentColor = accentColor, 
                            onChannelClick = { 
                                if (teacherChannel != null) {
                                    selectedChannel = teacherChannel
                                    currentScreen = "channel_detail"
                                } else {
                                    currentScreen = "create_channel"
                                }
                            },
                            onAddCourseClick = {
                                if (teacherChannel != null) {
                                    currentScreen = "add_course"
                                } else {
                                    Toast.makeText(context, "অনুগ্রহ করে কোর্স যোগ করার আগে আপনার চ্যানেল সেটআপ করুন।", Toast.LENGTH_SHORT).show()
                                    currentScreen = "create_channel"
                                }
                            },
                            onMentorsClick = { showMentorsDialog = true }
                        )
                    } else if (selectedTab == 3) {
                        CourseListScreen(
                            accentColor = accentColor, 
                            courses = courses.filter { it.channel_id == profile.user_id },
                            onCourseClick = { course ->
                                selectedCourse = course
                                initialSubjectId = null
                                initialChapterId = null
                                initialClassId = null
                                currentScreen = "course_detail"
                            }
                        )
                    } else if (selectedTab == 1) {
                        SettingsScreen(
                            profile = profile, 
                            teacherChannel = teacherChannel, 
                            onLogout = onLogout, 
                            accentColor = accentColor, 
                            onProfileUpdate = onProfileUpdate,
                            courses = courses,
                            enrollments = enrollments
                        )
                    }
                } else {
                    if (selectedTab == 0) {
                        StudentDashboardContent(
                            accentColor = accentColor,
                            courses = courses,
                            focusCourseId = focusCourseId,
                            onClassClick = { classInfo, chapter, subject, c ->
                                selectedCourse = c
                                initialSubjectId = subject.id
                                initialChapterId = chapter.id
                                initialClassId = classInfo.id
                                currentScreen = "course_detail"
                            }
                        )
                    } else if (selectedTab == 1) {
                        StudentCoursesScreen(
                            accentColor = accentColor,
                            courses = courses,
                            enrollments = enrollments,
                            profile = profile,
                            onCourseClick = { course ->
                                selectedCourse = course
                                initialSubjectId = null
                                initialChapterId = null
                                initialClassId = null
                                currentScreen = "course_detail"
                            }
                        )
                    } else if (selectedTab == 2) {
                        ExploreFeedScreen(
                            accentColor = accentColor, 
                            profile = profile, 
                            courses = courses,
                            allChannels = allChannels,
                            onChannelClick = { channel ->
                                selectedChannel = channel
                                currentScreen = "channel_detail"
                            },
                            onCourseClick = { course ->
                                selectedCourse = course
                                initialSubjectId = null
                                initialChapterId = null
                                initialClassId = null
                                currentScreen = "course_detail"
                            }
                        )
                    } else if (selectedTab == 4) {
                        SettingsScreen(
                            profile = profile, 
                            onLogout = onLogout, 
                            accentColor = accentColor, 
                            onProfileUpdate = onProfileUpdate,
                            courses = courses,
                            enrollments = enrollments
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                             Text("Coming Soon...", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
    
    if (showMentorsDialog) {
        MentorsListDialog(
            mentors = mentors,
            onAddMentor = { newMentor -> 
                val mentorWithChannel = newMentor.copy(channel_id = profile.user_id)
                coroutineScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            supabase.from("mentors").insert(mentorWithChannel)
                        }
                        mentors = mentors + mentorWithChannel
                        Toast.makeText(context, "মেন্টর যোগ করা হয়েছে", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        if (e.message?.contains("mentors") == true || e.message?.contains("relation") == true) {
                            Toast.makeText(context, "Error: Supabase এ mentors টেবিল তৈরি করুন!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onDismiss = { showMentorsDialog = false },
            accentColor = accentColor
        )
    }

    if (showPublishUpdateDialog) {
        PublishUpdateDialog(
            accentColor = accentColor,
            onDismiss = { showPublishUpdateDialog = false },
            onPublished = {
                Toast.makeText(context, "নতুন আপডেট সফলভাবে রিলিজ করা হয়েছে! 🎉", Toast.LENGTH_LONG).show()
            }
        )
    }

    if (showPublishNoticeDialog) {
        PublishNoticeDialog(
            accentColor = accentColor,
            onDismiss = { showPublishNoticeDialog = false },
            onPublished = {
                Toast.makeText(context, "জরুরি নোটিশ সফলভাবে প্রচার করা হয়েছে! 📢", Toast.LENGTH_LONG).show()
            }
        )
    }
}

@Composable
fun StudentDashboardContent(
    accentColor: Color,
    courses: List<CourseItem>,
    focusCourseId: String?,
    onClassClick: (CourseClass, CourseChapter, CourseSubject, CourseItem) -> Unit
) {
    var selectedDate by remember { mutableStateOf(java.time.LocalDate.now()) }
    var isCalendarExpanded by remember { mutableStateOf(false) }
    var viewingMonth by remember { mutableStateOf(java.time.YearMonth.from(selectedDate)) }
    val focusCourse = courses.find { it.id == focusCourseId }
    
    // Extract classes for the selected date
    val classDates = remember(focusCourse) {
        val dates = mutableSetOf<java.time.LocalDate>()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy")
        focusCourse?.subjects?.forEach { subject ->
            subject.chapters.forEach { chapter ->
                chapter.classes.forEach { courseClass ->
                    try {
                        dates.add(java.time.LocalDate.parse(courseClass.date, formatter))
                    } catch (e: Exception) {}
                }
            }
        }
        dates
    }
    
    val classesForDate = remember(focusCourse, selectedDate) {
        val classList = mutableListOf<Triple<CourseClass, CourseChapter, CourseSubject>>()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy")
        
        focusCourse?.subjects?.forEach { subject ->
            subject.chapters.forEach { chapter ->
                chapter.classes.forEach { courseClass ->
                    try {
                        val classDate = java.time.LocalDate.parse(courseClass.date, formatter)
                        if (classDate == selectedDate) {
                            classList.add(Triple(courseClass, chapter, subject))
                        }
                    } catch (e: Exception) {}
                }
            }
        }
        classList.sortedBy { it.first.time }
    }

    val bgGradient = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9))
    )
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(bgGradient),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 80.dp)
    ) {
        // Class Routine Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = Color(0xFF94A3B8).copy(alpha = 0.2f),
                        ambientColor = Color(0xFF94A3B8).copy(alpha = 0.2f)
                    ),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF818CF8), Color(0xFFC084FC))))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Class Routine",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Check your daily class schedule",
                        fontSize = 15.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { isCalendarExpanded = !isCalendarExpanded },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = "Calendar", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(if (isCalendarExpanded) "Hide Calendar" else "Weekly Routine", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowForward, contentDescription = "Go", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Calendar Area
        item {
            val monthFormatter = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    viewingMonth.format(monthFormatter),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                if (isCalendarExpanded) {
                    Row {
                        IconButton(
                            onClick = { viewingMonth = viewingMonth.minusMonths(1) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(4.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Month", tint = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewingMonth = viewingMonth.plusMonths(1) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(4.dp)
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = Color.Gray)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isCalendarExpanded) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF60A5FA), Color(0xFF3B82F6))))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                                Text(
                                    text = day,
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val firstDayOfMonth = viewingMonth.atDay(1)
                        val daysInMonth = viewingMonth.lengthOfMonth()
                        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // Sunday = 0
                        
                        val totalDays = firstDayOfWeek + daysInMonth
                        val weeks = Math.ceil(totalDays / 7.0).toInt()
                        
                        var dayCounter = 1
                        for (i in 0 until weeks) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                for (j in 0..6) {
                                    if (i == 0 && j < firstDayOfWeek || dayCounter > daysInMonth) {
                                        Box(modifier = Modifier.weight(1f))
                                    } else {
                                        val date = viewingMonth.atDay(dayCounter)
                                        val isSelected = date == selectedDate
                                        val hasClass = classDates.contains(date)
                                        
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { 
                                                    selectedDate = date 
                                                }
                                                .then(if (isSelected) Modifier.background(accentColor.copy(alpha = 0.1f)) else Modifier)
                                                .then(if (isSelected) Modifier.padding(2.dp).background(Color.White, RoundedCornerShape(8.dp)) else Modifier)
                                                .then(if (isSelected) Modifier.padding(2.dp).background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp)) else Modifier),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = dayCounter.toString(),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) accentColor else Color.DarkGray
                                            )
                                            if (hasClass) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(horizontalArrangement = Arrangement.Center) {
                                                    Box(modifier = Modifier.size(4.dp).background(accentColor, CircleShape))
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Box(modifier = Modifier.size(4.dp).background(accentColor, CircleShape))
                                                }
                                            }
                                        }
                                        dayCounter++
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { 
                        selectedDate = selectedDate.minusDays(1)
                        viewingMonth = java.time.YearMonth.from(selectedDate)
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = Color.Gray, modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                            .padding(4.dp))
                    }
                    
                    val prevDate = selectedDate.minusDays(1)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                        selectedDate = prevDate 
                        viewingMonth = java.time.YearMonth.from(selectedDate)
                    }) {
                        Text(prevDate.dayOfWeek.name.take(3).capitalize(), fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(prevDate.dayOfMonth.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(Color(0xFF9D2053), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(selectedDate.dayOfWeek.name.take(3).capitalize(), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(selectedDate.dayOfMonth.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    val nextDate = selectedDate.plusDays(1)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                        selectedDate = nextDate 
                        viewingMonth = java.time.YearMonth.from(selectedDate)
                    }) {
                        Text(nextDate.dayOfWeek.name.take(3).capitalize(), fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(nextDate.dayOfMonth.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    }
                    
                    IconButton(onClick = { 
                        selectedDate = selectedDate.plusDays(1) 
                        viewingMonth = java.time.YearMonth.from(selectedDate)
                    }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.Gray, modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                            .padding(4.dp))
                    }
                }
            }
        }

        // Today's Routine Title
        item {
            val dateLabel = if (selectedDate == java.time.LocalDate.now()) "Today's Routine" else "Routine for ${selectedDate.dayOfMonth} ${selectedDate.month.name.take(3)}"
            Text(
                dateLabel,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
        }

        // Classes List
        if (focusCourse == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(24.dp),
                            spotColor = Color(0xFF94A3B8).copy(alpha = 0.2f),
                            ambientColor = Color(0xFF94A3B8).copy(alpha = 0.2f)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF9CA3AF), Color(0xFFD1D5DB))))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("কোনো কোর্স সিলেক্ট করা নেই।", color = Color.Gray)
                    }
                }
            }
        } else if (classesForDate.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(24.dp),
                            spotColor = Color(0xFF94A3B8).copy(alpha = 0.2f),
                            ambientColor = Color(0xFF94A3B8).copy(alpha = 0.2f)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFF10B981))))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(20.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🐧", fontSize = 32.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "You have no live classes or",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            "exams on this date.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        } else {
            items(classesForDate.size) { index ->
                val (courseClass, chapter, subject) = classesForDate[index]
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(24.dp),
                            spotColor = Color(0xFF94A3B8).copy(alpha = 0.2f),
                            ambientColor = Color(0xFF94A3B8).copy(alpha = 0.2f)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFF472B6), Color(0xFFEC4899))))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(subject.title, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.size(4.dp).background(Color(0xFFCBD5E1), CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(courseClass.type, color = Color(0xFF334155), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("মিসড", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("${chapter.title} - ${courseClass.title}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF64748B))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(courseClass.time, fontSize = 14.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { onClassClick(courseClass, chapter, subject, focusCourse) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF1F5F9),
                                contentColor = accentColor
                            ),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(vertical = 14.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Icon(Icons.Outlined.PlayCircleOutline, contentDescription = "Play", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("রেকর্ডেড ভিডিও দেখো", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
        
        // Homework title
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Your Homework",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E293B)
            )
        }
        
        val classesWithHomework = classesForDate.filter { it.first.homeworkLink.isNotBlank() }
        
        if (classesWithHomework.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(24.dp),
                            spotColor = Color(0xFF94A3B8).copy(alpha = 0.2f),
                            ambientColor = Color(0xFF94A3B8).copy(alpha = 0.2f)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFFFFF7ED), RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFFFFEDD5), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Homework", modifier = Modifier.size(28.dp), tint = Color(0xFFF97316))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No homework today!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    }
                }
            }
        } else {
            items(classesWithHomework.size) { index ->
                val (courseClass, chapter, subject) = classesWithHomework[index]
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(20.dp),
                            spotColor = Color(0xFF94A3B8).copy(alpha = 0.2f),
                            ambientColor = Color(0xFF94A3B8).copy(alpha = 0.2f)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFF97316), Color(0xFFEA580C))))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFFFFF7ED), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Homework", tint = Color(0xFFEA580C))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${chapter.title} - ${courseClass.title}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(subject.title, fontSize = 13.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = { onClassClick(courseClass, chapter, subject, focusCourse!!) },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.1f)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Text("View", color = accentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentCoursesScreen(
    accentColor: Color,
    courses: List<CourseItem>,
    enrollments: List<Enrollment>,
    profile: UserProfile,
    onCourseClick: (CourseItem) -> Unit
) {
    val myEnrolledCourses = courses.filter { course ->
        enrollments.any { it.user_id == profile.user_id && it.course_id == course.id }
    }

    if (myEnrolledCourses.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFFDFD8C8), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("🐧", fontSize = 32.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "You have not purchased any courses yet.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
        }
    } else {
        CourseListScreen(
            accentColor = accentColor,
            courses = myEnrolledCourses,
            title = "আমার কেনা কোর্সসমূহ",
            subtitle = "আপনার কেনা সকল কোর্স এখানে দেখুন",
            onCourseClick = onCourseClick
        )
    }
}

@Composable
fun TeacherDashboardContent(accentColor: Color, onChannelClick: () -> Unit, onAddCourseClick: () -> Unit, onMentorsClick: () -> Unit) {
    val items = listOf(
        Pair("সকল কোর্স", Icons.Default.LibraryBooks),
        Pair("ক্লাস যোগ", Icons.Default.AddBox),
        Pair("ক্লাস লিংক যোগ", Icons.Default.AddLink),
        Pair("চ্যানেল", Icons.Default.LiveTv),
        Pair("হোম ওয়ার্ক", Icons.Default.Assignment),
        Pair("কোর্স কেনাদের পরিচালনা", Icons.Default.People),
        Pair("মেন্টর তালিকা", Icons.Default.GroupAdd)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "শিক্ষক ড্যাশবোর্ড",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF4A5568)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "আপনার ক্লাসরুম ও কোর্সের কাজগুলো পরিচালনা করুন",
                fontSize = 14.sp,
                color = Color(0xFF718096)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                for (i in items.indices step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TeacherDashboardCard(
                            title = items[i].first,
                            icon = items[i].second,
                            accentColor = accentColor,
                            onClick = { 
                                if (items[i].first == "চ্যানেল") onChannelClick()
                                else if (items[i].first == "ক্লাস যোগ") onAddCourseClick()
                                else if (items[i].first == "মেন্টর তালিকা") onMentorsClick()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (i + 1 < items.size) {
                            TeacherDashboardCard(
                                title = items[i + 1].first,
                                icon = items[i + 1].second,
                                accentColor = accentColor,
                                onClick = { 
                                    if (items[i + 1].first == "চ্যানেল") onChannelClick()
                                    else if (items[i + 1].first == "ক্লাস যোগ") onAddCourseClick()
                                    else if (items[i + 1].first == "মেন্টর তালিকা") onMentorsClick()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TeacherDashboardCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SettingsScreen(
    profile: UserProfile,
    teacherChannel: UserProfile? = null,
    onLogout: () -> Unit,
    accentColor: Color,
    onProfileUpdate: (UserProfile) -> Unit,
    courses: List<CourseItem> = emptyList(),
    enrollments: List<Enrollment> = emptyList()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isTeacher = profile.role == "teacher"
    val isAdmin = profile.role == "admin"
    var showDeviceSheet by remember { mutableStateOf(false) }
    var showProfileEditDialog by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showAdmissionInfoDialog by remember { mutableStateOf(false) }
    var showDownloadsDialog by remember { mutableStateOf(false) }
    
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var manualUpdateToPrompt by remember { mutableStateOf<AppUpdate?>(null) }
    var showPublishUpdateDialog by remember { mutableStateOf(false) }
    var showPublishNoticeDialog by remember { mutableStateOf(false) }
    var showAdminDashboardPanel by remember { mutableStateOf(false) }

    if (showAdmissionInfoDialog) {
        val myEnrolledCourses = courses.filter { course ->
            enrollments.any { it.user_id == profile.user_id && it.course_id == course.id }
        }
        AlertDialog(
            onDismissRequest = { showAdmissionInfoDialog = false },
            title = { Text("Admission Information", fontWeight = FontWeight.Bold) },
            text = {
                if (myEnrolledCourses.isEmpty()) {
                    Text("You have not purchased any courses yet.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(myEnrolledCourses) { course ->
                            val enrollment = enrollments.find { it.user_id == profile.user_id && it.course_id == course.id }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(course.title, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Price Paid: ৳${enrollment?.price_paid ?: "0"}", fontSize = 14.sp)
                                    Text("Purchase Date: ${enrollment?.created_at?.take(10) ?: "Just now"}", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAdmissionInfoDialog = false }) {
                    Text("Close", color = accentColor)
                }
            }
        )
    }

    if (showAdminDashboardPanel) {
        AdminDashboardContent(
            accentColor = accentColor,
            onPublishUpdateClick = { showPublishUpdateDialog = true },
            onPublishNoticeClick = { showPublishNoticeDialog = true },
            onBack = { showAdminDashboardPanel = false }
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        item {
            Text(
                text = "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF4A5568)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    SettingItem(
                        icon = Icons.Outlined.Person,
                        title = "Profile Settings",
                        subtitle = "Update your profile information",
                        accentColor = accentColor,
                        onClick = { showProfileEditDialog = true }
                    )
                    if (!isTeacher) {
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF3F4F6))
                        SettingItem(
                            icon = Icons.Outlined.Info,
                            title = "Admission Info",
                            subtitle = "View current admission details",
                            accentColor = accentColor,
                            onClick = { showAdmissionInfoDialog = true }
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Preferences",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF718096),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    SettingItem(
                        icon = Icons.Outlined.Language,
                        title = "Change Language",
                        subtitle = "English, বাংলা",
                        accentColor = accentColor,
                        onClick = { showLanguageSheet = true }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF3F4F6))
                    SettingItem(
                        icon = Icons.Outlined.Palette,
                        title = "Change Theme",
                        subtitle = "Light, Dark, System",
                        accentColor = accentColor,
                        onClick = { }
                    )
                    if (!isTeacher) {
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF3F4F6))
                        SettingItem(
                            icon = Icons.Outlined.Download,
                            title = "Offline Downloads",
                            subtitle = "Manage downloaded course materials",
                            accentColor = accentColor,
                            onClick = { showDownloadsDialog = true }
                        )
                    }
                }
            }
        }

        if (isAdmin) {
            item {
                Text(
                    text = "প্রশাসনিক প্যানেল (Administration)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF718096),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        SettingItem(
                            icon = Icons.Default.AdminPanelSettings,
                            title = "প্রশাসক ড্যাশবোর্ড (Admin Dashboard)",
                            subtitle = "ইউজার কন্ট্রোল, নোটিশ ও ডাটাবেজ নিয়ন্ত্রণ করুন",
                            accentColor = accentColor,
                            onClick = { showAdminDashboardPanel = true }
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "এপ আপডেট (App Updates)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF718096),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    SettingItem(
                        icon = Icons.Outlined.SystemUpdate,
                        title = if (isCheckingUpdate) "আপডেট চেক করা হচ্ছে..." else "চেক আপডেট (Check Update)",
                        subtitle = "এপ্লিকেশন এর নতুন আপডেট চেক করুন",
                        accentColor = accentColor,
                        onClick = {
                            if (!isCheckingUpdate) {
                                isCheckingUpdate = true
                                coroutineScope.launch {
                                    val update = AppUpdateManager.checkForUpdate(context)
                                    isCheckingUpdate = false
                                    if (update != null) {
                                        manualUpdateToPrompt = update
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "আপনার এপটি সম্পূর্ণ আপ-টু-ডেট আছে! (v${AppUpdateManager.getCurrentVersionName(context)})",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    )
                    if (isAdmin) {
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF3F4F6))
                        SettingItem(
                            icon = Icons.Outlined.CloudUpload,
                            title = "আপডেট পাবলিশ করুন (Publish Update)",
                            subtitle = "শিক্ষার্থীদের জন্য নতুন আপডেট রিলিজ করুন",
                            accentColor = accentColor,
                            onClick = { showPublishUpdateDialog = true }
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Account & Security",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF718096),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    SettingItem(
                        icon = Icons.Outlined.Devices,
                        title = "Device Management",
                        subtitle = "Manage logged in devices",
                        accentColor = accentColor,
                        onClick = { showDeviceSheet = true }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF3F4F6))
                    SettingItem(
                        icon = Icons.Outlined.Logout,
                        title = "Sign Out",
                        subtitle = "Log out from this device",
                        accentColor = Color.Red,
                        onClick = onLogout,
                        isDestructive = true
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    }

    if (showDeviceSheet) {
        DeviceManagementDialog(
            onDismiss = { showDeviceSheet = false },
            accentColor = accentColor
        )
    }

    if (showLanguageSheet) {
        LanguageSelectionSheet(
            onDismiss = { showLanguageSheet = false },
            accentColor = accentColor
        )
    }

    if (showProfileEditDialog) {
        ProfileEditDialog(
            profile = profile,
            onDismiss = { showProfileEditDialog = false },
            onProfileUpdate = onProfileUpdate,
            accentColor = accentColor
        )
    }

    if (showDownloadsDialog) {
        OfflineDownloadsDialog(
            onDismiss = { showDownloadsDialog = false },
            accentColor = accentColor
        )
    }

    if (manualUpdateToPrompt != null) {
        UpdatePromptDialog(
            update = manualUpdateToPrompt!!,
            accentColor = accentColor,
            onDismiss = { manualUpdateToPrompt = null }
        )
    }

    if (showPublishUpdateDialog) {
        PublishUpdateDialog(
            accentColor = accentColor,
            onDismiss = { showPublishUpdateDialog = false },
            onPublished = {
                // Clear state or trigger checks
            }
        )
    }

    if (showPublishNoticeDialog) {
        PublishNoticeDialog(
            accentColor = accentColor,
            onDismiss = { showPublishNoticeDialog = false },
            onPublished = {
                // Published
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionSheet(
    onDismiss: () -> Unit,
    accentColor: Color
) {
    var selectedLanguage by remember { mutableStateOf("English") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Select Language",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedLanguage = "English" }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedLanguage == "English",
                    onClick = { selectedLanguage = "English" },
                    colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("English", fontSize = 16.sp)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedLanguage = "বাংলা" }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedLanguage == "বাংলা",
                    onClick = { selectedLanguage = "বাংলা" },
                    colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("বাংলা (Bengali)", fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProfileEditDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onProfileUpdate: (UserProfile) -> Unit,
    accentColor: Color
) {
    var name by remember { mutableStateOf(profile.full_name) }
    var institution by remember { mutableStateOf(profile.institution) }
    var contact by remember { mutableStateOf(profile.contact) }
    var profileImageUrl by remember { mutableStateOf(profile.profile_image_url) }
    val coroutineScope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isSaving = true
                Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT).show()
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    if (bytes != null) {
                        val uploadedUrl = ImgBBClient.uploadImage(bytes)
                        if (uploadedUrl != null) {
                            profileImageUrl = uploadedUrl
                            Toast.makeText(context, "Image uploaded!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Upload failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isSaving = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6))
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageUrl != null) {
                        AsyncImage(
                            model = profileImageUrl,
                            contentDescription = "Profile Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Upload", tint = Color.Gray)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        coroutineScope.launch {
                            isSaving = true
                            val updatedProfile = profile.copy(
                                full_name = name,
                                profile_image_url = profileImageUrl
                            )
                            try {
                                withContext(Dispatchers.IO) {
                                    supabase.from("profiles").update(
                                        {
                                            set("full_name", updatedProfile.full_name)
                                            updatedProfile.profile_image_url?.let {
                                                set("profile_image_url", it)
                                            }
                                        }
                                    ) {
                                        filter { eq("user_id", profile.user_id) }
                                    }
                                }
                                onProfileUpdate(updatedProfile)
                                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } catch (e: Exception) {
                                // Assume success locally for offline mode simulation
                                onProfileUpdate(updatedProfile)
                                Toast.makeText(context, "Profile saved locally.", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Save Changes")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(if (isDestructive) Color(0xFFFEE2E2) else Color(0xFFF3F4F6), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDestructive) Color.Red else Color.DarkGray
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Go",
            tint = Color.LightGray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagementDialog(
    onDismiss: () -> Unit,
    accentColor: Color
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Device Management",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "You are currently logged in on 2 devices",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            // Current Device
            DeviceItem(
                deviceName = "Samsung Galaxy S22 Ultra",
                location = "Dhaka, Bangladesh",
                time = "Active now",
                icon = Icons.Outlined.Smartphone,
                isCurrent = true,
                accentColor = accentColor
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Other Device
            DeviceItem(
                deviceName = "Windows PC (Chrome)",
                location = "Dhaka, Bangladesh",
                time = "Last active: 2 hours ago",
                icon = Icons.Outlined.Computer,
                isCurrent = false,
                accentColor = accentColor
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DeviceItem(
    deviceName: String,
    location: String,
    time: String,
    icon: ImageVector,
    isCurrent: Boolean,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFFF3F4F6), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Device",
                tint = Color.DarkGray,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = deviceName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                if (isCurrent) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Current", fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(
                text = location,
                fontSize = 13.sp,
                color = Color.Gray
            )
            Text(
                text = time,
                fontSize = 12.sp,
                color = if (isCurrent) Color(0xFF10B981) else Color.Gray
            )
        }
        if (!isCurrent) {
            IconButton(onClick = { /* Log out device */ }) {
                Icon(
                    imageVector = Icons.Outlined.Logout,
                    contentDescription = "Log out device",
                    tint = Color.Red
                )
            }
        }
    }
}

@Composable
fun MentorsListDialog(
    mentors: List<Mentor>,
    onAddMentor: (Mentor) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color
) {
    var showAddMentor by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("মেন্টর তালিকা", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                if (showAddMentor) {
                    var name by remember { mutableStateOf("") }
                    var education by remember { mutableStateOf("") }
                    var subjects by remember { mutableStateOf("") }
                    var experience by remember { mutableStateOf("") }
                    
                    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("নাম * (যেমন: মোঃ সাব্বির হোসাইন)") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = education, onValueChange = { education = it }, label = { Text("কোথায় পড়াশোনা করেছেন? (যেমন: ঢাকা বিশ্ববিদ্যালয়)") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = subjects, onValueChange = { subjects = it }, label = { Text("কী কী বিষয় পড়ান? * (যেমন: বাংলা, ইংরেজি, গণিত)") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = experience, onValueChange = { experience = it }, label = { Text("শিক্ষাকতার অভিজ্ঞতা (যেমন: ৫ বছরের বেশি সময়...)") }, modifier = Modifier.fillMaxWidth())
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAddMentor = false }) { Text("বাতিল", color = Color.Gray) }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                if (name.isNotBlank() && subjects.isNotBlank()) {
                                    onAddMentor(Mentor(name = name, education = education, subjects = subjects, experience = experience))
                                    showAddMentor = false
                                }
                            }, colors = ButtonDefaults.buttonColors(containerColor = accentColor)) {
                                Text("যোগ করুন")
                            }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                        items(mentors) { mentor ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp).background(Color.LightGray, CircleShape).padding(8.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(mentor.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(mentor.subjects, color = Color.Gray, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                        item {
                            OutlinedButton(
                                onClick = { showAddMentor = true },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("নতুন মেন্টর যোগ করুন")
                            }
                        }
                    }
                }
            }
        }
    }
}