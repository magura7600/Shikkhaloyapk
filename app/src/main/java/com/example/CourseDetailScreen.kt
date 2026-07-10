package com.example

import java.io.File
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Share

import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.util.Calendar
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.MenuBook
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Share
import android.content.Intent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Download
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.source.MediaSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun CourseDetailScreen(
    course: CourseItem,
    profile: UserProfile,
    mentors: List<Mentor>,
    userEnrollment: Enrollment?,
    isLiked: Boolean,
    courseInteractions: List<CourseInteraction> = emptyList(),
    pendingRequest: EnrollmentRequest? = null,
    onPurchaseClick: () -> Unit = {},
    onEnroll: (purchasedQuarters: String) -> Unit,
    onLikeToggle: () -> Unit,
    onCourseUpdate: (CourseItem) -> Unit,
    onMultipleCoursesUpdate: (List<CourseItem>) -> Unit = {},
    accentColor: Color,
    initialSubjectId: String? = null,
    initialChapterId: String? = null,
    initialClassId: String? = null,
    onClearInitialNavigation: () -> Unit = {},
    onBack: () -> Unit
) {
    var selectedQuarters by remember { mutableStateOf(setOf<CourseQuarter>()) }
    var selectFullCourse by remember { mutableStateOf(true) }
    var isAddingSubjectTopBar by remember { mutableStateOf(false) }
    val mContext = LocalContext.current
    val isTeacher = course.channel_id == profile.user_id

    val activity = LocalContext.current as? androidx.activity.ComponentActivity
    val isAdmin = LocalContext.current.getSharedPreferences("shikkhaloy_prefs", android.content.Context.MODE_PRIVATE).getString("role", "") == "admin"
    androidx.compose.runtime.DisposableEffect(isAdmin) {
        val window = activity?.window
        if (window != null && !isAdmin) {
            window.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            if (window != null && !isAdmin) {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(course) {
        NotificationScheduler.scheduleClassNotifications(mContext, course)
    }
        
    var selectedChapterForView by remember { mutableStateOf<CourseChapter?>(null) }
    var selectedClassForView by remember { mutableStateOf<CourseClass?>(null) }
    val isClassActive = selectedClassForView != null
    val isChapterActive = selectedChapterForView != null

    // Check automatic quarter selection based on current date
    var initialSelectedQuarterName by remember { mutableStateOf<String?>(null) }
    var selectedSubjectForView by remember { mutableStateOf<CourseSubject?>(null) }
    var showLearningResourcesForSubject by remember { mutableStateOf<CourseSubject?>(null) }

    BackHandler(enabled = true) {
        if (selectedClassForView != null) {
            selectedClassForView = null
        } else if (selectedChapterForView != null) {
            selectedChapterForView = null
        } else if (showLearningResourcesForSubject != null) {
            showLearningResourcesForSubject = null
        } else if (selectedSubjectForView != null) {
            selectedSubjectForView = null
        } else {
            onBack()
        }
    }
    LaunchedEffect(course.quarters) {
        if (course.quarters.isNotEmpty()) {
            val currentDate = java.time.LocalDate.now()
            val formatter = java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy")
            
            // Map quarters to their parsed dates and sort them chronologically
            val quartersWithDates = course.quarters.mapNotNull { quarter ->
                try {
                    val start = java.time.LocalDate.parse(quarter.startDate.trim(), formatter)
                    val end = java.time.LocalDate.parse(quarter.endDate.trim(), formatter)
                    Triple(quarter, start, end)
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.second }
            
            val activeQuarter = if (quartersWithDates.isNotEmpty()) {
                // Find the first quarter where the currentDate is on or before the end date.
                // This correctly selects the current active quarter, or the next upcoming quarter if we are in a gap between quarters.
                val found = quartersWithDates.find { (_, _, end) ->
                    !currentDate.isAfter(end)
                }
                // Fallback to the last quarter if all quarters have ended
                found?.first ?: quartersWithDates.last().first
            } else {
                null
            }
            
            if (activeQuarter != null) {
                initialSelectedQuarterName = activeQuarter.name
            }
        }
    }

    // If quarters exist, default to not selecting full course automatically if they want to choose
    LaunchedEffect(course) {
        if (course.isQuarterOn && course.quarters.isNotEmpty()) {
            selectFullCourse = false
        }
    }

    val totalPrice = if (selectFullCourse) {
        course.discountPrice.toDoubleOrNull() ?: course.mainPrice.toDoubleOrNull() ?: 0.0
    } else {
        selectedQuarters.sumOf { it.price.toDoubleOrNull() ?: 0.0 }
    }

    Scaffold(
        topBar = {
            if (!isClassActive && !isChapterActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(Color(0xFFFBF8F1))
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Back Button
                    Card(
                        modifier = Modifier
                            .size(42.dp)
                            .clickable {
                                if (selectedSubjectForView != null) {
                                    selectedSubjectForView = null
                                } else {
                                    onBack()
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF1E293B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Course / Subject Title
                    Text(
                        text = selectedSubjectForView?.title ?: course.title,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        style = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.size(42.dp)) // Right side empty placeholder for balanced alignment
                }
            }
        },
        bottomBar = {
            if (!isClassActive && !isChapterActive && userEnrollment == null && !isTeacher) {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = {
                                if (course.pricingOption == "Fully Free") {
                                    onEnroll("")
                                } else {
                                    onPurchaseClick()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (pendingRequest?.status == "PENDING") Color.Gray else accentColor),
                            shape = RoundedCornerShape(12.dp),
                            enabled = pendingRequest?.status != "PENDING"
                        ) {
                            Text(
                                text = if (pendingRequest?.status == "PENDING") "পেন্ডিং..." else if (course.pricingOption == "Fully Free") "এনরোল করুন (Free)" else "কোর্স কিনুন",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFFBF8F1)
    ) { paddingValues ->
        val activeEnrollment = userEnrollment
        val isUserBanned = activeEnrollment != null && activeEnrollment.banned_until != null && (activeEnrollment.banned_until == -1L || activeEnrollment.banned_until > System.currentTimeMillis())
        
        if (isUserBanned && activeEnrollment != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Block, contentDescription = "Banned", tint = Color.Red, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("আপনি এই কোর্স থেকে সাময়িকভাবে বহিষ্কৃত", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("কারণ: ${activeEnrollment.ban_reason ?: "অজানা"}", fontSize = 16.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    val durationVal = activeEnrollment.banned_until ?: 0L
                    val durationText = if (durationVal == -1L) "সারাজীবনের জন্য" else "সময়কাল: " + java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(durationVal))
                    Text(durationText, fontSize = 14.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            val isInnerActive = isClassActive || isChapterActive || showLearningResourcesForSubject != null
            if (userEnrollment == null && !isTeacher) {
                UnenrolledCourseOverview(
                    course = course,
                    profile = profile,
                    courseInteractions = courseInteractions,
                    onLikeToggle = onLikeToggle,
                    accentColor = accentColor,
                    modifier = Modifier.padding(paddingValues)
                )
            } else if (isInnerActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = paddingValues.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                            end = paddingValues.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                            bottom = paddingValues.calculateBottomPadding(),
                            top = 0.dp
                        )
                        .background(Color(0xFFF1F5F9))
                ) {
                    CourseContentSection(
                        course = course,
                        mentors = mentors,
                        isTeacher = isTeacher,
                        teacherId = profile.user_id,
                        userEnrollment = userEnrollment,
                        onUpdate = { newSubjects: List<CourseSubject> -> onCourseUpdate(course.copy(subjects = newSubjects)) },
                        onMultipleCoursesUpdate = onMultipleCoursesUpdate,
                        accentColor = accentColor,
                        initialSubjectId = initialSubjectId,
                        initialChapterId = initialChapterId,
                        initialClassId = initialClassId,
                        onClearInitialNavigation = onClearInitialNavigation,
                        initialSelectedQuarterName = initialSelectedQuarterName,
                        onCourseUpdate = onCourseUpdate,
                        selectedSubjectForView = selectedSubjectForView,
                        onSelectedSubjectChange = { s: CourseSubject? -> selectedSubjectForView = s },
                        showLearningResourcesForSubject = showLearningResourcesForSubject,
                        onShowLearningResourcesForSubjectChange = { s -> showLearningResourcesForSubject = s },
                        selectedChapterForView = selectedChapterForView,
                        onSelectedChapterChange = { ch: CourseChapter? -> selectedChapterForView = ch },
                        selectedClassForView = selectedClassForView,
                        onSelectedClassChange = { cl: CourseClass? -> selectedClassForView = cl },
                        externalIsAddingSubject = isAddingSubjectTopBar,
                        onExternalAddHandled = { isAddingSubjectTopBar = false },
                        onPurchaseClick = onPurchaseClick
                    )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                CourseContentSection(
                            course = course,
                            mentors = mentors,
                            isTeacher = isTeacher,
                            teacherId = profile.user_id,
                            userEnrollment = userEnrollment,
                            onUpdate = { newSubjects: List<CourseSubject> -> onCourseUpdate(course.copy(subjects = newSubjects)) },
                            onMultipleCoursesUpdate = onMultipleCoursesUpdate,
                            accentColor = accentColor,
                            initialSubjectId = initialSubjectId,
                            initialChapterId = initialChapterId,
                            initialClassId = initialClassId,
                            onClearInitialNavigation = onClearInitialNavigation,
                            initialSelectedQuarterName = initialSelectedQuarterName,
                            onCourseUpdate = onCourseUpdate,
                            selectedSubjectForView = selectedSubjectForView,
                            onSelectedSubjectChange = { s: CourseSubject? -> selectedSubjectForView = s },
                            showLearningResourcesForSubject = showLearningResourcesForSubject,
                            onShowLearningResourcesForSubjectChange = { s -> showLearningResourcesForSubject = s },
                            selectedChapterForView = selectedChapterForView,
                            onSelectedChapterChange = { ch: CourseChapter? -> selectedChapterForView = ch },
                            selectedClassForView = selectedClassForView,
                            onSelectedClassChange = { cl: CourseClass? -> selectedClassForView = cl },
                            externalIsAddingSubject = isAddingSubjectTopBar,
                            onExternalAddHandled = { isAddingSubjectTopBar = false },
                            onPurchaseClick = onPurchaseClick
                        )
            } // Close else for isUserBanned
        }
    }
}
}

