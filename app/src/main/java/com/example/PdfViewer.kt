package com.example

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.content.pm.ActivityInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.asCoroutineDispatcher
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import android.view.WindowManager

// Single-threaded dispatcher to ensure thread safety with Android's PdfRenderer
private val pdfDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
private val pdfMutex = Mutex()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FullScreenPdfViewer(
    file: File,
    title: String,
    url: String = "",
    onClose: () -> Unit
) {
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = remember(context) { context as? androidx.activity.ComponentActivity }
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
    var isLandscape by remember { mutableStateOf(activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) }
    var showJumpToPageDialog by remember { mutableStateOf(false) }
    var jumpPageInput by remember { mutableStateOf("") }
    var controlsVisible by remember { mutableStateOf(true) }

    val bitmapCache = remember {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        object : LruCache<Int, Bitmap>(cacheSize) {
            override fun sizeOf(key: Int, value: Bitmap): Int {
                return value.byteCount / 1024
            }
        }
    }

    LaunchedEffect(file) {
        val oldRenderer = pdfRenderer
        val oldFd = fileDescriptor
        withContext(pdfDispatcher) {
            pdfMutex.withLock {
                try {
                    if (oldRenderer != null || oldFd != null) {
                        try {
                            oldRenderer?.close()
                            oldFd?.close()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    if (file.exists() && file.length() > 0) {
                        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                        val renderer = PdfRenderer(fd)
                        withContext(Dispatchers.Main) {
                            fileDescriptor = fd
                            pdfRenderer = renderer
                            pageCount = renderer.pageCount
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            error = true
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        error = true
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val r = pdfRenderer
            val fd = fileDescriptor
            if (r != null || fd != null) {
                kotlinx.coroutines.CoroutineScope(pdfDispatcher).launch {
                    pdfMutex.withLock {
                        try {
                            r?.close()
                            fd?.close()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            bitmapCache.evictAll()
        }
    }

    BackHandler(enabled = true) {
        onClose()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (error) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("পিডিএফ ফাইলটি লোড করা যায়নি।", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        } else if (pageCount > 0 && pdfRenderer != null) {
            val pagerState = rememberPagerState(pageCount = { pageCount })
            val coroutineScope = rememberCoroutineScope()
            var isPagerScrollEnabled by remember { mutableStateOf(true) }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = isPagerScrollEnabled,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 16.dp
            ) { index ->
                ZoomablePdfPage(
                    pdfRenderer = pdfRenderer!!,
                    pageIndex = index,
                    bitmapCache = bitmapCache,
                    onZoomChanged = { isZoomed ->
                        isPagerScrollEnabled = !isZoomed
                    },
                    onTap = { controlsVisible = !controlsVisible }
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = controlsVisible,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0x99000000), shape = RoundedCornerShape(20.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .background(Color(0x99000000), shape = RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 140.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF0F766E), shape = RoundedCornerShape(10.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                        .clickable { showJumpToPageDialog = true }
                                ) {
                                    Text(
                                        text = "${pagerState.currentPage + 1} / $pageCount পৃষ্ঠা",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        IconButton(
                            onClick = {
                                if (activity != null) {
                                    isLandscape = !isLandscape
                                    activity.requestedOrientation = if (isLandscape) {
                                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0x99000000), shape = RoundedCornerShape(20.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScreenRotation,
                                contentDescription = "Rotate Screen",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = controlsVisible,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xCC000000), shape = RoundedCornerShape(24.dp))
                            .border(1.dp, Color(0x33FFFFFF), shape = RoundedCornerShape(24.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (pagerState.currentPage > 0) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage > 0,
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (pagerState.currentPage > 0) Color(0x33FFFFFF) else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "আগের পৃষ্ঠা",
                                tint = if (pagerState.currentPage > 0) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = "${pagerState.currentPage + 1} / $pageCount পৃষ্ঠা",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        IconButton(
                            onClick = {
                                if (pagerState.currentPage < pageCount - 1) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage < pageCount - 1,
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (pagerState.currentPage < pageCount - 1) Color(0x33FFFFFF) else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "পরের পৃষ্ঠা",
                                tint = if (pagerState.currentPage < pageCount - 1) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (showJumpToPageDialog) {
                AlertDialog(
                    onDismissRequest = { showJumpToPageDialog = false },
                    title = { Text("পৃষ্ঠা নম্বর লিখুন") },
                    text = {
                        OutlinedTextField(
                            value = jumpPageInput,
                            onValueChange = { jumpPageInput = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text("১ থেকে $pageCount এর মধ্যে") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val page = jumpPageInput.toIntOrNull()
                            if (page != null && page in 1..pageCount) {
                                coroutineScope.launch {
                                    pagerState.scrollToPage(page - 1)
                                }
                            }
                            showJumpToPageDialog = false
                        }) {
                            Text("যান")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showJumpToPageDialog = false }) {
                            Text("বাতিল")
                        }
                    }
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF0F766E))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PdfViewerDialog(
    file: File,
    title: String,
    url: String = "",
    onClose: () -> Unit
) {
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = remember(context) { context as? androidx.activity.ComponentActivity }
    val isAdmin = PrefUtils.getSecurePrefs(context).getString("role", "") == "admin"
    val view = LocalView.current
    androidx.compose.runtime.DisposableEffect(isAdmin, view) {
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        if (dialogWindow != null && !isAdmin) {
            dialogWindow.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        onDispose {
            dialogWindow?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
    var isLandscape by remember { mutableStateOf(activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) }
    var showJumpToPageDialog by remember { mutableStateOf(false) }
    var jumpPageInput by remember { mutableStateOf("") }
    var controlsVisible by remember { mutableStateOf(true) }


    // Initialize an elegant memory-safe Bitmap cache
    val bitmapCache = remember {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8 // Use 1/8th of available memory for cache
        object : LruCache<Int, Bitmap>(cacheSize) {
            override fun sizeOf(key: Int, value: Bitmap): Int {
                return value.byteCount / 1024
            }
        }
    }

    LaunchedEffect(file) {
        val oldRenderer = pdfRenderer
        val oldFd = fileDescriptor
        withContext(pdfDispatcher) {
            pdfMutex.withLock {
                try {
                    if (oldRenderer != null || oldFd != null) {
                        try {
                            oldRenderer?.close()
                            oldFd?.close()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    if (file.exists() && file.length() > 0) {
                        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                        val renderer = PdfRenderer(fd)
                        withContext(Dispatchers.Main) {
                            fileDescriptor = fd
                            pdfRenderer = renderer
                            pageCount = renderer.pageCount
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            error = true
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        error = true
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val r = pdfRenderer
            val fd = fileDescriptor
            if (r != null || fd != null) {
                kotlinx.coroutines.CoroutineScope(pdfDispatcher).launch {
                    pdfMutex.withLock {
                        try {
                            r?.close()
                            fd?.close()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            bitmapCache.evictAll()
        }
    }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        // Force full screen window
        val dialogWindow = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        LaunchedEffect(dialogWindow, isAdmin) {
            dialogWindow?.let { window ->
                window.setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                window.setBackgroundDrawableResource(android.R.color.transparent)
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                
                if (!isAdmin) {
                    window.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background) // Immersive dark theme background (Sleek Dark Slate)
        ) {
            if (error) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("পিডিএফ ফাইলটি লোড করা যায়নি।", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            } else if (pageCount > 0 && pdfRenderer != null) {
                val pagerState = rememberPagerState(pageCount = { pageCount })
                val coroutineScope = rememberCoroutineScope()
                var isPagerScrollEnabled by remember { mutableStateOf(true) }

                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = isPagerScrollEnabled,
                    modifier = Modifier.fillMaxSize(),
                    pageSpacing = 16.dp
                ) { index ->
                    ZoomablePdfPage(
                        pdfRenderer = pdfRenderer!!,
                        pageIndex = index,
                        bitmapCache = bitmapCache,
                        onZoomChanged = { isZoomed ->
                            isPagerScrollEnabled = !isZoomed
                        },
                        onTap = { controlsVisible = !controlsVisible }
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = controlsVisible,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
// Beautiful translucent floating overlay header at the very top (starts exactly from y = 0)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button inside translucent circular card for perfect readability on any slide background
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0x99000000), shape = RoundedCornerShape(20.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Title & Page indicator in a beautiful translucent pill
                        Box(
                            modifier = Modifier
                                .background(Color(0x99000000), shape = RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 140.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF0F766E), shape = RoundedCornerShape(10.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                        .clickable { showJumpToPageDialog = true }
                                ) {
                                    Text(
                                        text = "${pagerState.currentPage + 1} / $pageCount পৃষ্ঠা",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        // Screen Rotation Button
                        IconButton(
                            onClick = {
                                if (activity != null) {
                                    isLandscape = !isLandscape
                                    activity.requestedOrientation = if (isLandscape) {
                                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0x99000000), shape = RoundedCornerShape(20.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScreenRotation,
                                contentDescription = "Rotate Screen",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                }

androidx.compose.animation.AnimatedVisibility(
                    visible = controlsVisible,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
// Translucent floating bottom navigation control bar for reliable page switching (Next/Prev)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xCC000000), shape = RoundedCornerShape(24.dp))
                            .border(1.dp, Color(0x33FFFFFF), shape = RoundedCornerShape(24.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Previous Page Button
                        IconButton(
                            onClick = {
                                if (pagerState.currentPage > 0) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage > 0,
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (pagerState.currentPage > 0) Color(0x33FFFFFF) else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "আগের পৃষ্ঠা",
                                tint = if (pagerState.currentPage > 0) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Page indicator text in bottom bar
                        Text(
                            text = "${pagerState.currentPage + 1} / $pageCount পৃষ্ঠা",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Next Page Button
                        IconButton(
                            onClick = {
                                if (pagerState.currentPage < pageCount - 1) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage < pageCount - 1,
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (pagerState.currentPage < pageCount - 1) Color(0x33FFFFFF) else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "পরের পৃষ্ঠা",
                                tint = if (pagerState.currentPage < pageCount - 1) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                }
                if (showJumpToPageDialog) {
                    AlertDialog(
                        onDismissRequest = { showJumpToPageDialog = false },
                        title = { Text("পৃষ্ঠা নম্বর লিখুন") },
                        text = {
                            OutlinedTextField(
                                value = jumpPageInput,
                                onValueChange = { jumpPageInput = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = { Text("১ থেকে $pageCount এর মধ্যে") },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val page = jumpPageInput.toIntOrNull()
                                if (page != null && page in 1..pageCount) {
                                    coroutineScope.launch {
                                        pagerState.scrollToPage(page - 1)
                                    }
                                }
                                showJumpToPageDialog = false
                            }) {
                                Text("যান")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showJumpToPageDialog = false }) {
                                Text("বাতিল")
                            }
                        }
                    )
                }

            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF0F766E))
                }
            }
        }
    }
}

@Composable
fun ZoomablePdfPage(
    pdfRenderer: PdfRenderer, 
    pageIndex: Int, 
    bitmapCache: LruCache<Int, Bitmap>,
    onZoomChanged: (Boolean) -> Unit,
    onTap: () -> Unit = {}
) {
    var scale by remember(pageIndex) { mutableStateOf(1f) }
    var offsetX by remember(pageIndex) { mutableStateOf(0f) }
    var offsetY by remember(pageIndex) { mutableStateOf(0f) }

    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(bitmapCache.get(pageIndex)) }
    var pageError by remember(pageIndex) { mutableStateOf(false) }

    LaunchedEffect(pageIndex, pdfRenderer) {
        if (bitmap != null) return@LaunchedEffect

        withContext(pdfDispatcher) {
            pdfMutex.withLock {
                try {
                    var bmp: Bitmap? = null
                    val page = pdfRenderer.openPage(pageIndex)
                    try {
                        val maxDim = 2048
                        val rawWidth = (page.width * 2f).toInt()
                        val rawHeight = (page.height * 2f).toInt()
                        val scaleFactor = if (rawWidth > maxDim || rawHeight > maxDim) {
                            val maxOfDims = if (rawWidth > rawHeight) rawWidth else rawHeight
                            maxDim.toFloat() / maxOfDims
                        } else {
                            1f
                        }
                        val width = (rawWidth * scaleFactor).toInt()
                        val height = (rawHeight * scaleFactor).toInt()

                        try {
                            bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                        } catch (e: OutOfMemoryError) {
                            System.gc()
                            try {
                                val fallbackWidth = (width * 0.75f).toInt()
                                val fallbackHeight = (height * 0.75f).toInt()
                                bmp = Bitmap.createBitmap(fallbackWidth, fallbackHeight, Bitmap.Config.RGB_565)
                            } catch (e2: OutOfMemoryError) {
                                try {
                                    bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.RGB_565)
                                } catch (e3: OutOfMemoryError) {
                                    e3.printStackTrace()
                                }
                            }
                        }

                        if (bmp != null) {
                            bmp!!.eraseColor(android.graphics.Color.WHITE)
                            page.render(bmp!!, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        }
                    } finally {
                        page.close()
                    }

                    if (bmp != null) {
                        bitmapCache.put(pageIndex, bmp)
                        withContext(Dispatchers.Main) {
                            bitmap = bmp
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        pageError = true
                    }
                }
            }
        }
    }

    LaunchedEffect(scale) {
        onZoomChanged(scale > 1f)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        val state = rememberTransformableState { zoomChange, offsetChange, _ ->
            scale = (scale * zoomChange).coerceIn(1f, 5f)
            val maxOffsetX = (scale - 1f) * widthPx / 2f
            val maxOffsetY = (scale - 1f) * heightPx / 2f
            offsetX = (offsetX + offsetChange.x).coerceIn(-maxOffsetX, maxOffsetX)
            offsetY = (offsetY + offsetChange.y).coerceIn(-maxOffsetY, maxOffsetY)
        }

        Card(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(scale > 1f) {
                    if (scale > 1f) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val maxOffsetX = (scale - 1f) * widthPx / 2f
                            val maxOffsetY = (scale - 1f) * heightPx / 2f
                            offsetX = (offsetX + dragAmount.x).coerceIn(-maxOffsetX, maxOffsetX)
                            offsetY = (offsetY + dragAmount.y).coerceIn(-maxOffsetY, maxOffsetY)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                scale = 2.5f
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    )
                }
                .transformable(state = state)
                .clipToBounds()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            shape = androidx.compose.ui.graphics.RectangleShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Page ${pageIndex + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else if (pageError) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = "Error",
                            tint = Color.Red,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("পৃষ্ঠাটি লোড করা যায়নি", color = Color.White, fontSize = 14.sp)
                    }
                } else {
                    CircularProgressIndicator(
                        color = Color(0xFF0F766E),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
