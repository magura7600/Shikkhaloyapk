package com.example

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

data class ClassWithCourseInfo(
    val course: CourseItem,
    val subject: CourseSubject,
    val chapter: CourseChapter,
    val courseClass: CourseClass
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClassLinkScreen(
    courses: List<CourseItem>,
    accentColor: Color,
    onCourseUpdate: (CourseItem) -> Unit,
    onBack: () -> Unit
) {
    val mContext = LocalContext.current
    val calendar = Calendar.getInstance()
    var selectedDate by remember { 
        mutableStateOf(String.format(java.util.Locale.US, "%02d/%02d/%04d", calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR)))
    }

    // Flatten courses to find classes matching the selected date
    val classesForDate = remember(courses, selectedDate) {
        val result = mutableListOf<ClassWithCourseInfo>()
        for (course in courses) {
            for (subject in course.subjects) {
                for (chapter in subject.chapters) {
                    for (clazz in chapter.classes) {
                        if (clazz.date == selectedDate) {
                            result.add(ClassWithCourseInfo(course, subject, chapter, clazz))
                        }
                    }
                }
            }
        }
        result
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ক্লাস লিংক যোগ করুন", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF1F5F9))
                .padding(16.dp)
        ) {
            // Date Picker Card
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    DatePickerDialog(
                        mContext,
                        { _, year, month, day ->
                            selectedDate = String.format(java.util.Locale.US, "%02d/%02d/%04d", day, month + 1, year)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select Date", tint = accentColor)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("তারিখ নির্বাচন করুন", fontSize = 12.sp, color = Color.Gray)
                        Text(selectedDate, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (classesForDate.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("এই তারিখে কোনো ক্লাস পাওয়া যায়নি", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(classesForDate) { item ->
                        ClassLinkEditCard(
                            classInfo = item,
                            accentColor = accentColor,
                            onSave = { updatedClass ->
                                val updatedCourse = item.course.copy(
                                    subjects = item.course.subjects.map { subj ->
                                        if (subj.id == item.subject.id) {
                                            subj.copy(
                                                chapters = subj.chapters.map { chap ->
                                                    if (chap.id == item.chapter.id) {
                                                        chap.copy(
                                                            classes = chap.classes.map { c ->
                                                                if (c.id == updatedClass.id) updatedClass else c
                                                            }
                                                        )
                                                    } else chap
                                                }
                                            )
                                        } else subj
                                    }
                                )
                                onCourseUpdate(updatedCourse)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClassLinkEditCard(
    classInfo: ClassWithCourseInfo,
    accentColor: Color,
    onSave: (CourseClass) -> Unit
) {
    var liveLink by remember { mutableStateOf(classInfo.courseClass.liveLink) }
    var recordedLink by remember { mutableStateOf(classInfo.courseClass.recordedLink) }
    var homeworkLink by remember { mutableStateOf(classInfo.courseClass.homeworkLink) }
    
    val initialSlideLink = classInfo.courseClass.pdfLinks.firstOrNull()?.url ?: ""
    var slideLink by remember { mutableStateOf(initialSlideLink) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(classInfo.courseClass.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
            Text("${classInfo.course.title} • ${classInfo.subject.title}", fontSize = 12.sp, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = liveLink,
                onValueChange = { liveLink = it },
                label = { Text("লাইভ ক্লাস লিংক") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = recordedLink,
                onValueChange = { recordedLink = it },
                label = { Text("রেকর্ড ভিডিও লিংক") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = homeworkLink,
                onValueChange = { homeworkLink = it },
                label = { Text("হোমওয়ার্ক লিংক") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = slideLink,
                onValueChange = { slideLink = it },
                label = { Text("ক্লাস স্লাইড (PDF) লিংক") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val updatedPdfLinks = if (slideLink.isNotBlank()) {
                        val pdf = classInfo.courseClass.pdfLinks.firstOrNull()?.copy(url = slideLink, title = "Class Slide") ?: PdfLink(title = "Class Slide", url = slideLink)
                        val rest = if (classInfo.courseClass.pdfLinks.size > 1) classInfo.courseClass.pdfLinks.drop(1) else emptyList()
                        listOf(pdf) + rest
                    } else {
                        if (classInfo.courseClass.pdfLinks.size > 1) classInfo.courseClass.pdfLinks.drop(1) else emptyList()
                    }

                    val updatedClass = classInfo.courseClass.copy(
                        liveLink = liveLink,
                        recordedLink = recordedLink,
                        homeworkLink = homeworkLink,
                        pdfLinks = updatedPdfLinks
                    )
                    onSave(updatedClass)
                },
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("সেভ করুন")
            }
        }
    }
}
