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
            } catch (e: Exception) {}
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
                     } catch(e: Exception) { enrollments }
                 }
                 if (newEnrollments != enrollments) {
                     enrollments = newEnrollments
                     try {
                         sharedPrefs.edit().putString("cached_enrollments_${profile.user_id}", Json.encodeToString(newEnrollments)).apply()
                     } catch (e: Exception) { e.printStackTrace() }
                 }
                 
                 val myRequests = withContext(Dispatchers.IO) {
                     try { supabase.from("enrollment_requests").select { filter { eq("user_id", profile.user_id) } }.decodeList<EnrollmentRequest>() } catch(e: Exception) { enrollmentRequests }
                 }
                 if (myRequests != enrollmentRequests) enrollmentRequests = myRequests
            }

            // 2. All Enrollments & Requests (ONLY needed if Teacher/Admin goes to Management)
            if ((isTeacher || isAdmin)) {
                 enrollments = withContext(Dispatchers.IO) {
                     try { supabase.from("enrollments").select().decodeList<Enrollment>() } catch(e: Exception) { enrollments }
                 }
                 if (selectedTab == 2 || currentScreen == "enrollment_requests") {
                     enrollmentRequests = withContext(Dispatchers.IO) {
                         try { supabase.from("enrollment_requests").select().decodeList<EnrollmentRequest>() } catch(e: Exception) { enrollmentRequests }
                     }
                 }
            }

            // 3. Courses
            val newCourses = withContext(Dispatchers.IO) {
                try { supabase.from("courses").select().decodeList<CourseItem>() } catch(e: Exception) { courses }
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
                } catch (e: Exception) { e.printStackTrace() }
