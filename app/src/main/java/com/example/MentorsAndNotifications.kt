package com.example

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

@Composable
fun MentorsListDialog(
    mentors: List<Mentor>,
    onAddMentor: (Mentor) -> Unit,
    onEditMentor: (Mentor) -> Unit,
    onDeleteMentor: (Mentor) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color
) {
    var showAddMentor by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F172A)
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
                    var imageUrl by remember { mutableStateOf("") }
                    var isUploading by remember { mutableStateOf(false) }
                    
                    val photoPickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: android.net.Uri? ->
                        uri?.let { selectedUri ->
                            isUploading = true
                            coroutineScope.launch {
                                try {
                                    val inputStream = context.contentResolver.openInputStream(selectedUri)
                                    val bytes = inputStream?.readBytes()
                                    inputStream?.close()
                                    
                                    if (bytes != null) {
                                        val uploadedUrl = ImgBBClient.uploadImage(bytes)
                                        if (uploadedUrl != null) {
                                            imageUrl = uploadedUrl
                                        } else {
                                            Toast.makeText(context, "ছবি আপলোড ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "ছবি আপলোড করতে সমস্যা হয়েছে: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isUploading = false
                                }
                            }
                        }
                    }
                    
                    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("নাম * (যেমন: মোঃ সাব্বির হোসাইন)") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = education, onValueChange = { education = it }, label = { Text("কোথায় পড়াশোনা করেছেন? (যেমন: ঢাকা বিশ্ববিদ্যালয়)") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = subjects, onValueChange = { subjects = it }, label = { Text("কী কী বিষয় পড়ান? * (যেমন: বাংলা, ইংরেজি, গণিত)") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = experience, onValueChange = { experience = it }, label = { Text("শিক্ষাকতার অভিজ্ঞতা (যেমন: ৫ বছরের বেশি সময়...)") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("মেন্টরের প্রোফাইল ছবি", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF3F4F6))
                                .clickable(enabled = !isUploading) { photoPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(color = accentColor, modifier = Modifier.size(24.dp))
                            } else if (imageUrl.isNotBlank()) {
                                coil.compose.AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Mentor Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Image", tint = Color.Gray)
                                    Text("ছবি আপলোড", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAddMentor = false }) { Text("বাতিল", color = Color.Gray) }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                if (name.isNotBlank() && subjects.isNotBlank()) {
                                    onAddMentor(Mentor(name = name, education = education, subjects = subjects, experience = experience, image_url = imageUrl))
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
                            var isEditing by remember { mutableStateOf(false) }
                            var editName by remember { mutableStateOf(mentor.name) }
                            var editEducation by remember { mutableStateOf(mentor.education) }
                            var editSubjects by remember { mutableStateOf(mentor.subjects) }
                            var editExperience by remember { mutableStateOf(mentor.experience) }
                            var editImageUrl by remember { mutableStateOf(mentor.image_url) }
                            var isEditingUploading by remember { mutableStateOf(false) }
                            
                            val editPhotoPickerLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.GetContent()
                            ) { uri: android.net.Uri? ->
                                uri?.let { selectedUri ->
                                    isEditingUploading = true
                                    coroutineScope.launch {
                                        try {
                                            val inputStream = context.contentResolver.openInputStream(selectedUri)
                                            val bytes = inputStream?.readBytes()
                                            inputStream?.close()
                                            
                                            if (bytes != null) {
                                                val uploadedUrl = ImgBBClient.uploadImage(bytes)
                                                if (uploadedUrl != null) {
                                                    editImageUrl = uploadedUrl
                                                } else {
                                                    Toast.makeText(context, "ছবি আপলোড ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "ছবি আপলোড করতে সমস্যা হয়েছে: ${e.message}", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isEditingUploading = false
                                        }
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6))
                            ) {
                                if (isEditing) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("নাম") }, modifier = Modifier.fillMaxWidth())
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(value = editEducation, onValueChange = { editEducation = it }, label = { Text("কোথায় পড়াশোনা করেছেন?") }, modifier = Modifier.fillMaxWidth())
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(value = editSubjects, onValueChange = { editSubjects = it }, label = { Text("কী কী বিষয় পড়ান?") }, modifier = Modifier.fillMaxWidth())
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(value = editExperience, onValueChange = { editExperience = it }, label = { Text("শিক্ষাকতার অভিজ্ঞতা") }, modifier = Modifier.fillMaxWidth())
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.LightGray)
                                                    .clickable(enabled = !isEditingUploading) { editPhotoPickerLauncher.launch("image/*") },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isEditingUploading) {
                                                    CircularProgressIndicator(color = accentColor, modifier = Modifier.size(16.dp))
                                                } else if (editImageUrl.isNotBlank()) {
                                                    coil.compose.AsyncImage(
                                                        model = editImageUrl,
                                                        contentDescription = "Mentor Image",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                    )
                                                } else {
                                                    Icon(Icons.Default.Add, contentDescription = "Add Image", tint = Color.White)
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text("ছবি পরিবর্তন করতে ক্লিক করুন", fontSize = 12.sp, color = Color.Gray)
                                        }
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            TextButton(onClick = { isEditing = false }) { Text("বাতিল", color = Color.Gray) }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(onClick = {
                                                if (editName.isNotBlank() && editSubjects.isNotBlank()) {
                                                    onEditMentor(mentor.copy(name = editName, education = editEducation, subjects = editSubjects, experience = editExperience, image_url = editImageUrl))
                                                    isEditing = false
                                                }
                                            }, colors = ButtonDefaults.buttonColors(containerColor = accentColor)) {
                                                Text("সংরক্ষণ করুন")
                                            }
                                        }
                                    }
                                } else {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (mentor.image_url.isNotBlank()) {
                                            coil.compose.AsyncImage(
                                                model = mentor.image_url,
                                                contentDescription = null,
                                                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.LightGray),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        } else {
                                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp).background(Color.LightGray, CircleShape).padding(8.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(mentor.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text(mentor.subjects, color = Color.Gray, fontSize = 14.sp)
                                        }
                                        
                                        var showMenu by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(onClick = { showMenu = true }) {
                                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                                            }
                                            DropdownMenu(
                                                expanded = showMenu,
                                                onDismissRequest = { showMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("এডিট করুন") },
                                                    onClick = { 
                                                        showMenu = false
                                                        isEditing = true
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("ডিলিট করুন", color = Color.Red) },
                                                    onClick = { 
                                                        showMenu = false
                                                        onDeleteMentor(mentor)
                                                    }
                                                )
                                            }
                                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllNotificationsDialog(
    activeNotice: AppNotice?,
    courses: List<CourseItem>,
    enrollments: List<Enrollment>,
    profile: UserProfile,
    accentColor: Color,
    onDismiss: () -> Unit,
    onClassClick: (CourseClass, CourseChapter, CourseSubject, CourseItem) -> Unit
) {
    val context = LocalContext.current
    val today = java.time.LocalDate.now()
    val formatter = java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy")
    
    // Extract today's classes across enrolled courses
    val todayClasses = remember(courses, enrollments, profile.user_id) {
        val list = mutableListOf<Triple<CourseItem, CourseClass, CourseSubject>>()
        courses.forEach { course ->
            val isEnrolled = enrollments.any { it.user_id == profile.user_id && it.course_id == course.id }
            if (isEnrolled || enrollments.isEmpty() || profile.role == "teacher") {
                course.subjects.forEach { subject ->
                    subject.chapters.forEach { chapter ->
                        chapter.classes.forEach { courseClass ->
                            try {
                                val classDate = java.time.LocalDate.parse(courseClass.date.trim(), formatter)
                                if (classDate == today) {
                                    list.add(Triple(course, courseClass, subject))
                                }
                            } catch (e: Exception) {}
                        }
                    }
                }
            }
        }
        list
    }

    // Extract upcoming classes as helpful suggestions
    val upcomingClasses = remember(courses, enrollments, profile.user_id) {
        val list = mutableListOf<Triple<CourseItem, CourseClass, CourseSubject>>()
        courses.forEach { course ->
            val isEnrolled = enrollments.any { it.user_id == profile.user_id && it.course_id == course.id }
            if (isEnrolled || enrollments.isEmpty() || profile.role == "teacher") {
                course.subjects.forEach { subject ->
                    subject.chapters.forEach { chapter ->
                        chapter.classes.forEach { courseClass ->
                            try {
                                val classDate = java.time.LocalDate.parse(courseClass.date.trim(), formatter)
                                if (classDate.isAfter(today)) {
                                    list.add(Triple(course, courseClass, subject))
                                }
                            } catch (e: Exception) {}
                        }
                    }
                }
            }
        }
        list.sortedBy { 
            try {
                java.time.LocalDate.parse(it.second.date.trim(), formatter).toEpochDay()
            } catch(e: Exception) { 
                Long.MAX_VALUE 
            }
        }.take(3)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(accentColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notification Center",
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "নোটিফিকেশন সেন্টার",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF0F172A)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. IMPORTANT NOTICES
                Text(
                    text = "গুরুত্বপূর্ণ নোটিশ ও ঘোষণা",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                if (activeNotice != null) {
                    val noticeTheme = remember(activeNotice.type) {
                        when (activeNotice.type.lowercase()) {
                            "warning" -> Triple(Color(0xFFEF4444), Color(0xFFFEF2F2), Color(0xFF991B1B))
                            "offer" -> Triple(Color(0xFF8B5CF6), Color(0xFFF5F3FF), Color(0xFF5B21B6))
                            "exam" -> Triple(Color(0xFFF4B400), Color(0xFFFFFBEB), Color(0xFF92400E))
                            else -> Triple(accentColor, Color(0xFFF8FAFC), Color(0xFF1E293B))
                        }
                    }
                    val (primaryColor, bgColor, onBgColor) = noticeTheme
                    val uriHandler = LocalUriHandler.current

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when(activeNotice.type.lowercase()) {
                                        "warning" -> Icons.Default.Warning
                                        "offer" -> Icons.Default.Star
                                        "exam" -> Icons.Default.MenuBook
                                        else -> Icons.Default.Campaign
                                    },
                                    contentDescription = activeNotice.type,
                                    tint = primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activeNotice.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = onBgColor
                                )
                            }
                            
                            if (!activeNotice.image_url.isNullOrBlank()) {
                                coil.compose.AsyncImage(
                                    model = activeNotice.image_url,
                                    contentDescription = "Notice Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 140.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }

                            Text(
                                text = activeNotice.content,
                                fontSize = 13.sp,
                                color = onBgColor.copy(alpha = 0.9f)
                            )

                            if (!activeNotice.action_url.isNullOrBlank()) {
                                Button(
                                    onClick = {
                                        try {
                                            uriHandler.openUri(activeNotice.action_url.trim())
                                        } catch (e: Exception) {}
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text("বিস্তারিত দেখুন 🔗", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "No Alerts",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "এই মুহূর্তে কোনো জরুরি নোটিশ নেই।",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // 2. DAILY SCHEDULED NOTIFICATIONS
                Text(
                    text = "আজকের ক্লাস নোটিফিকেশন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                if (todayClasses.isNotEmpty()) {
                    todayClasses.forEach { (course, courseClass, subject) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Class",
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "আজকের লাইভ ক্লাস!",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = accentColor
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = courseClass.time,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E40AF),
                                        modifier = Modifier
                                            .background(Color(0xFFDBEAFE), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${course.title} • ${subject.title}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "ক্লাসে যোগদান করতে নিচের বাটনে চাপ দিন।",
                                    fontSize = 12.sp,
                                    color = Color(0xFF475569)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        onDismiss()
                                        val chapter = course.subjects.flatMap { it.chapters }.find { ch -> ch.classes.any { it.id == courseClass.id } }
                                        if (chapter != null) {
                                            onClassClick(courseClass, chapter, subject, course)
                                        } else {
                                            Toast.makeText(context, "ক্লাসটি বিস্তারিত দেখতে কোর্স ট্যাবে যান।", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Text("ক্লাসে যোগ দাও", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = "No classes",
                                    tint = Color(0xFF0F172A).copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "আজকে আপনার কোনো লাইভ ক্লাস নেই।",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                            }
                            
                            if (upcomingClasses.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "আসন্ন ক্লাসসমূহ:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                upcomingClasses.forEach { (course, courseClass, subject) ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.size(6.dp).background(accentColor, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = "${course.title} • ${subject.title}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF334155)
                                                )
                                                Text(
                                                    text = "তারিখ: ${courseClass.date} • সময়: ${courseClass.time}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        }
                                    }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("বন্ধ করো", color = accentColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}
