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
    onClearInitialNavigation: () -> Unit = {},
    initialSelectedQuarterName: String? = null,
    onCourseUpdate: ((CourseItem) -> Unit)? = null,
    selectedSubjectForView: CourseSubject?,
    onSelectedSubjectChange: (CourseSubject?) -> Unit,
    showLearningResourcesForSubject: CourseSubject?,
    onShowLearningResourcesForSubjectChange: (CourseSubject?) -> Unit,
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
            onClearInitialNavigation()
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

    val activeClass = currentClass ?: selectedClassForView
    val activeSubjectForClass = currentSubject ?: selectedSubjectForView

    val activeChapter = currentChapter ?: selectedChapterForView
    val activeSubjectForChapter = currentSubject ?: selectedSubjectForView

    val resourcesSubject = showLearningResourcesForSubject

    if (activeClass != null && activeSubjectForClass != null) {
        ClassDetailView(
            clazz = activeClass,
            subject = activeSubjectForClass,
            mentors = mentors,
            accentColor = accentColor,
            courseName = course.title,
            onBack = { onSelectedClassChange(null) }
        )
    } else if (activeChapter != null && activeSubjectForChapter != null) {
        ChapterDetailScreen(
            subject = activeSubjectForChapter,
            chapter = activeChapter,
            mentors = mentors,
            isTeacher = isTeacher,
            userEnrollment = userEnrollment,
            accentColor = accentColor,
            onBack = { onSelectedChapterChange(null) },
            onAddClassClick = { chapterToAddClassTo = Pair(activeSubjectForChapter, activeChapter) },
            onEditClassClick = { clazz -> classToEdit = Triple(activeSubjectForChapter, activeChapter, clazz) },
            onDeleteClassClick = { clazz ->
                val updatedChapter = activeChapter.copy(classes = activeChapter.classes.filter { it.id != clazz.id })
                onSelectedChapterChange(updatedChapter)
                
                val updatedSubject = activeSubjectForChapter.copy(chapters = activeSubjectForChapter.chapters.map { if (it.id == activeChapter.id) updatedChapter else it })
                onSelectedSubjectChange(updatedSubject)
                
                val updatedSubjects = course.subjects.map { if (it.id == activeSubjectForChapter.id) updatedSubject else it }
                onUpdate(updatedSubjects)
                syncSubjectToAllCourses(updatedSubject)
            },
            onViewClassDetail = { clazz ->
                onSelectedClassChange(clazz)
            }
        )
    } else if (resourcesSubject != null) {
        val subject = currentSubject ?: resourcesSubject
        LearningResourcesScreen(
            subject = subject,
            course = course,
            isTeacher = isTeacher,
            accentColor = accentColor,
            onBack = { onShowLearningResourcesForSubjectChange(null) },
            onUpdateSubject = { updatedSubject ->
                onSelectedSubjectChange(updatedSubject)
                val updatedSubjects = course.subjects.map { if (it.id == updatedSubject.id) updatedSubject else it }
                onUpdate(updatedSubjects)
                syncSubjectToAllCourses(updatedSubject)
                // keep the dialog open with the updated subject
                onShowLearningResourcesForSubjectChange(updatedSubject)
            }
        )
    } else if (selectedSubjectForView == null) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)) {
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
                                            val formatter = java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy")
                                            val start = java.time.LocalDate.parse(quarterObj.startDate.trim(), formatter)
                                            val end = java.time.LocalDate.parse(quarterObj.endDate.trim(), formatter)
                                            val today = java.time.LocalDate.now()
                                            
                                            val hasClasses = course.subjects.flatMap { it.chapters }.filter { (it.quarter.ifBlank { "Quarter 1" }) == qName }.flatMap { it.classes }.isNotEmpty()
                                            if (today.isBefore(start)) {
                                                if (hasClasses) {
                                                    quarterStatus = "পড়ানো হচ্ছে"
                                                    statusBgColor = if (isSelected) Color(0xFFFEF08A).copy(alpha = 0.3f) else Color(0xFFFEF9C3)
                                                    statusTextColor = if (isSelected) Color.White else Color(0xFF854D0E)
                                                } else {
                                                    quarterStatus = "পড়ানো হবে"
                                                    statusBgColor = if (isSelected) Color(0xFF93C5FD).copy(alpha = 0.3f) else Color(0xFFDBEAFE)
                                                    statusTextColor = if (isSelected) Color.White else Color(0xFF1E3A8A)
                                                }
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
        val subj = currentSubject ?: selectedSubjectForView
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)) {

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
                                            val formatter = java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy")
                                            val start = java.time.LocalDate.parse(quarterObj.startDate.trim(), formatter)
                                            val end = java.time.LocalDate.parse(quarterObj.endDate.trim(), formatter)
                                            val today = java.time.LocalDate.now()
                                            
                                            val hasClasses = course.subjects.flatMap { it.chapters }.filter { (it.quarter.ifBlank { "Quarter 1" }) == qName }.flatMap { it.classes }.isNotEmpty()
                                            if (today.isBefore(start)) {
                                                if (hasClasses) {
                                                    quarterStatus = "পড়ানো হচ্ছে"
                                                    statusBgColor = if (isSelected) Color(0xFFFEF08A).copy(alpha = 0.3f) else Color(0xFFFEF9C3)
                                                    statusTextColor = if (isSelected) Color.White else Color(0xFF854D0E)
                                                } else {
                                                    quarterStatus = "পড়ানো হবে"
                                                    statusBgColor = if (isSelected) Color(0xFF93C5FD).copy(alpha = 0.3f) else Color(0xFFDBEAFE)
                                                    statusTextColor = if (isSelected) Color.White else Color(0xFF1E3A8A)
                                                }
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
                onClick = { onShowLearningResourcesForSubjectChange(subj) },
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
                        text = "→",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
            
            if (isAddingResource) {
                AddResourceDialog(
                    onDismiss = { isAddingResource = false },
                    onAdd = { title, url ->
                        val currentResources = (subj.learningResources as? List<*>?)?.filterIsInstance<PdfLink>() ?: emptyList()
                        val updatedResources = currentResources.toMutableList().apply { add(PdfLink(title, url)) }
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
                        .padding(vertical = 32.dp)
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = "Empty",
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "এই কোয়ার্টারে ক্লাস শুরু হয়নি",
                            color = Color(0xFF475569),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "খুব শীঘ্রই এই কোয়ার্টারের ক্লাস শুরু হবে। প্রস্তুতি নিতে থাকুন!",
                            color = Color(0xFF64748B),
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
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

    val currentSubjectToDelete = subjectToDelete
    if (currentSubjectToDelete != null) {
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { Text("বিষয় ডিলিট করুন") },
            text = { Text("আপনি কি নিশ্চিতভাবে '${currentSubjectToDelete.title}' বিষয় এবং এর ভিতরের সব অধ্যায় ও ক্লাস ডিলিট করতে চান?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updatedSubjects = course.subjects.filter { it.id != currentSubjectToDelete.id }
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

    val currentSubjectToAdd = subjectToAddChapterTo
    if (currentSubjectToAdd != null) {
        var newTitle by remember { mutableStateOf("") }
        var selectedQuarterForNewChapter by remember { mutableStateOf(quartersList.firstOrNull() ?: "Quarter 1") }
        var quarterDropdownExpanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { subjectToAddChapterTo = null },
            title = { Text("নতুন অধ্যায় (Chapter) যোগ করুন") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)) {
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
                        val updatedSubject = currentSubjectToAdd.copy(chapters = currentSubjectToAdd.chapters + newChapter)
                        val updatedSubjects = course.subjects.map {
                            if (it.id == currentSubjectToAdd.id) updatedSubject else it
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

    val currentChapterToEdit = chapterToEdit
    if (currentChapterToEdit != null) {
        val subject = currentChapterToEdit.first
        val chapter = currentChapterToEdit.second
        var editTitle by remember { mutableStateOf(chapter.title) }
        var selectedQuarterForEditChapter by remember { mutableStateOf(chapter.quarter.ifBlank { quartersList.firstOrNull() ?: "Quarter 1" }) }
        var quarterDropdownExpanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { chapterToEdit = null },
            title = { Text("অধ্যায় (Chapter) এডিট করুন") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)) {
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${index + 1}. ${pdf.title}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = pdf.url,
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            newPdfTitle = pdf.title
                                            newPdfUrl = pdf.url
                                            pdfLinks = pdfLinks.filterIndexed { idx, _ -> idx != index }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit PDF link",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = {
                                            pdfLinks = pdfLinks.filterIndexed { idx, _ -> idx != index }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete PDF link",
                                            tint = Color.Red,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
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

