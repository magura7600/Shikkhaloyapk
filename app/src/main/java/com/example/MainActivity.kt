package com.example

import android.content.Context
import android.view.WindowManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.FirebaseException
import java.util.concurrent.TimeUnit
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.example.ui.CustomBottomNavigation
import com.example.ui.BottomNavItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
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
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType


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
var appContext: android.content.Context? = null

val supabase by lazy {
    val rawUrl = BuildConfig.SUPABASE_URL
    val url = if (rawUrl.isBlank() || rawUrl.contains("YOUR_SUPABASE_URL")) {
        "https://gputlynskpbbiexbpphj.supabase.co"
    } else {
        rawUrl
    }
    
    val rawKey = BuildConfig.SUPABASE_KEY
    val key = if (rawKey.isBlank() || rawKey.contains("YOUR_SUPABASE_ANON_KEY")) {
        "sb_publishable_NwiSPi0Rl4VAf_B2v5Fp6g_KgwTb_Ol"
    } else {
        rawKey
    }
    createSupabaseClient(
        supabaseUrl = if (url.startsWith("http")) url else "https://gputlynskpbbiexbpphj.supabase.co",
        supabaseKey = key
    ) {
        install(Postgrest)
        install(Auth) {
            scheme = "shikkhaloy"
            host = "login-callback"
            appContext?.let { ctx ->
                sessionManager = SharedPreferencesSessionManager(ctx)
                codeVerifierCache = MyCodeVerifierCache(ctx)
            }
        }
        defaultSerializer = KotlinXSerializer(Json { 
            ignoreUnknownKeys = true 
            coerceInputValues = true 
        })
    }
}

// --- APP UI STATE ---
sealed interface AppState {
    object Splash : AppState
    object Login : AppState
    data class Onboarding(val email: String, val userId: String) : AppState
    data class Dashboard(val email: String, val userId: String, val profile: UserProfile) : AppState
}

