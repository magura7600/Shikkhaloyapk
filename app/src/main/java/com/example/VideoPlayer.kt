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
fun VideoPlayer(
    videoOptions: VideoOptions, 
    modifier: Modifier = Modifier,
    initialPosition: Long = 0L,
    onPositionChanged: (Long) -> Unit = {},
    initialPlaying: Boolean = true,
    onPlayingChanged: (Boolean) -> Unit = {},
    onQualityChanged: (VideoLink) -> Unit = {},
    isFullscreen: Boolean = false,
    onFullscreenToggle: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val isAdmin = PrefUtils.getSecurePrefs(context).getString("role", "") == "admin"
    androidx.compose.runtime.DisposableEffect(isAdmin) {
        val window = activity?.window
        if (window != null && !isAdmin) {
            window.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            if (window != null && !isAdmin) {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
    
    var showQualitySelector by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    
    // Controls Visibility & Interaction States
    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(initialPlaying) }
    var currentPosition by remember { mutableStateOf(initialPosition) }
    var duration by remember { mutableStateOf(0L) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var interactionCount by remember { mutableStateOf(0) }
    var dragDismissJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    var isBuffering by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    
    // Pinch-to-zoom and pan states for video
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val transformState = androidx.compose.foundation.gestures.rememberTransformableState { zoomChange, offsetChange, _ ->
        if (!VideoPipState.isInPip && !isLocked) {
            scale = (scale * zoomChange).coerceIn(1f, 5f)
            if (scale > 1f) {
                offset += offsetChange
                // Roughly bound offset to prevent dragging completely out (max 1000px per scale factor for simplicity)
                val maxOffset = 1500f * (scale - 1f)
                offset = androidx.compose.ui.geometry.Offset(
                    x = offset.x.coerceIn(-maxOffset, maxOffset),
                    y = offset.y.coerceIn(-maxOffset, maxOffset)
                )
            } else {
                offset = androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    // Reset zoom when picture-in-picture changes or a new video starts
    LaunchedEffect(VideoPipState.isInPip, videoOptions) {
        if (VideoPipState.isInPip) {
            scale = 1f
            offset = androidx.compose.ui.geometry.Offset.Zero
        }
    }

    // Update parent states when playing state or playback position changes
    LaunchedEffect(currentPosition) {
        onPositionChanged(currentPosition)
    }
    LaunchedEffect(isPlaying) {
        onPlayingChanged(isPlaying)
    }

    // Manage VideoPipState lifecycle registration
    DisposableEffect(Unit) {
        VideoPipState.isVideoActive = true
        VideoPipState.onEnterPip = {
            if (activity != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try {
                    val aspectRatio = android.util.Rational(16, 9)
                    val params = android.app.PictureInPictureParams.Builder()
                        .setAspectRatio(aspectRatio)
                        .build()
                    activity.enterPictureInPictureMode(params)
                } catch (e: Exception) {
                    try {
                        activity.enterPictureInPictureMode()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        }
        onDispose {
            VideoPipState.isVideoActive = false
            VideoPipState.onEnterPip = null
        }
    }


    
    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            kotlinx.coroutines.delay(1800L)
            statusMessage = null
        }
    }
    
    // Slider drag tracking
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(0f) }

    // Non-adaptive quality tracking
    var currentNonAdaptiveQuality by remember(videoOptions) { 
        mutableStateOf(videoOptions.links.firstOrNull()?.quality ?: "Unknown") 
    }

    var resizeMode by remember { mutableStateOf(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    
    // Gestures states
    
    var showRewindFeedback by remember { mutableStateOf(false) }
    var showForwardFeedback by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    val httpDataSourceFactory = remember {
        androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            
            .setAllowCrossProtocolRedirects(true)
    }

    val dataSourceFactory = remember {
        androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)
    }
    
    val exoPlayer = remember {
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
        
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
            
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15000, // minBufferMs
                50000, // maxBufferMs
                1000,  // bufferForPlaybackMs (starts playback faster)
                1500   // bufferForPlaybackAfterRebufferMs
            )
            .build()
            
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setLoadControl(loadControl)
            .build()
    }

    // Wire up PiP remote action callbacks
    DisposableEffect(exoPlayer) {
        VideoPipState.isPlaying = exoPlayer.isPlaying
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                VideoPipState.isPlaying = isPlaying
            }
        }
        exoPlayer.addListener(listener)
        
        VideoPipState.onPlayPauseToggle = { play ->
            if (play) {
                exoPlayer.play()
            } else {
                exoPlayer.pause()
            }
        }
        VideoPipState.onRewind = {
            val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
            exoPlayer.seekTo(newPos)
        }
        VideoPipState.onForward = {
            val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(exoPlayer.duration)
            exoPlayer.seekTo(newPos)
        }
        
        onDispose {
            try {
                exoPlayer.removeListener(listener)
            } catch (e: Exception) {}
            VideoPipState.onPlayPauseToggle = null
            VideoPipState.onRewind = null
            VideoPipState.onForward = null
        }
    }

    val mainActivity = activity as? MainActivity
    LaunchedEffect(VideoPipState.isPlaying, VideoPipState.isInPip) {
        if (VideoPipState.isInPip && mainActivity != null) {
            mainActivity.updatePipParams(VideoPipState.isPlaying)
        }
    }

    // Set playback speed
    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    // Monitor playback progress
    LaunchedEffect(exoPlayer, isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0L)
            kotlinx.coroutines.delay(250L)
        }
    }

    // Keep screen on while playing
    DisposableEffect(isPlaying, activity) {
        if (isPlaying) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Observe player listener
    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                duration = exoPlayer.duration.coerceAtLeast(0L)
                isBuffering = state == androidx.media3.common.Player.STATE_BUFFERING
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                super.onPlayerError(error)
                statusMessage = "Error: ${error.message}"
                isBuffering = false
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    LaunchedEffect(videoOptions, currentNonAdaptiveQuality) {
        val currentPos = if (exoPlayer.currentPosition > 0L) exoPlayer.currentPosition else initialPosition
        val wasPlaying = if (exoPlayer.playbackState == androidx.media3.common.Player.STATE_IDLE) {
            initialPlaying
        } else {
            exoPlayer.playWhenReady
        }
        
        val targetLink = videoOptions.links.find { it.quality == currentNonAdaptiveQuality } ?: videoOptions.links.firstOrNull()
        if (targetLink != null) {
            val defaultMediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
            
            // Format video URI safely (check for web vs local file)
            val videoUri = if (targetLink.url.startsWith("http://") || targetLink.url.startsWith("https://")) {
                android.net.Uri.parse(targetLink.url.trim().replace(" ", "%20"))
            } else {
                android.net.Uri.fromFile(java.io.File(targetLink.url))
            }
            
            val audioUri = videoOptions.audioUrl?.let { url ->
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    android.net.Uri.parse(url.trim().replace(" ", "%20"))
                } else {
                    android.net.Uri.fromFile(java.io.File(url))
                }
            }

            if (!targetLink.hasAudio && audioUri != null) {
                val videoSource = defaultMediaSourceFactory.createMediaSource(MediaItem.fromUri(videoUri))
                val audioSource = defaultMediaSourceFactory.createMediaSource(MediaItem.fromUri(audioUri))
                exoPlayer.setMediaSource(MergingMediaSource(videoSource, audioSource))
            } else {
                exoPlayer.setMediaItem(MediaItem.fromUri(videoUri))
            }
        }
        
        exoPlayer.prepare()
        if (currentPos > 0L) {
            exoPlayer.seekTo(currentPos)
        }
        exoPlayer.playWhenReady = wasPlaying
    }

    // Observe host activity lifecycle to pause playback on background/pause
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                if (!VideoPipState.isInPip) {
                    exoPlayer.pause()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val originalBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        onDispose {
            exoPlayer.release()
            if (activity != null) {
                activity.requestedOrientation = originalOrientation
                // Restore original brightness
                val lp = activity.window.attributes
                lp.screenBrightness = originalBrightness
                activity.window.attributes = lp
            }
        }
    }

    // Hide controls automatically, but NOT when user is interacting
    LaunchedEffect(controlsVisible, isLocked, isDraggingSlider, showSpeedDialog, showQualitySelector, interactionCount) {
        if (controlsVisible && !isDraggingSlider && !showSpeedDialog && !showQualitySelector) {
            kotlinx.coroutines.delay(4500L)
            controlsVisible = false
        }
    }

    // Keep SystemBars behavior elegant on landscape full screen
    DisposableEffect(isFullscreen) {
        if (isFullscreen && activity != null) {
            val window = activity.window
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else if (activity != null) {
            val window = activity.window
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (activity != null) {
                val window = activity.window
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
    ) {
        // Actual Video Player
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false // Disable default controllers
                    this.resizeMode = resizeMode
                    keepScreenOn = true // Keep screen on during active playback
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )

        // Gestures Overlay Row
        if (!VideoPipState.isInPip) {
            Row(modifier = Modifier.fillMaxSize().transformable(state = transformState)) {
                // Left Half Box (Brightness & Rewind)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .playerGestureDetector(
                            isLocked = isLocked,
                            onTap = {
                                controlsVisible = !controlsVisible
                                if (controlsVisible) {
                                    interactionCount++
                                }
                            },
                            onDoubleTap = {
                                val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(newPos)
                                currentPosition = newPos
                                showRewindFeedback = true
                            },
                        )
                )

                // Right Half Box (Volume & Fast Forward)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .playerGestureDetector(
                            isLocked = isLocked,
                            onTap = {
                                controlsVisible = !controlsVisible
                                if (controlsVisible) {
                                    interactionCount++
                                }
                            },
                            onDoubleTap = {
                                val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(exoPlayer.duration)
                                exoPlayer.seekTo(newPos)
                                currentPosition = newPos
                                showForwardFeedback = true
                            }
                        )

                )
            }
        }

        // Buffering Indicator
        if (isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color(0xFF1E3A8A),
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "বাফারিং হচ্ছে...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (!VideoPipState.isInPip) {
            // On-screen Transient Status Message
            if (statusMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusMessage!!,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Custom Overlay Indicators
        if (showRewindFeedback) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 48.dp)
                    .size(80.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FastRewind, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Text("-10s", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                LaunchedEffect(showRewindFeedback) {
                    kotlinx.coroutines.delay(650L)
                    showRewindFeedback = false
                }
            }
        }

        if (showForwardFeedback) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 48.dp)
                    .size(80.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Text("+10s", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                LaunchedEffect(showForwardFeedback) {
                    kotlinx.coroutines.delay(650L)
                    showForwardFeedback = false
                }
            }
        }

        // SCREEN IS LOCKED STATE
        if (isLocked) {
            // Semi-transparent background when controls are visible
            if (controlsVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )

                // Only lock icon is shown when controls are visible
                IconButton(
                    onClick = { 
                        isLocked = false
                        controlsVisible = true
                        Toast.makeText(context, "নিয়ন্ত্রণ প্যানেল আনলক করা হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(48.dp)
                        .background(Color.Red.copy(alpha = 0.7f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Unlock screen",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Quick helper text
                Text(
                    text = "প্যানেল আনলক করতে ওপরের লাল লক বাটনে ক্লিক করুন",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        } else {
            // SCREEN IS UNLOCKED - ALL ADVANCED CONTROLS
            if (controlsVisible) {
                // Dim Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                )

                // 1. TOP CONTROL BAR (Quality, Speed, Lock)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button only when in landscape full screen
                    if (isFullscreen) {
                        IconButton(
                            onClick = {
                                interactionCount++
                                onFullscreenToggle(false)
                            },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Fullscreen", tint = Color.White)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Top Right Actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        

                        // 1. Speed selector pill button
                        Button(
                            onClick = { 
                                interactionCount++
                                showSpeedDialog = true 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("গতি: ${playbackSpeed}x", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // 2. Video quality selector pill button
                        if (videoOptions.links.size > 1) {
                            Button(
                                onClick = { 
                                    interactionCount++
                                    showQualitySelector = true 
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(currentNonAdaptiveQuality, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // 3. Lock Screen button
                        IconButton(
                            onClick = { 
                                interactionCount++
                                isLocked = true
                                Toast.makeText(context, "নিয়ন্ত্রণ প্যানেল লক করা হয়েছে", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = "Lock Controls", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // 2. CENTER PLAYBACK CONTROLS (Rewind, Play/Pause, Forward)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind 10s button
                    IconButton(
                        onClick = {
                            interactionCount++
                            val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Play / Pause button
                    IconButton(
                        onClick = {
                            interactionCount++
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFF1E3A8A), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Fast Forward 10s button
                    IconButton(
                        onClick = {
                            interactionCount++
                            val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(exoPlayer.duration)
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 3. BOTTOM SEEKBAR & TIME INFO
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentText = formatTime(if (isDraggingSlider) dragPosition.toLong() else currentPosition)
                        val totalText = formatTime(duration)
                        
                        Text(
                            text = "$currentText / $totalText",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // PiP Pop-up Toggle Button
                            if (ThemeManager.isPipEnabled) {
                                IconButton(
                                    onClick = {
                                        interactionCount++
                                        VideoPipState.onEnterPip?.invoke()
                                    },
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(36.dp)
                                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = "Pop-up Mode",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Fullscreen Landscape/Portrait Toggle
                            IconButton(
                                onClick = {
                                    interactionCount++
                                    onFullscreenToggle(!isFullscreen)
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Toggle Landscape Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Seek slider
                    val sliderValue = if (isDraggingSlider) dragPosition else currentPosition.toFloat()
                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            interactionCount++
                            isDraggingSlider = true
                            dragPosition = it
                        },
                        onValueChangeFinished = {
                            interactionCount++
                            exoPlayer.seekTo(dragPosition.toLong())
                            currentPosition = dragPosition.toLong()
                            isDraggingSlider = false
                        },
                        valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF1E3A8A),
                            activeTrackColor = Color(0xFF1E3A8A),
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

    // Playback Speed Selector Dialog
    if (showSpeedDialog) {
        Dialog(onDismissRequest = { showSpeedDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "প্লেব্যাক গতি নির্ধারণ করুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF111827)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Display current speed
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp))
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format("%.2fx", playbackSpeed),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E3A8A)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Granular Slider
                    Text("সূক্ষ্ম সমন্বয় করুন (০.২৫x - ৩.০০x)", fontSize = 12.sp, color = Color(0xFF64748B))
                    Slider(
                        value = playbackSpeed,
                        onValueChange = { 
                            playbackSpeed = (Math.round(it * 100) / 100f).coerceIn(0.25f, 3.0f)
                        },
                        valueRange = 0.25f..3.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF1E3A8A),
                            activeTrackColor = Color(0xFF1E3A8A),
                            inactiveTrackColor = Color(0xFFE5E7EB)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Fine Adjustment Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { playbackSpeed = (playbackSpeed - 0.05f).coerceIn(0.25f, 3.0f) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("- ০.০৫x", fontSize = 13.sp, color = Color(0xFF1E3A8A))
                        }
                        
                        OutlinedButton(
                            onClick = { playbackSpeed = (playbackSpeed + 0.05f).coerceIn(0.25f, 3.0f) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("+ ০.০৫x", fontSize = 13.sp, color = Color(0xFF1E3A8A))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Quick Presets Header
                    Text("দ্রুত গতি নির্বাচন করুন", fontSize = 12.sp, color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val presets = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { speed ->
                            val isSelected = playbackSpeed == speed
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFFF3F4F6),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { 
                                        playbackSpeed = speed
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (speed == 1.0f) "স্বাভাবিক" else "${speed}x",
                                    color = if (isSelected) Color.White else Color(0xFF111827),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    TextButton(
                        onClick = { showSpeedDialog = false }, 
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("ঠিক আছে", color = Color(0xFF1E3A8A), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Video Quality Selector Dialog
    if (showQualitySelector) {
        Dialog(onDismissRequest = { showQualitySelector = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "ভিডিওর কোয়ালিটি নির্বাচন করুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF111827)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(videoOptions.links) { link ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentNonAdaptiveQuality = link.quality
                                        statusMessage = "কোয়ালিটি: ${link.quality}"
                                        showQualitySelector = false
                                        onQualityChanged(link)
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentNonAdaptiveQuality == link.quality, 
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1E3A8A))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = link.quality,
                                    fontSize = 15.sp,
                                    color = Color(0xFF111827),
                                    fontWeight = if (currentNonAdaptiveQuality == link.quality) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showQualitySelector = false }, 
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("বন্ধ করুন", color = Color(0xFF1E3A8A), fontWeight = FontWeight.Bold)
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
                brush = Brush.verticalGradient(colors = listOf(Color(0xFF111827), MaterialTheme.colorScheme.background))
            )
            .border(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant pulsing & rotating loading graphic
            val primaryColor = Color(0xFF1E3A8A)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale),
                contentAlignment = Alignment.Center
            ) {
                // Background outer soft glow ring
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.15f),
                        radius = size.minDimension / 1.8f
                    )
                }
                
                // Rotating color arc
                Canvas(modifier = Modifier.size(48.dp).graphicsLayer(rotationZ = angle)) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(primaryColor, Color(0xFF1E3A8A), primaryColor)
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
                text = "লোডিং হচ্ছে...",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ভিডিও লোড হচ্ছে, অনুগ্রহ করে অপেক্ষা করুন",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}


fun Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}


fun Modifier.playerGestureDetector(
    isLocked: Boolean,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit
): Modifier = this.pointerInput(isLocked) {
    if (isLocked) {
        detectTapGestures(
            onTap = { onTap() }
        )
    } else {
        detectTapGestures(
            onTap = { onTap() },
            onDoubleTap = { onDoubleTap() }
        )
    }
}

