package com.example

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

enum class PdfTheme {
    Light, Dark, Sepia, Night, Auto
}

private data class ViewerThemeColors(
    val viewerBg: Color,
    val cardBg: Color,
    val textColor: Color,
    val colorFilter: androidx.compose.ui.graphics.ColorFilter?
)

enum class PdfFitMode {
    FitWidth, FitPage
}

/**
 * Thread-safe, memory-efficient PDF rendering and caching engine.
 * Supports auto-tuning based on memory capacity and document size.
 */
class PdfRenderManager(
    private val context: Context,
    private val file: File
) : ComponentCallbacks2 {

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    
    val pageCount: Int
    val scaleFactor: Float
    private val maxCacheSize: Int
    
    private val rendererLock = Any()
    private val bitmapCache: LruCache<Int, Bitmap>
    private val thumbnailCache = LruCache<Int, Bitmap>(15)

    init {
        context.registerComponentCallbacks(this)
        
        fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        pdfRenderer = PdfRenderer(fileDescriptor!!)
        pageCount = pdfRenderer?.pageCount ?: 0

        // Use 1/6th of the available maximum JVM memory in Kilobytes.
        val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSizeKb = (maxMemoryKb / 6).coerceAtLeast(4096) // Use at least 4MB (4096 KB)
        maxCacheSize = cacheSizeKb

        // We use an LRU Cache sized in KB to cache loaded pages for buttery scrolling without OOM
        bitmapCache = object : LruCache<Int, Bitmap>(cacheSizeKb) {
            override fun sizeOf(key: Int, value: Bitmap): Int {
                return value.byteCount / 1024
            }
            override fun entryRemoved(evicted: Boolean, key: Int?, oldValue: Bitmap?, newValue: Bitmap?) {
                // Since Android Oreo (API 26+), native memory for Bitmaps is managed cleanly by JVM GC.
                // We drop the cache reference and let GC safely garbage-collect it when it leaves the viewport.
            }
        }

        // Target high-quality density
        val metrics = context.resources.displayMetrics
        scaleFactor = (metrics.densityDpi / 160f).coerceIn(1.5f, 3.0f)
    }

    fun getPage(pageIndex: Int): Bitmap? {
        if (pageIndex < 0 || pageIndex >= pageCount) return null
        
        synchronized(rendererLock) {
            val cached = bitmapCache.get(pageIndex)
            if (cached != null && !cached.isRecycled) {
                return cached
            }
            
            val rendered = renderPage(pageIndex)
            if (rendered != null) {
                bitmapCache.put(pageIndex, rendered)
            }
            return rendered
        }
    }

    private fun renderPage(pageIndex: Int): Bitmap? {
        if (pdfRenderer == null) return null
        
        var page: PdfRenderer.Page? = null
        try {
            page = pdfRenderer!!.openPage(pageIndex)
            var currentScale = scaleFactor
            var bitmap: Bitmap? = null
            
            // Try to allocate bitmap. If OOM, run GC and scale down to retry
            var attempts = 0
            while (attempts < 3) {
                try {
                    val width = (page.width * currentScale).toInt().coerceAtLeast(100)
                    val height = (page.height * currentScale).toInt().coerceAtLeast(100)
                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    break
                } catch (oom: OutOfMemoryError) {
                    System.gc()
                    currentScale *= 0.6f
                    attempts++
                }
            }
            
            if (bitmap == null) {
                // Minimum fallback using memory efficient RGB_565
                try {
                    val width = page.width.coerceAtLeast(100)
                    val height = page.height.coerceAtLeast(100)
                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                } catch (oom: OutOfMemoryError) {
                    System.gc()
                    try {
                        val width = (page.width * 0.5f).toInt().coerceAtLeast(100)
                        val height = (page.height * 0.5f).toInt().coerceAtLeast(100)
                        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                    } catch (t2: Throwable) {
                        return null
                    }
                }
            }
            
            // Clean white background before rendering
            bitmap.eraseColor(android.graphics.Color.WHITE)
            
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            return bitmap
        } catch (t: Throwable) {
            t.printStackTrace()
            try {
                page?.close()
            } catch (e: Exception) {}
            return null
        }
    }

    fun getThumbnail(pageIndex: Int): Bitmap? {
        if (pageIndex < 0 || pageIndex >= pageCount) return null
        
        synchronized(rendererLock) {
            val cached = thumbnailCache.get(pageIndex)
            if (cached != null && !cached.isRecycled) {
                return cached
            }
            
            if (pdfRenderer == null) return null
            var page: PdfRenderer.Page? = null
            try {
                page = pdfRenderer!!.openPage(pageIndex)
                // Small resolution thumbnail for sidebar preview
                val width = (page.width * 0.4f).toInt().coerceAtLeast(100)
                val height = (page.height * 0.4f).toInt().coerceAtLeast(140)
                
                var bitmap = try {
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                } catch (oom: OutOfMemoryError) {
                    System.gc()
                    Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                }
                bitmap.eraseColor(android.graphics.Color.WHITE)
                
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                
                thumbnailCache.put(pageIndex, bitmap)
                return bitmap
            } catch (t: Throwable) {
                t.printStackTrace()
                try {
                    page?.close()
                } catch (e: Exception) {}
                return null
            }
        }
    }

    fun getPageWidth(pageIndex: Int): Int {
        if (pdfRenderer == null || pageIndex < 0 || pageIndex >= pageCount) return 300
        synchronized(rendererLock) {
            try {
                val page = pdfRenderer!!.openPage(pageIndex)
                val w = page.width
                page.close()
                return w
            } catch (e: Exception) {
                return 300
            }
        }
    }

    fun getPageHeight(pageIndex: Int): Int {
        if (pdfRenderer == null || pageIndex < 0 || pageIndex >= pageCount) return 400
        synchronized(rendererLock) {
            try {
                val page = pdfRenderer!!.openPage(pageIndex)
                val h = page.height
                page.close()
                return h
            } catch (e: Exception) {
                return 400
            }
        }
    }

    fun close() {
        synchronized(rendererLock) {
            try {
                bitmapCache.evictAll()
                thumbnailCache.evictAll()
                pdfRenderer?.close()
                fileDescriptor?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pdfRenderer = null
                fileDescriptor = null
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            synchronized(rendererLock) {
                bitmapCache.evictAll()
                thumbnailCache.evictAll()
                System.gc()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {}
    override fun onLowMemory() {
        synchronized(rendererLock) {
            bitmapCache.evictAll()
            thumbnailCache.evictAll()
            System.gc()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerDialog(
    file: File,
    title: String,
    url: String = "",
    onClose: () -> Unit
) {
    var pageCount by remember { mutableStateOf(0) }
    var renderManager by remember { mutableStateOf<PdfRenderManager?>(null) }
    
    var isPasswordProtected by remember { mutableStateOf(false) }
    var isCorrupted by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }
    
    var isHorizontalMode by remember { mutableStateOf(false) }
    var fitMode by remember { mutableStateOf(PdfFitMode.FitWidth) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val lazyListState = rememberLazyListState()
    val pagerState = rememberPagerState(pageCount = { pageCount })

    // Comfort/Accessibility States
    var pdfTheme by remember { mutableStateOf(PdfTheme.Light) }
    var isReadingMode by remember { mutableStateOf(true) }
    var isRotationLocked by remember { mutableStateOf(false) }
    var isLargeTextMode by remember { mutableStateOf(false) }
    var isHighContrastMode by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Reading Position state
    val pdfKey = remember(file, url) { PdfHistoryManager.getPdfKey(file, url) }
    var initialPositionRestored by remember { mutableStateOf(false) }
    var lastReadPageMessage by remember { mutableStateOf("") }
    
    // Jump & Sidebar navigation dialogs
    var showJumpDialog by remember { mutableStateOf(false) }
    var isSidebarOpen by remember { mutableStateOf(false) }

    // Load PDF in background safely
    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            try {
                if (!file.exists() || file.length() == 0L) {
                    throw Exception("File does not exist or empty")
                }
                val manager = PdfRenderManager(context, file)
                renderManager = manager
                pageCount = manager.pageCount
                
                // Track reading history
                PdfHistoryManager.addRecent(context, title, file.absolutePath, url)
            } catch (e: SecurityException) {
                isPasswordProtected = true
                loadError = true
            } catch (e: Exception) {
                e.printStackTrace()
                // Check password pattern in exception message
                if (e.message?.contains("password", ignoreCase = true) == true) {
                    isPasswordProtected = true
                } else {
                    isCorrupted = true
                }
                loadError = true
            }
        }
    }

    // Adapt rotation orientation dynamically based on auto-rotate lock
    LaunchedEffect(isRotationLocked) {
        val activity = context as? android.app.Activity
        if (activity != null) {
            if (isRotationLocked) {
                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LOCKED
            } else {
                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    // Reset rotation lock when exiting PDF Viewer
    DisposableEffect(Unit) {
        onDispose {
            val activity = context as? android.app.Activity
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Determine current theme settings
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val currentTheme = remember(pdfTheme, systemDark) {
        if (pdfTheme == PdfTheme.Auto) {
            if (systemDark) PdfTheme.Dark else PdfTheme.Light
        } else {
            pdfTheme
        }
    }

    // Construct Theme colors
    val themeColors = remember(currentTheme, isHighContrastMode) {
        when (currentTheme) {
            PdfTheme.Dark -> {
                ViewerThemeColors(
                    viewerBg = Color(0xFF0F172A),
                    cardBg = Color(0xFF1E293B),
                    textColor = Color(0xFFF1F5F9),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
                        -1f,  0f,  0f, 0f, 255f,
                         0f, -1f,  0f, 0f, 255f,
                         0f,  0f, -1f, 0f, 255f,
                         0f,  0f,  0f, 1f,   0f
                    )))
                )
            }
            PdfTheme.Night -> {
                ViewerThemeColors(
                    viewerBg = Color(0xFF020617),
                    cardBg = Color(0xFF0F172A),
                    textColor = Color(0xFFE2E8F0),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
                        -1f,  0f,  0f, 0f, 220f,
                         0f, -1f,  0f, 0f, 220f,
                         0f,  0f, -1f, 0f, 220f,
                         0f,  0f,  0f, 1f,   0f
                    )))
                )
            }
            PdfTheme.Sepia -> {
                ViewerThemeColors(
                    viewerBg = Color(0xFFFDF6E3),
                    cardBg = Color(0xFFF4ECD8),
                    textColor = Color(0xFF5C4033),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
                         0.393f, 0.769f, 0.189f, 0f, 0f,
                         0.349f, 0.686f, 0.168f, 0f, 0f,
                         0.272f, 0.534f, 0.131f, 0f, 0f,
                         0f,     0f,     0f,     1f, 0f
                    )))
                )
            }
            PdfTheme.Light -> {
                ViewerThemeColors(
                    viewerBg = if (isHighContrastMode) Color.White else Color(0xFFF8FAFC),
                    cardBg = Color.White,
                    textColor = Color(0xFF0F172A),
                    colorFilter = if (isHighContrastMode) androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
                         2f,   0f,   0f, 0f, -120f,
                         0f,   2f,   0f, 0f, -120f,
                         0f,   0f,   2f, 0f, -120f,
                         0f,   0f,   0f,  1f,   0f
                    ))) else null
                )
            }
            else -> {
                ViewerThemeColors(
                    viewerBg = if (isHighContrastMode) Color.White else Color(0xFFF1F5F9),
                    cardBg = Color.White,
                    textColor = Color(0xFF1E293B),
                    colorFilter = null
                )
            }
        }
    }

    // Safely dispose PDF render manager on component removal
    DisposableEffect(renderManager) {
        onDispose {
            renderManager?.close()
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(500)
        focusRequester.requestFocus()
    }

    val visiblePage by remember {
        derivedStateOf {
            if (isHorizontalMode) {
                pagerState.currentPage + 1
            } else {
                if (pageCount > 0) {
                    (lazyListState.firstVisibleItemIndex + 1).coerceAtMost(pageCount)
                } else {
                    1
                }
            }
        }
    }

    // Continuously save reading position as user scrolls
    LaunchedEffect(visiblePage, pageCount) {
        if (pageCount > 0 && initialPositionRestored) {
            PdfHistoryManager.saveLastPage(context, pdfKey, visiblePage - 1)
        }
    }

    // Scroll to last read position once initially loaded
    LaunchedEffect(renderManager, pageCount) {
        if (renderManager != null && pageCount > 0 && !initialPositionRestored) {
            val lastPage = PdfHistoryManager.getLastPage(context, pdfKey)
            if (lastPage in 0 until pageCount) {
                if (lastPage > 0) {
                    delay(300) // Small delay to let the UI finish layout
                    if (isHorizontalMode) {
                        pagerState.scrollToPage(lastPage)
                    } else {
                        lazyListState.scrollToItem(lastPage)
                    }
                    lastReadPageMessage = "পূর্ববর্তী পঠিত পৃষ্ঠা ${convertToBengaliDigits((lastPage + 1).toString())}-এ স্বয়ংক্রিয়ভাবে নিয়ে যাওয়া হয়েছে।"
                }
            }
            initialPositionRestored = true
        }
    }

    // Dismiss message toast after 3.5 seconds
    LaunchedEffect(lastReadPageMessage) {
        if (lastReadPageMessage.isNotBlank()) {
            delay(3500)
            lastReadPageMessage = ""
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        val dialogWindow = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        LaunchedEffect(dialogWindow) {
            dialogWindow?.let { window ->
                window.setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                window.setBackgroundDrawableResource(android.R.color.transparent)
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.key) {
                            Key.DirectionLeft, Key.PageUp -> {
                                if (pageCount > 0) {
                                    scope.launch {
                                        val prev = (visiblePage - 2).coerceAtLeast(0)
                                        if (isHorizontalMode) pagerState.animateScrollToPage(prev)
                                        else lazyListState.animateScrollToItem(prev)
                                    }
                                }
                                true
                            }
                            Key.DirectionRight, Key.PageDown -> {
                                if (pageCount > 0) {
                                    scope.launch {
                                        val next = visiblePage.coerceAtMost(pageCount - 1)
                                        if (isHorizontalMode) pagerState.animateScrollToPage(next)
                                        else lazyListState.animateScrollToItem(next)
                                    }
                                }
                                true
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            Scaffold(
                containerColor = themeColors.viewerBg,
                topBar = {
                    if (!isReadingMode) {
                        TopAppBar(
                            title = {
                                Column(modifier = Modifier.clickable { if (pageCount > 0) showJumpDialog = true }) {
                                    Text(
                                        text = title,
                                        fontSize = getFontSize(15f, isLargeTextMode),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        color = themeColors.textColor
                                    )
                                    if (pageCount > 0) {
                                        Text(
                                            text = "পৃষ্ঠা: ${convertToBengaliDigits(visiblePage.toString())} / ${convertToBengaliDigits(pageCount.toString())} (ট্যাপ করুন)",
                                            fontSize = getFontSize(11f, isLargeTextMode),
                                            color = if (currentTheme == PdfTheme.Dark || currentTheme == PdfTheme.Night) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = onClose) {
                                    Icon(Icons.Default.Close, contentDescription = "Close PDF", tint = themeColors.textColor)
                                }
                            },
                            actions = {
                                if (pageCount > 0) {
                                    // Toggle Fit Mode (Page vs Width)
                                    IconButton(
                                        onClick = {
                                            fitMode = if (fitMode == PdfFitMode.FitWidth) PdfFitMode.FitPage else PdfFitMode.FitWidth
                                        }
                                    ) {
                                        Text(
                                            text = if (fitMode == PdfFitMode.FitWidth) "Page" else "Width",
                                            fontSize = getFontSize(12f, isLargeTextMode),
                                            fontWeight = FontWeight.Bold,
                                            color = if (currentTheme == PdfTheme.Dark || currentTheme == PdfTheme.Night) Color(0xFF60A5FA) else Color(0xFF3B82F6)
                                        )
                                    }
                                    
                                    // Toggle Scroll Mode (Vertical List vs Horizontal Pager)
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                val targetPage = visiblePage - 1
                                                isHorizontalMode = !isHorizontalMode
                                                if (isHorizontalMode) {
                                                    pagerState.scrollToPage(targetPage.coerceIn(0, pageCount - 1))
                                                } else {
                                                    lazyListState.scrollToItem(targetPage.coerceIn(0, pageCount - 1))
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isHorizontalMode) Icons.Default.List else Icons.Default.MenuBook,
                                            contentDescription = "Toggle Scroll Mode",
                                            tint = themeColors.textColor
                                        )
                                    }

                                    // Settings Button
                                    IconButton(onClick = { showSettingsDialog = true }) {
                                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = themeColors.textColor)
                                    }

                                    // Sidebar Thumbnails Navigation Menu Button
                                    IconButton(onClick = { isSidebarOpen = !isSidebarOpen }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Navigation sidebar", tint = themeColors.textColor)
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = themeColors.cardBg,
                                titleContentColor = themeColors.textColor
                            )
                        )
                    }
                },
                bottomBar = {
                    if (!isReadingMode && pageCount > 1) {
                        Surface(
                            tonalElevation = 4.dp,
                            shadowElevation = 8.dp,
                            color = themeColors.cardBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                val target = (visiblePage - 2).coerceAtLeast(0)
                                                if (isHorizontalMode) {
                                                    pagerState.animateScrollToPage(target)
                                                } else {
                                                    lazyListState.animateScrollToItem(target)
                                                }
                                            }
                                        },
                                        enabled = visiblePage > 1
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronLeft,
                                            contentDescription = "Previous Page",
                                            tint = if (visiblePage > 1) themeColors.textColor else themeColors.textColor.copy(alpha = 0.4f)
                                        )
                                    }
                                    
                                    Text(
                                        text = "পৃষ্ঠা ${convertToBengaliDigits(visiblePage.toString())} / ${convertToBengaliDigits(pageCount.toString())}",
                                        fontSize = getFontSize(14f, isLargeTextMode),
                                        fontWeight = FontWeight.Bold,
                                        color = themeColors.textColor,
                                        modifier = Modifier.clickable { showJumpDialog = true }
                                    )
                                    
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                val target = visiblePage.coerceAtMost(pageCount - 1)
                                                if (isHorizontalMode) {
                                                    pagerState.animateScrollToPage(target)
                                                } else {
                                                    lazyListState.animateScrollToItem(target)
                                                }
                                            }
                                        },
                                        enabled = visiblePage < pageCount
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Next Page",
                                            tint = if (visiblePage < pageCount) themeColors.textColor else themeColors.textColor.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(2.dp))
                                
                                Slider(
                                    value = visiblePage.toFloat(),
                                    onValueChange = { pageVal ->
                                        scope.launch {
                                            val target = (pageVal.toInt() - 1).coerceIn(0, pageCount - 1)
                                            if (isHorizontalMode) {
                                                pagerState.scrollToPage(target)
                                            } else {
                                                lazyListState.scrollToItem(target)
                                            }
                                        }
                                    },
                                    valueRange = 1f..pageCount.toFloat(),
                                    steps = if (pageCount > 2) pageCount - 2 else 0,
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = Color(0xFF3B82F6),
                                        inactiveTrackColor = if (currentTheme == PdfTheme.Dark || currentTheme == PdfTheme.Night) Color(0xFF334155) else Color(0xFFE2E8F0),
                                        thumbColor = Color(0xFF2563EB)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp)
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                if (loadError) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .background(Color.White)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color(0xFFFEF2F2), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Error icon",
                                    tint = Color.Red,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            val errorHeading = when {
                                isPasswordProtected -> "পিডিএফ ফাইলটি সুরক্ষিত"
                                isCorrupted -> "ফাইলটি ত্রুটিযুক্ত বা ক্ষতিগ্রস্ত"
                                else -> "ভিউ করা যাচ্ছে না"
                            }
                            
                            val errorDesc = when {
                                isPasswordProtected -> "নিরাপত্তার স্বার্থে পাসওয়ার্ড দ্বারা সুরক্ষিত পিডিএফ ফাইল সরাসরি অ্যাপে দেখা সম্ভব নয়। অনুগ্রহ করে বাহ্যিক ব্রাউজারে এটি ওপেন করুন।"
                                isCorrupted -> "পিডিএফ ফাইলটির ফরম্যাট সঠিক নয় বা ডাউনলোডের সময় ফাইলটি ড্যামেজ হয়ে গেছে।"
                                else -> "এটি সরাসরি অ্যাপে ভিউ করা যাচ্ছে না। অনুগ্রহ করে বাইরে ব্রাউজারে এটি দেখতে চেষ্টা করুন।"
                            }
                            
                            Text(
                                text = errorHeading,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = errorDesc,
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            if (url.isNotBlank()) {
                                Button(
                                    onClick = {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "ব্রাউজার ওপেন করা যায়নি", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text("ব্রাউজারে ওপেন করুন", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            
                            TextButton(onClick = onClose) {
                                Text("ফিরে যান", color = Color.Gray)
                            }
                        }
                    }
                } else if (renderManager == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF2563EB))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("লোডিং পিডিএফ...", fontSize = 14.sp, color = Color(0xFF64748B))
                        }
                    }
                } else if (pageCount == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .background(Color.White)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("পিডিএফ ফাইলটিতে কোনো পৃষ্ঠা পাওয়া যায়নি", color = Color.Red, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = onClose) {
                                Text("ফিরে যান", color = Color.Gray)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        if (isHorizontalMode) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                pageSpacing = 16.dp,
                                verticalAlignment = Alignment.CenterVertically
                            ) { index ->
                                PdfPageItem(
                                    renderManager = renderManager!!,
                                    pageIndex = index,
                                    fitMode = fitMode,
                                    modifier = Modifier.fillMaxSize(),
                                    pdfTheme = pdfTheme,
                                    isHighContrastMode = isHighContrastMode,
                                    onPageTap = { isReadingMode = !isReadingMode }
                                )
                            }
                        } else {
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                items(pageCount) { index ->
                                    PdfPageItem(
                                        renderManager = renderManager!!,
                                        pageIndex = index,
                                        fitMode = fitMode,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight(),
                                        pdfTheme = pdfTheme,
                                        isHighContrastMode = isHighContrastMode,
                                        onPageTap = { isReadingMode = !isReadingMode }
                                    )
                                }
                            }
                        }

                        // Last reading position toast overlay
                        AnimatedVisibility(
                            visible = lastReadPageMessage.isNotBlank(),
                            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .padding(bottom = 80.dp)
                        ) {
                            Surface(
                                color = Color(0xFF1E293B).copy(alpha = 0.95f),
                                shape = RoundedCornerShape(12.dp),
                                shadowElevation = 6.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.MenuBook, contentDescription = "Book", tint = Color(0xFF60A5FA), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = lastReadPageMessage,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Reading Mode Exit Overlay Button
                        if (isReadingMode) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .statusBarsPadding()
                                    .padding(16.dp)
                            ) {
                                androidx.compose.material3.FilledIconButton(
                                    onClick = { isReadingMode = false },
                                    colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                                        containerColor = Color.Black.copy(alpha = 0.4f),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FullscreenExit,
                                        contentDescription = "Exit Reading Mode"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Beautiful Sidebar Panel overlay (Thumbnails sidebar for super quick scrolling)
            if (isSidebarOpen && renderManager != null) {
                // Dim Backdrop
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { isSidebarOpen = false })
                        }
                )

                // Slider Panel container on the Right
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(280.dp)
                        .background(Color.White)
                        .align(Alignment.CenterEnd)
                        .clickable(enabled = false) {}
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .statusBarsPadding()
                    ) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "পৃষ্ঠা নেভিগেশন",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            IconButton(onClick = { isSidebarOpen = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close navigation", tint = Color(0xFF64748B))
                            }
                        }

                        // Pages Thumbnails List
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(pageCount) { idx ->
                                val isSelected = idx == (visiblePage - 1)
                                ThumbnailSidebarItem(
                                    renderManager = renderManager!!,
                                    pageIndex = idx,
                                    isSelected = isSelected,
                                    onClick = {
                                        scope.launch {
                                            if (isHorizontalMode) {
                                                pagerState.scrollToPage(idx)
                                            } else {
                                                lazyListState.scrollToItem(idx)
                                            }
                                        }
                                        isSidebarOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Jump to Page Dialog
            if (showJumpDialog && pageCount > 0) {
                var jumpInput by remember { mutableStateOf("") }
                var errorMsg by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showJumpDialog = false },
                    title = {
                        Text(
                            text = "পৃষ্ঠায় লাফ দিন (Jump to Page)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1E293B)
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "১ থেকে ${convertToBengaliDigits(pageCount.toString())} এর মধ্যে পৃষ্ঠা নম্বর লিখুন:",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = jumpInput,
                                onValueChange = {
                                    jumpInput = it
                                    errorMsg = ""
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("যেমন: ৫") }
                            )
                            if (errorMsg.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = errorMsg, color = Color.Red, fontSize = 11.sp)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val parsed = jumpInput.toIntOrNull()
                                if (parsed != null && parsed in 1..pageCount) {
                                    scope.launch {
                                        if (isHorizontalMode) {
                                            pagerState.scrollToPage(parsed - 1)
                                        } else {
                                            lazyListState.scrollToItem(parsed - 1)
                                        }
                                    }
                                    showJumpDialog = false
                                } else {
                                    errorMsg = "অনুগ্রহ করে ১ থেকে ${pageCount} এর মধ্যে একটি বৈধ পৃষ্ঠা নম্বর দিন।"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("নিশ্চিত", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showJumpDialog = false }) {
                            Text("বাতিল", color = Color.Gray)
                        }
                    }
                )
            }

            // Reader Settings & Information Dialog
            if (showSettingsDialog) {
                AlertDialog(
                    onDismissRequest = { showSettingsDialog = false },
                    title = {
                        Text(
                            text = "রিডার সেটিংস ও তথ্য (Reader Settings & Info)",
                            fontWeight = FontWeight.Bold,
                            fontSize = getFontSize(16f, isLargeTextMode),
                            color = Color(0xFF1E293B)
                        )
                    },
                    text = {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 1. Reading Mode & Rotation
                            item {
                                Column {
                                    Text(
                                        text = "রিডিং সেটিংস (Reading Settings)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = getFontSize(13f, isLargeTextMode),
                                        color = Color(0xFF3B82F6)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isReadingMode = !isReadingMode }
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("পড়ার মোড (Reading Mode)", fontSize = getFontSize(14f, isLargeTextMode), color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
                                            Text("টুলবার ও নেভিগেশন হাইড করে পরিচ্ছন্ন স্ক্রিন", fontSize = getFontSize(11f, isLargeTextMode), color = Color(0xFF64748B))
                                        }
                                        Switch(
                                            checked = isReadingMode,
                                            onCheckedChange = { isReadingMode = it }
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isRotationLocked = !isRotationLocked }
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("অটো রোটেট লক (Rotation Lock)", fontSize = getFontSize(14f, isLargeTextMode), color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
                                            Text("স্ক্রিন রোটেট হওয়া লক করে রাখবে", fontSize = getFontSize(11f, isLargeTextMode), color = Color(0xFF64748B))
                                        }
                                        Switch(
                                            checked = isRotationLocked,
                                            onCheckedChange = { isRotationLocked = it }
                                        )
                                    }
                                }
                            }
                            
                            // 2. Theme Selection
                            item {
                                Column {
                                    Text(
                                        text = "রিডার থিম (Reader Theme)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = getFontSize(13f, isLargeTextMode),
                                        color = Color(0xFF3B82F6)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        PdfTheme.values().forEach { themeOption ->
                                            val isSelected = pdfTheme == themeOption
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .background(
                                                        color = when (themeOption) {
                                                            PdfTheme.Light -> Color.White
                                                            PdfTheme.Dark -> Color(0xFF1E293B)
                                                            PdfTheme.Sepia -> Color(0xFFF4ECD8)
                                                            PdfTheme.Night -> Color(0xFF0F172A)
                                                            PdfTheme.Auto -> Color(0xFFF1F5F9)
                                                        },
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { pdfTheme = themeOption }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = when (themeOption) {
                                                        PdfTheme.Light -> "Light"
                                                        PdfTheme.Dark -> "Dark"
                                                        PdfTheme.Sepia -> "Sepia"
                                                        PdfTheme.Night -> "Night"
                                                        PdfTheme.Auto -> "Auto"
                                                    },
                                                    fontSize = getFontSize(11f, isLargeTextMode),
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (themeOption) {
                                                        PdfTheme.Light -> Color(0xFF1E293B)
                                                        PdfTheme.Dark -> Color.White
                                                        PdfTheme.Sepia -> Color(0xFF5C4033)
                                                        PdfTheme.Night -> Color(0xFF94A3B8)
                                                        PdfTheme.Auto -> Color(0xFF1E293B)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // 3. Accessibility Settings
                            item {
                                Column {
                                    Text(
                                        text = "অ্যাক্সেসিবিলিটি (Accessibility)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = getFontSize(13f, isLargeTextMode),
                                        color = Color(0xFF3B82F6)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isLargeTextMode = !isLargeTextMode }
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("বড় লেখা মোড (Large Text Mode)", fontSize = getFontSize(14f, isLargeTextMode), color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
                                            Text("মেনু ও ফন্টের আকার বৃদ্ধি করুন", fontSize = getFontSize(11f, isLargeTextMode), color = Color(0xFF64748B))
                                        }
                                        Switch(
                                            checked = isLargeTextMode,
                                            onCheckedChange = { isLargeTextMode = it }
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isHighContrastMode = !isHighContrastMode }
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("উচ্চ কনট্রাস্ট (High Contrast Mode)", fontSize = getFontSize(14f, isLargeTextMode), color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
                                            Text("উচ্চ দৃশ্যমানতা কালার ফিল্টার প্রয়োগ", fontSize = getFontSize(11f, isLargeTextMode), color = Color(0xFF64748B))
                                        }
                                        Switch(
                                            checked = isHighContrastMode,
                                            onCheckedChange = { isHighContrastMode = it }
                                        )
                                    }
                                }
                            }
                            
                            // 4. File Information
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "ফাইল তথ্য (File Information)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = getFontSize(13f, isLargeTextMode),
                                        color = Color(0xFF1E293B)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("ফাইলের সাইজ (File Size):", fontSize = getFontSize(12f, isLargeTextMode), color = Color(0xFF64748B))
                                        Text(formatFileSize(file.length()), fontSize = getFontSize(12f, isLargeTextMode), color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("মোট পৃষ্ঠা (Page Count):", fontSize = getFontSize(12f, isLargeTextMode), color = Color(0xFF64748B))
                                        Text("${convertToBengaliDigits(pageCount.toString())} টি", fontSize = getFontSize(12f, isLargeTextMode), color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("আনুমানিক পড়ার সময়:", fontSize = getFontSize(12f, isLargeTextMode), color = Color(0xFF64748B))
                                        Text(getEstimatedReadingTime(pageCount), fontSize = getFontSize(12f, isLargeTextMode), color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showSettingsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("ঠিক আছে", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ThumbnailSidebarItem(
    renderManager: PdfRenderManager,
    pageIndex: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageIndex) {
        withContext(Dispatchers.IO) {
            bitmap = renderManager.getThumbnail(pageIndex)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clickable { onClick() },
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2563EB)) else null,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFEFF6FF) else Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(50.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                    .clip(RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null && !bitmap!!.isRecycled) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Thumbnail ${pageIndex + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    CircularProgressIndicator(strokeWidth = 1.dp, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "পৃষ্ঠা ${convertToBengaliDigits((pageIndex + 1).toString())}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFF1E293B)
                )
                Text(
                    text = "ট্যাপ করে লাফ দিন",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
fun PdfPageItem(
    renderManager: PdfRenderManager,
    pageIndex: Int,
    fitMode: PdfFitMode,
    modifier: Modifier = Modifier,
    pdfTheme: PdfTheme = PdfTheme.Light,
    isHighContrastMode: Boolean = false,
    onPageTap: () -> Unit = {}
) {
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    
    val themeColorFilter = remember(pdfTheme, isHighContrastMode) {
        val matrix = when {
            isHighContrastMode -> {
                floatArrayOf(
                    2.0f,  0.0f,  0.0f, 0.0f, -120.0f,
                    0.0f,  2.0f,  0.0f, 0.0f, -120.0f,
                    0.0f,  0.0f,  2.0f, 0.0f, -120.0f,
                    0.0f,  0.0f,  0.0f, 1.0f,    0.0f
                )
            }
            pdfTheme == PdfTheme.Dark || pdfTheme == PdfTheme.Night -> {
                floatArrayOf(
                    -1.0f,  0.0f,  0.0f, 0.0f, 255.0f,
                     0.0f, -1.0f,  0.0f, 0.0f, 255.0f,
                     0.0f,  0.0f, -1.0f, 0.0f, 255.0f,
                     0.0f,  0.0f,  0.0f, 1.0f,   0.0f
                )
            }
            pdfTheme == PdfTheme.Sepia -> {
                floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0.000f, 0.000f, 0.000f, 1f, 0f
                )
            }
            else -> null
        }
        matrix?.let { androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(it)) }
    }

    // Zoom and Panning States
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "ZoomScale"
    )
    val animatedOffsetX by animateFloatAsState(
        targetValue = offset.x,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "ZoomOffsetX"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = offset.y,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "ZoomOffsetY"
    )

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 6f)
        if (scale == 1f) {
            offset = androidx.compose.ui.geometry.Offset.Zero
        }
    }

    LaunchedEffect(pageIndex, renderManager) {
        withContext(Dispatchers.IO) {
            val loaded = renderManager.getPage(pageIndex)
            bitmap = loaded
        }
    }

    Card(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = androidx.compose.ui.geometry.Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
                    onTap = {
                        onPageTap()
                    }
                )
            }
            .let { m ->
                if (scale > 1f) {
                    m.pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offset += dragAmount
                        }
                    }
                } else m
            },
        colors = CardDefaults.cardColors(
            containerColor = when (pdfTheme) {
                PdfTheme.Light -> Color.White
                PdfTheme.Dark -> Color(0xFF1E293B)
                PdfTheme.Sepia -> Color(0xFFF4ECD8)
                PdfTheme.Night -> Color(0xFF0F172A)
                PdfTheme.Auto -> Color.White
            }
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (bitmap != null && !bitmap!!.isRecycled) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .let {
                        if (fitMode == PdfFitMode.FitWidth) {
                            it.aspectRatio(bitmap!!.width.toFloat() / bitmap!!.height.toFloat())
                        } else {
                            it.fillMaxHeight()
                        }
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .transformable(state = transformState, enabled = true),
                contentAlignment = Alignment.Center
            ) {
                // Drag constraints based on scale
                val maxOffsetX = (scale - 1f) * 400f
                val maxOffsetY = (scale - 1f) * 600f
                LaunchedEffect(offset, scale) {
                    if (scale > 1f) {
                        offset = androidx.compose.ui.geometry.Offset(
                            offset.x.coerceIn(-maxOffsetX, maxOffsetX),
                            offset.y.coerceIn(-maxOffsetY, maxOffsetY)
                        )
                    }
                }

                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "পিডিএফ পৃষ্ঠা ${pageIndex + 1}",
                    colorFilter = themeColorFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = animatedScale,
                            scaleY = animatedScale,
                            translationX = animatedOffsetX,
                            translationY = animatedOffsetY
                        ),
                    contentScale = if (fitMode == PdfFitMode.FitWidth) ContentScale.FillWidth else ContentScale.FillHeight
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.707f)
                    .background(
                        when (pdfTheme) {
                            PdfTheme.Light -> Color(0xFFF8FAFC)
                            PdfTheme.Dark -> Color(0xFF1E293B)
                            PdfTheme.Sepia -> Color(0xFFF4ECD8)
                            PdfTheme.Night -> Color(0xFF0F172A)
                            PdfTheme.Auto -> Color(0xFFF8FAFC)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = when (pdfTheme) {
                        PdfTheme.Light -> Color(0xFF2563EB)
                        PdfTheme.Dark, PdfTheme.Night -> Color(0xFF60A5FA)
                        PdfTheme.Sepia -> Color(0xFF8B5CF6)
                        PdfTheme.Auto -> Color(0xFF2563EB)
                    },
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

fun getFontSize(base: Float, isLarge: Boolean): androidx.compose.ui.unit.TextUnit {
    val scale = if (isLarge) 1.25f else 1.0f
    return (base * scale).sp
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kilobytes = bytes / 1024.0
    val megabytes = kilobytes / 1024.0
    return if (megabytes >= 1.0) {
        String.format("%.2f MB", megabytes)
    } else {
        String.format("%.2f KB", kilobytes)
    }
}

fun getEstimatedReadingTime(pageCount: Int): String {
    val minutes = (pageCount * 1.5).toInt().coerceAtLeast(1)
    return "প্রায় ${convertToBengaliDigits(minutes.toString())} মিনিট"
}
