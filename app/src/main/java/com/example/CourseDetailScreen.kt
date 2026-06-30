package com.example

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
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onEnroll: (purchasedQuarters: String) -> Unit,
    onLikeToggle: () -> Unit,
    onCourseUpdate: (CourseItem) -> Unit,
    onMultipleCoursesUpdate: (List<CourseItem>) -> Unit = {},
    accentColor: Color,
    onBack: () -> Unit
) {
    var selectedQuarters by remember { mutableStateOf(setOf<CourseQuarter>()) }
    var selectFullCourse by remember { mutableStateOf(true) }
    val mContext = LocalContext.current
    val isTeacher = course.channel_id == profile.user_id

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
            TopAppBar(
                title = { Text("কোর্স বিস্তারিত", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (!isTeacher) {
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
                        if (course.pricingOption != "Fully Free") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total Price:", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Gray)
                                Text("৳${totalPrice.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = accentColor)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        Button(
                            onClick = {
                                if (userEnrollment == null) {
                                    val purchasedQuartersStr = if (selectFullCourse) "" else selectedQuarters.joinToString(",") { it.name }
                                    onEnroll(purchasedQuartersStr)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (userEnrollment != null) Color.Gray else accentColor),
                            shape = RoundedCornerShape(12.dp),
                            enabled = userEnrollment == null
                        ) {
                            Text(
                                text = if (userEnrollment != null) "আপনি ইতিমধ্যে কোর্সটি কিনেছেন ✔️" else if (course.pricingOption == "Fully Free") "এনরোল করুন (Free)" else "পেমেন্ট করুন (Pay)", 
                                fontSize = 16.sp, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("এটি আপনার তৈরি করা কোর্স", fontWeight = FontWeight.Bold, color = accentColor)
                    }
                }
            }
        },
        containerColor = Color(0xFFFBF8F1)
    ) { paddingValues ->
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
                    onUpdate = { newSubjects -> onCourseUpdate(course.copy(subjects = newSubjects)) },
                    onMultipleCoursesUpdate = onMultipleCoursesUpdate,
                    accentColor = accentColor
                )
            }

            // Pricing & Quarters Selection
            if (course.pricingOption != "Fully Free") {
                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("ক্রয় অপশন (Purchase Options)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D3748))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Full Course Option
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectFullCourse = true; selectedQuarters = emptySet() },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectFullCourse) accentColor.copy(alpha = 0.1f) else Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (selectFullCourse) accentColor else Color.LightGray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectFullCourse,
                                onClick = { selectFullCourse = true; selectedQuarters = emptySet() },
                                colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Full Course", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("সবগুলো কোয়ার্টার একসাথে", fontSize = 12.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                val dPrice = course.discountPrice.toDoubleOrNull()
                                val mPrice = course.mainPrice.toDoubleOrNull()
                                if (dPrice != null && mPrice != null && dPrice < mPrice) {
                                    Text("৳${mPrice.toInt()}", fontSize = 12.sp, color = Color.Gray, textDecoration = TextDecoration.LineThrough)
                                    Text("৳${dPrice.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                } else if (mPrice != null) {
                                    Text("৳${mPrice.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                }
                            }
                        }
                    }
                }

                if (course.isQuarterOn && course.quarters.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("অথবা নির্দিষ্ট কোয়ার্টার কিনুন:", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(course.quarters) { quarter ->
                        val isSelected = selectedQuarters.contains(quarter)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = isSelected && !selectFullCourse,
                                    onValueChange = { checked ->
                                        selectFullCourse = false
                                        selectedQuarters = if (checked) {
                                            selectedQuarters + quarter
                                        } else {
                                            selectedQuarters - quarter
                                        }
                                    }
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected && !selectFullCourse) accentColor.copy(alpha = 0.1f) else Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isSelected && !selectFullCourse) accentColor else Color.LightGray
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected && !selectFullCourse,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(checkedColor = accentColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(quarter.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    if (quarter.startDate.isNotBlank() || quarter.endDate.isNotBlank()) {
                                        Text("${quarter.startDate} - ${quarter.endDate}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                                Text("৳${quarter.price}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accentColor)
                            }
                        }
                    }
                }
            }
            
            // Payment Instructions
            if (course.pricingOption != "Fully Free" && (course.bkashNumber.isNotBlank() || course.nagadNumber.isNotBlank() || course.rocketNumber.isNotBlank())) {
                item {
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    Text("Payment Methods", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D3748))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (course.bkashNumber.isNotBlank()) {
                        Text("bKash: ${course.bkashNumber}", fontWeight = FontWeight.Medium)
                    }
                    if (course.nagadNumber.isNotBlank()) {
                        Text("Nagad: ${course.nagadNumber}", fontWeight = FontWeight.Medium)
                    }
                    if (course.rocketNumber.isNotBlank()) {
                        Text("Rocket: ${course.rocketNumber}", fontWeight = FontWeight.Medium)
                    }
                    if (course.paymentDetails.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(course.paymentDetails, color = Color.DarkGray, fontSize = 14.sp)
                    }
                }
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
    accentColor: Color
) {
    val mContext = LocalContext.current
    var subjectToEdit by remember { mutableStateOf<CourseSubject?>(null) }
    var isAddingSubject by remember { mutableStateOf(false) }
    var selectedSubjectForView by remember { mutableStateOf<CourseSubject?>(null) }
    var selectedClassForView by remember { mutableStateOf<CourseClass?>(null) }
    var subjectToAddChapterTo by remember { mutableStateOf<CourseSubject?>(null) }
    var chapterToEdit by remember { mutableStateOf<Pair<CourseSubject, CourseChapter>?>(null) }
    var chapterToAddClassTo by remember { mutableStateOf<Pair<CourseSubject, CourseChapter>?>(null) }
    var classToEdit by remember { mutableStateOf<Triple<CourseSubject, CourseChapter, CourseClass>?>(null) }
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
        ClassDetailView(
            clazz = selectedClassForView!!,
            subject = selectedSubjectForView!!,
            mentors = mentors,
            accentColor = accentColor,
            onBack = { selectedClassForView = null }
        )
    } else if (selectedSubjectForView == null) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("কোর্সের বিষয়বস্তু (Course Content)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D3748))
                if (isTeacher) {
                    IconButton(onClick = { isAddingSubject = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Subject", tint = accentColor)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (course.subjects.isEmpty()) {
                Text("এখনো কোনো বিষয়বস্তু যোগ করা হয়নি।", color = Color.Gray, fontSize = 14.sp)
            } else {
                course.subjects.chunked(2).forEach { rowSubjects ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowSubjects.forEach { subject ->
                            val bgColor = try { Color(android.graphics.Color.parseColor(subject.colorHex)) } catch (e: Exception) { Color(0xFFEF4444) }
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clickable { selectedSubjectForView = subject },
                                colors = CardDefaults.cardColors(containerColor = bgColor),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    val isMainCourse = subject.sourceCourseId == null || subject.sourceCourseId == course.id
                                    if (isTeacher && isMainCourse) {
                                        TextButton(
                                            onClick = { subjectToEdit = subject },
                                            modifier = Modifier.align(Alignment.TopEnd)
                                        ) {
                                            Text("এডিট", color = Color.White, fontSize = 12.sp)
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        if (subject.iconUrl.isNotBlank()) {
                                            coil.compose.AsyncImage(
                                                model = subject.iconUrl,
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        } else {
                                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(subject.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
        }
    } else {
        val subject = selectedSubjectForView!!
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedSubjectForView = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = accentColor)
                    }
                    Text(subject.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D3748))
                }
                val isMainCourse = subject.sourceCourseId == null || subject.sourceCourseId == course.id
                if (isTeacher && isMainCourse) {
                    IconButton(onClick = { subjectToAddChapterTo = subject }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Chapter", tint = accentColor)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (subject.chapters.isEmpty()) {
                Text("কোনো অধ্যায় নেই।", color = Color.Gray, fontSize = 14.sp)
            } else {
                subject.chapters.forEach { chapter ->
                    var isChapterExpanded by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { isChapterExpanded = !isChapterExpanded },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(chapter.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                val isMainCourse = subject.sourceCourseId == null || subject.sourceCourseId == course.id
                                if (isTeacher && isMainCourse) {
                                    IconButton(onClick = { chapterToAddClassTo = Pair(subject, chapter) }) {
                                        Icon(Icons.Default.Add, contentDescription = "Add Class", tint = accentColor, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = { chapterToEdit = Pair(subject, chapter) }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Chapter", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = {
                                        val updatedSubject = subject.copy(chapters = subject.chapters.filter { it.id != chapter.id })
                                        val updatedSubjects = course.subjects.map {
                                            if (it.id == subject.id) updatedSubject else it
                                        }
                                        onUpdate(updatedSubjects)
                                        syncSubjectToAllCourses(updatedSubject)
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Chapter", tint = Color.Red, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Icon(if (isChapterExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
                            }

                            if (isChapterExpanded) {
                                if (chapter.classes.isEmpty()) {
                                    Text("কোনো ক্লাস নেই।", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                                } else {
                                    chapter.classes.forEach { clazz ->
                                        val isFullCoursePurchased = userEnrollment != null && userEnrollment.purchased_quarters.isEmpty()
                                        val isQuarterPurchased = userEnrollment != null && clazz.quarterId.isNotEmpty() && userEnrollment.purchased_quarters.split(",").contains(clazz.quarterId)
                                        val canViewClass = isTeacher || clazz.isFree || isFullCoursePurchased || isQuarterPurchased

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { 
                                                if (canViewClass) {
                                                    selectedClassForView = clazz 
                                                } else {
                                                    Toast.makeText(mContext, "এই ক্লাসটি দেখার জন্য আপনাকে সঠিক কোয়ার্টার এনরোল করতে হবে", Toast.LENGTH_SHORT).show()
                                                }
                                            }.padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (canViewClass) {
                                                Icon(if (isClassUpcoming(clazz)) Icons.Default.DateRange else Icons.Default.PlayCircle, contentDescription = "Class", tint = if (isClassUpcoming(clazz)) accentColor else Color.Gray, modifier = Modifier.size(20.dp))
                                            } else {
                                                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.LightGray, modifier = Modifier.size(20.dp))
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                 Text(
                                                     text = "${clazz.type}: ${clazz.title}", 
                                                     fontSize = 14.sp, 
                                                     fontWeight = FontWeight.Medium,
                                                     color = if (canViewClass) Color.DarkGray else Color.LightGray
                                                 )
                                                 if (isClassUpcoming(clazz)) {
                                                     Spacer(modifier = Modifier.height(4.dp))
                                                     Row(verticalAlignment = Alignment.CenterVertically) {
                                                         Box(
                                                             modifier = Modifier
                                                                 .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                                 .padding(horizontal = 6.dp, vertical = 2.dp)
                                                         ) {
                                                             Text(
                                                                 text = "আসন্ন ক্লাস", 
                                                                 color = accentColor, 
                                                                 fontSize = 11.sp, 
                                                                 fontWeight = FontWeight.Bold
                                                             )
                                                         }
                                                         if (clazz.date.isNotBlank() || clazz.time.isNotBlank()) {
                                                             Spacer(modifier = Modifier.width(8.dp))
                                                             Text(
                                                                 text = "${clazz.date} • ${clazz.time}", 
                                                                 color = Color.Gray, 
                                                                 fontSize = 11.sp
                                                             )
                                                         }
                                                     }
                                                 }
                                             }
                                            if (isTeacher && (subject.sourceCourseId == null || subject.sourceCourseId == course.id)) {
                                                IconButton(onClick = { classToEdit = Triple(subject, chapter, clazz) }) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit Class", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                                }
                                                IconButton(onClick = {
                                                    val updatedChapter = chapter.copy(classes = chapter.classes.filter { it.id != clazz.id })
                                                    val updatedSubject = subject.copy(chapters = subject.chapters.map { if (it.id == chapter.id) updatedChapter else it })
                                                    val updatedSubjects = course.subjects.map { if (it.id == subject.id) updatedSubject else it }
                                                    onUpdate(updatedSubjects)
                                                    syncSubjectToAllCourses(updatedSubject)
                                                }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete Class", tint = Color.Red, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isAddingSubject || subjectToEdit != null) {
        val initialSubj = subjectToEdit
        AddEditSubjectDialog(
            initialSubject = initialSubj,
            isTeacher = isTeacher,
            channelId = course.channel_id,
            courseId = course.id,
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
                    selectedSubjectForView = updatedSubject
                }
                isAddingSubject = false
                subjectToEdit = null
            }
        )
    }

    if (subjectToAddChapterTo != null) {
        var newTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { subjectToAddChapterTo = null },
            title = { Text("নতুন অধ্যায় (Chapter) যোগ করুন") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("অধ্যায়ের নাম") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTitle.isNotBlank()) {
                        val newChapter = CourseChapter(title = newTitle)
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
        AlertDialog(
            onDismissRequest = { chapterToEdit = null },
            title = { Text("অধ্যায় (Chapter) এডিট করুন") },
            text = {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("অধ্যায়ের নাম") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editTitle.isNotBlank()) {
                        val updatedChapter = chapter.copy(title = editTitle)
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
                                        newDate = "$day/${month + 1}/$year"
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
                                    pdfLinks = pdfLinks
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
                                    pdfLinks = pdfLinks
                                )
                                val updatedSubject = subject.copy(chapters = subject.chapters.map { ch ->
                                    if (ch.id == chapter.id) {
                                        val updatedClasses = if (existingClass != null) {
                                            ch.classes.map { if (it.id == existingClass.id) newClass else it }
                                        } else {
                                            ch.classes + newClass
                                        }
                                        ch.copy(classes = updatedClasses)
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
    courseId: String,
    onDismiss: () -> Unit,
    onSave: (CourseSubject, List<CourseItem>) -> Unit
) {
    var newTitle by remember { mutableStateOf(initialSubject?.title ?: "") }
    var newColorHex by remember { mutableStateOf(initialSubject?.colorHex ?: "#FF6B6B") }
    var newIconUrl by remember { mutableStateOf(initialSubject?.iconUrl ?: "") }
    
    val colors = listOf("#EF4444", "#F97316", "#F59E0B", "#10B981", "#3B82F6", "#8B5CF6", "#EC4899", "#64748B")
    
    var otherCourses by remember { mutableStateOf<List<CourseItem>>(emptyList()) }
    var selectedOtherCourseIds by remember { mutableStateOf(setOf<String>()) }
    
    LaunchedEffect(channelId) {
        if (isTeacher && channelId != null) {
            withContext(Dispatchers.IO) {
                try {
                    val courses = supabase.from("courses")
                        .select { filter { eq("channel_id", channelId) } }
                        .decodeList<CourseItem>()
                    otherCourses = courses.filter { it.id != courseId }
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
                
                Text("লোগো/আইকন URL (ঐচ্ছিক)", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = newIconUrl, onValueChange = { newIconUrl = it }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("বিষয়ের রঙ", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    colors.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .clickable { newColorHex = hex }
                                .border(if (newColorHex == hex) 3.dp else 0.dp, if (newColorHex == hex) Color.Black else Color.Transparent, CircleShape)
                        )
                    }
                }
                
                val isMainCourse = initialSubject == null || initialSubject.sourceCourseId == null || initialSubject.sourceCourseId == courseId
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
                            sourceCourseId = initialSubject.sourceCourseId ?: courseId
                        ) ?: CourseSubject(title = newTitle, colorHex = newColorHex, iconUrl = newIconUrl, sourceCourseId = courseId)
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

@Composable
fun VideoPlayer(
    videoOptions: VideoOptions, 
    modifier: Modifier = Modifier,
    onQualityChanged: (VideoLink) -> Unit = {}
) {
    val context = LocalContext.current
    var showQualitySelector by remember { mutableStateOf(false) }
    
    // Non-adaptive quality tracking
    var currentNonAdaptiveQuality by remember(videoOptions) { 
        mutableStateOf(videoOptions.links.firstOrNull()?.quality ?: "Unknown") 
    }
    
    val httpDataSourceFactory = remember {
        androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .setDefaultRequestProperties(mapOf("Referer" to "https://www.facebook.com/"))
            .setAllowCrossProtocolRedirects(true)
    }
    
    val exoPlayer = remember {
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(httpDataSourceFactory)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    LaunchedEffect(videoOptions, currentNonAdaptiveQuality) {
        val currentPos = exoPlayer.currentPosition
        val isPlaying = exoPlayer.isPlaying
        
        val targetLink = videoOptions.links.find { it.quality == currentNonAdaptiveQuality } ?: videoOptions.links.firstOrNull()
        if (targetLink != null) {
            val defaultMediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(httpDataSourceFactory)
            if (!targetLink.hasAudio && videoOptions.audioUrl != null) {
                val videoSource = defaultMediaSourceFactory.createMediaSource(MediaItem.fromUri(targetLink.url))
                val audioSource = defaultMediaSourceFactory.createMediaSource(MediaItem.fromUri(videoOptions.audioUrl))
                exoPlayer.setMediaSource(MergingMediaSource(videoSource, audioSource))
            } else {
                exoPlayer.setMediaItem(MediaItem.fromUri(targetLink.url))
            }
        }
        
        exoPlayer.prepare()
        if (currentPos > 0) {
            exoPlayer.seekTo(currentPos)
        }
        exoPlayer.playWhenReady = if (currentPos > 0) isPlaying else true
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        if (videoOptions.links.size > 1) {
            IconButton(
                onClick = { showQualitySelector = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Quality", tint = Color.White)
            }
        }
    }

    if (showQualitySelector) {
        Dialog(onDismissRequest = { showQualitySelector = false }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Video Quality", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(videoOptions.links) { link ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    currentNonAdaptiveQuality = link.quality
                                    showQualitySelector = false
                                    onQualityChanged(link)
                                }.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = currentNonAdaptiveQuality == link.quality, onClick = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(link.quality)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showQualitySelector = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("Close")
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
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var videoOptions by remember { mutableStateOf<VideoOptions?>(null) }
    var isLoadingVideo by remember { mutableStateOf(false) }
    var currentVideoUrl by remember { mutableStateOf<String?>(null) }
    var showQualitySelector by remember { mutableStateOf(false) }

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

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.White, shape = RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.DarkGray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(clazz.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Text(clazz.type, fontSize = 14.sp, color = Color.Gray)
            }
        }

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
            if (isLoadingVideo) {
                VideoLoadingPlaceholder()
            } else if (videoOptions != null) {
                Box(modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(16.dp))) {
                    VideoPlayer(
                        videoOptions = videoOptions!!, 
                        modifier = Modifier.fillMaxSize().background(Color.Black),
                        onQualityChanged = { link ->
                            currentVideoUrl = link.url
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    val context = LocalContext.current
                    Button(onClick = {
                        val dlUrl = currentVideoUrl ?: videoOptions?.links?.firstOrNull()?.url
                        if (dlUrl != null) {
                            val request = android.app.DownloadManager.Request(android.net.Uri.parse(dlUrl))
                                .setTitle(clazz.title)
                                .setDescription("Downloading video...")
                                .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, "Class_${clazz.title}.mp4")
                            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                            downloadManager.enqueue(request)
                            Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Download, contentDescription = "Download")
                        Spacer(Modifier.width(8.dp))
                        Text("Download Video")
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp).background(Color.Black, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Failed to load video", color = Color.White)
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
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Download", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("অফলাইনে দেখতে ডাউনলোড করুন", color = Color(0xFF475569), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = "PDF", tint = Color(0xFF64748B), modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(pdf.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                Text("লেকচার স্লাইড দেখুন", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
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
