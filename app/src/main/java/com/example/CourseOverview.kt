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
            modifier = Modifier.fillMaxWidth().height(200.dp).layout { measurable, constraints -> val pad = 16.dp.roundToPx(); val p = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + pad * 2)); layout(p.width, p.height) { p.place(-pad, 0) } }.background(Color.LightGray, RoundedCornerShape(16.dp))
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
            val formatter = java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy")
            
            course.quarters.forEach { quarter ->
                var statusText = "অজানা"
                var statusColor = Color.Gray
                var statusIcon = Icons.Default.HelpOutline
                
                try {
                    val start = java.time.LocalDate.parse(quarter.startDate.trim(), formatter)
                    val end = java.time.LocalDate.parse(quarter.endDate.trim(), formatter)
                    
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


@Composable
fun LearningResourcesScreen(
    subject: CourseSubject,
    course: CourseItem,
    isTeacher: Boolean,
    accentColor: Color,
    onBack: () -> Unit,
    onUpdateSubject: (CourseSubject) -> Unit
) {
    val context = LocalContext.current
    val downloadStates by OfflineDownloadManager.downloadStates.collectAsState()
    var downloadsList by remember { mutableStateOf<List<DownloadRecord>>(emptyList()) }

    LaunchedEffect(Unit) {
        downloadsList = OfflineDownloadManager.getDownloadRecords(context)
    }
    LaunchedEffect(downloadStates) {
        downloadsList = OfflineDownloadManager.getDownloadRecords(context)
    }

    var activePdfToView by remember { mutableStateOf<File?>(null) }
    var activePdfTitle by remember { mutableStateOf("") }
    var activePdfUrl by remember { mutableStateOf("") }
    var downloadingPdfUrl by remember { mutableStateOf<String?>(null) }
    var recordToDelete by remember { mutableStateOf<DownloadRecord?>(null) }
    var isAddingResource by remember { mutableStateOf(false) }

    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("ডিলিট নিশ্চিত করুন", fontWeight = FontWeight.Bold) },
            text = { Text("আপনি কি নিশ্চিত যে আপনি এই পিডিএফটি ডিলিট করতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        OfflineDownloadManager.deleteDownload(context, recordToDelete!!)
                        recordToDelete = null
                        downloadsList = OfflineDownloadManager.getDownloadRecords(context)
                        Toast.makeText(context, "ডিলিট সম্পন্ন হয়েছে!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("ডিলিট করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    if (isAddingResource) {
        AddResourceDialog(
            onDismiss = { isAddingResource = false },
            onAdd = { title, url ->
                val currentResources = (subject.learningResources as? List<*>?)?.filterIsInstance<PdfLink>() ?: emptyList()
                val updatedResources = currentResources.toMutableList().apply { add(PdfLink(title, url)) }
                val updatedSubject = subject.copy(learningResources = updatedResources)
                onUpdateSubject(updatedSubject)
                isAddingResource = false
            }
        )
    }

    if (activePdfToView != null) {
        FullScreenPdfViewer(
            file = activePdfToView!!,
            title = activePdfTitle,
            url = activePdfUrl,
            onClose = { activePdfToView = null }
        )
    } else {
        Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "বিষয়ভিত্তিক লার্নিং রিসোর্স",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        containerColor = Color(0xFFF1F5F9), // Light gray background
        floatingActionButton = {
            if (isTeacher) {
                FloatingActionButton(
                    onClick = { isAddingResource = true },
                    containerColor = accentColor,
                    contentColor = Color.White,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Resource")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val resourcesList = (subject.learningResources as? List<*>?)?.filterIsInstance<PdfLink>() ?: emptyList()
            if (resourcesList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MenuBook, contentDescription = "Empty", modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("কোনো রিসোর্স যোগ করা হয়নি", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                resourcesList.forEachIndexed { index, pdf ->
                    val downloadedRecord = remember(downloadsList, pdf.url) {
                        downloadsList.find { it.url == pdf.url }
                    }
                    val isDownloaded = downloadedRecord != null
                    val downloadState = downloadStates[pdf.url]
                    
                    val isCloudOrWebUrl = remember { { url: String -> false } }
                    val openBrowserIntent = remember {
                        { targetUrl: String ->
                            try {
                                val fixedUrl = if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) "https://$targetUrl" else targetUrl
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(fixedUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "লিংক ওপেন করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "Resource",
                                    tint = accentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pdf.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isDownloaded) "অফলাইন (ডাউনলোডকৃত)" else "অনলাইন রিসোর্স",
                                    fontSize = 12.sp,
                                    color = if (isDownloaded) Color(0xFF10B981) else Color(0xFF64748B),
                                    fontWeight = if (isDownloaded) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Delete from Course (Teacher only)
                            if (isTeacher) {
                                IconButton(
                                    onClick = {
                                        val currentResources = (subject.learningResources as? List<*>?)?.filterIsInstance<PdfLink>() ?: emptyList()
                                        val updatedResources = currentResources.toMutableList().apply { removeAt(index) }
                                        val updatedSubject = subject.copy(learningResources = updatedResources)
                                        onUpdateSubject(updatedSubject)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                                }
                            }
                            
                            // View Button
                            Button(
                                onClick = {
                                    if (isCloudOrWebUrl(pdf.url)) {
                                        Toast.makeText(context, "ক্লাউড/ড্রাইভ লিংক ব্রাউজারে ওপেন করা হচ্ছে...", Toast.LENGTH_SHORT).show()
                                        openBrowserIntent(pdf.url)
                                    } else {
                                        if (isDownloaded) {
                                            activePdfToView = File(downloadedRecord!!.localPath)
                                            activePdfTitle = pdf.title
                                            activePdfUrl = pdf.url
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
                                                    activePdfUrl = pdf.url
                                                },
                                                onError = { errMsg ->
                                                    downloadingPdfUrl = null
                                                    Toast.makeText(context, "পিডিএফ লোড করতে সমস্যা হচ্ছে।", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor.copy(alpha = 0.15f),
                                    contentColor = accentColor
                                )
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

                            Spacer(modifier = Modifier.width(4.dp))

                            // Download Button / Status
                            if (isDownloaded) {
                                IconButton(
                                    onClick = { recordToDelete = downloadedRecord },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Downloaded",
                                        tint = Color(0xFF10B981)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        if (isCloudOrWebUrl(pdf.url)) {
                                            Toast.makeText(context, "ক্লাউড/ড্রাইভ ফাইলটি ব্রাউজার থেকে ডাউনলোড করুন", Toast.LENGTH_LONG).show()
                                            openBrowserIntent(pdf.url)
                                        } else {
                                            if (downloadState !is DownloadState.Downloading) {
                                                OfflineDownloadManager.downloadPermanently(
                                                    context = context,
                                                    url = pdf.url,
                                                    title = pdf.title,
                                                    fileType = "pdf",
                                                    courseName = course.title,
                                                    className = subject.title
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    if (downloadState is DownloadState.Downloading) {
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
                    }
                }
            }
        }
    }
    
    }
}

