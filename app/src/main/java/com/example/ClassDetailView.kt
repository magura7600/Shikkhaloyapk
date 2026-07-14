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
import androidx.compose.material3.MaterialTheme
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
fun ClassDetailView(
    clazz: CourseClass,
    subject: CourseSubject,
    mentors: List<Mentor>,
    accentColor: Color,
    courseName: String = "",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
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
    var activePdfUrl by remember { mutableStateOf("") }
    var downloadingPdfUrl by remember { mutableStateOf<String?>(null) }
    var recordToDelete by remember { mutableStateOf<DownloadRecord?>(null) }

    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("ডাউনলোড ডিলিট করুন", fontWeight = FontWeight.Bold, color = Color.Red) },
            text = { Text("আপনি কি নিশ্চিতভাবে এই ফাইলটি ('${recordToDelete?.title}') ডিলিট করতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        recordToDelete?.let { OfflineDownloadManager.deleteDownload(context, it) }
                        recordToDelete = null
                        downloadsList = OfflineDownloadManager.getDownloadRecords(context)
                        Toast.makeText(context, "ডিলিট সম্পন্ন হয়েছে!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("ডিলিট করুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        downloadsList = OfflineDownloadManager.getDownloadRecords(context)
    }

    LaunchedEffect(downloadStates) {
        downloadsList = OfflineDownloadManager.getDownloadRecords(context)
    }

    val targetTime = remember(clazz.date, clazz.time) { getClassCalendar(clazz.date, clazz.time)?.timeInMillis ?: 0L }
    var timeRemainingMillis by remember(targetTime) { mutableStateOf(0L) }
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
    val isLiveActive = timeRemainingMillis <= 0L

    var youtubeVideoId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(clazz.recordedLink, clazz.liveLink, isLiveActive) {
        val linkToExtract = if (clazz.recordedLink.isNotBlank()) {
            clazz.recordedLink
        } else if (clazz.liveLink.isNotBlank() && isLiveActive) {
            clazz.liveLink
        } else {
            ""
        }

        if (linkToExtract.isNotBlank()) {
            isLoadingVideo = true
            youtubeVideoId = YouTubeVideoExtractor.extractVideoId(linkToExtract)
            if (youtubeVideoId != null) {
                videoOptions = null
                currentVideoUrl = null
            } else {
                val options = FacebookVideoExtractor.extractVideoOptions(context, linkToExtract)
                videoOptions = options
                if (options != null) {
                    currentVideoUrl = options.links.firstOrNull()?.url ?: options.adaptiveUrl
                } else {
                    currentVideoUrl = null
                }
            }
            isLoadingVideo = false
        } else {
            youtubeVideoId = null
            videoOptions = null
            currentVideoUrl = null
        }
    }

    var isManualFullscreen by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isDeviceLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Automatically sync fullscreen state with physical device orientation changes
    // Automatically sync fullscreen state with physical device orientation changes
    LaunchedEffect(isDeviceLandscape) {
        if (activePdfToView == null) {
            isManualFullscreen = isDeviceLandscape
            if (activity != null) {
                if (!isDeviceLandscape) {
                    // Reset manual override when physically in portrait to let sensor work freely
                    activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
        }
    }

    // Intercept back-press while in fullscreen to exit fullscreen mode first
    BackHandler(enabled = isManualFullscreen || isDeviceLandscape) {
        isManualFullscreen = false
        if (activity != null) {
            activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // Use movableContentOf to keep VideoPlayer stable across orientation/PiP changes
    val movableVideoPlayer = remember(videoOptions, currentVideoUrl, youtubeVideoId) {
        movableContentOf { modifier: Modifier ->
            if (youtubeVideoId != null) {
                YouTubePlayerWidget(
                    videoId = youtubeVideoId!!,
                    modifier = modifier,
                    isFullscreen = isManualFullscreen,
                    onFullscreenToggle = { fullscreen ->
                        isManualFullscreen = fullscreen
                        if (activity != null) {
                            activity.requestedOrientation = if (fullscreen) {
                                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            } else {
                                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            }
                        }
                    }
                )
            } else {
                videoOptions?.let {
                    VideoPlayer(
                        videoOptions = it,
                        modifier = modifier,
                        initialPosition = savedVideoPosition,
                        onPositionChanged = { savedVideoPosition = it },
                        initialPlaying = savedVideoPlaying,
                        onPlayingChanged = { savedVideoPlaying = it },
                        onQualityChanged = { link ->
                            currentVideoUrl = link.url
                        },
                        isFullscreen = isManualFullscreen,
                        onFullscreenToggle = { fullscreen ->
                            isManualFullscreen = fullscreen
                            if (activity != null) {
                                activity.requestedOrientation = if (fullscreen) {
                                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                } else {
                                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (VideoPipState.isInPip) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            movableVideoPlayer(Modifier.fillMaxSize())
        }
        return
    }

    if (activePdfToView != null) {
        FullScreenPdfViewer(
            file = activePdfToView ?: File(""),
            title = activePdfTitle,
            url = activePdfUrl,
            onClose = { activePdfToView = null }
        )
        return
    }

    val isVideoPlayingActive = clazz.recordedLink.isNotBlank() || (clazz.liveLink.isNotBlank() && isLiveActive && (videoOptions != null || youtubeVideoId != null))

    // Apply immersive mode for full screen video
    androidx.compose.runtime.LaunchedEffect(isManualFullscreen, isDeviceLandscape) {
        val window = activity?.window
        if (window != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            if (isManualFullscreen || isDeviceLandscape) {
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    if ((isManualFullscreen || isDeviceLandscape) && isVideoPlayingActive && activePdfToView == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = androidx.compose.ui.Alignment.Center) {
            if (isLoadingVideo) {
                VideoLoadingPlaceholder(modifier = Modifier.fillMaxSize())
            } else if (videoOptions != null || youtubeVideoId != null) {
                movableVideoPlayer(Modifier.fillMaxSize())
            } else {
                androidx.compose.material3.Text("Failed to load video", color = Color.White)
            }
        }
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp, top = 12.dp)
    ) {
        // Top Bar with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(vertical = 12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            androidx.compose.material3.Card(
                modifier = Modifier
                    .size(42.dp)
                    .clickable { onBack() },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            androidx.compose.material3.Text(
                text = clazz.title,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 17.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                ),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(42.dp))
        }

        // Video Player Placeholder or Actual Player or Live Countdown Timer
        if (isVideoPlayingActive) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                    if (isLoadingVideo) {
                        VideoLoadingPlaceholder(modifier = Modifier.fillMaxSize())
                    } else if (videoOptions != null || youtubeVideoId != null) {
                        movableVideoPlayer(Modifier.fillMaxSize().background(Color.Black))
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                            Text("Failed to load video", color = Color.White)
                        }
                    }
                }
                
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Spacer(modifier = Modifier.height(16.dp))
                
                if (clazz.recordedLink.isNotBlank()) {
                    // Unified Premium Download Button directly under the Video Player!
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
                                val record = downloadsList.find { it.url == dlUrl }
                                if (record != null) {
                                    recordToDelete = record
                                } else {
                                    Toast.makeText(context, "ভিডিওটি ইতিমধ্যে ডাউনলোড করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDownloaded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                                Text("ডাউনলোড সম্পন্ন (ডিলিট করতে ট্যাপ করুন)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
            }
            } // Close the padded column
        } else if (clazz.liveLink.isNotBlank() || clazz.recordedLink.isBlank()) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            // Live/Countdown/Waiting UI
            val days = timeRemainingMillis / (1000 * 60 * 60 * 24)
            val hours = (timeRemainingMillis / (1000 * 60 * 60)) % 24
            val minutes = (timeRemainingMillis / (1000 * 60)) % 60
            val seconds = (timeRemainingMillis / 1000) % 60
            
            val isWaitingForLive = isLiveActive && clazz.liveLink.isBlank()
            
            DisposableEffect(isWaitingForLive) {
                var mediaPlayer: android.media.MediaPlayer? = null
                if (isWaitingForLive) {
                    try {
                        val waitingMusicRes = context.resources.getIdentifier("waiting_music", "raw", context.packageName)
                        if (waitingMusicRes != 0) {
                            mediaPlayer = android.media.MediaPlayer.create(context, waitingMusicRes)
                            mediaPlayer?.isLooping = true
                            mediaPlayer?.start()
                        }
                    } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) }
                }
                onDispose {
                    try {
                        if (mediaPlayer?.isPlaying == true) {
                            mediaPlayer?.stop()
                        }
                        mediaPlayer?.release()
                    } catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                            .background(if (isLiveActive) MaterialTheme.colorScheme.error else accentColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                            .border(1.dp, if (isLiveActive) MaterialTheme.colorScheme.error else accentColor, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isLiveActive) Color.White else accentColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isLiveActive && !isWaitingForLive) "লাইভ ক্লাস চলছে" else if (isWaitingForLive) "ক্লাস শুরু হচ্ছে..." else "লাইভ ক্লাস শুরু হতে বাকি",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    if (isWaitingForLive) {
                        Spacer(modifier = Modifier.height(24.dp))
                        // Pulsing / glowing animation for "Getting ready"
                        val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.05f,
                            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                            )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(32.dp).graphicsLayer(scaleX = scale, scaleY = scale)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "ক্লাস শুরু হচ্ছে!",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "খাতা কলম নিয়ে রেডি হন",
                                    color = Color.LightGray,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    if (!isLiveActive) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CountdownUnit(value = days, label = "দিন", modifier = Modifier.weight(1f))
                            CountdownDivider()
                            CountdownUnit(value = hours, label = "ঘণ্টা", modifier = Modifier.weight(1f))
                            CountdownDivider()
                            CountdownUnit(value = minutes, label = "মিনিট", modifier = Modifier.weight(1f))
                            CountdownDivider()
                            CountdownUnit(value = seconds, label = "সেকেন্ড", modifier = Modifier.weight(1f))
                        }
                    }
                    
                    if (!isWaitingForLive) {
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
                                    tint = if (isLiveActive) MaterialTheme.colorScheme.error else accentColor,
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
                    }
                    
                    if (clazz.liveLink.isNotBlank()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                if (isLiveActive) {
                                    Toast.makeText(context, "ভিডিও লোড হচ্ছে, অনুগ্রহ করে অপেক্ষা করুন...", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "নির্দিষ্ট সময়ে লাইভ ক্লাস সচল হবে", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLiveActive) MaterialTheme.colorScheme.error else Color.Gray.copy(alpha = 0.3f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isLiveActive) "লাইভ ক্লাস লোড হচ্ছে..." else "নির্দিষ্ট সময়ে জয়েন বাটন সচল হবে",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
            } // Close the padded column of the Live block
        } else {
            // Placeholder when no video
            Box(
                modifier = Modifier.fillMaxWidth().height(250.dp).background(
                        brush = Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.onBackground, MaterialTheme.colorScheme.surfaceVariant))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(24.dp).background(Color.White, CircleShape))
                }
                
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.error, RoundedCornerShape(4.dp))
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
                        .background(MaterialTheme.colorScheme.error, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(subject.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(clazz.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, contentDescription = "Date", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    val dateStr = if (clazz.date.isNotBlank()) clazz.date else "5 May 2026"
                    val timeStr = if (clazz.time.isNotBlank()) clazz.time else "11:01 am"
                    Text("$dateStr • $timeStr", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                val mentorObj = remember(mentors, clazz.mentorId) { mentors.find { it.id == clazz.mentorId } }
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (mentorObj != null && mentorObj.image_url.isNotBlank()) {
                            coil.compose.AsyncImage(
                                model = mentorObj.image_url,
                                contentDescription = "Mentor Image",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = "Mentor", tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val mentorName = mentorObj?.name ?: "অজানা শিক্ষক"
                    Text(mentorName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                var isContentExpanded by remember { mutableStateOf(false) }
                val rotationAngle by animateFloatAsState(
                    targetValue = if (isContentExpanded) 180f else 0f,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "arrowRotation"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .clickable { isContentExpanded = !isContentExpanded }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ক্লাসের বিষয়বস্তু", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.rotate(rotationAngle)
                    )
                }
                AnimatedVisibility(
                    visible = isContentExpanded && clazz.description.isNotBlank(),
                    enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300))
                ) {
                    Text(
                        text = clazz.description,
                        color = Color.DarkGray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp),
                        lineHeight = 20.sp
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
                    Text("ক্লাস রিসোর্সেস ও হোমওয়ার্ক", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
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
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = if (isDownloaded) "অফলাইন (ডাউনলোডকৃত)" else "অনলাইন ডকুমেন্ট",
                                    fontSize = 11.sp,
                                    color = if (isDownloaded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                    fontWeight = if (isDownloaded) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Helper lambdas for cloud storage links
                            val isCloudOrWebUrl = remember {
                                { url: String ->
                                    val lower = url.lowercase().trim()
                                    val clean = if (lower.contains("?")) lower.substringBefore("?") else lower
                                    clean.contains("drive.google.com") ||
                                    clean.contains("docs.google.com") ||
                                    clean.contains("dropbox.com") ||
                                    clean.contains("mega.nz") ||
                                    clean.contains("mediafire.com") ||
                                    clean.contains("onedrive") ||
                                    clean.contains("google.com/open") ||
                                    (!clean.endsWith(".pdf") && clean.isNotEmpty())
                                }
                            }
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

                            // View Button
                            Button(
                                onClick = {
                                    if (isCloudOrWebUrl(pdf.url)) {
                                        Toast.makeText(context, "ক্লাউড/ড্রাইভ লিংক ব্রাউজারে ওপেন করা হচ্ছে...", Toast.LENGTH_SHORT).show()
                                        openBrowserIntent(pdf.url)
                                    } else {
                                        if (isDownloaded) {
                                            downloadedRecord?.localPath?.let { activePdfToView = File(it) }
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Downloaded",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            recordToDelete = downloadedRecord
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete PDF",
                                            tint = Color.Red,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
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
                                                    courseName = courseName,
                                                    className = clazz.title
                                                )
                                            }
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
                                            tint = MaterialTheme.colorScheme.primary
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
fun CountdownUnit(value: Long, label: String, modifier: Modifier = Modifier) {
    val valueStr = convertToBengaliDigits(String.format("%02d", value))
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 2.dp)
    ) {
        Text(
            text = valueStr,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 24.sp,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun CountdownDivider() {
    Text(
        text = ":",
        color = Color.White.copy(alpha = 0.5f),
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.offset(y = (-2).dp)
    )
}

fun saveImageToGallery(context: android.content.Context, imageUrl: String, filename: String, onResult: (Boolean, String?) -> Unit) {
    val client = okhttp3.OkHttpClient()
    val request = okhttp3.Request.Builder().url(imageUrl).build()
    
    Thread {
        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                handler.post { onResult(false, "ডাউনলোড ব্যর্থ হয়েছে। সার্ভার কোড: ${response.code}") }
                return@Thread
            }
            val bytes = response.body?.bytes()
            if (bytes == null) {
                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                handler.post { onResult(false, "কোনো ডেটা পাওয়া যায়নি।") }
                return@Thread
            }
            
            val contentResolver = context.contentResolver
            val imageCollection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.provider.MediaStore.Images.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/Shikkhaloy")
                    put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            
            val uri = contentResolver.insert(imageCollection, contentValues)
            if (uri == null) {
                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                handler.post { onResult(false, "গ্যালারিতে ফাইল তৈরি করা যায়নি।") }
                return@Thread
            }
            
            val outputStream = contentResolver.openOutputStream(uri)
            if (outputStream == null) {
                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                handler.post { onResult(false, "ফাইল রাইট করা যায়নি।") }
                return@Thread
            }
            
            outputStream.use { out ->
                out.write(bytes)
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)
            }
            
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            handler.post { onResult(true, "গ্যালারিতে সফলভাবে সংরক্ষণ করা হয়েছে!") }
        } catch (e: Exception) {
            e.printStackTrace()
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            handler.post { onResult(false, "ত্রুটি: ${e.localizedMessage ?: "অজানা ত্রুটি"}") }
        }
    }.start()
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
    var isDownloading by remember { mutableStateOf(false) }

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Custom App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.onBackground)
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ক্লাস রুটিন",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentRoutineUrl.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    if (!isDownloading) {
                                        isDownloading = true
                                        Toast.makeText(context, "ডাউনলোড শুরু হচ্ছে...", Toast.LENGTH_SHORT).show()
                                        saveImageToGallery(context, currentRoutineUrl, "Routine_${System.currentTimeMillis()}.jpg") { success, msg ->
                                            isDownloading = false
                                            Toast.makeText(context, msg ?: (if (success) "ডাউনলোড সম্পন্ন হয়েছে!" else "ডাউনলোড ব্যর্থ হয়েছে"), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = !isDownloading
                            ) {
                                if (isDownloading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Download",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        if (isTeacher) {
                            IconButton(
                                onClick = { routinePickerLauncher.launch("image/*") },
                                enabled = !isUploading
                            ) {
                                if (isUploading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Routine",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Main Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentRoutineUrl.isNotBlank()) {
                        var scale by remember { mutableStateOf(1f) }
                        var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                        val state = rememberTransformableState { zoomChange, offsetChange, _ ->
                            scale = (scale * zoomChange).coerceIn(1f, 5f)
                            offset += offsetChange
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clipToBounds()
                                .background(Color.Black)
                                .transformable(state = state),
                            contentAlignment = Alignment.Center
                        ) {
                            coil.compose.AsyncImage(
                                model = currentRoutineUrl,
                                contentDescription = "Routine",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    ),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "No Routine",
                                tint = Color.Gray.copy(alpha = 0.5f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "কোনো রুটিন ছবি আপলোড করা হয়নি।",
                                color = Color.Gray,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (isTeacher) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { routinePickerLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Upload, contentDescription = "Upload")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("রুটিন ছবি আপলোড করুন")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

