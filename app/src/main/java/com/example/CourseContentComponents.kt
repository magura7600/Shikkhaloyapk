package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RoutineBannerCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1E3A8A), Color(0xFF1E3A8A))
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
                            contentDescription = stringResource(R.string.routine),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.view_routine),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = stringResource(R.string.today_class_exam_schedule),
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
}

@Composable
fun QuarterTabs(
    selectedQuarterName: String,
    onQuarterSelected: (String) -> Unit,
    course: CourseItem,
    quartersList: List<String>,
    isQuarterLocked: (String) -> Boolean,
    subj: CourseSubject? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        quartersList.forEach { qName ->
            val isSelected = selectedQuarterName == qName
            
            // Calculate Quarter Progress % dynamically
            val quarterChapters = if (subj != null) {
                subj.chapters.filter { (it.quarter.ifBlank { "Quarter 1" }) == qName }
            } else {
                course.subjects.flatMap { s ->
                    s.chapters.filter { (it.quarter.ifBlank { "Quarter 1" }) == qName }
                }
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

            val cardBgColor = if (isSelected) Color(0xFF1E3A8A) else Color.White
            val cardTextColor = if (isSelected) Color.White else Color(0xFF1E3A8A)
            val cardSubTextColor = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF1E3A8A)
            val cardBorderColor = if (isSelected) Color(0xFF1E3A8A) else Color(0xFFF3F4F6)

            Card(
                onClick = { onQuarterSelected(qName) },
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
                        var statusTextColor = if (isSelected) Color.White else Color(0xFF10B981)

                        if (quarterObj != null && quarterObj.startDate.isNotBlank() && quarterObj.endDate.isNotBlank()) {
                            try {
                                val formatter = java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy")
                                val start = java.time.LocalDate.parse(quarterObj.startDate.trim(), formatter)
                                val end = java.time.LocalDate.parse(quarterObj.endDate.trim(), formatter)
                                val today = java.time.LocalDate.now()
                                
                                val hasClasses = course.subjects.flatMap { s -> s.chapters }.filter { (it.quarter.ifBlank { "Quarter 1" }) == qName }.flatMap { it.classes }.isNotEmpty()
                                if (today.isBefore(start)) {
                                    if (hasClasses) {
                                        quarterStatus = "পড়ানো হচ্ছে"
                                        statusBgColor = if (isSelected) Color(0xFFF59E0B).copy(alpha = 0.3f) else Color(0xFF1E3A8A)
                                        statusTextColor = if (isSelected) Color.White else Color(0xFFEF4444)
                                    } else {
                                        quarterStatus = "পড়ানো হবে"
                                        statusBgColor = if (isSelected) Color(0xFF1E3A8A).copy(alpha = 0.3f) else Color(0xFFEFF6FF)
                                        statusTextColor = if (isSelected) Color.White else Color(0xFF1E3A8A)
                                    }
                                } else if (today.isAfter(end)) {
                                    quarterStatus = "পড়ানো শেষ"
                                    statusBgColor = if (isSelected) Color(0xFFD1FAE5).copy(alpha = 0.3f) else Color(0xFFD1FAE5)
                                    statusTextColor = if (isSelected) Color.White else Color(0xFF10B981)
                                } else {
                                    quarterStatus = "পড়ানো হচ্ছে"
                                    statusBgColor = if (isSelected) Color(0xFFF59E0B).copy(alpha = 0.3f) else Color(0xFF1E3A8A)
                                    statusTextColor = if (isSelected) Color.White else Color(0xFFEF4444)
                                }
                            } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) }
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

@Composable
fun QuarterLockedCard(
    accentColor: Color,
    onPurchaseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.locked_quarter_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.locked_quarter_desc), fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onPurchaseClick,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text(stringResource(R.string.unlock_button))
            }
        }
    }
}