class MainActivity : ComponentActivity() {
    private val pipReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
            if (intent.action == "com.example.PIP_CONTROL") {
                val type = intent.getIntExtra("control_type", 0)
                when (type) {
                    1 -> VideoPipState.onPlayPauseToggle?.invoke(true)  // Play
                    2 -> VideoPipState.onPlayPauseToggle?.invoke(false) // Pause
                    3 -> VideoPipState.onRewind?.invoke()               // Rewind 10s
                    4 -> VideoPipState.onForward?.invoke()              // Forward 10s
                }
            }
        }
    }

    fun updatePipParams(isPlaying: Boolean) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val actions = java.util.ArrayList<android.app.RemoteAction>()

            fun createRemoteAction(iconId: Int, title: String, controlType: Int, reqCode: Int): android.app.RemoteAction {
                val intent = android.content.Intent("com.example.PIP_CONTROL").apply {
                    putExtra("control_type", controlType)
                    `package` = packageName
                }
                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    this@MainActivity,
                    reqCode,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                val icon = android.graphics.drawable.Icon.createWithResource(this@MainActivity, iconId)
                return android.app.RemoteAction(icon, title, title, pendingIntent)
            }

            actions.add(
                createRemoteAction(
                    android.R.drawable.ic_media_previous,
                    "Rewind 10s",
                    3,
                    301
                )
            )

            if (isPlaying) {
                actions.add(
                    createRemoteAction(
                        android.R.drawable.ic_media_pause,
                        "Pause",
                        2,
                        302
                    )
                )
            } else {
                actions.add(
                    createRemoteAction(
                        android.R.drawable.ic_media_play,
                        "Play",
                        1,
                        302
                    )
                )
            }

            actions.add(
                createRemoteAction(
                    android.R.drawable.ic_media_next,
                    "Forward 10s",
                    4,
                    304
                )
            )

            try {
                val params = android.app.PictureInPictureParams.Builder()
                    .setActions(actions)
                    .build()
                setPictureInPictureParams(params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(pipReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (ThemeManager.isPipEnabled && VideoPipState.isVideoActive) {
            VideoPipState.onEnterPip?.invoke()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        VideoPipState.isInPip = isInPictureInPictureMode
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        try {
            supabase.handleDeeplinks(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Setup Uncaught Exception Handler to capture any minification or runtime crashes
        try {
            val crashFile = java.io.File(cacheDir, "crash_report.txt")
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    val writer = java.io.StringWriter()
                    throwable.printStackTrace(java.io.PrintWriter(writer))
                    val stackTrace = writer.toString()
                    crashFile.writeText(stackTrace)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                defaultHandler?.uncaughtException(thread, throwable)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        appContext = applicationContext
        try {
            supabase.handleDeeplinks(intent)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        try {
            L.init(this)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        try {
            ThemeManager.init(this)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        // Initialize Coil ImageLoader with cache for faster image loading
        try {
            val imageLoader = coil.ImageLoader.Builder(this)
                .memoryCache {
                    coil.memory.MemoryCache.Builder(this)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    coil.disk.DiskCache.Builder()
                        .directory(this.cacheDir.resolve("image_cache"))
                        .maxSizePercent(0.1)
                        .build()
                }
                .crossfade(true)
                .build()
            coil.Coil.setImageLoader(imageLoader)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Safe OneSignal and Cache Initialization
        try {
            // OneSignal debug logging (optional but highly recommended for setup)
            OneSignal.Debug.logLevel = LogLevel.VERBOSE

            // OneSignal Initialization
            OneSignal.initWithContext(this, "9b18010c-9761-4d89-abfc-ae8a437f4943")
            
            lifecycleScope.launch {
                kotlinx.coroutines.delay(2000) // Delay to ensure no conflict with other dialogs
                OneSignal.Notifications.requestPermission(true)
            }

            // Standard System Permission Request for Storage and Notifications
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val permissions = mutableListOf<String>()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(android.Manifest.permission.READ_MEDIA_VIDEO)
                    permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
                    permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                
                val neededPermissions = permissions.filter {
                    androidx.core.content.ContextCompat.checkSelfPermission(this@MainActivity, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
                }
                
                if (neededPermissions.isNotEmpty()) {
                    androidx.core.app.ActivityCompat.requestPermissions(this@MainActivity, neededPermissions.toTypedArray(), 101)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        try {
            // Clear temporary PDF cache
            OfflineDownloadManager.clearTemporaryCache(this)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        try {
            androidx.core.content.ContextCompat.registerReceiver(
                this,
                pipReceiver,
                android.content.IntentFilter("com.example.PIP_CONTROL"),
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        var initialCrashReport: String? = null
        try {
            val file = java.io.File(cacheDir, "crash_report.txt")
            if (file.exists()) {
                initialCrashReport = file.readText()
                file.delete() // Clear so it only shows once
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            val isDark = ThemeManager.isDarkTheme()
            val colorScheme = if (isDark) ThemeManager.DarkColorScheme else ThemeManager.LightColorScheme

            // Check for previous crash logs
            var crashReport by remember { mutableStateOf(initialCrashReport) }

            if (crashReport != null) {
                MaterialTheme(
                    colorScheme = colorScheme,
                    typography = com.example.ui.theme.Typography,
                    shapes = com.example.ui.theme.Shapes
                ) {
                    androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize()) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { crashReport = null },
                            title = { Text("এপ ক্র্যাশ রিপোর্ট (Error Report)") },
                            text = {
                                Column {
                                    Text("পূর্বে এপটি বন্ধ হয়ে গিয়েছিল। নিচের টেক্সটটি কপি করে ডেভেলপারকে পাঠান:")
                                    Spacer(Modifier.height(8.dp))
                                    androidx.compose.foundation.lazy.LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 300.dp)
                                            .background(Color.LightGray.copy(alpha = 0.2f))
                                            .padding(8.dp)
                                    ) {
                                        item {
                                            Text(
                                                text = crashReport ?: "",
                                                fontSize = 11.sp,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    try {
                                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Crash Report", crashReport)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(this@MainActivity, "ক্র্যাশ রিপোর্ট কপি হয়েছে!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    crashReport = null
                                }) {
                                    Text("কপি করুন")
                                }
                            },
                            dismissButton = {
                                androidx.compose.material3.TextButton(onClick = { crashReport = null }) {
                                    Text("বন্ধ করুন")
                                }
                            }
                        )
                    }
                }
                return@setContent
            }

            MaterialTheme(
                colorScheme = colorScheme,
                typography = com.example.ui.theme.Typography,
                shapes = com.example.ui.theme.Shapes
            ) {
                val bgGradient = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary)
                    } else {
                        listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
                    }
                )
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgGradient),
                    color = Color.Transparent
                ) {
                    MainAppContent()
                }
            }
        }
    }
}

@Composable
fun MainAppContent() {
    val context = LocalContext.current
    val sharedPrefs = remember { PrefUtils.getSecurePrefs(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var appState by remember { mutableStateOf<AppState>(AppState.Splash) }
    var activeUpdateToPrompt by remember { mutableStateOf<AppUpdate?>(null) }
    var activeNoticeToPrompt by remember { mutableStateOf<AppNotice?>(null) }

    // Auto-migrate legacy unencrypted credentials to secure storage
    LaunchedEffect(Unit) {
        try {
            val legacyPrefs = context.getSharedPreferences("shikkhaloy_prefs", Context.MODE_PRIVATE)
            if (legacyPrefs.contains("user_id") && !sharedPrefs.contains("user_id")) {
                val uId = legacyPrefs.getString("user_id", null)
                val email = legacyPrefs.getString("email", null)
                val role = legacyPrefs.getString("role", null)
                val fullName = legacyPrefs.getString("full_name", null)
                val institution = legacyPrefs.getString("institution", "")
                val contact = legacyPrefs.getString("contact", "")
                val uidCode = legacyPrefs.getString("uid_code", null)
                
                sharedPrefs.edit()
                    .putString("user_id", uId)
                    .putString("email", email)
                    .putString("role", role)
                    .putString("full_name", fullName)
                    .putString("institution", institution)
                    .putString("contact", contact)
                    .putString("uid_code", uidCode)
                    .apply()
                    
                // Clear credentials from legacy preferences
                legacyPrefs.edit()
                    .remove("user_id")
                    .remove("email")
                    .remove("role")
                    .remove("full_name")
                    .remove("institution")
                    .remove("contact")
                    .remove("uid_code")
                    .apply()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        
        // Check for updates asynchronously in background
        coroutineScope.launch {
            try {
                val update = AppUpdateManager.checkForUpdate(context)
                if (update != null) {
                    activeUpdateToPrompt = update
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        
        // Check for notices asynchronously in background
        coroutineScope.launch {
            try {
                val notice = AppNoticeManager.getActiveNotice()
                if (notice != null) {
                    activeNoticeToPrompt = notice
                    showNoticeNotification(context, notice)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        // Read cached credentials & sync with Supabase profiles
        val cachedUserId = sharedPrefs.getString("user_id", null)
        val cachedEmail = sharedPrefs.getString("email", null)
        var nextState: AppState = AppState.Login

        if (cachedUserId != null && cachedEmail != null) {
            val cachedRole = sharedPrefs.getString("role", null)
            val cachedFullName = sharedPrefs.getString("full_name", null)
            val cachedInstitution = sharedPrefs.getString("institution", "") ?: ""
            val cachedContact = sharedPrefs.getString("contact", "") ?: ""
            val cachedUidCode = sharedPrefs.getString("uid_code", null)

            if (cachedRole != null && cachedFullName != null && cachedUidCode != null) {
                // Instantly construct profile from cache to let user in immediately!
                val cachedProfile = UserProfile(
                    user_id = cachedUserId,
                    email = cachedEmail,
                    role = cachedRole,
                    full_name = cachedFullName,
                    institution = cachedInstitution,
                    contact = cachedContact,
                    uid_code = cachedUidCode
                )
                nextState = AppState.Dashboard(
                    email = cachedEmail,
                    userId = cachedUserId,
                    profile = cachedProfile
                )

                // Sync with latest DB profile in the background
                coroutineScope.launch {
                    try {
                        val dbProfile = withContext(Dispatchers.IO) {
                            supabase.from("profiles")
                                .select {
                                    filter {
                                        eq("user_id", cachedUserId)
                                    }
                                }.decodeSingleOrNull<UserProfile>()
                        }
                        if (dbProfile != null) {
                            // Sync cache
                            sharedPrefs.edit()
                                .putString("role", dbProfile.role)
                                .putString("full_name", dbProfile.full_name)
                                .putString("institution", dbProfile.institution)
                                .putString("contact", dbProfile.contact)
                                .putString("uid_code", dbProfile.uid_code)
                                .apply()
                            
                            // If profile details changed, update dashboard state seamlessly
                            if (appState is AppState.Dashboard) {
                                appState = AppState.Dashboard(
                                    email = cachedEmail,
                                    userId = cachedUserId,
                                    profile = dbProfile
                                )
                            }
                        } else {
                            // User deleted from DB, sign out
                            try {
                                withContext(Dispatchers.IO) {
                                    supabase.auth.signOut()
                                }
                            } catch (e: Throwable) { android.util.Log.e("SilentCatch", "Error", e) }
                            sharedPrefs.edit().clear().apply()
                            appState = AppState.Login
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            } else {
                // Incomplete cache, try to fetch from DB
                var dbProfile: UserProfile? = null
                try {
                    dbProfile = withContext(Dispatchers.IO) {
                        supabase.from("profiles")
                            .select {
                                filter {
                                    eq("user_id", cachedUserId)
                                }
                            }.decodeSingleOrNull<UserProfile>()
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }

                if (dbProfile != null) {
                    sharedPrefs.edit()
                        .putString("role", dbProfile.role)
                        .putString("full_name", dbProfile.full_name)
                        .putString("institution", dbProfile.institution)
                        .putString("contact", dbProfile.contact)
                        .putString("uid_code", dbProfile.uid_code)
                        .apply()

                    nextState = AppState.Dashboard(
                        email = cachedEmail,
                        userId = cachedUserId,
                        profile = dbProfile
                    )
                } else {
                    nextState = AppState.Login
                }
            }
        }

        // Sleek minimum splash duration of 800ms
        val elapsed = System.currentTimeMillis() - startTime
        val remaining = 800 - elapsed
        if (remaining > 0) {
            kotlinx.coroutines.delay(remaining)
        }

        // Only apply if nothing else has moved us on from the splash screen
        if (appState is AppState.Splash) {
            appState = nextState
        }
    }

    val handleLoginSuccess: (String, String) -> Unit = { email, userId ->
        coroutineScope.launch {
            if (userId.startsWith("demo_")) {
                val role = if (userId.contains("teacher")) "teacher" else "student"
                val demoProfile = UserProfile(
                    user_id = userId,
                    email = email,
                    role = role,
                    full_name = if (role == "teacher") "ডেমো শিক্ষক (Demo Teacher)" else "ডেমো শিক্ষার্থী (Demo Student)",
                    institution = "শিক্ষালয় ডেমো কলেজ",
                    contact = "01700000000",
                    uid_code = if (role == "teacher") "SL-999999" else "SL-111111",
                    profile_image_url = null,
                    handle = if (role == "teacher") "demoteacher" else null,
                    description = if (role == "teacher") "This is a demo teacher profile" else null,
                    cover_image_url = null,
                    is_banned = false
                )
                sharedPrefs.edit()
                    .putString("user_id", userId)
                    .putString("email", email)
                    .putString("role", demoProfile.role)
                    .putString("full_name", demoProfile.full_name)
                    .putString("institution", demoProfile.institution)
                    .putString("contact", demoProfile.contact)
                    .putString("uid_code", demoProfile.uid_code)
                    .apply()

                appState = AppState.Dashboard(
                    email = email,
                    userId = userId,
                    profile = demoProfile
                )
                return@launch
            }

            var dbProfile: UserProfile? = null
            var dbFetchSuccess = false
            try {
                withContext(Dispatchers.IO) {
                    dbProfile = supabase.from("profiles")
                        .select {
                            filter {
                                eq("user_id", userId)
                            }
                        }.decodeSingleOrNull<UserProfile>()
                }
                dbFetchSuccess = true
            } catch (e: Exception) {
                // DB error (table not found or network)
            }

            if (dbProfile != null) {
                val profileVal = dbProfile
                // Save complete login state
                sharedPrefs.edit()
                    .putString("user_id", userId)
                    .putString("email", email)
                    .putString("role", profileVal.role)
                    .putString("full_name", profileVal.full_name)
                    .putString("institution", profileVal.institution ?: "")
                    .putString("contact", profileVal.contact ?: "")
                    .putString("uid_code", profileVal.uid_code)
                    .apply()

                appState = AppState.Dashboard(
                    email = email,
                    userId = userId,
                    profile = profileVal
                )
            } else {
                if (dbFetchSuccess) {
                    // Fetch was successful, but returned null. 
                    // This means they have a valid Supabase Auth session but no profile in 'profiles' table.
                    // This is a new or deleted user. Send them to onboarding to complete their profile.
                    sharedPrefs.edit()
                        .putString("user_id", userId)
                        .putString("email", email)
                        .apply()
                    appState = AppState.Onboarding(email, userId)
                } else {
                    // DB check failed (offline or network error)
                    // Fallback to cache if available
                    val cachedRole = sharedPrefs.getString("role", null)
                    val cachedFullName = sharedPrefs.getString("full_name", null)
                    val cachedUidCode = sharedPrefs.getString("uid_code", null)
                    if (cachedRole != null && cachedFullName != null && cachedUidCode != null) {
                        val cachedProfile = UserProfile(
                            user_id = userId,
                            email = email,
                            role = cachedRole,
                            full_name = cachedFullName,
                            institution = sharedPrefs.getString("institution", "") ?: "",
                            contact = sharedPrefs.getString("contact", "") ?: "",
                            uid_code = cachedUidCode
                        )
                        appState = AppState.Dashboard(
                            email = email,
                            userId = userId,
                            profile = cachedProfile
                        )
                    } else {
                        // Offline but no cache, send to Login
                        appState = AppState.Login
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        supabase.auth.sessionStatus.collect { status ->
            if (status is io.github.jan.supabase.auth.status.SessionStatus.Authenticated) {
                val email = status.session.user?.email ?: ""
                val userId = status.session.user?.id ?: ""
                if (appState is AppState.Login || appState is AppState.Splash) {
                    handleLoginSuccess(email, userId)
                }
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
                coroutineScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            supabase.auth.signOut()
                        }
                    } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) }
                }
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
                        onLoginSuccess = handleLoginSuccess
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
                        },
                        onLogout = {
                            coroutineScope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        supabase.auth.signOut()
                                    }
                                } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) }
                            }
                            sharedPrefs.edit().clear().apply()
                            appState = AppState.Login
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
                            coroutineScope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        supabase.auth.signOut()
                                    }
                                } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) }
                            }
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

    val userIsAdmin = remember(appState) {
        val state = appState
        if (state is AppState.Dashboard) {
            state.profile.role == "admin"
        } else {
            false
        }
    }

    val currentUpdate = activeUpdateToPrompt
    if (currentUpdate != null) {
        UpdatePromptDialog(
            update = currentUpdate,
            accentColor = MaterialTheme.colorScheme.primary,
            isAdmin = userIsAdmin,
            onDismiss = { activeUpdateToPrompt = null }
        )
    }

    val currentNotice = activeNoticeToPrompt
    if (currentNotice != null) {
        EmergencyNoticeDialog(
            notice = currentNotice,
            accentColor = MaterialTheme.colorScheme.primary,
            onDismiss = { activeNoticeToPrompt = null }
        )
    }
}

