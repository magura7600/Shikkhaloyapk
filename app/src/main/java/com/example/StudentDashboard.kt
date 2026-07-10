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

@Composable
fun StudentDashboardContent(
    accentColor: Color,
    courses: List<CourseItem>,
    focusCourseId: String?,
    onClassClick: (CourseClass, CourseChapter, CourseSubject, CourseItem) -> Unit
) {
    var selectedDate by remember { mutableStateOf(java.time.LocalDate.now()) }
    var isCalendarExpanded by remember { mutableStateOf(false) }
    var viewingMonth by remember { mutableStateOf(java.time.YearMonth.from(selectedDate)) }

    // Automatically sync calendar to today's date whenever screen is active/re-entered
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val today = java.time.LocalDate.now()
        selectedDate = today
        viewingMonth = java.time.YearMonth.from(today)
    }
    val focusCourse = courses.find { it.id == focusCourseId }
    
    // Extract classes for the selected date
    val classDates = remember(focusCourse) {
        val dates = mutableSetOf<java.time.LocalDate>()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy")
        focusCourse?.subjects?.forEach { subject ->
            subject.chapters.forEach { chapter ->
                chapter.classes.forEach { courseClass ->
                    try {
                        dates.add(java.time.LocalDate.parse(courseClass.date.trim(), formatter))
                    } catch (e: Exception) {}
                }
            }
        }
        dates
    }
    
    val classesForDate = remember(focusCourse, selectedDate) {
        val classList = mutableListOf<Triple<CourseClass, CourseChapter, CourseSubject>>()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy")
        
        focusCourse?.subjects?.forEach { subject ->
            subject.chapters.forEach { chapter ->
                chapter.classes.forEach { courseClass ->
                    try {
                        val classDate = java.time.LocalDate.parse(courseClass.date.trim(), formatter)
                        if (classDate == selectedDate) {
                            classList.add(Triple(courseClass, chapter, subject))
                        }
                    } catch (e: Exception) {}
                }
            }
        }
        classList.sortedBy { it.first.time }
    }

    val isDark = ThemeManager.isDarkTheme()
    val bgGradient = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = if (isDark) listOf(Color(0xFF0F172A), Color(0xFF1E293B)) else listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9))
    )
    val textColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBgColor = if (isDark) Color(0xFF1E293B) else Color.White
    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
    ) {
        // Class Routine Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = Color(0xFF94A3B8).copy(alpha = 0.15f),
                        ambientColor = Color(0xFF94A3B8).copy(alpha = 0.15f)
                    ),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF818CF8).copy(alpha = 0.6f), Color(0xFFC084FC).copy(alpha = 0.6f))))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Class Routine",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Check your daily class schedule",
                        fontSize = 13.sp,
                        color = subTextColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { isCalendarExpanded = !isCalendarExpanded },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(Icons.Outlined.CalendarToday, contentDescription = "Calendar", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isCalendarExpanded) "Hide Calendar" else "Weekly Routine", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowForward, contentDescription = "Go", modifier = Modifier.size(16.dp))
                        }

                        if (selectedDate != java.time.LocalDate.now()) {
                            Button(
                                onClick = {
                                    val today = java.time.LocalDate.now()
                                    selectedDate = today
                                    viewingMonth = java.time.YearMonth.from(today)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF1E293B)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                            ) {
                                Icon(Icons.Outlined.Today, contentDescription = "Today", modifier = Modifier.size(16.dp), tint = Color(0xFF1E293B))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Today", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Calendar Area
        item {
            val monthFormatter = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    viewingMonth.format(monthFormatter),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                if (isCalendarExpanded) {
                    Row {
                        IconButton(
                            onClick = { viewingMonth = viewingMonth.minusMonths(1) },
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color.White, RoundedCornerShape(6.dp))
                                .padding(2.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Month", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { viewingMonth = viewingMonth.plusMonths(1) },
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color.White, RoundedCornerShape(6.dp))
                                .padding(2.dp)
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            
            if (isCalendarExpanded) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF60A5FA).copy(alpha = 0.5f), Color(0xFF3B82F6).copy(alpha = 0.5f))))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                                Text(
                                    text = day,
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        val firstDayOfMonth = viewingMonth.atDay(1)
                        val daysInMonth = viewingMonth.lengthOfMonth()
                        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // Sunday = 0
                        
                        val totalDays = firstDayOfWeek + daysInMonth
                        val weeks = Math.ceil(totalDays / 7.0).toInt()
                        
                        var dayCounter = 1
                        for (i in 0 until weeks) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                for (j in 0..6) {
                                    if (i == 0 && j < firstDayOfWeek || dayCounter > daysInMonth) {
                                        Box(modifier = Modifier.weight(1f))
                                    } else {
                                        val date = viewingMonth.atDay(dayCounter)
                                        val isSelected = date == selectedDate
                                        val isToday = date == java.time.LocalDate.now()
                                        val hasClass = classDates.contains(date)
                                        
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { 
                                                    selectedDate = date 
                                                }
                                                .then(if (isSelected) Modifier.background(accentColor.copy(alpha = 0.1f)) else Modifier)
                                                .then(if (isSelected) Modifier.padding(1.dp).background(Color.White, RoundedCornerShape(6.dp)) else Modifier)
                                                .then(if (isSelected) Modifier.padding(1.dp).background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(5.dp)) else Modifier)
                                                .then(if (!isSelected && isToday) Modifier.border(1.dp, accentColor, RoundedCornerShape(6.dp)) else Modifier),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = dayCounter.toString(),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) accentColor else if (isToday) accentColor else Color.DarkGray
                                            )
                                            if (hasClass) {
                                                Spacer(modifier = Modifier.height(1.dp))
                                                Row(horizontalArrangement = Arrangement.Center) {
                                                    Box(modifier = Modifier.size(3.dp).background(accentColor, CircleShape))
                                                    Spacer(modifier = Modifier.width(1.dp))
                                                    Box(modifier = Modifier.size(3.dp).background(accentColor, CircleShape))
                                                }
                                            }
                                        }
                                        dayCounter++
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(vertical = 8.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { 
                            selectedDate = selectedDate.minusDays(1)
                            viewingMonth = java.time.YearMonth.from(selectedDate)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = Color.Gray, modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                            .padding(2.dp))
                    }
                    
                    val prevDate = selectedDate.minusDays(1)
                    val isPrevToday = prevDate == java.time.LocalDate.now()
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                        selectedDate = prevDate 
                        viewingMonth = java.time.YearMonth.from(selectedDate)
                    }) {
                        Text(
                            text = if (isPrevToday) "Today" else prevDate.dayOfWeek.name.take(3).capitalize(), 
                            fontSize = 11.sp, 
                            color = if (isPrevToday) accentColor else Color.Gray, 
                            fontWeight = if (isPrevToday) FontWeight.Bold else FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(prevDate.dayOfMonth.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isPrevToday) accentColor else Color.DarkGray)
                    }

                    val isSelectedToday = selectedDate == java.time.LocalDate.now()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(accentColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isSelectedToday) "Today" else selectedDate.dayOfWeek.name.take(3).capitalize(), 
                            fontSize = 11.sp, 
                            color = Color.White, 
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(selectedDate.dayOfMonth.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    val nextDate = selectedDate.plusDays(1)
                    val isNextToday = nextDate == java.time.LocalDate.now()
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                        selectedDate = nextDate 
                        viewingMonth = java.time.YearMonth.from(selectedDate)
                    }) {
                        Text(
                            text = if (isNextToday) "Today" else nextDate.dayOfWeek.name.take(3).capitalize(), 
                            fontSize = 11.sp, 
                            color = if (isNextToday) accentColor else Color.Gray, 
                            fontWeight = if (isNextToday) FontWeight.Bold else FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(nextDate.dayOfMonth.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isNextToday) accentColor else Color.DarkGray)
                    }
                    
                    IconButton(
                        onClick = { 
                            selectedDate = selectedDate.plusDays(1) 
                            viewingMonth = java.time.YearMonth.from(selectedDate)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.Gray, modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                            .padding(2.dp))
                    }
                }
            }
        }

        // Today's Routine Title
        item {
            val dateLabel = if (selectedDate == java.time.LocalDate.now()) "Today's Routine" else "Routine for ${selectedDate.dayOfMonth} ${selectedDate.month.name.take(3)}"
            Text(
                dateLabel,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
        }

        // Classes List
        if (focusCourse == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(140.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = Color(0xFF94A3B8).copy(alpha = 0.15f),
                            ambientColor = Color(0xFF94A3B8).copy(alpha = 0.15f)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("কোনো কোর্স সিলেক্ট করা নেই।", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
        } else if (classesForDate.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = Color(0xFF94A3B8).copy(alpha = 0.15f),
                            ambientColor = Color(0xFF94A3B8).copy(alpha = 0.15f)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF34D399).copy(alpha = 0.5f), Color(0xFF10B981).copy(alpha = 0.5f))))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🐧", fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "You have no live classes or exams on this date.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        } else {
            items(classesForDate.size) { index ->
                val (courseClass, chapter, subject) = classesForDate[index]
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = Color(0xFF94A3B8).copy(alpha = 0.15f),
                            ambientColor = Color(0xFF94A3B8).copy(alpha = 0.15f)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFF472B6).copy(alpha = 0.5f), Color(0xFFEC4899).copy(alpha = 0.5f))))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(subject.title, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.size(3.dp).background(Color(0xFFCBD5E1), CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(courseClass.type, color = Color(0xFF475569), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFEF2F2), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("মিসড", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${chapter.title} - ${courseClass.title}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF64748B))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(courseClass.time, fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { onClassClick(courseClass, chapter, subject, focusCourse) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF1F5F9),
                                contentColor = accentColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 10.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Icon(Icons.Outlined.PlayCircleOutline, contentDescription = "Play", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("রেকর্ডেড ভিডিও দেখো", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        
        // Homework title
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Your Homework",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E293B)
            )
        }
        
        val classesWithHomework = classesForDate.filter { it.first.homeworkLink.isNotBlank() }
        
        if (classesWithHomework.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(24.dp),
                            spotColor = Color(0xFF94A3B8).copy(alpha = 0.2f),
                            ambientColor = Color(0xFF94A3B8).copy(alpha = 0.2f)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFFFFF7ED), RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFFFFEDD5), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Homework", modifier = Modifier.size(28.dp), tint = Color(0xFFF97316))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No homework today!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    }
                }
            }
        } else {
            items(classesWithHomework.size) { index ->
                val (courseClass, chapter, subject) = classesWithHomework[index]
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(20.dp),
                            spotColor = Color(0xFF94A3B8).copy(alpha = 0.2f),
                            ambientColor = Color(0xFF94A3B8).copy(alpha = 0.2f)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFF97316), Color(0xFFEA580C))))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFFFFF7ED), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Homework", tint = Color(0xFFEA580C))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${chapter.title} - ${courseClass.title}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(subject.title, fontSize = 13.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = { onClassClick(courseClass, chapter, subject, focusCourse!!) },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.1f)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Text("View", color = accentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentCoursesScreen(
    accentColor: Color,
    courses: List<CourseItem>,
    enrollments: List<Enrollment>,
    profile: UserProfile,
    onCourseClick: (CourseItem) -> Unit
) {
    val myEnrolledCourses = courses.filter { course ->
        enrollments.any { it.user_id == profile.user_id && it.course_id == course.id }
    }

    if (myEnrolledCourses.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFFDFD8C8), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("🐧", fontSize = 32.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "You have not purchased any courses yet.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
        }
    } else {
        CourseListScreen(
            accentColor = accentColor,
            courses = myEnrolledCourses,
            title = "আমার কেনা কোর্সসমূহ",
            subtitle = "আপনার কেনা সকল কোর্স এখানে দেখুন",
            onCourseClick = onCourseClick
        )
    }
}

