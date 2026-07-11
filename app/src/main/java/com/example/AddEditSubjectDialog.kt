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
    
    // Custom Slider States for visual color selection
    var sliderHue by remember { mutableStateOf(350f) }
    var sliderSaturation by remember { mutableStateOf(0.6f) }
    var sliderValue by remember { mutableStateOf(1.0f) }

    LaunchedEffect(newColorHex) {
        try {
            val cleanHex = if (newColorHex.startsWith("#")) newColorHex else "#$newColorHex"
            if (cleanHex.length == 7 || cleanHex.length == 9) {
                val colorInt = android.graphics.Color.parseColor(cleanHex)
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(colorInt, hsv)
                sliderHue = hsv[0]
                sliderSaturation = hsv[1]
                sliderValue = hsv[2]
            }
        } catch (e: Exception) {
            // Ignore temporary parsing errors while typing
        }
    }
    
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
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0F172A)) {
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
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
                        onValueChange = { input ->
                            // Automatically add # if missing and restrict to HEX characters
                            val cleaned = if (input.isNotEmpty() && !input.startsWith("#")) "#$input" else input
                            newColorHex = cleaned
                        },
                        label = { Text("কাস্টম কালার কোড (HEX)") },
                        placeholder = { Text("#FF6B6B") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    // Color Preview
                    val previewColor = try {
                        val cleanHex = if (newColorHex.startsWith("#")) newColorHex else "#$newColorHex"
                        Color(android.graphics.Color.parseColor(cleanHex))
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

                // Interactive sliders to visually pick any custom color
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF334155).copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("কালার স্লাইডার ব্যবহার করে রং পছন্দ করুন:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                        
                        // Hue slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("রং পরিবর্তন (Hue)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("${sliderHue.toInt()}°", fontSize = 12.sp, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.Red, Color.Yellow, Color.Green, Color.Cyan, 
                                                Color.Blue, Color.Magenta, Color.Red
                                            )
                                        )
                                    )
                            )
                            Slider(
                                value = sliderHue,
                                onValueChange = { valHue ->
                                    sliderHue = valHue
                                    val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(sliderHue, sliderSaturation, sliderValue))
                                    newColorHex = String.format("#%06X", 0xFFFFFF and colorInt)
                                },
                                valueRange = 0f..360f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.Transparent,
                                    inactiveTrackColor = Color.Transparent
                                ),
                                modifier = Modifier.height(28.dp)
                            )
                        }

                        // Saturation slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("কালারের ঘনত্ব (Saturation)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("${(sliderSaturation * 100).toInt()}%", fontSize = 12.sp, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val saturationGradient = remember(sliderHue) {
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.White, 
                                        Color(android.graphics.Color.HSVToColor(floatArrayOf(sliderHue, 1f, 1f)))
                                    )
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(saturationGradient)
                            )
                            Slider(
                                value = sliderSaturation,
                                onValueChange = { valSat ->
                                    sliderSaturation = valSat
                                    val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(sliderHue, sliderSaturation, sliderValue))
                                    newColorHex = String.format("#%06X", 0xFFFFFF and colorInt)
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.Transparent,
                                    inactiveTrackColor = Color.Transparent
                                ),
                                modifier = Modifier.height(28.dp)
                            )
                        }

                        // Value/Brightness/Lightness slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("উজ্জ্বলতা (Brightness)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("${(sliderValue * 100).toInt()}%", fontSize = 12.sp, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val valueGradient = remember(sliderHue, sliderSaturation) {
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Black, 
                                        Color(android.graphics.Color.HSVToColor(floatArrayOf(sliderHue, sliderSaturation, 1f)))
                                    )
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(valueGradient)
                            )
                            Slider(
                                value = sliderValue,
                                onValueChange = { valVal ->
                                    sliderValue = valVal
                                    val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(sliderHue, sliderSaturation, sliderValue))
                                    newColorHex = String.format("#%06X", 0xFFFFFF and colorInt)
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.Transparent,
                                    inactiveTrackColor = Color.Transparent
                                ),
                                modifier = Modifier.height(28.dp)
                            )
                        }
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

