package com.example

import java.io.File
import androidx.compose.ui.res.stringResource
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
    onPurchaseClick: () -> Unit = {},
    viewModel: com.example.viewmodel.CourseViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
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
            RoutineBannerCard(onClick = { showRoutineDialog = true })

            // 2. Selectable Quarter Tabs - Premium Card Layout with Dates & Progress Bars
            if (course.isQuarterOn && course.quarters.isNotEmpty()) {
                QuarterTabs(
                    selectedQuarterName = selectedQuarterName,
                    onQuarterSelected = { selectedQuarterName = it },
                    course = course,
                    quartersList = quartersList,
                    isQuarterLocked = isQuarterLocked
                )
            }

            // 3. Subjects Grid with Video and PDF details and Premium Overlays
            val isCurrentQuarterLocked = isQuarterLocked(selectedQuarterName)
            if (isCurrentQuarterLocked) {
                QuarterLockedCard(accentColor = accentColor, onPurchaseClick = onPurchaseClick)
            } else if (course.subjects.isEmpty()) {
                Text("এখনো কোনো বিষয়বস্তু যোগ করা হয়নি।", color = Color.Gray, fontSize = 14.sp)
            } else {
                course.subjects.chunked(2).forEach { rowSubjects ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowSubjects.forEach { subject ->
                            SubjectCard(
                                subject = subject,
                                course = course,
                                isTeacher = isTeacher,
                                onSelectedSubjectChange = onSelectedSubjectChange,
                                onEditSubjectClick = { subjectToEdit = subject },
                                onDeleteSubjectClick = { subjectToDelete = subject },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowSubjects.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Elegant "নতুন বিষয় যোগ করুন" Card for Teachers at the bottom of the list
            if (isTeacher) {
                ActionCardButton(
                    text = "নতুন বিষয় যোগ করুন",
                    accentColor = accentColor,
                    onClick = { isAddingSubject = true },
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    } else {
        val subj = currentSubject ?: selectedSubjectForView
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)) {

            // 1. Quarters Selectable Tabs inside subject details - Premium Card style
            if (course.isQuarterOn && course.quarters.isNotEmpty()) {
                QuarterTabs(
                    selectedQuarterName = selectedQuarterName,
                    onQuarterSelected = { selectedQuarterName = it },
                    course = course,
                    quartersList = quartersList,
                    isQuarterLocked = isQuarterLocked,
                    subj = subj
                )
            }

            // 2. Learning Resources Banner (Screenshot 2 style)
            LearningResourcesBanner(onClick = { onShowLearningResourcesForSubjectChange(subj) })
            
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
                QuarterLockedCard(accentColor = accentColor, onPurchaseClick = onPurchaseClick)
            } else if (chaptersToShow.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                        .background(Color(0xFFF3F4F6), RoundedCornerShape(16.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = "Empty",
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF1E3A8A).copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "এই কোয়ার্টারে ক্লাস শুরু হয়নি",
                            color = Color(0xFF1E3A8A),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "খুব শীঘ্রই এই কোয়ার্টারের ক্লাস শুরু হবে। প্রস্তুতি নিতে থাকুন!",
                            color = Color(0xFF1E3A8A),
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                chaptersToShow.forEach { chapter ->
                    ChapterCard(
                        chapter = chapter,
                        subj = subj,
                        isTeacher = isTeacher,
                        onSelectedChapterChange = onSelectedChapterChange,
                        onEditChapterClick = { chapterToEdit = Pair(subj, chapter) },
                        onDeleteChapterClick = {
                            val updatedSubject = subj.copy(chapters = subj.chapters.filter { it.id != chapter.id })
                            val updatedSubjects = course.subjects.map { if (it.id == subj.id) updatedSubject else it }
                            onUpdate(updatedSubjects)
                            syncSubjectToAllCourses(updatedSubject)
                        },
                        onStatusChange = { targetStatus ->
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
            if (isTeacher) {
                ActionCardButton(
                    text = "নতুন অধ্যায় যোগ করুন",
                    accentColor = accentColor,
                    onClick = { subjectToAddChapterTo = subj },
                    modifier = Modifier.padding(vertical = 12.dp)
                )
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
            title = { Text(stringResource(R.string.add_new_chapter)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text(stringResource(R.string.chapter_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (course.isQuarterOn && course.quarters.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.select_quarter_title), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().clickable { quarterDropdownExpanded = true }) {
                            OutlinedTextField(
                                value = selectedQuarterForNewChapter,
                                onValueChange = {},
                                enabled = false,
                                label = { Text(stringResource(R.string.quarter_label)) },
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
                }) { Text(stringResource(R.string.add_new_chapter)) }
            },
            dismissButton = {
                TextButton(onClick = { subjectToAddChapterTo = null }) { Text(stringResource(R.string.cancel_button)) }
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
            title = { Text(stringResource(R.string.edit_chapter_title)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text(stringResource(R.string.chapter_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (course.isQuarterOn && course.quarters.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.select_quarter_title), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().clickable { quarterDropdownExpanded = true }) {
                            OutlinedTextField(
                                value = selectedQuarterForEditChapter,
                                onValueChange = {},
                                enabled = false,
                                label = { Text(stringResource(R.string.quarter_label)) },
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
                }) { Text(stringResource(R.string.save_button)) }
            },
            dismissButton = {
                TextButton(onClick = { chapterToEdit = null }) { Text(stringResource(R.string.cancel_button)) }
            }
        )
    }

    val classDialogState = chapterToAddClassTo ?: classToEdit?.let { Pair(it.first, it.second) }
    if (classDialogState != null) {
        val subject = classDialogState.first
        val chapter = classDialogState.second
        val existingClass = classToEdit?.third

        AddEditClassDialog(
            existingClass = existingClass,
            course = course,
            mentors = mentors,
            accentColor = accentColor,
            onDismiss = {
                chapterToAddClassTo = null
                classToEdit = null
            },
            onSave = { savedClass, selectedMentorName ->
                val updatedSubject = subject.copy(chapters = subject.chapters.map { ch ->
                    if (ch.id == chapter.id) {
                        val updatedClasses = if (existingClass != null) {
                            ch.classes.map { if (it.id == existingClass.id) savedClass else it }
                        } else {
                            ch.classes + savedClass
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
                    classId = savedClass.id,
                    subjectTitle = subject.title,
                    chapterTitle = chapter.title,
                    classTitle = savedClass.title,
                    mentorName = if (selectedMentorName.isBlank()) "অজানা শিক্ষক" else selectedMentorName,
                    dateStr = savedClass.date,
                    timeStr = savedClass.time
                )
                ClassNotificationScheduler.scheduleOneSignalPushNotification(
                    context = mContext,
                    courseId = course.id,
                    quarterId = savedClass.quarterId,
                    subjectTitle = subject.title,
                    chapterTitle = chapter.title,
                    classTitle = savedClass.title,
                    mentorName = if (selectedMentorName.isBlank()) "অজানা শিক্ষক" else selectedMentorName,
                    dateStr = savedClass.date,
                    timeStr = savedClass.time
                )

                chapterToAddClassTo = null
                classToEdit = null
            }
        )
    }
}

