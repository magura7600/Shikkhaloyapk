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
    var editingCourse by remember { mutableStateOf<CourseItem?>(null) }
    var initialSubjectId by remember { mutableStateOf<String?>(null) }
    var initialChapterId by remember { mutableStateOf<String?>(null) }
    var initialClassId by remember { mutableStateOf<String?>(null) }
    var showMentorsDialog by remember { mutableStateOf(false) }
    var showPublishUpdateDialog by remember { mutableStateOf(false) }
    var showPublishNoticeDialog by remember { mutableStateOf(false) }
    var showAllNotificationsDialog by remember { mutableStateOf(false) }
    var mentors by remember { mutableStateOf(listOf<Mentor>()) }
    val isTeacher = profile.role == "teacher"
    val isAdmin = profile.role == "admin"
    val isManagementUser = isTeacher || isAdmin
    var teacherChannel by remember { mutableStateOf<UserProfile?>(null) }
    var isLoadingChannel by remember { mutableStateOf(isTeacher) }

    val context = LocalContext.current
    val sharedPrefs = remember { PrefUtils.getSecurePrefs(context) }

    // Read cached values immediately on start to prevent blank state!
    val cachedCoursesJson = remember { sharedPrefs.getString("cached_courses_${profile.user_id}", null) }
    val cachedEnrollmentsJson = remember { sharedPrefs.getString("cached_enrollments_${profile.user_id}", null) }
    val cachedFocusCourseId = remember { sharedPrefs.getString("cached_focus_course_id_${profile.user_id}", null) }

    val initialCourses = remember(cachedCoursesJson) {
        if (!cachedCoursesJson.isNullOrBlank()) {
            try {
                Json.decodeFromString<List<CourseItem>>(cachedCoursesJson)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    val initialEnrollments = remember(cachedEnrollmentsJson) {
        if (!cachedEnrollmentsJson.isNullOrBlank()) {
            try {
                Json.decodeFromString<List<Enrollment>>(cachedEnrollmentsJson)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    var courses by remember { mutableStateOf(initialCourses) }
    var allChannels by remember { mutableStateOf(listOf<UserProfile>()) }
    var enrollments by remember { mutableStateOf(initialEnrollments) }

    var enrollmentRequests by remember { mutableStateOf(listOf<EnrollmentRequest>()) }

    var courseInteractions by remember { mutableStateOf(listOf<CourseInteraction>()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isOffline by remember { mutableStateOf(false) }
    var hasPromptedOffline by remember { mutableStateOf(false) }
    var showOfflineDownloadsGlobal by remember { mutableStateOf(false) }
    var isInitialLoadComplete by remember { mutableStateOf(initialCourses.isNotEmpty()) }
    val coroutineScope = rememberCoroutineScope()
    var lastBackPressTime by remember { mutableStateOf(0L) }
    val activity = context as? ComponentActivity

    BackHandler {
        if (currentScreen != "dashboard") {
            currentScreen = "dashboard"
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 3000) {
                activity?.finish()
            } else {
                lastBackPressTime = currentTime
                Toast.makeText(context, "আবার ক্লিক করলে এপ কেটে যাবে", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var focusCourseId by remember { mutableStateOf(cachedFocusCourseId) }
    var hasLoadedAllCourses by remember { mutableStateOf(false) }

    // Sync OneSignal User identity and purchased course tags so they only receive notifications of courses they bought
    LaunchedEffect(enrollments, profile.user_id) {
        val myEnrolledCourseIds = enrollments.filter { it.user_id == profile.user_id }.map { it.course_id }
        if (focusCourseId == null && myEnrolledCourseIds.isNotEmpty()) {
            focusCourseId = myEnrolledCourseIds.firstOrNull()
            try {
                sharedPrefs.edit().putString("cached_focus_course_id_${profile.user_id}", focusCourseId).apply()
            } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) }
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

    var enteredWithNetwork by remember { mutableStateOf(false) }
    LaunchedEffect(isOffline, enteredWithNetwork, hasPromptedOffline) {
        if (isOffline && !enteredWithNetwork && !hasPromptedOffline) {
            var elapsed = 0
            while (elapsed < 120) {
                kotlinx.coroutines.delay(1000)
                elapsed += 1
                if (!isOffline || enteredWithNetwork || hasPromptedOffline) return@LaunchedEffect
            }
            showOfflineDownloadsGlobal = true
            hasPromptedOffline = true
            Toast.makeText(context, "কোনো ইন্টারনেট সংযোগ নেই! অফলাইন মোড চালু করা হয়েছে।", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        NetworkUtils.observeInternetAccess(context).collect { hasInternet ->
            isOffline = !hasInternet
            if (hasInternet) {
                enteredWithNetwork = true
                hasPromptedOffline = false
            }
        }
    }

    // Data Loading Logic optimized to truly only load what's needed for the current screen
    LaunchedEffect(currentScreen, selectedTab, focusCourseId, selectedCourse?.id, isOffline, enteredWithNetwork, profile.user_id) {
        try {
            // 1. My Enrollments (needed globally for a student to know what they own)
            if (!isTeacher && !isAdmin) {
                 val newEnrollments = withContext(Dispatchers.IO) {
                     try {
                         supabase.from("enrollments").select { filter { eq("user_id", profile.user_id) } }.decodeList<Enrollment>()
                     } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error enrollments", e); enrollments }
                 }
                 if (newEnrollments != enrollments) {
                     enrollments = newEnrollments
                     try {
                         sharedPrefs.edit().putString("cached_enrollments_${profile.user_id}", Json.encodeToString(newEnrollments)).apply()
                     } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) }
                 }
                 
                 val myRequests = withContext(Dispatchers.IO) {
                     try { supabase.from("enrollment_requests").select { filter { eq("user_id", profile.user_id) } }.decodeList<EnrollmentRequest>() } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error enrollmentsReq", e); enrollmentRequests }
                 }
                 if (myRequests != enrollmentRequests) enrollmentRequests = myRequests
            }

            // 2. All Enrollments & Requests (ONLY needed if Teacher/Admin goes to Management)
            if ((isTeacher || isAdmin)) {
                 enrollments = withContext(Dispatchers.IO) {
                     try { supabase.from("enrollments").select().decodeList<Enrollment>() } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error enrollments", e); enrollments }
                 }
                 if (selectedTab == 2 || currentScreen == "enrollment_requests") {
                     enrollmentRequests = withContext(Dispatchers.IO) {
                         try { supabase.from("enrollment_requests").select().decodeList<EnrollmentRequest>() } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error enrollmentsReq", e); enrollmentRequests }
                     }
                 }
            }

            // 3. Courses
            val newCourses = withContext(Dispatchers.IO) {
                try { supabase.from("courses").select().decodeList<CourseItem>() } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error courses", e); courses }
            }
            val mappedCourses = if (isTeacher || isAdmin) {
                newCourses.map { c -> c.copy(studentsCount = enrollments.count { it.course_id == c.id }) }
            } else {
                newCourses
            }
            if (mappedCourses != courses) {
                courses = mappedCourses
                try {
                    sharedPrefs.edit().putString("cached_courses_${profile.user_id}", Json.encodeToString(mappedCourses)).apply()
                } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) }
            }
            hasLoadedAllCourses = true

            // 4. Channels
            allChannels = withContext(Dispatchers.IO) {
                try {
                    supabase.from("public_channels").select().decodeList<UserProfile>()
                } catch (e: Exception) {
                    try {
                        supabase.from("profiles").select().decodeList<UserProfile>().filter { it.handle != null && it.handle.isNotBlank() }
                    } catch (ex: Exception) {
                        allChannels
                    }
                }
            }

            // 5. Course Interactions & Mentors (ONLY needed for Course Detail screen)
            if (currentScreen == "course_detail" && selectedCourse != null) {
                 courseInteractions = withContext(Dispatchers.IO) {
                     try { selectedCourse?.id?.let { cid -> supabase.from("course_interactions").select { filter { eq("course_id", cid) } }.decodeList<CourseInteraction>() } ?: courseInteractions } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error interactions", e); courseInteractions }
                 }
                 mentors = withContext(Dispatchers.IO) {
                     try {
                         val cid = selectedCourse?.channel_id
                         if (!cid.isNullOrBlank()) {
                             supabase.from("mentors").select { filter { eq("channel_id", cid) } }.decodeList<Mentor>()
                         } else {
                             supabase.from("mentors").select().decodeList<Mentor>()
                         }
                     } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error mentors", e); mentors }
                 }
            }
        } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) } finally {
            isInitialLoadComplete = true
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
                teacherChannel = fetchedChannels.firstOrNull { it.handle != null && it.handle.isNotBlank() }
                
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
    if (!isInitialLoadComplete && !isOffline) {
        ShikkhaloySplashScreen()
        return
    }
    
    val bgGradient = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
    )
    
    val bgColor = Color.Transparent
    val accentColor = MaterialTheme.colorScheme.primary

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (currentScreen == "dashboard") {
                Column(modifier = Modifier.background(Color.White)) {
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
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
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
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 15.sp,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 140.dp)
                                            ) 
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Expand", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
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
                                                        try {
                                                            sharedPrefs.edit().putString("cached_focus_course_id_${profile.user_id}", course.id).apply()
                                                        } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) }
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
                                        color = MaterialTheme.colorScheme.primary,
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
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 18.sp
                                ) 
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAllNotificationsDialog = true }) {
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
                        containerColor = Color.White
                    )
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                if (isOffline) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showOfflineDownloadsGlobal = true }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "Offline Mode",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "কোনো ইন্টারনেট সংযোগ নেই! আপনি অফলাইন মোডে আছেন।",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ডাউনলোডকৃত ক্লাস দেখুন >",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.error, thickness = 1.dp)
                }
                }
            }
        },
        bottomBar = {
            if (currentScreen == "dashboard") {
                if (!isManagementUser) {
                val studentNavItems = listOf(
                    BottomNavItem("হোম", Icons.Outlined.Home, MaterialTheme.colorScheme.secondary),
                    BottomNavItem("কোর্স", Icons.Outlined.MenuBook, MaterialTheme.colorScheme.primary),
                    BottomNavItem("ম্যানেজমেন্ট", Icons.Outlined.Dashboard, MaterialTheme.colorScheme.error),
                    BottomNavItem("এক্সপ্লোর", Icons.Outlined.Explore, MaterialTheme.colorScheme.primary),
                    BottomNavItem("সেটিংস", Icons.Outlined.Settings, MaterialTheme.colorScheme.primary)
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
                    BottomNavItem("চ্যানেল", Icons.Outlined.Home, MaterialTheme.colorScheme.secondary),
                    BottomNavItem("কোর্স", Icons.Outlined.MenuBook, MaterialTheme.colorScheme.primary),
                    BottomNavItem("ম্যানেজমেন্ট", Icons.Outlined.Dashboard, MaterialTheme.colorScheme.error),
                    BottomNavItem("সেটিংস", Icons.Outlined.Settings, MaterialTheme.colorScheme.primary)
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
                    contentColor = Color.White,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Course")
                }
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                coroutineScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            val newCourses = try { supabase.from("courses").select().decodeList<CourseItem>() } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error courses", e); courses }
                            val newEnrollments = try {
                                if (isTeacher || isAdmin) {
                                    supabase.from("enrollments").select().decodeList<Enrollment>()
                                } else {
                                    supabase.from("enrollments").select { filter { eq("user_id", profile.user_id) } }.decodeList<Enrollment>()
                                }
                            } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error enrollments", e); enrollments }
                            val newRequests = try { 
                                if (isTeacher || isAdmin) {
                                    supabase.from("enrollment_requests").select().decodeList<EnrollmentRequest>()
                                } else {
                                    supabase.from("enrollment_requests").select { filter { eq("user_id", profile.user_id) } }.decodeList<EnrollmentRequest>()
                                }
                            } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error enrollmentsReq", e); enrollmentRequests }
                            
                            withContext(Dispatchers.Main) {
                                if (newEnrollments != enrollments) {
                                    enrollments = newEnrollments
                                    try {
                                        sharedPrefs.edit().putString("cached_enrollments_${profile.user_id}", Json.encodeToString(newEnrollments)).apply()
                                    } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) }
                                }
                                val mappedCourses = if (isTeacher || isAdmin) {
                                    newCourses.map { c -> c.copy(studentsCount = newEnrollments.count { it.course_id == c.id }) }
                                } else {
                                    newCourses
                                }
                                if (mappedCourses != courses) {
                                    courses = mappedCourses
                                    try {
                                        sharedPrefs.edit().putString("cached_courses_${profile.user_id}", Json.encodeToString(mappedCourses)).apply()
                                    } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) }
                                }
                                enrollmentRequests = newRequests
                            }
                        }
                    } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) } finally {
                        isRefreshing = false
                    }
                }
            },
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Content will draw behind transparent TopAppBar and BottomAppBar
            ) {
            if (currentScreen == "add_course" || currentScreen == "edit_course") {
                AddCourseScreen(
                    profile = profile,
                    accentColor = accentColor,
                    initialCourse = if (currentScreen == "edit_course") editingCourse else null,
                    onBack = { 
                        currentScreen = "dashboard"
                        editingCourse = null
                    },
                    onCourseAdded = { newCourse ->
                        val isEditMode = currentScreen == "edit_course"
                        val courseToSave = newCourse.copy(channel_id = teacherChannel?.user_id)
                        coroutineScope.launch {
                            try {
                                if (isEditMode) {
                                    withContext(Dispatchers.IO) {
                                        supabase.from("courses").update(courseToSave) {
                                            filter { eq("id", courseToSave.id) }
                                        }
                                    }
                                    courses = courses.map { if (it.id == courseToSave.id) courseToSave else it }
                                } else {
                                    withContext(Dispatchers.IO) {
                                        supabase.from("courses").insert(courseToSave)
                                    }
                                    courses = courses + courseToSave
                                }
                                currentScreen = "dashboard"
                                editingCourse = null
                            } catch (e: Exception) {
                                val msg = e.message ?: ""
                                if (msg.contains("subjects") || msg.contains("quarters") || msg.contains("isQuarterOn")) {
                                    Toast.makeText(context, "Error: Supabase-এ কলাম অনুপস্থিত বা ত্রুটি! (${e.message})", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Error saving course: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )
            } else if (currentScreen == "select_course_for_students") {
                CourseListScreen(
                    accentColor = accentColor,
                    courses = courses.filter { it.channel_id == profile.user_id },
                    title = "শিক্ষার্থীদের পরিচালনা",
                    subtitle = "শিক্ষার্থী পরিচালনা করতে একটি কোর্স নির্বাচন করুন",
                    onCourseClick = { course ->
                        selectedCourse = course
                        currentScreen = "manage_students"
                    }
                )
            } else if (currentScreen == "add_class_link") {
                AddClassLinkScreen(
                    courses = courses.filter { it.channel_id == profile.user_id },
                    accentColor = accentColor,
                    onCourseUpdate = { updatedCourse ->
                        coroutineScope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    supabase.from("courses").update(updatedCourse) {
                                        filter { eq("id", updatedCourse.id) }
                                    }
                                }
                                val newCourses = courses.map { if (it.id == updatedCourse.id) updatedCourse else it }
                                courses = newCourses
                                Toast.makeText(context, "ক্লাস লিংক সফলভাবে আপডেট করা হয়েছে", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error updating link: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onBack = { currentScreen = "dashboard" }
                )
            } else if (currentScreen == "manage_students" && selectedCourse != null) {
                val currentSelectedCourse = selectedCourse
                if (currentSelectedCourse != null) {
                    ManageStudentsScreen(
                        course = currentSelectedCourse,
                        accentColor = accentColor,
                        onBack = { 
                            currentScreen = "dashboard"
                            selectedCourse = null
                        }
                    )
                }
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
                                    val updatedProf = fetchedChannels.firstOrNull()
                                    if (updatedProf != null) {
                                        onProfileUpdate(updatedProf)
                                        teacherChannel = if (updatedProf.handle != null && updatedProf.handle.isNotBlank()) updatedProf else null
                                    }
                                } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) } finally {
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
            } else if (currentScreen == "channel_detail") {
                val activeChannel = selectedChannel
                if (activeChannel != null) {
                    ChannelDetailScreen(
                        channel = activeChannel,
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
                }
            } else if (currentScreen == "course_detail") {
                val activeCourse = selectedCourse
                if (activeCourse != null) {
                    LaunchedEffect(activeCourse.id) {
                        val hasView = courseInteractions.any { it.course_id == activeCourse.id && it.user_id == profile.user_id && !it.is_like }
                        if (!hasView) {
                            try {
                                val newView = CourseInteraction(user_id = profile.user_id, course_id = activeCourse.id, is_like = false)
                                withContext(Dispatchers.IO) {
                                    supabase.from("course_interactions").insert(newView)
                                }
                                courseInteractions = courseInteractions + newView
                            } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) }
                        }
                    }
                    CourseDetailScreen(
                        initialCourse = activeCourse,
                        profile = profile,
                        userEnrollment = enrollments.find { it.course_id == activeCourse.id && it.user_id == profile.user_id },
                        initialSubjectId = initialSubjectId,
                        initialChapterId = initialChapterId,
                        initialClassId = initialClassId,
                        onClearInitialNavigation = {
                            initialSubjectId = null
                            initialChapterId = null
                            initialClassId = null
                        },
                        pendingRequest = enrollmentRequests.find { it.course_id == activeCourse.id && it.user_id == profile.user_id },
                    onPurchaseClick = { currentScreen = "purchase_course" },
                    onEnroll = { purchasedQuarters ->
                        coroutineScope.launch {
                            try {
                                val currentCourse = selectedCourse ?: return@launch
                                if (currentCourse.pricingOption != "Fully Free") {
                                    Toast.makeText(context, "পেইড কোর্সে সরাসরি এনরোলমেন্ট অনুমোদিত নয়!", Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                                val pricePaid = "0"
                                val enrollment = Enrollment(user_id = profile.user_id, course_id = currentCourse.id, price_paid = pricePaid, purchased_quarters = purchasedQuarters)
                                withContext(Dispatchers.IO) {
                                    supabase.from("enrollments").insert(enrollment)
                                }
                                enrollments = enrollments + enrollment
                                courses = courses.map { if (it.id == currentCourse.id) it.copy(studentsCount = it.studentsCount + 1) else it }
                                selectedCourse = courses.find { it.id == currentCourse.id }
                                Toast.makeText(context, "Successfully Enrolled!", Toast.LENGTH_SHORT).show()
                            } catch(e: Exception) {
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
                }
            }
            
            val activeCourseForPurchase = selectedCourse
            if (currentScreen == "purchase_course" && activeCourseForPurchase != null) {
                PurchaseCourseScreen(
                    course = activeCourseForPurchase,
                    profile = profile,
                    accentColor = accentColor,
                    onBack = { currentScreen = "course_detail" },
                    onPurchaseSubmitted = {
                        coroutineScope.launch {
                            try {
                                enrollmentRequests = supabase.from("enrollment_requests").select().decodeList<EnrollmentRequest>()
                            } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) }
                        }
                        currentScreen = "course_detail"
                    }
                )
            } else if (currentScreen == "enrollment_requests" && teacherChannel != null) {
                EnrollmentRequestsScreen(
                    teacherChannel = profile,
                    requests = enrollmentRequests,
                    courses = courses,
                    accentColor = accentColor,
                    onBack = { currentScreen = "dashboard" },
                    onUpdateRequests = {
                        coroutineScope.launch {
                            try {
                                enrollmentRequests = supabase.from("enrollment_requests").select().decodeList<EnrollmentRequest>()
                                val fetched = supabase.from("enrollments").select().decodeList<Enrollment>()
                                enrollments = fetched
                            } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) }
                        }
                    }
                )
            } else if (currentScreen == "my_enrollments") {
                MyEnrollmentsScreen(
                    profile = profile,
                    enrollments = enrollments,
                    requests = enrollmentRequests,
                    courses = courses,
                    accentColor = accentColor,
                    onBack = { currentScreen = "dashboard" }
                )
            } else {
                if (isManagementUser) {
                    if (selectedTab == 0) {
                        ExploreFeedScreen(
                            accentColor = accentColor, 
                            profile = profile, 
                            actingChannel = teacherChannel, 
                            courses = courses,
                            allChannels = allChannels,
                            courseInteractions = courseInteractions,
                            onLikeToggle = { course ->
                                coroutineScope.launch {
                                    try {
                                        val existing = courseInteractions.find { it.course_id == course.id && it.user_id == profile.user_id && it.is_like }
                                        if (existing != null) {
                                            withContext(Dispatchers.IO) {
                                                supabase.from("course_interactions").delete {
                                                    filter { eq("id", existing.id) }
                                                }
                                            }
                                            courseInteractions = courseInteractions.filter { it.id != existing.id }
                                        } else {
                                            val newInteraction = CourseInteraction(user_id = profile.user_id, course_id = course.id, is_like = true)
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
                        if (isAdmin) {
                            AdminDashboardContent(
                                accentColor = accentColor,
                                onPublishUpdateClick = { showPublishUpdateDialog = true },
                                onPublishNoticeClick = { showPublishNoticeDialog = true }
                            )
                        } else {
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
                                onAddClassLinkClick = { currentScreen = "add_class_link" },
                                onMentorsClick = { showMentorsDialog = true },
                                onManageStudentsClick = { currentScreen = "select_course_for_students" },
                                onEnrollmentRequestsClick = { currentScreen = "enrollment_requests" }
                            )
                        }
                    } else if (selectedTab == 3) {
                        CourseListScreen(
                            accentColor = accentColor, 
                            courses = if (isAdmin) courses else courses.filter { it.channel_id == profile.user_id },
                            onCourseClick = { course ->
                                selectedCourse = course
                                initialSubjectId = null
                                initialChapterId = null
                                initialClassId = null
                                currentScreen = "course_detail"
                            },
                            onEditCourse = { course ->
                                editingCourse = course
                                currentScreen = "edit_course"
                            },
                            onDeleteCourse = { course ->
                                coroutineScope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            // Delete related enrollments
                                            try {
                                                supabase.from("enrollments").delete {
                                                    filter { eq("course_id", course.id) }
                                                }
                                            } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) }
                                            
                                            // Delete related interactions
                                            try {
                                                supabase.from("course_interactions").delete {
                                                    filter { eq("course_id", course.id) }
                                                }
                                            } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) }

                                            // Delete related enrollment_requests
                                            try {
                                                supabase.from("enrollment_requests").delete {
                                                    filter { eq("course_id", course.id) }
                                                }
                                            } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) }

                                            // Delete the course itself
                                            supabase.from("courses").delete {
                                                filter { eq("id", course.id) }
                                            }
                                        }
                                        courses = courses.filter { it.id != course.id }
                                        enrollmentRequests = enrollmentRequests.filter { it.course_id != course.id }
                                        Toast.makeText(context, "কোর্স মুছে ফেলা হয়েছে!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error deleting course: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
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
                            enrollments = enrollments,
                            onNavigateToMyEnrollments = { currentScreen = "my_enrollments" },
                            onTeacherChannelSetupClick = { currentScreen = "create_channel" }
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
                        MyEnrollmentsScreen(
                            profile = profile,
                            enrollments = enrollments,
                            requests = enrollmentRequests,
                            courses = courses,
                            accentColor = accentColor,
                            onBack = { selectedTab = 0 }
                        )
                    } else if (selectedTab == 3) {
                        ExploreFeedScreen(
                            accentColor = accentColor, 
                            profile = profile, 
                            courses = courses,
                            allChannels = allChannels,
                            courseInteractions = courseInteractions,
                            onLikeToggle = { course ->
                                coroutineScope.launch {
                                    try {
                                        val existing = courseInteractions.find { it.course_id == course.id && it.user_id == profile.user_id && it.is_like }
                                        if (existing != null) {
                                            withContext(Dispatchers.IO) {
                                                supabase.from("course_interactions").delete {
                                                    filter { eq("id", existing.id) }
                                                }
                                            }
                                            courseInteractions = courseInteractions.filter { it.id != existing.id }
                                        } else {
                                            val newInteraction = CourseInteraction(user_id = profile.user_id, course_id = course.id, is_like = true)
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
                        ,
                            onNavigateToMyEnrollments = { currentScreen = "my_enrollments" }
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
        }
    
    if (showOfflineDownloadsGlobal) {
        OfflineDownloadsDialog(
            onDismiss = { showOfflineDownloadsGlobal = false },
            accentColor = accentColor
        )
    }

    if (showMentorsDialog) {
        MentorsListDialog(
            mentors = mentors,
            onAddMentor = { newMentor -> 
                val mentorWithChannel = newMentor.copy(channel_id = profile.user_id)
                coroutineScope.launch {
                    try {
                        val fetchedMentors = withContext(Dispatchers.IO) {
                            supabase.from("mentors").insert(mentorWithChannel)
                            supabase.from("mentors").select {
                                filter { eq("channel_id", profile.user_id) }
                            }.decodeList<Mentor>()
                        }
                        mentors = fetchedMentors
                        Toast.makeText(context, "মেন্টর যোগ করা হয়েছে", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onEditMentor = { editedMentor ->
                coroutineScope.launch {
                    try {
                        val fetchedMentors = withContext(Dispatchers.IO) {
                            supabase.from("mentors").update(editedMentor) {
                                filter {
                                    eq("id", editedMentor.id)
                                }
                            }
                            supabase.from("mentors").select {
                                filter { eq("channel_id", profile.user_id) }
                            }.decodeList<Mentor>()
                        }
                        mentors = fetchedMentors
                        Toast.makeText(context, "মেন্টর আপডেট করা হয়েছে", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onDeleteMentor = { mentorToDelete ->
                coroutineScope.launch {
                    try {
                        val fetchedMentors = withContext(Dispatchers.IO) {
                            supabase.from("mentors").delete {
                                filter {
                                    eq("id", mentorToDelete.id)
                                }
                            }
                            supabase.from("mentors").select {
                                filter { eq("channel_id", profile.user_id) }
                            }.decodeList<Mentor>()
                        }
                        mentors = fetchedMentors
                        Toast.makeText(context, "মেন্টর ডিলিট করা হয়েছে", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
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

    if (showAllNotificationsDialog) {
        val activeNotice by produceState<AppNotice?>(initialValue = null) {
            value = AppNoticeManager.getActiveNotice()
        }
        AllNotificationsDialog(
            activeNotice = activeNotice,
            courses = courses,
            enrollments = enrollments,
            profile = profile,
            accentColor = accentColor,
            onDismiss = { showAllNotificationsDialog = false },
            onClassClick = { classInfo, chapter, subject, course ->
                selectedCourse = course
                initialSubjectId = subject.id
                initialChapterId = chapter.id
                initialClassId = classInfo.id
                currentScreen = "course_detail"
            }
        )
    }
}