@Composable
fun SubjectCard(
    subject: CourseSubject,
    course: CourseItem,
    isTeacher: Boolean,
    onSelectedSubjectChange: (CourseSubject) -> Unit,
    onEditSubjectClick: () -> Unit,
    onDeleteSubjectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = try { Color(android.graphics.Color.parseColor(subject.colorHex)) } catch (e: Exception) { Color(0xFFEF4444) }
    
    // Calculate dynamic Videos & PDFs inside this subject
    val totalVideos = subject.chapters.sumOf { chapter ->
        chapter.classes.count { clazz -> clazz.recordedLink.isNotBlank() || clazz.liveLink.isNotBlank() }
    }
    val totalPdfs = subject.chapters.sumOf { chapter ->
        chapter.classes.sumOf { clazz -> clazz.pdfLinks.size }
    }

    Card(
        modifier = modifier
            .height(180.dp)
            .clickable { onSelectedSubjectChange(subject) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
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
                                onEditSubjectClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("ডিলিট করুন", color = Color.Red) },
                            onClick = {
                                isMenuExpanded = false
                                onDeleteSubjectClick()
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
                        contentScale = ContentScale.Crop
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
                    color = Color(0xFF1E3A8A),
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
                            .background(Color(0xFFF3F4F6), shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Videos",
                            tint = Color(0xFF1E3A8A),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${convertToBengaliDigits(totalVideos.toString())} ভিডিও",
                            color = Color(0xFF1E3A8A),
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
                            .background(Color(0xFFF3F4F6), shape = RoundedCornerShape(6.dp))
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
                            color = Color(0xFF10B981),
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

@Composable
fun LearningResourcesBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A))
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
                    text = stringResource(R.string.view_subject_learning_resources),
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
}

@Composable
fun ChapterCard(
    chapter: CourseChapter,
    subj: CourseSubject,
    isTeacher: Boolean,
    onSelectedChapterChange: (CourseChapter) -> Unit,
    onEditChapterClick: () -> Unit,
    onDeleteChapterClick: () -> Unit,
    onStatusChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isStatusMenuExpanded by remember { mutableStateOf(false) }
    
    // Display Badge according to Teaching Status
    val displayStatus = if (chapter.classes.isEmpty()) "পড়ানো হবে" else chapter.teachingStatus
    val (statusBgColor, statusTextColor) = when (displayStatus) {
        "পড়ানো শেষ" -> Pair(Color(0xFFD1FAE5), Color(0xFF10B981))
        "পড়ানো হচ্ছে" -> Pair(Color(0xFFEFF6FF), Color(0xFF1E3A8A))
        else -> Pair(Color(0xFFF3F4F6), Color(0xFF1E3A8A))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFF1E3A8A).copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
        border = BorderStroke(1.dp, Color(0xFFF3F4F6))
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
                    
                    DropdownMenu(
                        expanded = isStatusMenuExpanded,
                        onDismissRequest = { isStatusMenuExpanded = false }
                    ) {
                        listOf("পড়ানো হবে", "পড়ানো হচ্ছে", "পড়ানো শেষ").forEach { targetStatus ->
                            DropdownMenuItem(
                                text = { Text(targetStatus, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    isStatusMenuExpanded = false
                                    onStatusChange(targetStatus)
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
                    // Class Count Badge
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ListAlt,
                            contentDescription = null,
                            tint = Color(0xFF1E3A8A),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${chapter.classes.size} ক্লাস",
                            color = Color(0xFF1E3A8A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    
                    if (isTeacher) {
                        IconButton(
                            onClick = onEditChapterClick,
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
                            onClick = onDeleteChapterClick,
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
                    
                    Card(
                        onClick = { onSelectedChapterChange(chapter) },
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Open Chapter",
                                tint = Color(0xFF1E3A8A),
                                modifier = Modifier.size(16.dp).rotate(180f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = chapter.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E3A8A),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectedChapterChange(chapter) }
            )
        }
    }
}

@Composable
fun ActionCardButton(
    text: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
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
            Icon(Icons.Default.Add, contentDescription = text, tint = accentColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}
