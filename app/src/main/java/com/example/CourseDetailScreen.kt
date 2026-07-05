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

import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.rememberTransformableState
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
    onBack: () -> Unit
) {
    var selectedQuarters by remember { mutableStateOf(setOf<CourseQuarter>()) }
    var selectFullCourse by remember { mutableStateOf(true) }
    var isAddingSubjectTopBar by remember { mutableStateOf(false) }
    val mContext = LocalContext.current
    val isTeacher = course.channel_id == profile.user_id
    
    var selectedChapterForView by remember { mutableStateOf<CourseChapter?>(null) }
    var selectedClassForView by remember { mutableStateOf<CourseClass?>(null) }
    val isClassActive = selectedClassForView != null
    val isChapterActive = selectedChapterForView != null

    // Check automatic quarter selection based on current date
    var initialSelectedQuarterName by remember { mutableStateOf<String?>(null) }
    var selectedSubjectForView by remember { mutableStateOf<CourseSubject?>(null) }

    BackHandler(enabled = true) {
        if (selectedClassForView != null) {
            selectedClassForView = null
        } else if (selectedChapterForView != null) {
            selectedChapterForView = null
        } else if (selectedSubjectForView != null) {
            selectedSubjectForView = null
        } else {
            onBack()
        }
    }
    LaunchedEffect(course.quarters) {
        if (course.quarters.isNotEmpty()) {
            val currentDate = java.time.LocalDate.now()
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val activeQuarter = course.quarters.find { quarter ->
                try {
                    val start = java.time.LocalDate.parse(quarter.startDate, formatter)
                    val end = java.time.LocalDate.parse(quarter.endDate, formatter)
                    !currentDate.isBefore(start) && !currentDate.isAfter(end)
                } catch(e: Exception) {
                    false
                }
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
                        .background(Color(0xFFFBF8F1))
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 4.dp),
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
        val isUserBanned = userEnrollment != null && userEnrollment.banned_until != null && (userEnrollment.banned_until == -1L || userEnrollment.banned_until > System.currentTimeMillis())
        
        if (isUserBanned) {
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
                    Text("কারণ: ${userEnrollment!!.ban_reason ?: "অজানা"}", fontSize = 16.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    val durationText = if (userEnrollment!!.banned_until == -1L) "সারাজীবনের জন্য" else "সময়কাল: " + java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(userEnrollment!!.banned_until!!))
                    Text(durationText, fontSize = 14.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            val isInnerActive = isClassActive || isChapterActive
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
                    .padding(paddingValues)
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
                        initialSelectedQuarterName = initialSelectedQuarterName,
                        onCourseUpdate = onCourseUpdate,
                        selectedSubjectForView = selectedSubjectForView,
                        onSelectedSubjectChange = { s: CourseSubject? -> selectedSubjectForView = s },
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
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
                            initialSelectedQuarterName = initialSelectedQuarterName,
                            onCourseUpdate = onCourseUpdate,
                            selectedSubjectForView = selectedSubjectForView,
                            onSelectedSubjectChange = { s: CourseSubject? -> selectedSubjectForView = s },
                            selectedChapterForView = selectedChapterForView,
                            onSelectedChapterChange = { ch: CourseChapter? -> selectedChapterForView = ch },
                            selectedClassForView = selectedClassForView,
                            onSelectedClassChange = { cl: CourseClass? -> selectedClassForView = cl },
                            externalIsAddingSubject = isAddingSubjectTopBar,
                            onExternalAddHandled = { isAddingSubjectTopBar = false },
                            onPurchaseClick = onPurchaseClick
                        )
                }
            } // Close else for isUserBanned
        }
    }
}
}

@Composable
fun CourseContentSection(
    course: CourseItem,
    mentors: List<Mentor>,
    isTeacher: Boolean,
    teacherId: String,
    userEnrollment: Enrollment?,
    onUpdate: (List<CourseSubject>) -> Unit,
    onMultipleCoursesUpdate: (List<CourseItem>) -> Unit = {},
    accentColor: Color,
    initialSubjectId: String? = null,
    initialChapterId: String? = null,
    initialClassId: String? = null,
    initialSelectedQuarterName: String? = null,
    onCourseUpdate: ((CourseItem) -> Unit)? = null,
    selectedSubjectForView: CourseSubject?,
    onSelectedSubjectChange: (CourseSubject?) -> Unit,
    selectedChapterForView: CourseChapter?,
    onSelectedChapterChange: (CourseChapter?) -> Unit,
    selectedClassForView: CourseClass?,
    onSelectedClassChange: (CourseClass?) -> Unit,
    externalIsAddingSubject: Boolean = false,
    onExternalAddHandled: () -> Unit = {},
    onPurchaseClick: () -> Unit = {}
) {
    val mContext = LocalContext.current
    var subjectToEdit by remember { mutableStateOf<CourseSubject?>(null) }
    var subjectToDelete by remember { mutableStateOf<CourseSubject?>(null) }
    var isAddingSubject by remember { mutableStateOf(false) }
    
    val isFullyPurchased = userEnrollment?.purchased_quarters.isNullOrBlank() && userEnrollment != null
    val purchasedQuartersList = userEnrollment?.purchased_quarters?.split(",")?.map { it.trim() } ?: emptyList()
    val isQuarterLocked = fun(qName: String): Boolean {
        if (isTeacher) return false
        if (course.pricingOption == "Fully Free") return false
        if (userEnrollment == null) return true
        if (isFullyPurchased) return false
        return qName !in purchasedQuartersList
    }

    var subjectToAddChapterTo by remember { mutableStateOf<CourseSubject?>(null) }
    var chapterToEdit by remember { mutableStateOf<Pair<CourseSubject, CourseChapter>?>(null) }
    var chapterToAddClassTo by remember { mutableStateOf<Pair<CourseSubject, CourseChapter>?>(null) }
    var classToEdit by remember { mutableStateOf<Triple<CourseSubject, CourseChapter, CourseClass>?>(null) }
    var isResourcesExpanded by remember(selectedSubjectForView?.id) { mutableStateOf(false) }
    var isAddingResource by remember { mutableStateOf(false) }

    LaunchedEffect(externalIsAddingSubject) {
        if (externalIsAddingSubject) {
            isAddingSubject = true
            onExternalAddHandled()
        }
    }

    LaunchedEffect(initialSubjectId, initialChapterId, initialClassId) {
        if (initialSubjectId != null && initialChapterId != null && initialClassId != null) {
            val s = course.subjects.find { it.id == initialSubjectId }
            val ch = s?.chapters?.find { it.id == initialChapterId }
            val c = ch?.classes?.find { it.id == initialClassId }
            if (s != null && ch != null && c != null) {
                onSelectedSubjectChange(s)
                onSelectedChapterChange(ch)
                onSelectedClassChange(c)
            }
        }
    }

    val currentSubject = course.subjects.find { it.id == selectedSubjectForView?.id } ?: selectedSubjectForView
    val currentChapter = currentSubject?.chapters?.find { it.id == selectedChapterForView?.id } ?: selectedChapterForView
    val currentClass = currentChapter?.classes?.find { it.id == selectedClassForView?.id } ?: selectedClassForView

    val quartersList = if (course.quarters.isNotEmpty()) course.quarters.map { it.name } else listOf("Quarter 1", "Quarter 2", "Quarter 3", "Quarter 4")
    var selectedQuarterName by remember(course, initialSelectedQuarterName) {
        mutableStateOf(initialSelectedQuarterName ?: quartersList.firstOrNull() ?: "Quarter 1")
    }
    var showRoutineDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val syncSubjectToAllCourses = { updatedSubject: CourseSubject ->
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val courses = supabase.from("courses")
                        .select { filter { eq("channel_id", course.channel_id ?: "") } }
                        .decodeList<CourseItem>()
                    
                    val updatedOtherCourses = mutableListOf<CourseItem>()
                    courses.forEach { otherCourse ->
                        if (otherCourse.id != course.id) {
                            val otherCourseSubjects = otherCourse.subjects.toMutableList()
                            val existingIdx = otherCourseSubjects.indexOfFirst { it.id == updatedSubject.id }
                            if (existingIdx != -1) {
                                otherCourseSubjects[existingIdx] = updatedSubject
                                val updatedOtherCourse = otherCourse.copy(subjects = otherCourseSubjects)
                                supabase.from("courses").update(updatedOtherCourse) {
                                    filter { eq("id", otherCourse.id) }
                                }
                                updatedOtherCourses.add(updatedOtherCourse)
                            }
                        }
                    }
                    if (updatedOtherCourses.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            onMultipleCoursesUpdate(updatedOtherCourses)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    if (selectedClassForView != null) {
        val subject = currentSubject ?: selectedSubjectForView!!
        val clazz = currentClass ?: selectedClassForView!!
        ClassDetailView(
            clazz = clazz,
            subject = subject,
            mentors = mentors,
            accentColor = accentColor,
            courseName = course.title,
            onBack = { onSelectedClassChange(null) }
        )
    } else if (selectedChapterForView != null) {
        val subject = currentSubject ?: selectedSubjectForView!!
        val chapter = currentChapter ?: selectedChapterForView!!
        ChapterDetailScreen(
            subject = subject,
            chapter = chapter,
            mentors = mentors,
            isTeacher = isTeacher,
            userEnrollment = userEnrollment,
            accentColor = accentColor,
            onBack = { onSelectedChapterChange(null) },
            onAddClassClick = { chapterToAddClassTo = Pair(subject, chapter) },
            onEditClassClick = { clazz -> classToEdit = Triple(subject, chapter, clazz) },
            onDeleteClassClick = { clazz ->
                val updatedChapter = chapter.copy(classes = chapter.classes.filter { it.id != clazz.id })
                onSelectedChapterChange(updatedChapter)
                
                val updatedSubject = subject.copy(chapters = subject.chapters.map { if (it.id == chapter.id) updatedChapter else it })
                onSelectedSubjectChange(updatedSubject)
                
                val updatedSubjects = course.subjects.map { if (it.id == subject.id) updatedSubject else it }
                onUpdate(updatedSubjects)
                syncSubjectToAllCourses(updatedSubject)
            },
            onViewClassDetail = { clazz ->
                onSelectedClassChange(clazz)
            }
        )
    } else if (selectedSubjectForView == null) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. "রুটিন দেখে নাও →" Banner Button - Premium Gradient Styling
            Card(
                onClick = { showRoutineDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF2563EB), Color(0xFF4F46E5))
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Routine",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "রুটিন দেখে নাও",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "আজকের ক্লাস ও পরীক্ষার সময়সূচী",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Arrow",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp).rotate(180f)
                        )
                    }
                }
            }

            // 2. Selectable Quarter Tabs - Premium Card Layout with Dates & Progress Bars
            if (course.isQuarterOn && course.quarters.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    quartersList.forEach { qName ->
                        val isSelected = selectedQuarterName == qName
                        
                        // Calculate Quarter Progress % dynamically
                        val quarterChapters = course.subjects.flatMap { subj ->
                            subj.chapters.filter { (it.quarter.ifBlank { "Quarter 1" }) == qName }
                        }
                        val totalChapters = quarterChapters.size
                        val completedWeight = quarterChapters.sumOf { ch ->
                            val displayStatus = if (ch.classes.isEmpty()) "পড়ানো হবে" else ch.teachingStatus
                            when (displayStatus) {
                                "পড়ানো শেষ" -> 1.0
                                "পড়ানো হচ্ছে" -> 0.5
                                else -> 0.0
                            }
                        }
                        val progressPercent = if (totalChapters > 0) {
                            ((completedWeight / totalChapters) * 100).toInt()
                        } else {
                            0
                        }
                        val progressText = convertToBengaliDigits(progressPercent.toString()) + "% পড়া হয়েছে"

                        val quarterObj = course.quarters.find { it.name == qName }
                        val dateRangeText = if (quarterObj != null && quarterObj.startDate.isNotBlank() && quarterObj.endDate.isNotBlank()) {
                            "${quarterObj.startDate} - ${quarterObj.endDate}"
                        } else {
                            ""
                        }

                        val cardBgColor = if (isSelected) Color(0xFF2563EB) else Color.White
                        val cardTextColor = if (isSelected) Color.White else Color(0xFF1E293B)
                        val cardSubTextColor = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF64748B)
                        val cardBorderColor = if (isSelected) Color(0xFF1D4ED8) else Color(0xFFE2E8F0)

                        Card(
                            onClick = { selectedQuarterName = qName },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBgColor),
                            border = BorderStroke(1.dp, cardBorderColor),
                            modifier = Modifier
                                .width(200.dp)
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = qName,
                                        color = cardTextColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    
                                    // Dynamic Quarter Status Badge
                                    var quarterStatus = "আনলক"
                                    var statusBgColor = if (isSelected) Color.White.copy(alpha = 0.2f) else Color(0xFFD1FAE5)
                                    var statusTextColor = if (isSelected) Color.White else Color(0xFF065F46)

                                    if (quarterObj != null && quarterObj.startDate.isNotBlank() && quarterObj.endDate.isNotBlank()) {
                                        try {
                                            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                                            val start = java.time.LocalDate.parse(quarterObj.startDate, formatter)
                                            val end = java.time.LocalDate.parse(quarterObj.endDate, formatter)
                                            val today = java.time.LocalDate.now()
                                            if (today.isBefore(start)) {
                                                quarterStatus = "পড়ানো হবে"
                                                statusBgColor = if (isSelected) Color(0xFF93C5FD).copy(alpha = 0.3f) else Color(0xFFDBEAFE)
                                                statusTextColor = if (isSelected) Color.White else Color(0xFF1E3A8A)
                                            } else if (today.isAfter(end)) {
                                                quarterStatus = "পড়ানো শেষ"
                                                statusBgColor = if (isSelected) Color(0xFFD1FAE5).copy(alpha = 0.3f) else Color(0xFFD1FAE5)
                                                statusTextColor = if (isSelected) Color.White else Color(0xFF065F46)
                                            } else {
                                                quarterStatus = "পড়ানো হচ্ছে"
                                                statusBgColor = if (isSelected) Color(0xFFFEF08A).copy(alpha = 0.3f) else Color(0xFFFEF9C3)
                                                statusTextColor = if (isSelected) Color.White else Color(0xFF854D0E)
                                            }
                                        } catch (e: Exception) { }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(statusBgColor, shape = RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = quarterStatus,
                                            color = statusTextColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                if (dateRangeText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = dateRangeText,
                                        color = cardSubTextColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                

                            }
                        }
                    }
                }
            }

            // 3. Subjects Grid with Video and PDF details and Premium Overlays
            val isCurrentQuarterLocked = isQuarterLocked(selectedQuarterName)
            if (isCurrentQuarterLocked) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("এই কোয়ার্টারটি লক করা আছে", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("বিস্তারিত দেখতে কোয়ার্টারটি কিনুন বা আনলক করুন।", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onPurchaseClick,
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("আনলক করুন")
                        }
                    }
                }
            } else if (course.subjects.isEmpty()) {
                Text("এখনো কোনো বিষয়বস্তু যোগ করা হয়নি।", color = Color.Gray, fontSize = 14.sp)
            } else {
                course.subjects.chunked(2).forEach { rowSubjects ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowSubjects.forEach { subject ->
                            val bgColor = try { Color(android.graphics.Color.parseColor(subject.colorHex)) } catch (e: Exception) { Color(0xFFEF4444) }
                            
                            // Calculate dynamic Videos & PDFs inside this subject
                            val totalVideos = subject.chapters.sumOf { chapter ->
                                chapter.classes.count { clazz -> clazz.recordedLink.isNotBlank() || clazz.liveLink.isNotBlank() }
                            }
                            val totalPdfs = subject.chapters.sumOf { chapter ->
                                chapter.classes.sumOf { clazz -> clazz.pdfLinks.size }
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(180.dp)
                                    .clickable { onSelectedSubjectChange(subject) },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    // Decorative Top Banner with subject custom color
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(bgColor, bgColor.copy(alpha = 0.8f))
                                                )
                                            )
                                    ) {
                                        // Simple elegant design overlay
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawCircle(
                                                color = Color.White.copy(alpha = 0.15f),
                                                radius = size.minDimension / 1.5f,
                                                center = androidx.compose.ui.geometry.Offset(size.width, 0f)
                                            )
                                        }
                                    }
                                    
                                    val isMainCourse = subject.sourceCourseId == null || subject.sourceCourseId == course.id
                                    if (isTeacher && isMainCourse) {
                                        var isMenuExpanded by remember { mutableStateOf(false) }
                                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                                            IconButton(onClick = { isMenuExpanded = true }) {
                                                Icon(
                                                    Icons.Default.MoreVert,
                                                    contentDescription = "Subject Options",
                                                    tint = Color.White
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = isMenuExpanded,
                                                onDismissRequest = { isMenuExpanded = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("এডিট করুন") },
                                                    onClick = {
                                                        isMenuExpanded = false
                                                        subjectToEdit = subject
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("ডিলিট করুন", color = Color.Red) },
                                                    onClick = {
                                                        isMenuExpanded = false
                                                        subjectToDelete = subject
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Floating Circular Icon Badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(top = 45.dp, start = 16.dp)
                                            .size(52.dp)
                                            .background(Color.White, shape = CircleShape)
                                            .border(2.dp, Color.White, CircleShape)
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (subject.iconUrl.isNotBlank()) {
                                            coil.compose.AsyncImage(
                                                model = subject.iconUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.MenuBook,
                                                contentDescription = null,
                                                tint = bgColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    // Title & Custom Stats Details in the card body
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = 105.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = subject.title,
                                            color = Color(0xFF0F172A),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        
                                        // Dynamic Stats Column (Videos & PDFs) with Bengali Digits
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            horizontalAlignment = Alignment.Start
                                        ) {
                                            // Videos
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .background(Color(0xFFEFF6FF), shape = RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayCircle,
                                                    contentDescription = "Videos",
                                                    tint = Color(0xFF3B82F6),
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "${convertToBengaliDigits(totalVideos.toString())} ভিডিও",
                                                    color = Color(0xFF1D4ED8),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            // PDFs
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .background(Color(0xFFF0FDF4), shape = RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ListAlt,
                                                    contentDescription = "PDFs",
                                                    tint = Color(0xFF10B981),
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "${convertToBengaliDigits(totalPdfs.toString())} ডকুমেন্ট",
                                                    color = Color(0xFF047857),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (rowSubjects.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Elegant "নতুন বিষয় যোগ করুন" Card for Teachers at the bottom of the list
            if (isTeacher) {
                Card(
                    onClick = { isAddingSubject = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Subject", tint = accentColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("নতুন বিষয় যোগ করুন", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    } else {
        val subj = currentSubject ?: selectedSubjectForView!!
        Column(modifier = Modifier.fillMaxWidth()) {

            // 1. Quarters Selectable Tabs inside subject details - Premium Card style
            if (course.isQuarterOn && course.quarters.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    quartersList.forEach { qName ->
                        val isSelected = selectedQuarterName == qName
                        
                        // Calculate Quarter Progress % inside this subject dynamically
                        val quarterChapters = subj.chapters.filter { (it.quarter.ifBlank { "Quarter 1" }) == qName }
                        val totalChapters = quarterChapters.size
                        val completedWeight = quarterChapters.sumOf { ch ->
                            val displayStatus = if (ch.classes.isEmpty()) "পড়ানো হবে" else ch.teachingStatus
                            when (displayStatus) {
                                "পড়ানো শেষ" -> 1.0
                                "পড়ানো হচ্ছে" -> 0.5
                                else -> 0.0
                            }
                        }
                        val progressPercent = if (totalChapters > 0) {
                            ((completedWeight / totalChapters) * 100).toInt()
                        } else {
                            0
                        }
                        val progressText = convertToBengaliDigits(progressPercent.toString()) + "% পড়া হয়েছে"

                        val quarterObj = course.quarters.find { it.name == qName }
                        val dateRangeText = if (quarterObj != null && quarterObj.startDate.isNotBlank() && quarterObj.endDate.isNotBlank()) {
                            "${quarterObj.startDate} - ${quarterObj.endDate}"
                        } else {
                            ""
                        }

                        val cardBgColor = if (isSelected) Color(0xFF2563EB) else Color.White
                        val cardTextColor = if (isSelected) Color.White else Color(0xFF1E293B)
                        val cardSubTextColor = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF64748B)
                        val cardBorderColor = if (isSelected) Color(0xFF1D4ED8) else Color(0xFFE2E8F0)

                        Card(
                            onClick = { selectedQuarterName = qName },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBgColor),
                            border = BorderStroke(1.dp, cardBorderColor),
                            modifier = Modifier
                                .width(200.dp)
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = qName,
                                        color = cardTextColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    // Dynamic Quarter Status Badge
                                    var quarterStatus = "আনলক"
                                    var statusBgColor = if (isSelected) Color.White.copy(alpha = 0.2f) else Color(0xFFD1FAE5)
                                    var statusTextColor = if (isSelected) Color.White else Color(0xFF065F46)

                                    if (quarterObj != null && quarterObj.startDate.isNotBlank() && quarterObj.endDate.isNotBlank()) {
                                        try {
                                            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                                            val start = java.time.LocalDate.parse(quarterObj.startDate, formatter)
                                            val end = java.time.LocalDate.parse(quarterObj.endDate, formatter)
                                            val today = java.time.LocalDate.now()
                                            if (today.isBefore(start)) {
                                                quarterStatus = "পড়ানো হবে"
                                                statusBgColor = if (isSelected) Color(0xFF93C5FD).copy(alpha = 0.3f) else Color(0xFFDBEAFE)
                                                statusTextColor = if (isSelected) Color.White else Color(0xFF1E3A8A)
                                            } else if (today.isAfter(end)) {
                                                quarterStatus = "পড়ানো শেষ"
                                                statusBgColor = if (isSelected) Color(0xFFD1FAE5).copy(alpha = 0.3f) else Color(0xFFD1FAE5)
                                                statusTextColor = if (isSelected) Color.White else Color(0xFF065F46)
                                            } else {
                                                quarterStatus = "পড়ানো হচ্ছে"
                                                statusBgColor = if (isSelected) Color(0xFFFEF08A).copy(alpha = 0.3f) else Color(0xFFFEF9C3)
                                                statusTextColor = if (isSelected) Color.White else Color(0xFF854D0E)
                                            }
                                        } catch (e: Exception) { }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(statusBgColor, shape = RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = quarterStatus,
                                            color = statusTextColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                if (dateRangeText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = dateRangeText,
                                        color = cardSubTextColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                

                            }
                        }
                    }
                }
            }

            // 2. Learning Resources Banner (Screenshot 2 style)
            Card(
                onClick = { isResourcesExpanded = !isResourcesExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A)) // deep blue/teal
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Resources",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "বিষয়ভিত্তিক লার্নিং রিসোর্স দেখো",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = if (isResourcesExpanded) "↓" else "→",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
            if (isResourcesExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (subj.learningResources.isEmpty()) {
                        Text("কোনো রিসোর্স যোগ করা হয়নি।", color = Color.Gray, fontSize = 14.sp)
                    } else {
                        subj.learningResources.forEachIndexed { index, resource ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(resource.url))
                                        try { mContext.startActivity(intent) } catch (e: Exception) { }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Link, contentDescription = "Link", tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(resource.title, color = Color(0xFF1D4ED8), fontWeight = FontWeight.Medium, fontSize = 15.sp, modifier = Modifier.weight(1f))
                                if (isTeacher) {
                                    IconButton(
                                        onClick = {
                                            val updatedResources = subj.learningResources.toMutableList().apply { removeAt(index) }
                                            val updatedSubject = subj.copy(learningResources = updatedResources)
                                            val updatedSubjects = course.subjects.map { if (it.id == subj.id) updatedSubject else it }
                                            onUpdate(updatedSubjects)
                                            syncSubjectToAllCourses(updatedSubject)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            if (index < subj.learningResources.size - 1) {
                                Divider(color = Color(0xFFE2E8F0))
                            }
                        }
                    }
                    if (isTeacher) {
                        Button(
                            onClick = { isAddingResource = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF1D4ED8)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("নতুন রিসোর্স যোগ করুন")
                        }
                    }
                }
            }
            
            if (isAddingResource) {
                AddResourceDialog(
                    onDismiss = { isAddingResource = false },
                    onAdd = { title, url ->
                        val updatedResources = subj.learningResources.toMutableList().apply { add(PdfLink(title, url)) }
                        val updatedSubject = subj.copy(learningResources = updatedResources)
                        val updatedSubjects = course.subjects.map { if (it.id == subj.id) updatedSubject else it }
                        onUpdate(updatedSubjects)
                        syncSubjectToAllCourses(updatedSubject)
                        isAddingResource = false
                    }
                )
            }


            // Filter chapters by active selected Quarter
            val isCurrentQuarterLocked = isQuarterLocked(selectedQuarterName)
            val chaptersToShow = if (course.isQuarterOn && course.quarters.isNotEmpty()) {
                subj.chapters.filter { (it.quarter.ifBlank { "Quarter 1" }) == selectedQuarterName }
            } else {
                subj.chapters
            }

            if (isCurrentQuarterLocked) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("এই কোয়ার্টারটি লক করা আছে", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("বিস্তারিত দেখতে কোয়ার্টারটি কিনুন বা আনলক করুন।", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onPurchaseClick,
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("আনলক করুন")
                        }
                    }
                }
            } else if (chaptersToShow.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("এই কোয়ার্টারে কোনো অধ্যায় যোগ করা হয়নি।", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                chaptersToShow.forEach { chapter ->
                    var isStatusMenuExpanded by remember { mutableStateOf(false) }
                    
                    // Display Badge according to Teaching Status
                    val displayStatus = if (chapter.classes.isEmpty()) "পড়ানো হবে" else chapter.teachingStatus
                    val (statusBgColor, statusTextColor) = when (displayStatus) {
                        "পড়ানো শেষ" -> Pair(Color(0xFFD1FAE5), Color(0xFF065F46))
                        "পড়ানো হচ্ছে" -> Pair(Color(0xFFDBEAFE), Color(0xFF1E40AF))
                        else -> Pair(Color(0xFFF1F5F9), Color(0xFF475569)) // পড়ানো হবে
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFF64748B).copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Status Badge & Optional Dropdown Trigger
                                Box {
                                    Card(
                                        onClick = { 
                                            if (isTeacher) {
                                                isStatusMenuExpanded = true 
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = statusBgColor),
                                        border = BorderStroke(1.dp, statusTextColor.copy(alpha = 0.2f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = displayStatus,
                                                color = statusTextColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            if (isTeacher) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Change Status",
                                                    tint = statusTextColor,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Dropdown Menu for Teachers to manually override Status (Screenshot 3)
                                    DropdownMenu(
                                        expanded = isStatusMenuExpanded,
                                        onDismissRequest = { isStatusMenuExpanded = false }
                                    ) {
                                        listOf("পড়ানো হবে", "পড়ানো হচ্ছে", "পড়ানো শেষ").forEach { targetStatus ->
                                            DropdownMenuItem(
                                                text = { Text(targetStatus, fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    isStatusMenuExpanded = false
                                                    val updatedChapter = chapter.copy(teachingStatus = targetStatus)
                                                    val updatedSubject = subj.copy(chapters = subj.chapters.map { if (it.id == chapter.id) updatedChapter else it })
                                                    val updatedSubjects = course.subjects.map { if (it.id == subj.id) updatedSubject else it }
                                                    onUpdate(updatedSubjects)
                                                    syncSubjectToAllCourses(updatedSubject)
                                                    
                                                    if (selectedChapterForView?.id == chapter.id) {
                                                        onSelectedChapterChange(updatedChapter)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                                
                                // Classes Count & Action Icons Row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Class Count Badge (e.g., "২ ক্লাস")
                                    Row(
                                        modifier = Modifier
                                            .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ListAlt,
                                            contentDescription = null,
                                            tint = Color(0xFF3B82F6),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${chapter.classes.size} ক্লাস",
                                            color = Color(0xFF1D4ED8),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    
                                    // Edit/Delete for Teachers
                                    if (isTeacher) {
                                        IconButton(
                                            onClick = { chapterToEdit = Pair(subj, chapter) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Chapter",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                val updatedSubject = subj.copy(chapters = subj.chapters.filter { it.id != chapter.id })
                                                val updatedSubjects = course.subjects.map { if (it.id == subj.id) updatedSubject else it }
                                                onUpdate(updatedSubjects)
                                                syncSubjectToAllCourses(updatedSubject)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Chapter",
                                                tint = Color.Red,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    
                                    // Enter Chapter details circle go button
                                    Card(
                                        onClick = { onSelectedChapterChange(chapter) },
                                        shape = CircleShape,
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Open Chapter",
                                                tint = Color(0xFF475569),
                                                modifier = Modifier.size(16.dp).rotate(180f)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Chapter Title Click takes user inside chapter
                            Text(
                                text = chapter.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectedChapterChange(chapter) }
                            )
                        }
                    }
                }
            }
            if (isTeacher) {
                Card(
                    onClick = { subjectToAddChapterTo = subj },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Chapter", tint = accentColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("নতুন অধ্যায় যোগ করুন", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }

    if (subjectToDelete != null) {
        val subject = subjectToDelete!!
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { Text("বিষয় ডিলিট করুন") },
            text = { Text("আপনি কি নিশ্চিতভাবে '${subject.title}' বিষয় এবং এর ভিতরের সব অধ্যায় ও ক্লাস ডিলিট করতে চান?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updatedSubjects = course.subjects.filter { it.id != subject.id }
                        onUpdate(updatedSubjects)
                        subjectToDelete = null
                    }
                ) {
                    Text("ডিলিট", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    if (isAddingSubject || subjectToEdit != null) {
        val initialSubj = subjectToEdit
        AddEditSubjectDialog(
            initialSubject = initialSubj,
            isTeacher = isTeacher,
            channelId = course.channel_id,
            course = course,
            onDismiss = {
                isAddingSubject = false
                subjectToEdit = null
            },
            onSave = { updatedSubject, selectedOtherCourses ->
                val newSubjectsList = if (initialSubj != null) {
                    course.subjects.map { if (it.id == updatedSubject.id) updatedSubject else it }
                } else {
                    course.subjects + updatedSubject
                }
                
                coroutineScope.launch {
                    onUpdate(newSubjectsList)
                    withContext(Dispatchers.IO) {
                        try {
                            val allCourses = supabase.from("courses")
                                .select { filter { eq("channel_id", course.channel_id ?: "") } }
                                .decodeList<CourseItem>()
                            
                            val updatedOtherCourses = mutableListOf<CourseItem>()
                            val selectedIds = selectedOtherCourses.map { it.id }.toSet()
                            
                            allCourses.forEach { otherCourse ->
                                if (otherCourse.id != course.id) {
                                    val otherCourseSubjects = otherCourse.subjects.toMutableList()
                                    val existingIdx = otherCourseSubjects.indexOfFirst { it.id == updatedSubject.id }
                                    
                                    var changed = false
                                    if (selectedIds.contains(otherCourse.id)) {
                                        if (existingIdx != -1) {
                                            otherCourseSubjects[existingIdx] = updatedSubject
                                            changed = true
                                        } else {
                                            otherCourseSubjects.add(updatedSubject)
                                            changed = true
                                        }
                                    } else {
                                        if (existingIdx != -1) {
                                            otherCourseSubjects.removeAt(existingIdx)
                                            changed = true
                                        }
                                    }
                                    
                                    if (changed) {
                                        val updatedOtherCourse = otherCourse.copy(subjects = otherCourseSubjects)
                                        supabase.from("courses").update(updatedOtherCourse) {
                                            filter { eq("id", otherCourse.id) }
                                        }
                                        updatedOtherCourses.add(updatedOtherCourse)
                                    }
                                }
                            }
                            if (updatedOtherCourses.isNotEmpty()) {
                                withContext(Dispatchers.Main) {
                                    onMultipleCoursesUpdate(updatedOtherCourses)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                
                if (selectedSubjectForView?.id == updatedSubject.id) {
                    onSelectedSubjectChange(updatedSubject)
                }
                isAddingSubject = false
                subjectToEdit = null
            }
        )
    }

    if (showRoutineDialog) {
        RoutineDialog(
            routineUrl = course.realRoutineUrl,
            isTeacher = isTeacher,
            onDismiss = { showRoutineDialog = false },
            onSaveRoutine = { updatedRoutineUrl ->
                val finalRoutineUrl = "v2;${course.bannerUrl};${course.startDate};${course.endDate};$updatedRoutineUrl"
                val finalPaymentDetails = if (course.cleanPaymentDetails.isNotBlank()) {
                    "${course.cleanPaymentDetails}|||ROUTINE_DATA:$finalRoutineUrl"
                } else {
                    "|||ROUTINE_DATA:$finalRoutineUrl"
                }
                val updatedCourse = course.copy(paymentDetails = finalPaymentDetails)
                coroutineScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            supabase.from("courses").update(updatedCourse) {
                                filter { eq("id", course.id) }
                            }
                        }
                        onCourseUpdate?.invoke(updatedCourse)
                        Toast.makeText(mContext, "রুটিন আপডেট করা হয়েছে!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(mContext, "রুটিন সংরক্ষণ করতে ত্রুটি: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (subjectToAddChapterTo != null) {
        var newTitle by remember { mutableStateOf("") }
        var selectedQuarterForNewChapter by remember { mutableStateOf(quartersList.firstOrNull() ?: "Quarter 1") }
        var quarterDropdownExpanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { subjectToAddChapterTo = null },
            title = { Text("নতুন অধ্যায় (Chapter) যোগ করুন") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("অধ্যায়ের নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (course.isQuarterOn && course.quarters.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("কোয়ার্টার নির্বাচন করুন:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().clickable { quarterDropdownExpanded = true }) {
                            OutlinedTextField(
                                value = selectedQuarterForNewChapter,
                                onValueChange = {},
                                enabled = false,
                                label = { Text("কোয়ার্টার") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) }
                            )
                            DropdownMenu(
                                expanded = quarterDropdownExpanded,
                                onDismissRequest = { quarterDropdownExpanded = false }
                            ) {
                                quartersList.forEach { qName ->
                                    DropdownMenuItem(
                                        text = { Text(qName) },
                                        onClick = {
                                            selectedQuarterForNewChapter = qName
                                            quarterDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTitle.isNotBlank()) {
                        val newChapter = CourseChapter(
                            title = newTitle,
                            quarter = if (course.isQuarterOn) selectedQuarterForNewChapter else "Quarter 1"
                        )
                        val updatedSubject = subjectToAddChapterTo!!.copy(chapters = subjectToAddChapterTo!!.chapters + newChapter)
                        val updatedSubjects = course.subjects.map {
                            if (it.id == subjectToAddChapterTo!!.id) updatedSubject else it
                        }
                        onUpdate(updatedSubjects)
                        syncSubjectToAllCourses(updatedSubject)
                        subjectToAddChapterTo = null
                    }
                }) { Text("যোগ করুন") }
            },
            dismissButton = {
                TextButton(onClick = { subjectToAddChapterTo = null }) { Text("বাতিল") }
            }
        )
    }

    if (chapterToEdit != null) {
        val subject = chapterToEdit!!.first
        val chapter = chapterToEdit!!.second
        var editTitle by remember { mutableStateOf(chapter.title) }
        var selectedQuarterForEditChapter by remember { mutableStateOf(chapter.quarter.ifBlank { quartersList.firstOrNull() ?: "Quarter 1" }) }
        var quarterDropdownExpanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { chapterToEdit = null },
            title = { Text("অধ্যায় (Chapter) এডিট করুন") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("অধ্যায়ের নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (course.isQuarterOn && course.quarters.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("কোয়ার্টার নির্বাচন করুন:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().clickable { quarterDropdownExpanded = true }) {
                            OutlinedTextField(
                                value = selectedQuarterForEditChapter,
                                onValueChange = {},
                                enabled = false,
                                label = { Text("কোয়ার্টার") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) }
                            )
                            DropdownMenu(
                                expanded = quarterDropdownExpanded,
                                onDismissRequest = { quarterDropdownExpanded = false }
                            ) {
                                quartersList.forEach { qName ->
                                    DropdownMenuItem(
                                        text = { Text(qName) },
                                        onClick = {
                                            selectedQuarterForEditChapter = qName
                                            quarterDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editTitle.isNotBlank()) {
                        val updatedChapter = chapter.copy(
                            title = editTitle,
                            quarter = if (course.isQuarterOn) selectedQuarterForEditChapter else "Quarter 1"
                        )
                        val updatedSubject = subject.copy(chapters = subject.chapters.map { if (it.id == chapter.id) updatedChapter else it })
                        val updatedSubjects = course.subjects.map {
                            if (it.id == subject.id) updatedSubject else it
                        }
                        onUpdate(updatedSubjects)
                        syncSubjectToAllCourses(updatedSubject)
                        chapterToEdit = null
                    }
                }) { Text("সেভ করুন") }
            },
            dismissButton = {
                TextButton(onClick = { chapterToEdit = null }) { Text("বাতিল") }
            }
        )
    }

    val classDialogState = chapterToAddClassTo ?: classToEdit?.let { Pair(it.first, it.second) }
    if (classDialogState != null) {
        val subject = classDialogState.first
        val chapter = classDialogState.second
        val existingClass = classToEdit?.third
        
        var selectedType by remember { mutableStateOf(existingClass?.type ?: "লেকচার ক্লাস") }
        val types = listOf("লেকচার ক্লাস", "সলভিং ক্লাস", "গাইডলাইন ক্লাস", "অতিরিক্ত ক্লাস", "লেকচার শিট/PDF")
        
        var newTitle by remember { mutableStateOf(existingClass?.title ?: "") }
        var newDescription by remember { mutableStateOf(existingClass?.description ?: "") }
        var newDate by remember { mutableStateOf(existingClass?.date ?: "") }
        var newTime by remember { mutableStateOf(existingClass?.time ?: "") }
        var newQuarter by remember { mutableStateOf(existingClass?.quarterId ?: "") }
        val initialMentorName = existingClass?.mentorId?.let { mId -> mentors.find { it.id == mId }?.name } ?: ""
        var newMentor by remember { mutableStateOf(initialMentorName) }
        var newLiveLink by remember { mutableStateOf(existingClass?.liveLink ?: "") }
        var newRecordedLink by remember { mutableStateOf(existingClass?.recordedLink ?: "") }
        var newHomeworkLink by remember { mutableStateOf(existingClass?.homeworkLink ?: "") }
        var pdfLinks by remember { mutableStateOf(existingClass?.pdfLinks ?: listOf<PdfLink>()) }
        var newPdfTitle by remember { mutableStateOf("") }
        var newPdfUrl by remember { mutableStateOf("") }

        Dialog(
            onDismissRequest = { 
                chapterToAddClassTo = null
                classToEdit = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                ) {
                    Text(if (existingClass != null) "ক্লাস এডিট করুন" else "নতুন ক্লাস/PDF যোগ করুন", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("উপকরণের ধরন", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            types.take(2).forEach { type ->
                                OutlinedButton(
                                    onClick = { selectedType = type },
                                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                                    border = BorderStroke(1.dp, if (selectedType == type) accentColor else Color.LightGray),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (selectedType == type) accentColor else Color.Gray)
                                ) {
                                    Text(type, fontSize = 12.sp)
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            types.drop(2).take(2).forEach { type ->
                                OutlinedButton(
                                    onClick = { selectedType = type },
                                    modifier = Modifier.weight(1f).padding(end = 4.dp, top = 4.dp),
                                    border = BorderStroke(1.dp, if (selectedType == type) accentColor else Color.LightGray),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (selectedType == type) accentColor else Color.Gray)
                                ) {
                                    Text(type, fontSize = 12.sp)
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            types.drop(4).forEach { type ->
                                OutlinedButton(
                                    onClick = { selectedType = type },
                                    modifier = Modifier.weight(1f).padding(end = 4.dp, top = 4.dp),
                                    border = BorderStroke(1.dp, if (selectedType == type) accentColor else Color.LightGray),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (selectedType == type) accentColor else Color.Gray)
                                ) {
                                    Text(type, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, label = { Text("উপকরণের নাম লিখুন...") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = newDescription, onValueChange = { newDescription = it }, label = { Text("এই উপকরণের সংক্ষিপ্ত বর্ণনা...") }, modifier = Modifier.fillMaxWidth())
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f).padding(end = 4.dp).clickable {
                                val calendar = Calendar.getInstance()
                                DatePickerDialog(
                                    mContext,
                                    { _, year, month, day ->
                                        newDate = String.format(java.util.Locale.US, "%02d/%02d/%04d", day, month + 1, year)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }) {
                                OutlinedTextField(
                                    value = newDate, 
                                    onValueChange = {}, 
                                    label = { Text("তারিখ") }, 
                                    readOnly = true,
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            Box(modifier = Modifier.weight(1f).padding(start = 4.dp).clickable {
                                val calendar = Calendar.getInstance()
                                TimePickerDialog(
                                    mContext,
                                    { _, hour, minute ->
                                        val amPm = if (hour >= 12) "PM" else "AM"
                                        val hour12 = if (hour % 12 == 0) 12 else hour % 12
                                        val hourStr = if (hour12 == 0) 12 else hour12
                                        newTime = String.format("%02d:%02d %s", hourStr, minute, amPm)
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    false
                                ).show()
                            }) {
                                OutlinedTextField(
                                    value = newTime, 
                                    onValueChange = {}, 
                                    label = { Text("সময়") }, 
                                    readOnly = true,
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        var quarterDropdownExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.fillMaxWidth().clickable { quarterDropdownExpanded = true }) {
                                OutlinedTextField(
                                    value = newQuarter,
                                    onValueChange = {},
                                    label = { Text("কোয়ার্টার নির্বাচন করুন") },
                                    readOnly = true,
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            DropdownMenu(
                                expanded = quarterDropdownExpanded,
                                onDismissRequest = { quarterDropdownExpanded = false }
                            ) {
                                course.quarters.forEach { quarter ->
                                    DropdownMenuItem(
                                        text = { Text(quarter.name) },
                                        onClick = {
                                            newQuarter = quarter.name
                                            quarterDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        var mentorDropdownExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.fillMaxWidth().clickable { mentorDropdownExpanded = true }) {
                                OutlinedTextField(
                                    value = newMentor,
                                    onValueChange = {},
                                    label = { Text("মেন্টর / শিক্ষক নির্বাচিত করুন") },
                                    readOnly = true,
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            DropdownMenu(
                                expanded = mentorDropdownExpanded,
                                onDismissRequest = { mentorDropdownExpanded = false }
                            ) {
                                mentors.forEach { mentor ->
                                    DropdownMenuItem(
                                        text = { Text(mentor.name) },
                                        onClick = {
                                            newMentor = mentor.name
                                            mentorDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(value = newLiveLink, onValueChange = { newLiveLink = it }, label = { Text("সরাসরি লাইভ লিংক (Live Stream Link)") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = newRecordedLink, onValueChange = { newRecordedLink = it }, label = { Text("রেকর্ড ভিডিও লিংক (Recorded Video Link)") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = newHomeworkLink, onValueChange = { newHomeworkLink = it }, label = { Text("হোমওয়ার্ক লিংক (Homework Link)") }, modifier = Modifier.fillMaxWidth())

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("PDF/লেকচার শিট লিংক সমূহ", fontWeight = FontWeight.Bold)
                        pdfLinks.forEachIndexed { index, pdf ->
                            Text("${index+1}. ${pdf.title}: ${pdf.url}", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = newPdfTitle, onValueChange = { newPdfTitle = it }, label = { Text("টাইটেল") }, modifier = Modifier.weight(1f).padding(end = 4.dp))
                            OutlinedTextField(value = newPdfUrl, onValueChange = { newPdfUrl = it }, label = { Text("লিংক") }, modifier = Modifier.weight(1f).padding(start = 4.dp))
                        }
                        TextButton(onClick = {
                            if (newPdfTitle.isNotBlank() && newPdfUrl.isNotBlank()) {
                                pdfLinks = pdfLinks + PdfLink(newPdfTitle, newPdfUrl)
                                newPdfTitle = ""
                                newPdfUrl = ""
                            }
                        }) {
                            Text("আরো PDF যোগ করুন")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        val keyboardController = LocalSoftwareKeyboardController.current
                        TextButton(onClick = { 
                            chapterToAddClassTo = null
                            classToEdit = null
                        }) {
                            Text("বাতিল", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            keyboardController?.hide()
                            
                            if (newTitle.isNotBlank()) {
                                val selectedQuarterId = newQuarter
                                val selectedMentorId = mentors.find { it.name == newMentor }?.id ?: ""
                                
                                val finalPdfLinks = if (newPdfTitle.isNotBlank() && newPdfUrl.isNotBlank()) {
                                    pdfLinks + PdfLink(newPdfTitle, newPdfUrl)
                                } else {
                                    pdfLinks
                                }

                                val newClass = existingClass?.copy(
                                    type = selectedType,
                                    title = newTitle,
                                    description = newDescription,
                                    date = newDate,
                                    time = newTime,
                                    quarterId = selectedQuarterId,
                                    mentorId = selectedMentorId,
                                    liveLink = newLiveLink,
                                    recordedLink = newRecordedLink,
                                    homeworkLink = newHomeworkLink,
                                    pdfLinks = finalPdfLinks
                                ) ?: CourseClass(
                                    type = selectedType,
                                    title = newTitle,
                                    description = newDescription,
                                    date = newDate,
                                    time = newTime,
                                    quarterId = selectedQuarterId,
                                    mentorId = selectedMentorId,
                                    liveLink = newLiveLink,
                                    recordedLink = newRecordedLink,
                                    homeworkLink = newHomeworkLink,
                                    pdfLinks = finalPdfLinks
                                )
                                val updatedSubject = subject.copy(chapters = subject.chapters.map { ch ->
                                    if (ch.id == chapter.id) {
                                        val updatedClasses = if (existingClass != null) {
                                            ch.classes.map { if (it.id == existingClass.id) newClass else it }
                                        } else {
                                            ch.classes + newClass
                                        }
                                        val newStatus = if (ch.teachingStatus == "পড়ানো হবে") "পড়ানো হচ্ছে" else ch.teachingStatus
                                        ch.copy(classes = updatedClasses, teachingStatus = newStatus)
                                    } else ch
                                })
                                val updatedSubjects = course.subjects.map { subj ->
                                    if (subj.id == subject.id) updatedSubject else subj
                                }
                                onUpdate(updatedSubjects)
                                syncSubjectToAllCourses(updatedSubject)

                                // Auto-schedule local and OneSignal push notification on class creation/modification
                                ClassNotificationScheduler.scheduleLocalNotification(
                                    context = mContext,
                                    classId = newClass.id,
                                    subjectTitle = subject.title,
                                    chapterTitle = chapter.title,
                                    classTitle = newClass.title,
                                    mentorName = if (newMentor.isBlank()) "অজানা শিক্ষক" else newMentor,
                                    dateStr = newClass.date,
                                    timeStr = newClass.time
                                )
                                ClassNotificationScheduler.scheduleOneSignalPushNotification(
                                    context = mContext,
                                    courseId = course.id,
                                    quarterId = newClass.quarterId,
                                    subjectTitle = subject.title,
                                    chapterTitle = chapter.title,
                                    classTitle = newClass.title,
                                    mentorName = if (newMentor.isBlank()) "অজানা শিক্ষক" else newMentor,
                                    dateStr = newClass.date,
                                    timeStr = newClass.time
                                )

                                chapterToAddClassTo = null
                                classToEdit = null
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = accentColor)) {
                            Text(if (existingClass != null) "সেভ করুন" else "তৈরি করুন")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditSubjectDialog(
    initialSubject: CourseSubject?,
    isTeacher: Boolean,
    channelId: String?,
    course: CourseItem,
    onDismiss: () -> Unit,
    onSave: (CourseSubject, List<CourseItem>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var newTitle by remember { mutableStateOf(initialSubject?.title ?: "") }
    var newColorHex by remember { mutableStateOf(initialSubject?.colorHex ?: "#FF6B6B") }
    var newIconUrl by remember { mutableStateOf(initialSubject?.iconUrl ?: "") }
    var isUploadingLogo by remember { mutableStateOf(false) }
    
    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isUploadingLogo = true
                Toast.makeText(context, "লোগো আপলোড হচ্ছে...", Toast.LENGTH_SHORT).show()
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    if (bytes != null) {
                        val uploadedUrl = ImgBBClient.uploadImage(bytes)
                        if (uploadedUrl != null) {
                            newIconUrl = uploadedUrl
                            Toast.makeText(context, "লোগো সফলভাবে আপলোড হয়েছে!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "লোগো আপলোড ব্যর্থ হয়েছে।", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "ত্রুটি: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isUploadingLogo = false
                }
            }
        }
    }
    
    val colors = listOf(
        "#EF4444", "#F97316", "#F59E0B", "#10B981", "#06B6D4", "#3B82F6", 
        "#6366F1", "#8B5CF6", "#D946EF", "#EC4899", "#14B8A6", "#475569"
    )
    
    var otherCourses by remember { mutableStateOf<List<CourseItem>>(emptyList()) }
    var selectedOtherCourseIds by remember { mutableStateOf(setOf<String>()) }
    
    LaunchedEffect(channelId) {
        if (isTeacher && channelId != null) {
            withContext(Dispatchers.IO) {
                try {
                    val courses = supabase.from("courses")
                        .select { filter { eq("channel_id", channelId) } }
                        .decodeList<CourseItem>()
                    
                    val currentIsQuarterActive = course.isQuarterOn && course.quarters.isNotEmpty()
                    val currentQuartersCount = if (currentIsQuarterActive) course.quarters.size else 0

                    otherCourses = courses.filter { otherCourse ->
                        if (otherCourse.id == course.id) return@filter false
                        
                        val otherIsQuarterActive = otherCourse.isQuarterOn && otherCourse.quarters.isNotEmpty()
                        val otherQuartersCount = if (otherIsQuarterActive) otherCourse.quarters.size else 0
                        
                        currentIsQuarterActive == otherIsQuarterActive && currentQuartersCount == otherQuartersCount
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (initialSubject == null) "নতুন বিষয় যোগ করুন" else "বিষয় এডিট করুন", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close") }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("বিষয়ের নাম", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("বিষয়ের লোগো/আইকন", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newIconUrl, 
                        onValueChange = { newIconUrl = it }, 
                        placeholder = { Text("লোগো URL লিখুন বা আপলোড করুন") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { logoPickerLauncher.launch("image/*") },
                        enabled = !isUploadingLogo,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isUploadingLogo) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.Upload, contentDescription = "Upload Logo")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("আপলোড")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("বিষয়ের রঙ", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Color presets grid (2 rows of 6 circles)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    colors.chunked(6).forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowColors.forEach { hex ->
                                val isSelected = newColorHex.uppercase() == hex.uppercase()
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                        .clickable { newColorHex = hex }
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Custom HEX color input row with instant preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = newColorHex,
                        onValueChange = { 
                            newColorHex = it
                        },
                        label = { Text("নিজের মতো কাস্টম কালার কোড (HEX)") },
                        placeholder = { Text("#FF6B6B") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    // Color Preview
                    val previewColor = try {
                        Color(android.graphics.Color.parseColor(newColorHex))
                    } catch (e: Exception) {
                        Color.Gray
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(previewColor)
                                        .border(1.5.dp, Color.LightGray, RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("প্রিভিউ", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                val isMainCourse = initialSubject == null || initialSubject.sourceCourseId == null || initialSubject.sourceCourseId == course.id
                if (otherCourses.isNotEmpty() && isMainCourse) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("যেসব কোর্সে এই বিষয়টি যুক্ত থাকবে", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6))) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            otherCourses.forEach { otherCourse ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .toggleable(
                                            value = selectedOtherCourseIds.contains(otherCourse.id),
                                            onValueChange = { isChecked ->
                                                selectedOtherCourseIds = if (isChecked) {
                                                    selectedOtherCourseIds + otherCourse.id
                                                } else {
                                                    selectedOtherCourseIds - otherCourse.id
                                                }
                                            }
                                        )
                                        .padding(vertical = 4.dp), 
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(checked = selectedOtherCourseIds.contains(otherCourse.id), onCheckedChange = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(otherCourse.title, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        val finalSubject = initialSubject?.copy(
                            title = newTitle, 
                            colorHex = newColorHex, 
                            iconUrl = newIconUrl,
                            sourceCourseId = initialSubject.sourceCourseId ?: course.id
                        ) ?: CourseSubject(title = newTitle, colorHex = newColorHex, iconUrl = newIconUrl, sourceCourseId = course.id)
                        val selectedCourses = otherCourses.filter { selectedOtherCourseIds.contains(it.id) }
                        onSave(finalSubject, selectedCourses)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(android.graphics.Color.parseColor(newColorHex)))
                ) {
                    Text("সংরক্ষণ করুন", fontSize = 16.sp)
                }
            }
        }
    }
}

fun Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@Composable
fun VideoPlayer(
    videoOptions: VideoOptions, 
    modifier: Modifier = Modifier,
    initialPosition: Long = 0L,
    onPositionChanged: (Long) -> Unit = {},
    initialPlaying: Boolean = true,
    onPlayingChanged: (Boolean) -> Unit = {},
    onQualityChanged: (VideoLink) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    
    var showQualitySelector by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    
    // Controls Visibility & Interaction States
    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(initialPlaying) }
    var currentPosition by remember { mutableStateOf(initialPosition) }
    var duration by remember { mutableStateOf(0L) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    
    var isBuffering by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    
    // Pinch-to-zoom and pan states for video
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val transformState = androidx.compose.foundation.gestures.rememberTransformableState { zoomChange, offsetChange, _ ->
        if (!VideoPipState.isInPip) {
            scale = (scale * zoomChange).coerceIn(1f, 5f)
            if (scale > 1f) {
                offset += offsetChange
            } else {
                offset = androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    // Reset zoom when picture-in-picture changes or a new video starts
    LaunchedEffect(VideoPipState.isInPip, videoOptions) {
        if (VideoPipState.isInPip) {
            scale = 1f
            offset = androidx.compose.ui.geometry.Offset.Zero
        }
    }

    // Update parent states when playing state or playback position changes
    LaunchedEffect(currentPosition) {
        onPositionChanged(currentPosition)
    }
    LaunchedEffect(isPlaying) {
        onPlayingChanged(isPlaying)
    }

    // Manage VideoPipState lifecycle registration
    DisposableEffect(Unit) {
        VideoPipState.isVideoActive = true
        VideoPipState.onEnterPip = {
            if (activity != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try {
                    val aspectRatio = android.util.Rational(16, 9)
                    val params = android.app.PictureInPictureParams.Builder()
                        .setAspectRatio(aspectRatio)
                        .build()
                    activity.enterPictureInPictureMode(params)
                } catch (e: Exception) {
                    try {
                        activity.enterPictureInPictureMode()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        }
        onDispose {
            VideoPipState.isVideoActive = false
            VideoPipState.onEnterPip = null
        }
    }
    
    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            kotlinx.coroutines.delay(1800L)
            statusMessage = null
        }
    }
    
    // Slider drag tracking
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(0f) }

    // Non-adaptive quality tracking
    var currentNonAdaptiveQuality by remember(videoOptions) { 
        mutableStateOf(videoOptions.links.firstOrNull()?.quality ?: "Unknown") 
    }

    var resizeMode by remember { mutableStateOf(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    
    // Gestures states
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) }
    var currentVolume by remember { mutableStateOf(audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)) }
    
    var currentBrightness by remember {
        mutableStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0 } ?: 0.5f
        )
    }
    
    var showVolumeIndicator by remember { mutableStateOf(false) }
    var showBrightnessIndicator by remember { mutableStateOf(false) }
    
    var showRewindFeedback by remember { mutableStateOf(false) }
    var showForwardFeedback by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    val httpDataSourceFactory = remember {
        androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .setDefaultRequestProperties(mapOf("Referer" to "https://www.facebook.com/"))
            .setAllowCrossProtocolRedirects(true)
    }

    val dataSourceFactory = remember {
        androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)
    }
    
    val exoPlayer = remember {
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
        
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
            
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15000, // minBufferMs
                50000, // maxBufferMs
                1000,  // bufferForPlaybackMs (starts playback faster)
                1500   // bufferForPlaybackAfterRebufferMs
            )
            .build()
            
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setLoadControl(loadControl)
            .build()
    }

    // Set playback speed
    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    // Monitor playback progress
    LaunchedEffect(exoPlayer, isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0L)
            kotlinx.coroutines.delay(250L)
        }
    }

    // Observe player listener
    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                duration = exoPlayer.duration.coerceAtLeast(0L)
                isBuffering = state == androidx.media3.common.Player.STATE_BUFFERING
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                super.onPlayerError(error)
                statusMessage = "Error: ${error.message}"
                isBuffering = false
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    LaunchedEffect(videoOptions, currentNonAdaptiveQuality) {
        val currentPos = if (exoPlayer.currentPosition > 0L) exoPlayer.currentPosition else initialPosition
        val wasPlaying = exoPlayer.isPlaying
        
        val targetLink = videoOptions.links.find { it.quality == currentNonAdaptiveQuality } ?: videoOptions.links.firstOrNull()
        if (targetLink != null) {
            val defaultMediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
            
            // Format video URI safely (check for web vs local file)
            val videoUri = if (targetLink.url.startsWith("http://") || targetLink.url.startsWith("https://")) {
                android.net.Uri.parse(targetLink.url)
            } else {
                android.net.Uri.fromFile(java.io.File(targetLink.url))
            }
            
            val audioUri = videoOptions.audioUrl?.let { url ->
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    android.net.Uri.parse(url)
                } else {
                    android.net.Uri.fromFile(java.io.File(url))
                }
            }

            if (!targetLink.hasAudio && audioUri != null) {
                val videoSource = defaultMediaSourceFactory.createMediaSource(MediaItem.fromUri(videoUri))
                val audioSource = defaultMediaSourceFactory.createMediaSource(MediaItem.fromUri(audioUri))
                exoPlayer.setMediaSource(MergingMediaSource(videoSource, audioSource))
            } else {
                exoPlayer.setMediaItem(MediaItem.fromUri(videoUri))
            }
        }
        
        exoPlayer.prepare()
        if (currentPos > 0L) {
            exoPlayer.seekTo(currentPos)
        }
        exoPlayer.playWhenReady = if (exoPlayer.currentPosition > 0L) wasPlaying else initialPlaying
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Hide controls automatically, but NOT when user is interacting
    LaunchedEffect(controlsVisible, isLocked, isDraggingSlider, showSpeedDialog, showQualitySelector) {
        if (controlsVisible && !isLocked && !isDraggingSlider && !showSpeedDialog && !showQualitySelector) {
            kotlinx.coroutines.delay(4500L)
            controlsVisible = false
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Keep SystemBars behavior elegant on landscape full screen
    DisposableEffect(isLandscape) {
        if (isLandscape && activity != null) {
            val window = activity.window
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else if (activity != null) {
            val window = activity.window
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
        onDispose {}
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (!isLocked) {
                            val halfWidth = size.width / 2
                            if (offset.x < halfWidth) {
                                val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(newPos)
                                currentPosition = newPos
                                showRewindFeedback = true
                            } else {
                                val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(exoPlayer.duration)
                                exoPlayer.seekTo(newPos)
                                currentPosition = newPos
                                showForwardFeedback = true
                            }
                        }
                    },
                    onTap = {
                        controlsVisible = !controlsVisible
                    }
                )
            }
            .then(
                if (!isLocked) {
                    Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { _ -> },
                            onDragEnd = {
                                scope.launch {
                                    kotlinx.coroutines.delay(800L)
                                    showVolumeIndicator = false
                                    showBrightnessIndicator = false
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                val changePercent = -dragAmount / size.height
                                val isLeftHalf = change.position.x < (size.width / 2)
                                if (isLeftHalf) {
                                    if (activity != null) {
                                        showBrightnessIndicator = true
                                        showVolumeIndicator = false
                                        val newBrightness = (currentBrightness + changePercent).coerceIn(0f, 1f)
                                        currentBrightness = newBrightness
                                        val lp = activity.window.attributes
                                        lp.screenBrightness = newBrightness
                                        activity.window.attributes = lp
                                    }
                                } else {
                                    showVolumeIndicator = true
                                    showBrightnessIndicator = false
                                    val currentVolFloat = currentVolume.toFloat()
                                    val deltaVol = changePercent * maxVolume
                                    val newVol = (currentVolFloat + deltaVol).coerceIn(0f, maxVolume.toFloat())
                                    currentVolume = newVol.toInt()
                                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, currentVolume, 0)
                                }
                            }
                        )
                    }
                } else Modifier
            )
    ) {
        // Actual Video Player
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false // Disable default controllers
                    this.resizeMode = resizeMode
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .transformable(state = transformState)
        )

        // Buffering Indicator
        if (isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color(0xFF3B82F6),
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "বাফারিং হচ্ছে...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (!VideoPipState.isInPip) {
            // On-screen Transient Status Message
            if (statusMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusMessage!!,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Custom Overlay Indicators
        if (showVolumeIndicator) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
                    .width(64.dp)
                    .height(180.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .width(6.dp)
                            .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(currentVolume.toFloat() / maxVolume.toFloat().coerceAtLeast(1f))
                                .background(Color(0xFF3B82F6), RoundedCornerShape(3.dp))
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(currentVolume * 100 / maxVolume.coerceAtLeast(1))}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showBrightnessIndicator) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp)
                    .width(64.dp)
                    .height(180.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                    Icon(
                        imageVector = Icons.Default.Brightness5,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .width(6.dp)
                            .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(currentBrightness)
                                .background(Color(0xFFFBBF24), RoundedCornerShape(3.dp))
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(currentBrightness * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showRewindFeedback) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 48.dp)
                    .size(80.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FastRewind, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Text("-10s", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                LaunchedEffect(showRewindFeedback) {
                    kotlinx.coroutines.delay(650L)
                    showRewindFeedback = false
                }
            }
        }

        if (showForwardFeedback) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 48.dp)
                    .size(80.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Text("+10s", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                LaunchedEffect(showForwardFeedback) {
                    kotlinx.coroutines.delay(650L)
                    showForwardFeedback = false
                }
            }
        }

        // SCREEN IS LOCKED STATE
        if (isLocked) {
            // Semi-transparent background when controls are visible
            if (controlsVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
            }
            
            // Only lock icon is shown
            IconButton(
                onClick = { 
                    isLocked = false
                    controlsVisible = true
                    Toast.makeText(context, "নিয়ন্ত্রণ প্যানেল আনলক করা হয়েছে", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(48.dp)
                    .background(Color.Red.copy(alpha = 0.7f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Unlock screen",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Quick helper text
            if (controlsVisible) {
                Text(
                    text = "প্যানেল আনলক করতে ওপরের লাল লক বাটনে ক্লিক করুন",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        } else {
            // SCREEN IS UNLOCKED - ALL ADVANCED CONTROLS
            if (controlsVisible) {
                // Dim Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                )

                // 1. TOP CONTROL BAR (Quality, Speed, Lock)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button only when in landscape full screen
                    if (isLandscape) {
                        IconButton(
                            onClick = {
                                if (activity != null) {
                                    activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                }
                            },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Fullscreen", tint = Color.White)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Top Right Actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Aspect Ratio Toggle
                        Button(
                            onClick = {
                                resizeMode = when (resizeMode) {
                                    androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT -> {
                                        statusMessage = "ভিডিও মোড: জুম"
                                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    }
                                    androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> {
                                        statusMessage = "ভিডিও মোড: ফুল স্ক্রিন"
                                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                                    }
                                    else -> {
                                        statusMessage = "ভিডিও মোড: ফিট"
                                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.AspectRatio, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            val modeText = when (resizeMode) {
                                androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT -> "ফিট"
                                androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "জুম"
                                else -> "ফুল স্ক্রিন"
                            }
                            Text(modeText, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // 1. Speed selector pill button
                        Button(
                            onClick = { showSpeedDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("গতি: ${playbackSpeed}x", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // 2. Video quality selector pill button
                        if (videoOptions.links.size > 1) {
                            Button(
                                onClick = { showQualitySelector = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(currentNonAdaptiveQuality, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // 3. Lock Screen button
                        IconButton(
                            onClick = { 
                                isLocked = true
                                Toast.makeText(context, "নিয়ন্ত্রণ প্যানেল লক করা হয়েছে", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = "Lock Controls", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // 2. CENTER PLAYBACK CONTROLS (Rewind, Play/Pause, Forward)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind 10s button
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Play / Pause button
                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFF3B82F6), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Fast Forward 10s button
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(exoPlayer.duration)
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 3. BOTTOM SEEKBAR & TIME INFO
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentText = formatTime(if (isDraggingSlider) dragPosition.toLong() else currentPosition)
                        val totalText = formatTime(duration)
                        
                        Text(
                            text = "$currentText / $totalText",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // PiP Pop-up Toggle Button
                            if (ThemeManager.isPipEnabled) {
                                IconButton(
                                    onClick = {
                                        VideoPipState.onEnterPip?.invoke()
                                    },
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(36.dp)
                                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = "Pop-up Mode",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Fullscreen Landscape/Portrait Toggle
                            IconButton(
                                onClick = {
                                    if (activity != null) {
                                        activity.requestedOrientation = if (isLandscape) {
                                            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        } else {
                                            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Toggle Landscape Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Seek slider
                    val sliderValue = if (isDraggingSlider) dragPosition else currentPosition.toFloat()
                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            isDraggingSlider = true
                            dragPosition = it
                        },
                        onValueChangeFinished = {
                            exoPlayer.seekTo(dragPosition.toLong())
                            currentPosition = dragPosition.toLong()
                            isDraggingSlider = false
                        },
                        valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF3B82F6),
                            activeTrackColor = Color(0xFF3B82F6),
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

    // Playback Speed Selector Dialog
    if (showSpeedDialog) {
        Dialog(onDismissRequest = { showSpeedDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "প্লেব্যাক গতি নির্ধারণ করুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    speeds.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playbackSpeed = speed
                                    statusMessage = if (speed == 1.0f) "গতি: স্বাভাবিক" else "গতি: ${speed}x"
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = playbackSpeed == speed, 
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF3B82F6))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (speed == 1.0f) "স্বাভাবিক (১.০x)" else "${speed}x",
                                fontSize = 15.sp,
                                fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showSpeedDialog = false }, 
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("বাতিল করুন", color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Video Quality Selector Dialog
    if (showQualitySelector) {
        Dialog(onDismissRequest = { showQualitySelector = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "ভিডিওর কোয়ালিটি নির্বাচন করুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(videoOptions.links) { link ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentNonAdaptiveQuality = link.quality
                                        statusMessage = "কোয়ালিটি: ${link.quality}"
                                        showQualitySelector = false
                                        onQualityChanged(link)
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentNonAdaptiveQuality == link.quality, 
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF3B82F6))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = link.quality,
                                    fontSize = 15.sp,
                                    color = Color(0xFF1E293B),
                                    fontWeight = if (currentNonAdaptiveQuality == link.quality) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showQualitySelector = false }, 
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("বন্ধ করুন", color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ClassDetailView(
    clazz: CourseClass,
    subject: CourseSubject,
    mentors: List<Mentor>,
    accentColor: Color,
    courseName: String = "",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var videoOptions by remember { mutableStateOf<VideoOptions?>(null) }
    var savedVideoPosition by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(0L) }
    var savedVideoPlaying by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var isLoadingVideo by remember { mutableStateOf(false) }
    var currentVideoUrl by remember { mutableStateOf<String?>(null) }
    var showQualitySelector by remember { mutableStateOf(false) }

    val downloadStates by OfflineDownloadManager.downloadStates.collectAsState()
    var downloadsList by remember { mutableStateOf(emptyList<DownloadRecord>()) }
    
    // PDF Viewer dialog states
    var activePdfToView by remember { mutableStateOf<File?>(null) }
    var activePdfTitle by remember { mutableStateOf("") }
    var downloadingPdfUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        downloadsList = OfflineDownloadManager.getDownloadRecords(context)
    }

    LaunchedEffect(downloadStates) {
        downloadsList = OfflineDownloadManager.getDownloadRecords(context)
    }

    LaunchedEffect(clazz.recordedLink) {
        if (clazz.recordedLink.isNotBlank()) {
            isLoadingVideo = true
            val options = FacebookVideoExtractor.extractVideoOptions(context, clazz.recordedLink)
            videoOptions = options
            if (options != null) {
                currentVideoUrl = options.links.firstOrNull()?.url ?: options.adaptiveUrl
            }
            isLoadingVideo = false
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Use movableContentOf to keep VideoPlayer stable across orientation/PiP changes
    val movableVideoPlayer = remember(videoOptions, currentVideoUrl) {
        movableContentOf { modifier: Modifier ->
            if (videoOptions != null) {
                VideoPlayer(
                    videoOptions = videoOptions!!,
                    modifier = modifier,
                    initialPosition = savedVideoPosition,
                    onPositionChanged = { savedVideoPosition = it },
                    initialPlaying = savedVideoPlaying,
                    onPlayingChanged = { savedVideoPlaying = it },
                    onQualityChanged = { link ->
                        currentVideoUrl = link.url
                    }
                )
            }
        }
    }

    if (VideoPipState.isInPip) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            movableVideoPlayer(Modifier.fillMaxSize())
        }
        return
    }

    if (isLandscape && clazz.recordedLink.isNotBlank() && videoOptions != null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            movableVideoPlayer(Modifier.fillMaxSize())
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {

        // Video Player Placeholder or Actual Player or Live Countdown Timer
        if (isClassUpcoming(clazz)) {
            val targetTime = getClassCalendar(clazz.date, clazz.time)?.timeInMillis ?: 0L
            var timeRemainingMillis by remember { mutableStateOf(0L) }

            LaunchedEffect(targetTime) {
                while (true) {
                    val now = System.currentTimeMillis()
                    timeRemainingMillis = (targetTime - now).coerceAtLeast(0L)
                    if (timeRemainingMillis <= 0L) {
                        break
                    }
                    kotlinx.coroutines.delay(1000)
                }
            }

            val days = timeRemainingMillis / (1000 * 60 * 60 * 24)
            val hours = (timeRemainingMillis / (1000 * 60 * 60)) % 24
            val minutes = (timeRemainingMillis / (1000 * 60)) % 60
            val seconds = (timeRemainingMillis / 1000) % 60

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                            .border(1.dp, accentColor, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(accentColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "লাইভ ক্লাস শুরু হতে বাকি",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CountdownUnit(value = days, label = "দিন")
                        CountdownDivider()
                        CountdownUnit(value = hours, label = "ঘণ্টা")
                        CountdownDivider()
                        CountdownUnit(value = minutes, label = "মিনিট")
                        CountdownDivider()
                        CountdownUnit(value = seconds, label = "সেকেন্ড")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Surface(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${if (clazz.date.isNotBlank()) clazz.date else "5 May 2026"} • ${if (clazz.time.isNotBlank()) clazz.time else "11:01 AM"}",
                                color = Color.LightGray,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (clazz.liveLink.isNotBlank()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        val isLiveActive = timeRemainingMillis <= 0L
                        Button(
                            onClick = {
                                if (clazz.liveLink.startsWith("http://") || clazz.liveLink.startsWith("https://")) {
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(clazz.liveLink))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "লিঙ্ক ওপেন করা যায়নি", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "সঠিক লিঙ্ক পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLiveActive) accentColor else Color.Gray.copy(alpha = 0.3f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isLiveActive) "লাইভ ক্লাসে যোগ দিন" else "নির্দিষ্ট সময়ে জয়েন বাটন সচল হবে",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        } else if (clazz.recordedLink.isNotBlank()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                    if (isLoadingVideo) {
                        VideoLoadingPlaceholder(modifier = Modifier.fillMaxSize())
                    } else if (videoOptions != null) {
                        movableVideoPlayer(Modifier.fillMaxSize().background(Color.Black))
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                            Text("Failed to load video", color = Color.White)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                
        Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp)) {
        }

        // Unified Premium Download Button directly under the Video Player!
                // Always visible, so layout doesn't jump
                val dlUrl = currentVideoUrl ?: videoOptions?.links?.firstOrNull()?.url
                val isDownloaded = remember(downloadsList, dlUrl) {
                    dlUrl != null && downloadsList.any { it.url == dlUrl }
                }
                val downloadState = downloadStates[dlUrl ?: ""]
                
                Button(
                    onClick = {
                        if (dlUrl != null && !isDownloaded && downloadState !is DownloadState.Downloading) {
                            OfflineDownloadManager.downloadPermanently(
                                context = context,
                                url = dlUrl,
                                title = clazz.title,
                                fileType = "video",
                                courseName = courseName,
                                className = clazz.title
                            )
                        } else if (isDownloaded) {
                            Toast.makeText(context, "ভিডিওটি ইতিমধ্যে ডাউনলোড করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDownloaded) Color(0xFF10B981) else Color(0xFF3B82F6),
                        disabledContainerColor = Color(0xFF94A3B8)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoadingVideo && dlUrl != null
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isLoadingVideo) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("ভিডিও প্রস্তুত হচ্ছে...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        } else if (isDownloaded) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Downloaded", tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("ডাউনলোড সম্পন্ন", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        } else if (downloadState is DownloadState.Downloading) {
                            val pct = (downloadState.progress * 100).toInt()
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("ডাউনলোড হচ্ছে ($pct%)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        } else {
                            Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("ডাউনলোড ভিডিও", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        } else {
            // Placeholder when no video
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFF3B82F6), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(24.dp).background(Color.White, CircleShape))
                }
                
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEF4444), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("ভিডিও", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(clazz.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Class Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFEF4444), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(subject.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(clazz.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, contentDescription = "Date", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    val dateStr = if (clazz.date.isNotBlank()) clazz.date else "5 May 2026"
                    val timeStr = if (clazz.time.isNotBlank()) clazz.time else "11:01 am"
                    Text("$dateStr • $timeStr", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(24.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(32.dp).background(Color(0xFFE2E8F0), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = "Mentor", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val mentorName = mentors.find { it.id == clazz.mentorId }?.name ?: "অজানা শিক্ষক"
                    Text(mentorName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                var isContentExpanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                        .clickable { isContentExpanded = !isContentExpanded }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ক্লাসের বিষয়বস্তু", color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Icon(
                        if (isContentExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = Color(0xFF3B82F6)
                    )
                }
                if (isContentExpanded && clazz.description.isNotBlank()) {
                    Text(
                        text = clazz.description,
                        color = Color.DarkGray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (clazz.pdfLinks.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("ক্লাস রিসোর্সেস ও হোমওয়ার্ক", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    clazz.pdfLinks.forEach { pdf ->
                        val downloadedRecord = remember(downloadsList, pdf.url) {
                            downloadsList.find { it.url == pdf.url }
                        }
                        val isDownloaded = downloadedRecord != null
                        val downloadState = downloadStates[pdf.url]

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "PDF",
                                    tint = accentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pdf.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = if (isDownloaded) "অফলাইন (ডাউনলোডকৃত)" else "অনলাইন ডকুমেন্ট",
                                    fontSize = 11.sp,
                                    color = if (isDownloaded) Color(0xFF10B981) else Color(0xFF64748B),
                                    fontWeight = if (isDownloaded) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // View Button
                            Button(
                                onClick = {
                                    if (isDownloaded) {
                                        activePdfToView = File(downloadedRecord!!.localPath)
                                        activePdfTitle = pdf.title
                                    } else {
                                        downloadingPdfUrl = pdf.url
                                        OfflineDownloadManager.downloadToCache(
                                            context = context,
                                            url = pdf.url,
                                            title = pdf.title,
                                            onComplete = { tempFile ->
                                                downloadingPdfUrl = null
                                                activePdfToView = tempFile
                                                activePdfTitle = pdf.title
                                            },
                                            onError = { errMsg ->
                                                downloadingPdfUrl = null
                                                Toast.makeText(context, "ডাউনলোড ব্যর্থ হয়েছে: $errMsg", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor.copy(alpha = 0.15f),
                                    contentColor = accentColor
                                ),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                if (downloadingPdfUrl == pdf.url) {
                                    CircularProgressIndicator(
                                        color = accentColor,
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 1.5.dp
                                    )
                                } else {
                                    Text("ভিউ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Download Button / Status
                            if (isDownloaded) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Downloaded",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(28.dp)
                                )
                            } else {
                                IconButton(
                                    onClick = {
                                        if (downloadState !is DownloadState.Downloading) {
                                            OfflineDownloadManager.downloadPermanently(
                                                context = context,
                                                url = pdf.url,
                                                title = pdf.title,
                                                fileType = "pdf",
                                                courseName = courseName,
                                                className = clazz.title
                                            )
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    if (downloadState is DownloadState.Downloading) {
                                        val pct = (downloadState.progress * 100).toInt()
                                        CircularProgressIndicator(
                                            color = accentColor,
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Download",
                                            tint = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
        


        if (activePdfToView != null) {
            PdfViewerDialog(
                file = activePdfToView!!,
                title = activePdfTitle,
                onClose = { activePdfToView = null }
            )
        }
    }
}

@Composable
fun VideoLoadingPlaceholder(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    
    // Rotating arc animation
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Pulsing opacity/scale for the background glow
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant pulsing & rotating loading graphic
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale),
                contentAlignment = Alignment.Center
            ) {
                // Background outer soft glow ring
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                        radius = size.minDimension / 1.8f
                    )
                }
                
                // Rotating color arc
                Canvas(modifier = Modifier.size(48.dp).graphicsLayer(rotationZ = angle)) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFF3B82F6))
                        ),
                        startAngle = 0f,
                        sweepAngle = 280f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                
                // Centered subtle Play Icon (using existing PlayCircle)
                Icon(
                    imageVector = Icons.Filled.PlayCircle,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = alpha),
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Bengali loading texts
            Text(
                text = "ভিডিও লিংক খোঁজা হচ্ছে...",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "সেরা রেজুলেশনগুলো প্রস্তুত করা হচ্ছে, অনুগ্রহ করে অপেক্ষা করুন",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

fun getClassCalendar(dateStr: String, timeStr: String): Calendar? {
    if (dateStr.isBlank()) return null
    try {
        val dateParts = dateStr.trim().split("/")
        if (dateParts.size != 3) return null
        val day = dateParts[0].toIntOrNull() ?: return null
        val month = (dateParts[1].toIntOrNull() ?: return null) - 1
        val year = dateParts[2].toIntOrNull() ?: return null

        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.YEAR, year)

        if (timeStr.isNotBlank()) {
            val timeLower = timeStr.trim().uppercase()
            val amPm = if (timeLower.contains("PM")) Calendar.PM else Calendar.AM
            val cleanTime = timeLower.replace("AM", "").replace("PM", "").trim()
            val timeParts = cleanTime.split(":")
            if (timeParts.size >= 2) {
                val hour12 = timeParts[0].toIntOrNull() ?: 12
                val minute = timeParts[1].toIntOrNull() ?: 0
                cal.set(Calendar.HOUR, if (hour12 == 12) 0 else hour12)
                cal.set(Calendar.AM_PM, amPm)
                cal.set(Calendar.MINUTE, minute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            } else {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
        } else {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        return cal
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun isClassUpcoming(clazz: CourseClass): Boolean {
    val cal = getClassCalendar(clazz.date, clazz.time) ?: return false
    return cal.timeInMillis > System.currentTimeMillis()
}

fun convertToBengaliDigits(input: String): String {
    val english = listOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    val benglish = listOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    var result = input
    for (i in 0..9) {
        result = result.replace(english[i], benglish[i])
    }
    return result
}

@Composable
fun CountdownUnit(value: Long, label: String) {
    val valueStr = convertToBengaliDigits(String.format("%02d", value))
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = valueStr,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 28.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CountdownDivider() {
    Text(
        text = ":",
        color = Color.White.copy(alpha = 0.5f),
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.offset(y = (-4).dp)
    )
}

@Composable
fun RoutineDialog(
    routineUrl: String,
    isTeacher: Boolean,
    onDismiss: () -> Unit,
    onSaveRoutine: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }
    var currentRoutineUrl by remember { mutableStateOf(routineUrl) }

    val routinePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isUploading = true
                Toast.makeText(context, "রুটিন ছবি আপলোড হচ্ছে...", Toast.LENGTH_SHORT).show()
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    if (bytes != null) {
                        val uploadedUrl = ImgBBClient.uploadImage(bytes)
                        if (uploadedUrl != null) {
                            currentRoutineUrl = uploadedUrl
                            onSaveRoutine(uploadedUrl)
                            Toast.makeText(context, "রুটিন সফলভাবে আপলোড হয়েছে!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "আপলোড ব্যর্থ হয়েছে।", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "ত্রুটি: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isUploading = false
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ক্লাস রুটিন",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (currentRoutineUrl.isNotBlank()) {
                    coil.compose.AsyncImage(
                        model = currentRoutineUrl,
                        contentDescription = "Routine",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "কোনো রুটিন ছবি আপলোড করা হয়নি।",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }

                if (isTeacher) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { routinePickerLauncher.launch("image/*") },
                        enabled = !isUploading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.Upload, contentDescription = "Upload")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("নতুন রুটিন ছবি আপলোড করুন")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterDetailScreen(
    subject: CourseSubject,
    chapter: CourseChapter,
    mentors: List<Mentor>,
    isTeacher: Boolean,
    userEnrollment: Enrollment?,
    accentColor: Color,
    onBack: () -> Unit,
    onAddClassClick: () -> Unit,
    onEditClassClick: (CourseClass) -> Unit,
    onDeleteClassClick: (CourseClass) -> Unit,
    onViewClassDetail: (CourseClass) -> Unit
) {
    val mContext = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                onClick = onBack,
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF3B82F6), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ListAlt,
                    contentDescription = "List",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "অধ্যায় বিস্তারিত - ${subject.title}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }
            
            if (isTeacher) {
                Card(
                    onClick = onAddClassClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3B82F6)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Class",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(0xFF3B82F6), CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "ক্লাস ও উপকরণসমূহ",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
        }
        
        if (chapter.classes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp)
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "কোনো ক্লাস বা উপকরণ যোগ করা হয়নি।",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            chapter.classes.forEachIndexed { index, clazz ->
                val isFullCoursePurchased = userEnrollment != null && userEnrollment.purchased_quarters.isEmpty()
                val isQuarterPurchased = userEnrollment != null && clazz.quarterId.isNotEmpty() && userEnrollment.purchased_quarters.split(",").contains(clazz.quarterId)
                val canViewClass = isTeacher || clazz.isFree || isFullCoursePurchased || isQuarterPurchased
                
                val cardBorderColor = if (index % 2 == 0) Color(0xFFDBEAFE) else Color(0xFFD1FAE5)
                val cardBgColor = if (index % 2 == 0) Color(0xFFEFF6FF) else Color(0xFFECFDF5)
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clickable {
                            if (canViewClass) {
                                onViewClassDetail(clazz)
                            } else {
                                Toast.makeText(mContext, "এই ক্লাসটি দেখার জন্য আপনাকে সঠিক কোয়ার্টার এনরোল করতে হবে", Toast.LENGTH_SHORT).show()
                            }
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    border = BorderStroke(1.dp, cardBorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = clazz.type,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFD1FAE5), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (clazz.recordedLink.isNotBlank()) "রেকর্ডেড" else "লাইভ",
                                        color = Color(0xFF065F46),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isTeacher) {
                                    IconButton(
                                        onClick = { onEditClassClick(clazz) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Class",
                                            tint = Color(0xFF64748B)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteClassClick(clazz) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Class",
                                            tint = Color.Red
                                        )
                                    }
                                }
                                
                                Card(
                                    onClick = {
                                        if (canViewClass) {
                                            onViewClassDetail(clazz)
                                        } else {
                                            Toast.makeText(mContext, "এই ক্লাসটি দেখার জন্য আপনাকে সঠিক কোয়ার্টার এনরোল করতে হবে", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = CircleShape,
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    modifier = Modifier.size(40.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Enter class",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(18.dp).rotate(180f)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = clazz.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        
                        if (clazz.date.isNotBlank() || clazz.time.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${clazz.date} • ${clazz.time}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        val mentorObj = mentors.find { it.id == clazz.mentorId }
                        if (mentorObj != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (mentorObj.image_url.isNotBlank()) {
                                    coil.compose.AsyncImage(
                                        model = mentorObj.image_url,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = mentorObj.name.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddResourceDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন রিসোর্স যোগ করুন", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("রিসোর্সের নাম") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("রিসোর্সের লিংক (URL)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && url.isNotBlank()) {
                        onAdd(title, url)
                    }
                }
            ) {
                Text("যোগ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
fun UnenrolledCourseOverview(
    course: CourseItem,
    profile: UserProfile,
    courseInteractions: List<CourseInteraction>,
    onLikeToggle: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())) {
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp).background(Color.LightGray, RoundedCornerShape(16.dp))
        ) {
            if (course.bannerUrl.isNotBlank()) {
                coil.compose.AsyncImage(
                    model = course.bannerUrl,
                    contentDescription = "Course Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(accentColor), contentAlignment = Alignment.Center) {
                    Text("No Banner", color = Color.White)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(course.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text(course.description, fontSize = 16.sp, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("দাম", color = Color.Gray, fontSize = 12.sp)
                val originalPrice = course.mainPrice.toDoubleOrNull() ?: 0.0
                val discountPriceVal = course.discountPrice.toDoubleOrNull() ?: 0.0
                val hasDiscount = originalPrice > 0 && discountPriceVal > 0 && originalPrice > discountPriceVal
                
                if (course.pricingOption == "Fully Free") {
                    Text("ফ্রি", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentColor)
                } else if (hasDiscount) {
                    val pct = (((originalPrice - discountPriceVal) / originalPrice) * 100).toInt()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("৳${course.discountPrice}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "৳${course.mainPrice}",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFEE2E2), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("$pct% ছাড়", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text("৳${course.mainPrice}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentColor)
                }
            }
            
            Column {
                Text("মোট বিষয়", color = Color.Gray, fontSize = 12.sp)
                Text("${course.subjects.size} টি", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("পছন্দ (Likes)", color = Color.Gray, fontSize = 12.sp)
                val likedByMe = courseInteractions.any { it.course_id == course.id && it.user_id == profile.user_id && it.is_like }
                val totalLikes = courseInteractions.count { it.course_id == course.id && it.is_like }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLikeToggle() }
                ) {
                    Icon(
                        imageVector = if (likedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (likedByMe) Color.Red else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("$totalLikes টি", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (likedByMe) Color.Red else Color.Black)
                }
            }
            Column {
                Text("ভিউ (Views)", color = Color.Gray, fontSize = 12.sp)
                val totalViews = courseInteractions.count { it.course_id == course.id && !it.is_like }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Views",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("$totalViews বার", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        if (course.isQuarterOn && course.quarters.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("কোয়ার্টার বা প্যাকেজ সমূহ", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            val currentDate = java.time.LocalDate.now()
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
            
            course.quarters.forEach { quarter ->
                var statusText = "অজানা"
                var statusColor = Color.Gray
                var statusIcon = Icons.Default.HelpOutline
                
                try {
                    val start = java.time.LocalDate.parse(quarter.startDate, formatter)
                    val end = java.time.LocalDate.parse(quarter.endDate, formatter)
                    
                    if (currentDate.isBefore(start)) {
                        statusText = "পড়ানো হবে"
                        statusColor = Color(0xFF3B82F6) // Blue
                        statusIcon = Icons.Default.Schedule
                    } else if (currentDate.isAfter(end)) {
                        statusText = "সম্পন্ন"
                        statusColor = Color(0xFF22C55E) // Green
                        statusIcon = Icons.Default.CheckCircle
                    } else {
                        statusText = "পড়ানো হচ্ছে"
                        statusColor = Color(0xFFEAB308) // Yellow/Orange
                        statusIcon = Icons.Default.PlayCircle
                    }
                } catch (e: Exception) { }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(quarter.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("${quarter.startDate} - ${quarter.endDate}", fontSize = 12.sp, color = Color.Gray)
                            }
                            Text("৳${quarter.price}", fontWeight = FontWeight.Bold, color = accentColor)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(statusText, color = statusColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(120.dp))
    }
}

