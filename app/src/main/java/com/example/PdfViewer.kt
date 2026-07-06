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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import android.graphics.PointF
import android.graphics.RectF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.ColorMatrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

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
        // 1. Initialize ParcelFileDescriptor and PdfRenderer
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        fileDescriptor = fd
        val renderer = PdfRenderer(fd)
        pdfRenderer = renderer
        pageCount = renderer.pageCount

        // 2. Query system resources to optimize caching & resolution parameters
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val isLowRam = activityManager.isLowRamDevice
        val maxMemoryHeapMb = Runtime.getRuntime().maxMemory() / (1024 * 1024)

        // 3. Auto-tune based on document complexity and device capacity
        when {
            // Highly constrained environment or extremely large PDF -> minimal cache, lower rendering resolution
            pageCount >= 1000 || isLowRam || maxMemoryHeapMb < 128 -> {
                scaleFactor = 1.1f
                maxCacheSize = 2
            }
            // Moderate sized document or medium tier device -> moderate cache, standard crisp resolution
            pageCount >= 200 || maxMemoryHeapMb < 256 -> {
                scaleFactor = 1.5f
                maxCacheSize = 3
            }
            // Normal document on a modern device -> larger cache, highly detailed crisp resolution
            pageCount >= 50 -> {
                scaleFactor = 1.9f
                maxCacheSize = 4
            }
            // Very short/simple document -> fast access cache, high-fidelity rendering resolution
            else -> {
                scaleFactor = 2.3f
                maxCacheSize = 5
            }
        }

        // 4. Initialize LRU bitmap cache with safe resource recycling
        bitmapCache = object : LruCache<Int, Bitmap>(maxCacheSize) {
            override fun entryRemoved(evicted: Boolean, key: Int?, oldValue: Bitmap?, newValue: Bitmap?) {
                if (evicted && oldValue != null && !oldValue.isRecycled) {
                    oldValue.recycle()
                }
            }
        }

        // 5. Register with the OS memory trim notifications
        context.registerComponentCallbacks(this)
    }

    /**
     * Retrieve the rendered bitmap of the specified page, either from the LRU cache or newly rendered.
     */
    fun getPage(pageIndex: Int): Bitmap? {
        if (pageIndex < 0 || pageIndex >= pageCount) return null

        // Try hit from Cache first
        synchronized(bitmapCache) {
            val cached = bitmapCache.get(pageIndex)
            if (cached != null && !cached.isRecycled) {
                return cached
            }
        }

        // Render page synchronously under renderer lock
        return renderPage(pageIndex)
    }

    private fun renderPage(pageIndex: Int): Bitmap? {
        synchronized(rendererLock) {
            val renderer = pdfRenderer ?: return null
            try {
                val page = renderer.openPage(pageIndex)
                
                val width = (page.width * scaleFactor).toInt()
                val height = (page.height * scaleFactor).toInt()
                
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                
                // Save to cache
                synchronized(bitmapCache) {
                    val old = bitmapCache.put(pageIndex, bmp)
                    if (old != null && old != bmp && !old.isRecycled) {
                        old.recycle()
                    }
                }
                return bmp
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
    }

    /**
     * Renders a highly optimized, lightweight thumbnail of the page.
     * Consumes minimal RAM and isolates itself in a separate LRU cache.
     */
    fun getThumbnail(pageIndex: Int): Bitmap? {
        if (pageIndex < 0 || pageIndex >= pageCount) return null

        synchronized(thumbnailCache) {
            val cached = thumbnailCache.get(pageIndex)
            if (cached != null && !cached.isRecycled) {
                return cached
            }
        }

        synchronized(rendererLock) {
            val renderer = pdfRenderer ?: return null
            try {
                val page = renderer.openPage(pageIndex)
                
                // Low resolution (0.25x scale) thumbnail
                val thumbScale = 0.25f
                val width = (page.width * thumbScale).toInt().coerceAtLeast(120)
                val height = (page.height * thumbScale).toInt().coerceAtLeast(160)
                
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                
                synchronized(thumbnailCache) {
                    val old = thumbnailCache.put(pageIndex, bmp)
                    if (old != null && old != bmp && !old.isRecycled) {
                        old.recycle()
                    }
                }
                return bmp
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
    }

    fun getPageWidth(pageIndex: Int): Int {
        synchronized(rendererLock) {
            val renderer = pdfRenderer ?: return 0
            if (pageIndex in 0 until pageCount) {
                try {
                    val page = renderer.openPage(pageIndex)
                    val w = page.width
                    page.close()
                    return w
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return 0
        }
    }

    fun getPageHeight(pageIndex: Int): Int {
        synchronized(rendererLock) {
            val renderer = pdfRenderer ?: return 0
            if (pageIndex in 0 until pageCount) {
                try {
                    val page = renderer.openPage(pageIndex)
                    val h = page.height
                    page.close()
                    return h
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return 0
        }
    }

    /**
     * Release all native files, handles, and bitmap memory caches immediately.
     */
    fun close() {
        context.unregisterComponentCallbacks(this)
        
        synchronized(rendererLock) {
            try {
                pdfRenderer?.close()
                fileDescriptor?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            pdfRenderer = null
            fileDescriptor = null
        }

        synchronized(bitmapCache) {
            bitmapCache.evictAll()
        }
        
        synchronized(thumbnailCache) {
            thumbnailCache.evictAll()
        }
        
        System.gc()
    }

    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW || level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            synchronized(bitmapCache) {
                bitmapCache.evictAll()
            }
            synchronized(thumbnailCache) {
                thumbnailCache.evictAll()
            }
            System.gc()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {}
    
    override fun onLowMemory() {
        synchronized(bitmapCache) {
            bitmapCache.evictAll()
        }
        synchronized(thumbnailCache) {
            thumbnailCache.evictAll()
        }
        System.gc()
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

    // Phase 5 States
    var pdfTheme by remember { mutableStateOf(PdfTheme.Light) }
    var isReadingMode by remember { mutableStateOf(false) }
    var isRotationLocked by remember { mutableStateOf(false) }
    var isLargeTextMode by remember { mutableStateOf(false) }
    var isHighContrastMode by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }



    // Bookmarks, Favorites & Reading Position state
    val pdfKey = remember(file, url) { PdfHistoryManager.getPdfKey(file, url) }
    var isFavorite by remember { mutableStateOf(false) }
    var initialPositionRestored by remember { mutableStateOf(false) }
    var lastReadPageMessage by remember { mutableStateOf("") }
    
    // UI Dialogs
    var showJumpDialog by remember { mutableStateOf(false) }
    var showBookmarkDialog by remember { mutableStateOf(false) }
    var isSidebarOpen by remember { mutableStateOf(false) }
    var activeSidebarTab by remember { mutableStateOf(0) } // 0: Pages, 1: Bookmarks
    var bookmarksList by remember { mutableStateOf(emptyList<BookmarkRecord>()) }

    // --- PDF Search & Annotation States ---
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var caseSensitiveSearch by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var searchProgress by remember { mutableStateOf(0f) }
    var searchResults by remember { mutableStateOf(emptyList<SearchResultRect>()) }
    var currentMatchIndex by remember { mutableStateOf(-1) }
    
    val searchJob = remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    var isEditMode by remember { mutableStateOf(false) }
    var activeTool by remember { mutableStateOf("draw") } // "draw", "highlight", "underline", "strikethrough", "rectangle", "circle", "line", "arrow", "text", "sticky"
    var activeColor by remember { mutableStateOf(Color.Yellow) }
    var strokeWidth by remember { mutableStateOf(5f) }
    var annotationsList by remember { mutableStateOf(emptyList<PdfAnnotation>()) }
    
    val undoStack = remember { mutableStateListOf<List<PdfAnnotation>>() }
    val redoStack = remember { mutableStateListOf<List<PdfAnnotation>>() }

    // Load annotations initially
    LaunchedEffect(pdfKey) {
        annotationsList = PdfAnnotationManager.getAnnotations(context, pdfKey)
    }

    fun updateAnnotations(newList: List<PdfAnnotation>) {
        undoStack.add(annotationsList)
        redoStack.clear()
        annotationsList = newList
        PdfAnnotationManager.saveAnnotations(context, pdfKey, newList)
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(annotationsList)
            annotationsList = previous
            PdfAnnotationManager.saveAnnotations(context, pdfKey, previous)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(annotationsList)
            annotationsList = next
            PdfAnnotationManager.saveAnnotations(context, pdfKey, next)
        }
    }

    fun startSearch() {
        searchJob.value?.cancel()
        if (searchQuery.trim().isEmpty()) {
            searchResults = emptyList()
            currentMatchIndex = -1
            return
        }
        isSearching = true
        searchProgress = 0f
        
        searchJob.value = scope.launch {
            val results = PdfSearchEngine.searchPdf(
                context = context,
                file = file,
                query = searchQuery,
                caseSensitive = caseSensitiveSearch,
                onProgress = { searched, total ->
                    searchProgress = searched.toFloat() / total
                }
            )
            searchResults = results
            isSearching = false
            if (results.isNotEmpty()) {
                currentMatchIndex = 0
                val firstMatch = results[0]
                if (isHorizontalMode) {
                    pagerState.scrollToPage(firstMatch.pageIndex)
                } else {
                    lazyListState.scrollToItem(firstMatch.pageIndex)
                }
            } else {
                currentMatchIndex = -1
                android.widget.Toast.makeText(context, "কোনো মিল পাওয়া যায়নি", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun nextSearchMatch() {
        if (searchResults.isNotEmpty()) {
            currentMatchIndex = (currentMatchIndex + 1) % searchResults.size
            val match = searchResults[currentMatchIndex]
            scope.launch {
                if (isHorizontalMode) {
                    pagerState.animateScrollToPage(match.pageIndex)
                } else {
                    lazyListState.animateScrollToItem(match.pageIndex)
                }
            }
        }
    }

    fun previousSearchMatch() {
        if (searchResults.isNotEmpty()) {
            currentMatchIndex = if (currentMatchIndex - 1 < 0) searchResults.size - 1 else currentMatchIndex - 1
            val match = searchResults[currentMatchIndex]
            scope.launch {
                if (isHorizontalMode) {
                    pagerState.animateScrollToPage(match.pageIndex)
                } else {
                    lazyListState.animateScrollToItem(match.pageIndex)
                }
            }
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                searchQuery = spokenText
                startSearch()
            }
        }
    }

    // Initialize PdfRenderManager
    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            try {
                val manager = PdfRenderManager(context, file)
                renderManager = manager
                pageCount = manager.pageCount
                loadError = false
                isPasswordProtected = false
                isCorrupted = false
                
                // Track recent PDF
                PdfHistoryManager.addRecent(context, title, file.absolutePath, url)
            } catch (e: SecurityException) {
                e.printStackTrace()
                isPasswordProtected = true
                loadError = true
            } catch (e: Exception) {
                e.printStackTrace()
                val msg = e.message ?: ""
                if (msg.contains("password", ignoreCase = true) || msg.contains("encrypted", ignoreCase = true)) {
                    isPasswordProtected = true
                } else {
                    isCorrupted = true
                }
                loadError = true
            }
        }
    }

    // Refresh favorite and bookmark lists
    LaunchedEffect(pdfKey) {
        isFavorite = PdfHistoryManager.isFavorite(context, file.absolutePath, url)
        bookmarksList = PdfHistoryManager.getBookmarks(context, pdfKey)
    }

    // Rotation Lock/Unlock effect
    LaunchedEffect(isRotationLocked) {
        val activity = context as? android.app.Activity
        if (activity != null) {
            if (isRotationLocked) {
                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LOCKED
            } else {
                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR
            }
        }
    }

    // Helper to calculate scaled font sizes
    fun getFontSize(base: Float, isLarge: Boolean): androidx.compose.ui.unit.TextUnit {
        return (if (isLarge) base * 1.35f else base).sp
    }

    // Resolve colors based on current theme selection
    val currentTheme = if (pdfTheme == PdfTheme.Auto) {
        if (androidx.compose.foundation.isSystemInDarkTheme()) PdfTheme.Dark else PdfTheme.Light
    } else {
        pdfTheme
    }

    val themeColors = remember(currentTheme, isHighContrastMode) {
        when (currentTheme) {
            PdfTheme.Light -> {
                ViewerThemeColors(
                    viewerBg = if (isHighContrastMode) Color.White else Color(0xFFF1F5F9),
                    cardBg = Color.White,
                    textColor = Color(0xFF1E293B),
                    colorFilter = null
                )
            }
            PdfTheme.Dark -> {
                ViewerThemeColors(
                    viewerBg = if (isHighContrastMode) Color.Black else Color(0xFF0F172A),
                    cardBg = if (isHighContrastMode) Color.Black else Color(0xFF1E293B),
                    textColor = Color(0xFFF8FAFC),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                        -1f,  0f,  0f, 0f, 255f,
                         0f, -1f,  0f, 0f, 255f,
                         0f,  0f, -1f, 0f, 255f,
                         0f,  0f,  0f, 1f,   0f
                    )))
                )
            }
            PdfTheme.Sepia -> {
                ViewerThemeColors(
                    viewerBg = Color(0xFFFDF6E3),
                    cardBg = Color(0xFFF4ECD8),
                    textColor = Color(0xFF586E75),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f,     0f,     0f,     1f, 0f
                    )))
                )
            }
            PdfTheme.Night -> {
                ViewerThemeColors(
                    viewerBg = Color(0xFF050505),
                    cardBg = Color(0xFF151010),
                    textColor = Color(0xFFFDA4AF),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                        -0.9f,  0f,   0f,  0f, 230f,
                         0f,  -0.7f,  0f,  0f, 180f,
                         0f,   0f,  -0.5f, 0f, 120f,
                         0f,   0f,   0f,  1f,   0f
                    )))
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

    // Dismiss message toast after 3 seconds
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
                        if (isSearchActive) {
                            Surface(
                                color = themeColors.cardBg,
                                shadowElevation = 4.dp,
                                modifier = Modifier.fillMaxWidth().statusBarsPadding()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(onClick = { 
                                            isSearchActive = false
                                            searchQuery = ""
                                            searchResults = emptyList()
                                            currentMatchIndex = -1
                                            searchJob.value?.cancel()
                                        }) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Close Search", tint = themeColors.textColor)
                                        }
                                        
                                        OutlinedTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            placeholder = { Text("পিডিএফ-এ খুঁজুন...", fontSize = getFontSize(13f, isLargeTextMode)) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            trailingIcon = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(onClick = {
                                                        try {
                                                            val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                                                                putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "অনুগ্রহ করে কথা বলুন...")
                                                            }
                                                            voiceLauncher.launch(intent)
                                                        } catch (e: Exception) {
                                                            android.widget.Toast.makeText(context, "ভয়েস সার্চ উপলব্ধ নয়", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    }) {
                                                        Icon(Icons.Default.Mic, contentDescription = "ভয়েস সার্চ", tint = themeColors.textColor.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                                    }
                                                    if (searchQuery.isNotEmpty()) {
                                                        IconButton(onClick = { searchQuery = "" }) {
                                                            Icon(Icons.Default.Clear, contentDescription = "Clear input", modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                }
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF2563EB),
                                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                                focusedTextColor = themeColors.textColor,
                                                unfocusedTextColor = themeColors.textColor
                                            )
                                        )
                                    
                                    Button(
                                        onClick = { startSearch() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text("খুঁজুন", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = caseSensitiveSearch,
                                            onCheckedChange = { caseSensitiveSearch = it }
                                        )
                                        Text("ক্যাপস সংবেদনশীল (Case Sensitive)", fontSize = 11.sp, color = Color(0xFF475569))
                                    }
                                    
                                    if (searchResults.isNotEmpty()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(onClick = { previousSearchMatch() }, modifier = Modifier.size(28.dp)) {
                                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Prev match")
                                            }
                                            Text(
                                                text = "${convertToBengaliDigits((currentMatchIndex + 1).toString())}/${convertToBengaliDigits(searchResults.size.toString())}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B)
                                            )
                                            IconButton(onClick = { nextSearchMatch() }, modifier = Modifier.size(28.dp)) {
                                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next match")
                                            }
                                        }
                                    }
                                }
                                
                                if (isSearching) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { searchProgress },
                                        color = Color(0xFF2563EB),
                                        trackColor = Color(0xFFE2E8F0),
                                        modifier = Modifier.fillMaxWidth().height(4.dp)
                                    )
                                    Text(
                                        text = "খোঁজা হচ্ছে... ${convertToBengaliDigits((searchProgress * 100).toInt().toString())}%",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    } else {
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
                                            text = "পৃষ্ঠা: ${convertToBengaliDigits(visiblePage.toString())} / ${convertToBengaliDigits(pageCount.toString())} (ট্যাপ করে লাফ দিন)",
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
                                    // Favorite Star
                                    IconButton(
                                        onClick = {
                                            PdfHistoryManager.toggleFavorite(context, title, file.absolutePath, url)
                                            isFavorite = !isFavorite
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "Favorite PDF",
                                            tint = if (isFavorite) Color(0xFFEAB308) else themeColors.textColor.copy(alpha = 0.7f)
                                        )
                                    }

                                    // Bookmark current page
                                    IconButton(onClick = { showBookmarkDialog = true }) {
                                        Icon(Icons.Default.BookmarkBorder, contentDescription = "Bookmark current page", tint = themeColors.textColor.copy(alpha = 0.7f))
                                    }

                                    // Toggle Fit Mode
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
                                    
                                    // Toggle Continuous vs Single Page Horizontal Mode
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

                                    // Text Search Button
                                    IconButton(onClick = { isSearchActive = true }) {
                                        Icon(Icons.Default.Search, contentDescription = "Search text", tint = themeColors.textColor)
                                    }

                                    // Edit Annotations Toggle Button
                                    IconButton(onClick = { isEditMode = !isEditMode }) {
                                        Icon(
                                            imageVector = if (isEditMode) Icons.Default.EditOff else Icons.Default.Edit,
                                            contentDescription = "Edit Annotations",
                                            tint = if (isEditMode) Color(0xFF2563EB) else themeColors.textColor
                                        )
                                    }

                                    // Settings Button
                                    IconButton(onClick = { showSettingsDialog = true }) {
                                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = themeColors.textColor)
                                    }

                                    // Sidebar Menu Button
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
                                isCorrupted -> "পিডিএফ ফাইলটির ফরম্যাট সঠিক নয় বা ডাউনলোডের সময় ফাইলটি ড্যামেজ হয়ে গেছে। আপনি চাইলে ব্রাউজারে ট্রাই করতে পারেন।"
                                else -> "এটি একটি ড্রাইভ, মেগা বা ফেসবুক লিংক হতে পারে যা সরাসরি অ্যাপে ভিউ করা যায় না। আপনি সরাসরি ব্রাউজারে এটি দেখতে বা ডাউনলোড করতে পারেন।"
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
                                    Text("ব্রাউজারে ভিউ ও ডাউনলোড করুন", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        if (isEditMode) {
                            Surface(
                                color = Color(0xFFF8FAFC),
                                tonalElevation = 4.dp,
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        val tools = listOf(
                                            Triple("draw", Icons.Default.Gesture, "অঙ্কন"),
                                            Triple("highlight", Icons.Default.BorderColor, "হাইলাইট"),
                                            Triple("underline", Icons.Default.FormatUnderlined, "আন্ডারলাইন"),
                                            Triple("strikethrough", Icons.Default.FormatStrikethrough, "স্ট্রাইক"),
                                            Triple("rectangle", Icons.Default.CheckBoxOutlineBlank, "বক্স"),
                                            Triple("circle", Icons.Default.RadioButtonUnchecked, "বৃত্ত"),
                                            Triple("line", Icons.Default.HorizontalRule, "লাইন"),
                                            Triple("arrow", Icons.Default.TrendingFlat, "তীর"),
                                            Triple("text", Icons.Default.TextFields, "টেক্সট"),
                                            Triple("sticky", Icons.Default.RateReview, "নোট")
                                        )
                                        
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            items(tools) { (tool, icon, label) ->
                                                val selected = activeTool == tool
                                                FilterChip(
                                                    selected = selected,
                                                    onClick = { activeTool = tool },
                                                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                    leadingIcon = { Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp)) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = Color(0xFFDBEAFE),
                                                        selectedLabelColor = Color(0xFF1E40AF),
                                                        selectedLeadingIconColor = Color(0xFF1E40AF)
                                                    )
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.width(8.dp))
                                        
                                        IconButton(
                                            onClick = { undo() },
                                            enabled = undoStack.isNotEmpty(),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Undo, 
                                                contentDescription = "Undo", 
                                                tint = if (undoStack.isNotEmpty()) Color(0xFF1E293B) else Color(0xFF94A3B8),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        
                                        IconButton(
                                            onClick = { redo() },
                                            enabled = redoStack.isNotEmpty(),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Redo, 
                                                contentDescription = "Redo", 
                                                tint = if (redoStack.isNotEmpty()) Color(0xFF1E293B) else Color(0xFF94A3B8),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val colors = listOf(
                                            Color.Yellow to 0xFFFACC15,
                                            Color(0xFF4ADE80) to 0xFF4ADE80,
                                            Color(0xFF60A5FA) to 0xFF60A5FA,
                                            Color(0xFFF87171) to 0xFFF87171,
                                            Color.Black to 0xFF000000,
                                            Color(0xFF8B5CF6) to 0xFF8B5CF6
                                        )
                                        
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            colors.forEach { (c, hex) ->
                                                val selected = activeColor.value == c.value
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(c, CircleShape)
                                                        .clickable { activeColor = c }
                                                        .border(
                                                            width = if (selected) 2.dp else 1.dp,
                                                            color = if (selected) Color(0xFF1E293B) else Color.White,
                                                            shape = CircleShape
                                                        )
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.width(16.dp))
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("সাইজ: ", fontSize = 11.sp, color = Color(0xFF475569))
                                            Slider(
                                                value = strokeWidth,
                                                onValueChange = { strokeWidth = it },
                                                valueRange = 1f..25f,
                                                modifier = Modifier.weight(1f).height(24.dp),
                                                colors = SliderDefaults.colors(
                                                    activeTrackColor = Color(0xFF2563EB),
                                                    thumbColor = Color(0xFF2563EB)
                                                )
                                            )
                                            Text("${strokeWidth.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.width(20.dp))
                                        }
                                        
                                        Spacer(modifier = Modifier.width(8.dp))
                                        
                                        TextButton(
                                            onClick = {
                                                if (annotationsList.isNotEmpty()) {
                                                    updateAnnotations(emptyList())
                                                }
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("সব মুছুন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
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
                                        searchQuery = searchQuery,
                                        searchResults = searchResults,
                                        currentMatchIndex = currentMatchIndex,
                                        isEditMode = isEditMode,
                                        activeTool = activeTool,
                                        activeColor = activeColor,
                                        strokeWidth = strokeWidth,
                                        annotationsList = annotationsList,
                                        onAddAnnotation = { newAnn ->
                                            val exists = annotationsList.any { it.id == newAnn.id }
                                            val updatedList = if (exists) {
                                                annotationsList.map { if (it.id == newAnn.id) newAnn else it }
                                            } else {
                                                annotationsList + newAnn
                                            }
                                            updateAnnotations(updatedList)
                                        },
                                        onDeleteAnnotation = { annId ->
                                            val updatedList = annotationsList.filterNot { it.id == annId }
                                            updateAnnotations(updatedList)
                                        },
                                        pdfTheme = pdfTheme,
                                        isHighContrastMode = isHighContrastMode
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
                                            searchQuery = searchQuery,
                                            searchResults = searchResults,
                                            currentMatchIndex = currentMatchIndex,
                                            isEditMode = isEditMode,
                                            activeTool = activeTool,
                                            activeColor = activeColor,
                                            strokeWidth = strokeWidth,
                                            annotationsList = annotationsList,
                                            onAddAnnotation = { newAnn ->
                                                val exists = annotationsList.any { it.id == newAnn.id }
                                                val updatedList = if (exists) {
                                                    annotationsList.map { if (it.id == newAnn.id) newAnn else it }
                                                } else {
                                                    annotationsList + newAnn
                                                }
                                                updateAnnotations(updatedList)
                                            },
                                            onDeleteAnnotation = { annId ->
                                                val updatedList = annotationsList.filterNot { it.id == annId }
                                                updateAnnotations(updatedList)
                                            },
                                            pdfTheme = pdfTheme,
                                            isHighContrastMode = isHighContrastMode
                                        )
                                    }
                                }
                            }

                        // Last reading position toast overlay
                        androidx.compose.animation.AnimatedVisibility(
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

            // Beautiful Sidebar Panel overlay (custom Navigation Drawer effect)
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
                        .width(290.dp)
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
                                text = "নেভিগেশন ও বুকমার্ক",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            IconButton(onClick = { isSidebarOpen = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close navigation", tint = Color(0xFF64748B))
                            }
                        }

                        // TabRow inside Sidebar
                        TabRow(
                            selectedTabIndex = activeSidebarTab,
                            containerColor = Color.White,
                            contentColor = Color(0xFF2563EB)
                        ) {
                            Tab(
                                selected = activeSidebarTab == 0,
                                onClick = { activeSidebarTab = 0 },
                                text = { Text("পৃষ্ঠাসমূহ", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                            )
                            Tab(
                                selected = activeSidebarTab == 1,
                                onClick = { activeSidebarTab = 1 },
                                text = { Text("বুকমার্ক (${bookmarksList.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFF1F5F9))
                        ) {
                            if (activeSidebarTab == 0) {
                                // Dynamic Thumbnail List with low memory rendering
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(pageCount) { idx ->
                                        ThumbnailSidebarItem(
                                            renderManager = renderManager!!,
                                            pageIndex = idx,
                                            isSelected = visiblePage - 1 == idx,
                                            onClick = {
                                                isSidebarOpen = false
                                                scope.launch {
                                                    if (isHorizontalMode) {
                                                        pagerState.scrollToPage(idx)
                                                    } else {
                                                        lazyListState.scrollToItem(idx)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            } else {
                                // User Bookmarks List
                                if (bookmarksList.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "কোনো বুকমার্ক যোগ করা হয়নি। উপরে বুকমার্ক আইকন ট্যাপ করে যুক্ত করুন।",
                                            color = Color(0xFF64748B),
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(bookmarksList.size) { i ->
                                            val bookmark = bookmarksList[i]
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        isSidebarOpen = false
                                                        scope.launch {
                                                            if (isHorizontalMode) {
                                                                pagerState.scrollToPage(bookmark.pageIndex)
                                                            } else {
                                                                lazyListState.scrollToItem(bookmark.pageIndex)
                                                            }
                                                        }
                                                    },
                                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "পৃষ্ঠা ${convertToBengaliDigits((bookmark.pageIndex + 1).toString())}",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = Color(0xFF2563EB)
                                                        )
                                                        if (bookmark.note.isNotBlank()) {
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = bookmark.note,
                                                                fontSize = 12.sp,
                                                                color = Color(0xFF334155),
                                                                maxLines = 2
                                                            )
                                                        }
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            PdfHistoryManager.deleteBookmark(context, pdfKey, bookmark.id)
                                                            bookmarksList = PdfHistoryManager.getBookmarks(context, pdfKey)
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Outlined.Delete, contentDescription = "Delete Bookmark", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
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
        }
    }
}

    // Custom Page Jump Dialog
    if (showJumpDialog && pageCount > 0) {
        var jumpInput by remember { mutableStateOf("") }
        var jumpError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("পৃষ্ঠায় লাফ দিন (Jump to Page)", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    Text(
                        text = "১ থেকে ${convertToBengaliDigits(pageCount.toString())} এর মধ্যে পৃষ্ঠা নম্বর লিখুন:",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = jumpInput,
                        onValueChange = {
                            jumpInput = it
                            jumpError = false
                        },
                        placeholder = { Text("উদাঃ ৫") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = jumpError,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                    if (jumpError) {
                        Text("অনুগ্রহ করে সঠিক পৃষ্ঠা নম্বর লিখুন!", color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pg = jumpInput.trim().toIntOrNull()
                        if (pg != null && pg in 1..pageCount) {
                            showJumpDialog = false
                            scope.launch {
                                if (isHorizontalMode) {
                                    pagerState.scrollToPage(pg - 1)
                                } else {
                                    lazyListState.scrollToItem(pg - 1)
                                }
                            }
                        } else {
                            jumpError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("নিশ্চিত করুন", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("বাতিল", color = Color.Gray)
                }
            }
        )
    }

    // Custom Add Bookmark Dialog
    if (showBookmarkDialog && pageCount > 0) {
        var bookmarkNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showBookmarkDialog = false },
            title = { Text("বুকমার্ক যুক্ত করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    Text(
                        text = "পৃষ্ঠা ${convertToBengaliDigits(visiblePage.toString())}-এ বুকমার্ক করার জন্য একটি সংক্ষিপ্ত বিবরণ লিখুন:",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = bookmarkNote,
                        onValueChange = { bookmarkNote = it },
                        placeholder = { Text("উদাঃ সূত্রসমূহ / গুরুত্বপূর্ণ টপিক") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        PdfHistoryManager.addBookmark(context, pdfKey, visiblePage - 1, bookmarkNote)
                        bookmarksList = PdfHistoryManager.getBookmarks(context, pdfKey)
                        showBookmarkDialog = false
                        android.widget.Toast.makeText(context, "পৃষ্ঠা ${visiblePage} বুকমার্ক করা হয়েছে!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("যুক্ত করুন", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBookmarkDialog = false }) {
                    Text("বাতিল", color = Color.Gray)
                }
            }
        )
    }

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
                    
                    // 5. Actions: Share, Print, Download
                    item {
                        Column {
                            Text(
                                text = "ফাইল অ্যাকশন (File Actions)",
                                fontWeight = FontWeight.Bold,
                                fontSize = getFontSize(13f, isLargeTextMode),
                                color = Color(0xFF3B82F6)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Share
                                Button(
                                    onClick = { sharePdfFile(context, file, title) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Share, contentDescription = "Share PDF", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("শেয়ার", fontSize = getFontSize(11f, isLargeTextMode), fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                // Print
                                Button(
                                    onClick = { printPdfFile(context, file, title) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Print, contentDescription = "Print PDF", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("প্রিন্ট", fontSize = getFontSize(11f, isLargeTextMode), fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                // Download
                                Button(
                                    onClick = { downloadPdfFile(context, file, title) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Download, contentDescription = "Download PDF", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("ডাউনলোড", fontSize = getFontSize(11f, isLargeTextMode), fontWeight = FontWeight.Bold)
                                    }
                                }
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

@Composable
fun ThumbnailSidebarItem(
    renderManager: PdfRenderManager,
    pageIndex: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

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
    searchQuery: String = "",
    searchResults: List<SearchResultRect> = emptyList(),
    currentMatchIndex: Int = -1,
    isEditMode: Boolean = false,
    activeTool: String = "draw",
    activeColor: Color = Color.Yellow,
    strokeWidth: Float = 5f,
    annotationsList: List<PdfAnnotation> = emptyList(),
    onAddAnnotation: (PdfAnnotation) -> Unit = {},
    onDeleteAnnotation: (String) -> Unit = {},
    pdfTheme: PdfTheme = PdfTheme.Light,
    isHighContrastMode: Boolean = false
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    val themeColorFilter = remember(pdfTheme, isHighContrastMode) {
        val matrix = when {
            isHighContrastMode -> {
                // High contrast monochrome
                floatArrayOf(
                    2.0f,  0.0f,  0.0f, 0.0f, -120.0f,
                    0.0f,  2.0f,  0.0f, 0.0f, -120.0f,
                    0.0f,  0.0f,  2.0f, 0.0f, -120.0f,
                    0.0f,  0.0f,  0.0f, 1.0f,    0.0f
                )
            }
            pdfTheme == PdfTheme.Dark || pdfTheme == PdfTheme.Night -> {
                // Invert colors for dark background
                floatArrayOf(
                    -1.0f,  0.0f,  0.0f, 0.0f, 255.0f,
                     0.0f, -1.0f,  0.0f, 0.0f, 255.0f,
                     0.0f,  0.0f, -1.0f, 0.0f, 255.0f,
                     0.0f,  0.0f,  0.0f, 1.0f,   0.0f
                )
            }
            pdfTheme == PdfTheme.Sepia -> {
                // Warm sepia tones
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

    // Dynamic zoom states for buttery smooth animations
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

    // Current active drawing path/points or shape bounds
    var currentPathPoints by remember { mutableStateOf<List<androidx.compose.ui.geometry.Offset>>(emptyList()) }
    var currentStartPoint by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    var currentEndPoint by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    
    // Text annotation state
    var showTextDialog by remember { mutableStateOf(false) }
    var textInputText by remember { mutableStateOf("") }
    var textInputPosition by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    
    // Sticky note state
    var showStickyDialog by remember { mutableStateOf(false) }
    var stickyInputText by remember { mutableStateOf("") }
    var stickyInputPosition by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }

    // Renders/loads the specific PDF page on a background thread using the manager
    LaunchedEffect(pageIndex, renderManager, bitmap) {
        if (bitmap == null || bitmap!!.isRecycled) {
            withContext(Dispatchers.IO) {
                bitmap = renderManager.getPage(pageIndex)
            }
        }
    }

    // Touch transforms
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        // Constrain scale between 1x and 6x
        scale = (scale * zoomChange).coerceIn(1f, 6f)
        if (scale > 1f) {
            offset += offsetChange * scale
        } else {
            offset = androidx.compose.ui.geometry.Offset.Zero
        }
    }

    Card(
        modifier = modifier
            .pointerInput(isEditMode) {
                if (!isEditMode) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offset = androidx.compose.ui.geometry.Offset.Zero
                            } else {
                                scale = 2.5f
                            }
                        },
                        onLongPress = {
                            if (scale > 1f) {
                                scale = 1f
                                offset = androidx.compose.ui.geometry.Offset.Zero
                            } else {
                                scale = 4.5f
                            }
                        }
                    )
                }
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
                    .transformable(state = transformState, enabled = !isEditMode),
                contentAlignment = Alignment.Center
            ) {
                val density = androidx.compose.ui.platform.LocalDensity.current
                val pageOrigWidth = remember(pageIndex, renderManager) { renderManager.getPageWidth(pageIndex).toFloat() }
                val pageOrigHeight = remember(pageIndex, renderManager) { renderManager.getPageHeight(pageIndex).toFloat() }
                
                val gestureModifier = if (isEditMode) {
                    Modifier.pointerInput(activeTool) {
                        detectDragGestures(
                            onDragStart = { pointer ->
                                currentStartPoint = pointer
                                currentEndPoint = pointer
                                if (activeTool == "draw") {
                                    currentPathPoints = listOf(pointer)
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val current = change.position
                                currentEndPoint = current
                                if (activeTool == "draw") {
                                    currentPathPoints = currentPathPoints + current
                                }
                            },
                            onDragEnd = {
                                val start = currentStartPoint
                                val end = currentEndPoint
                                if (start != null && end != null) {
                                    val localWidth = size.width.toFloat()
                                    val localHeight = size.height.toFloat()
                                    
                                    val relativeStart = PointF(start.x / localWidth, start.y / localHeight)
                                    val relativeEnd = PointF(end.x / localWidth, end.y / localHeight)
                                    
                                    val id = java.util.UUID.randomUUID().toString()
                                    
                                    when (activeTool) {
                                        "draw" -> {
                                            if (currentPathPoints.size > 1) {
                                                val relPoints = currentPathPoints.map { PointData(it.x / localWidth, it.y / localHeight) }
                                                onAddAnnotation(
                                                    PdfAnnotation(
                                                        id = id,
                                                        pageIndex = pageIndex,
                                                        type = "draw",
                                                        color = activeColor.toArgb(),
                                                        strokeWidth = strokeWidth,
                                                        points = relPoints
                                                    )
                                                )
                                            }
                                        }
                                        "highlight", "underline", "strikethrough", "rectangle", "circle", "line", "arrow" -> {
                                            onAddAnnotation(
                                                PdfAnnotation(
                                                    id = id,
                                                    pageIndex = pageIndex,
                                                    type = activeTool,
                                                    color = activeColor.toArgb(),
                                                    strokeWidth = strokeWidth,
                                                    startX = relativeStart.x,
                                                    startY = relativeStart.y,
                                                    endX = relativeEnd.x,
                                                    endY = relativeEnd.y
                                                )
                                            )
                                        }
                                    }
                                }
                                currentStartPoint = null
                                currentEndPoint = null
                                currentPathPoints = emptyList()
                            },
                            onDragCancel = {
                                currentStartPoint = null
                                currentEndPoint = null
                                currentPathPoints = emptyList()
                            }
                        )
                    }.pointerInput(activeTool) {
                        detectTapGestures(
                            onTap = { pointer ->
                                val localWidth = size.width.toFloat()
                                val localHeight = size.height.toFloat()
                                val relPoint = PointF(pointer.x / localWidth, pointer.y / localHeight)
                                
                                val tappedAnn = annotationsList.firstOrNull { ann ->
                                    ann.pageIndex == pageIndex && isAnnotationTapped(ann, relPoint)
                                }
                                if (tappedAnn != null) {
                                    onDeleteAnnotation(tappedAnn.id)
                                    android.widget.Toast.makeText(context, "টীকা মুছে ফেলা হয়েছে", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    if (activeTool == "text") {
                                        textInputPosition = pointer
                                        textInputText = ""
                                        showTextDialog = true
                                    } else if (activeTool == "sticky") {
                                        stickyInputPosition = pointer
                                        stickyInputText = ""
                                        showStickyDialog = true
                                    }
                                }
                            }
                        )
                    }
                } else {
                    Modifier
                }

                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = animatedScale,
                            scaleY = animatedScale,
                            translationX = animatedOffsetX,
                            translationY = animatedOffsetY
                        ),
                    contentScale = if (fitMode == PdfFitMode.FitWidth) ContentScale.FillWidth else ContentScale.Fit,
                    colorFilter = themeColorFilter
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = animatedScale,
                            scaleY = animatedScale,
                            translationX = animatedOffsetX,
                            translationY = animatedOffsetY
                        )
                        .then(gestureModifier)
                ) {
                    val w = size.width
                    val h = size.height
                    
                    // Draw saved annotations
                    annotationsList.filter { it.pageIndex == pageIndex }.forEach { ann ->
                        val paintColor = Color(ann.color)
                        val stroke = ann.strokeWidth
                        
                        when (ann.type) {
                            "draw" -> {
                                if (ann.points.size > 1) {
                                    val path = androidx.compose.ui.graphics.Path()
                                    path.moveTo(ann.points[0].x * w, ann.points[0].y * h)
                                    for (i in 1 until ann.points.size) {
                                        path.lineTo(ann.points[i].x * w, ann.points[i].y * h)
                                    }
                                    drawPath(
                                        path = path,
                                        color = paintColor,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = stroke,
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                                        )
                                    )
                                }
                            }
                            "highlight" -> {
                                val yVal = ann.startY * h
                                drawLine(
                                    color = paintColor.copy(alpha = 0.4f),
                                    start = androidx.compose.ui.geometry.Offset(ann.startX * w, yVal),
                                    end = androidx.compose.ui.geometry.Offset(ann.endX * w, yVal),
                                    strokeWidth = stroke * 3f,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Square
                                )
                            }
                            "underline" -> {
                                val yVal = ann.startY * h + stroke
                                drawLine(
                                    color = paintColor,
                                    start = androidx.compose.ui.geometry.Offset(ann.startX * w, yVal),
                                    end = androidx.compose.ui.geometry.Offset(ann.endX * w, yVal),
                                    strokeWidth = stroke,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            }
                            "strikethrough" -> {
                                val yVal = ann.startY * h
                                drawLine(
                                    color = paintColor,
                                    start = androidx.compose.ui.geometry.Offset(ann.startX * w, yVal),
                                    end = androidx.compose.ui.geometry.Offset(ann.endX * w, yVal),
                                    strokeWidth = stroke,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            }
                            "rectangle" -> {
                                val x1 = minOf(ann.startX, ann.endX)
                                val y1 = minOf(ann.startY, ann.endY)
                                val x2 = maxOf(ann.startX, ann.endX)
                                val y2 = maxOf(ann.startY, ann.endY)
                                drawRect(
                                    color = paintColor,
                                    topLeft = androidx.compose.ui.geometry.Offset(x1 * w, y1 * h),
                                    size = androidx.compose.ui.geometry.Size((x2 - x1) * w, (y2 - y1) * h),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                                )
                            }
                            "circle" -> {
                                val x1 = minOf(ann.startX, ann.endX)
                                val y1 = minOf(ann.startY, ann.endY)
                                val x2 = maxOf(ann.startX, ann.endX)
                                val y2 = maxOf(ann.startY, ann.endY)
                                drawOval(
                                    color = paintColor,
                                    topLeft = androidx.compose.ui.geometry.Offset(x1 * w, y1 * h),
                                    size = androidx.compose.ui.geometry.Size((x2 - x1) * w, (y2 - y1) * h),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                                )
                            }
                            "line" -> {
                                drawLine(
                                    color = paintColor,
                                    start = androidx.compose.ui.geometry.Offset(ann.startX * w, ann.startY * h),
                                    end = androidx.compose.ui.geometry.Offset(ann.endX * w, ann.endY * h),
                                    strokeWidth = stroke,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            }
                            "arrow" -> {
                                val sX = ann.startX * w
                                val sY = ann.startY * h
                                val eX = ann.endX * w
                                val eY = ann.endY * h
                                
                                drawLine(
                                    color = paintColor,
                                    start = androidx.compose.ui.geometry.Offset(sX, sY),
                                    end = androidx.compose.ui.geometry.Offset(eX, eY),
                                    strokeWidth = stroke,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                
                                val angle = Math.atan2((eY - sY).toDouble(), (eX - sX).toDouble())
                                val arrowLength = stroke * 4f
                                val arrowAngle = Math.PI / 6.0
                                
                                val x1 = eX - arrowLength * Math.cos(angle - arrowAngle)
                                val y1 = eY - arrowLength * Math.sin(angle - arrowAngle)
                                val x2 = eX - arrowLength * Math.cos(angle + arrowAngle)
                                val y2 = eY - arrowLength * Math.sin(angle + arrowAngle)
                                
                                drawLine(
                                    color = paintColor,
                                    start = androidx.compose.ui.geometry.Offset(eX, eY),
                                    end = androidx.compose.ui.geometry.Offset(x1.toFloat(), y1.toFloat()),
                                    strokeWidth = stroke,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                drawLine(
                                    color = paintColor,
                                    start = androidx.compose.ui.geometry.Offset(eX, eY),
                                    end = androidx.compose.ui.geometry.Offset(x2.toFloat(), y2.toFloat()),
                                    strokeWidth = stroke,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            }
                        }
                    }
                    
                    // Draw active drag gesture path
                    val st = currentStartPoint
                    val en = currentEndPoint
                    if (st != null && en != null) {
                        val activeStroke = strokeWidth
                        val activeColorPaint = activeColor
                        
                        when (activeTool) {
                            "draw" -> {
                                if (currentPathPoints.size > 1) {
                                    val path = androidx.compose.ui.graphics.Path()
                                    path.moveTo(currentPathPoints[0].x, currentPathPoints[0].y)
                                    for (i in 1 until currentPathPoints.size) {
                                        path.lineTo(currentPathPoints[i].x, currentPathPoints[i].y)
                                    }
                                    drawPath(
                                        path = path,
                                        color = activeColorPaint,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = activeStroke,
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                                        )
                                    )
                                }
                            }
                            "highlight" -> {
                                drawLine(
                                    color = activeColorPaint.copy(alpha = 0.4f),
                                    start = androidx.compose.ui.geometry.Offset(st.x, st.y),
                                    end = androidx.compose.ui.geometry.Offset(en.x, st.y),
                                    strokeWidth = activeStroke * 3f,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Square
                                )
                            }
                            "underline" -> {
                                drawLine(
                                    color = activeColorPaint,
                                    start = androidx.compose.ui.geometry.Offset(st.x, st.y + activeStroke),
                                    end = androidx.compose.ui.geometry.Offset(en.x, st.y + activeStroke),
                                    strokeWidth = activeStroke,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            }
                            "strikethrough" -> {
                                drawLine(
                                    color = activeColorPaint,
                                    start = androidx.compose.ui.geometry.Offset(st.x, st.y),
                                    end = androidx.compose.ui.geometry.Offset(en.x, st.y),
                                    strokeWidth = activeStroke,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            }
                            "rectangle" -> {
                                val x1 = minOf(st.x, en.x)
                                val y1 = minOf(st.y, en.y)
                                val x2 = maxOf(st.x, en.x)
                                val y2 = maxOf(st.y, en.y)
                                drawRect(
                                    color = activeColorPaint,
                                    topLeft = androidx.compose.ui.geometry.Offset(x1, y1),
                                    size = androidx.compose.ui.geometry.Size(x2 - x1, y2 - y1),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = activeStroke)
                                )
                            }
                            "circle" -> {
                                val x1 = minOf(st.x, en.x)
                                val y1 = minOf(st.y, en.y)
                                val x2 = maxOf(st.x, en.x)
                                val y2 = maxOf(st.y, en.y)
                                drawOval(
                                    color = activeColorPaint,
                                    topLeft = androidx.compose.ui.geometry.Offset(x1, y1),
                                    size = androidx.compose.ui.geometry.Size(x2 - x1, y2 - y1),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = activeStroke)
                                )
                            }
                            "line" -> {
                                drawLine(
                                    color = activeColorPaint,
                                    start = androidx.compose.ui.geometry.Offset(st.x, st.y),
                                    end = androidx.compose.ui.geometry.Offset(en.x, en.y),
                                    strokeWidth = activeStroke,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            }
                            "arrow" -> {
                                drawLine(
                                    color = activeColorPaint,
                                    start = androidx.compose.ui.geometry.Offset(st.x, st.y),
                                    end = androidx.compose.ui.geometry.Offset(en.x, en.y),
                                    strokeWidth = activeStroke,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                val angle = Math.atan2((en.y - st.y).toDouble(), (en.x - st.x).toDouble())
                                val arrowLength = activeStroke * 4f
                                val arrowAngle = Math.PI / 6.0
                                
                                val x1 = en.x - arrowLength * Math.cos(angle - arrowAngle)
                                val y1 = en.y - arrowLength * Math.sin(angle - arrowAngle)
                                val x2 = en.x - arrowLength * Math.cos(angle + arrowAngle)
                                val y2 = en.y - arrowLength * Math.sin(angle + arrowAngle)
                                
                                drawLine(
                                    color = activeColorPaint,
                                    start = androidx.compose.ui.geometry.Offset(en.x, en.y),
                                    end = androidx.compose.ui.geometry.Offset(x1.toFloat(), y1.toFloat()),
                                    strokeWidth = activeStroke,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                drawLine(
                                    color = activeColorPaint,
                                    start = androidx.compose.ui.geometry.Offset(en.x, en.y),
                                    end = androidx.compose.ui.geometry.Offset(x2.toFloat(), y2.toFloat()),
                                    strokeWidth = activeStroke,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            }
                        }
                    }
                    
                    // Render Search Result Highlights!
                    if (searchQuery.isNotEmpty()) {
                        searchResults.filter { it.pageIndex == pageIndex }.forEach { match ->
                            val isFocused = (match == searchResults.getOrNull(currentMatchIndex))
                            val highlightColor = if (isFocused) Color(0xFFFF5722).copy(alpha = 0.5f) else Color(0xFFFACC15).copy(alpha = 0.45f)
                            
                            val pageW = if (pageOrigWidth > 0f) pageOrigWidth else 1f
                            val pageH = if (pageOrigHeight > 0f) pageOrigHeight else 1f
                            
                            val relLeft = match.x / pageW
                            val relTop = match.y / pageH
                            val relWidth = match.width / pageW
                            val relHeight = match.height / pageH
                            
                            val left = relLeft * w
                            val top = relTop * h
                            val width = relWidth * w
                            val height = relHeight * h
                            
                            drawRect(
                                color = highlightColor,
                                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                                size = androidx.compose.ui.geometry.Size(width, height)
                            )
                        }
                    }
                }

                // Overlaid Text and Sticky Note elements
                annotationsList.filter { it.pageIndex == pageIndex && (it.type == "text" || it.type == "sticky") }.forEach { ann ->
                    val x = ann.startX
                    val y = ann.startY
                    val xDp = with(density) { (x * constraints.maxWidth).toDp() }
                    val yDp = with(density) { (y * constraints.maxHeight).toDp() }
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = xDp, y = yDp)
                            .graphicsLayer(
                                scaleX = animatedScale,
                                scaleY = animatedScale,
                                translationX = animatedOffsetX,
                                translationY = animatedOffsetY
                            )
                    ) {
                        if (ann.type == "text") {
                            Text(
                                text = ann.text,
                                color = Color(ann.color),
                                fontSize = (12f * (ann.strokeWidth / 5f).coerceIn(0.5f, 3f)).sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .let {
                                        if (isEditMode) {
                                            it.border(1.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                              .clickable { onDeleteAnnotation(ann.id) }
                                        } else {
                                            it
                                        }
                                    }
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.RateReview,
                                contentDescription = "Sticky Note",
                                tint = Color(ann.color),
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color.White, CircleShape)
                                    .border(1.dp, Color(ann.color), CircleShape)
                                    .padding(4.dp)
                                    .clickable {
                                        android.widget.Toast.makeText(context, ann.text, android.widget.Toast.LENGTH_LONG).show()
                                        if (isEditMode) {
                                            onDeleteAnnotation(ann.id)
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp, color = Color(0xFF64748B))
            }
        }
    }

    if (showTextDialog) {
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = { Text("টেক্সট নোট যোগ করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = textInputText,
                    onValueChange = { textInputText = it },
                    placeholder = { Text("আপনার লেখাটি লিখুন...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pos = textInputPosition
                        if (pos != null && textInputText.isNotBlank()) {
                            val localWidth = bitmap?.width?.toFloat() ?: 1f
                            val localHeight = bitmap?.height?.toFloat() ?: 1f
                            onAddAnnotation(
                                PdfAnnotation(
                                    id = java.util.UUID.randomUUID().toString(),
                                    pageIndex = pageIndex,
                                    type = "text",
                                    color = activeColor.toArgb(),
                                    strokeWidth = strokeWidth,
                                    startX = pos.x / localWidth,
                                    startY = pos.y / localHeight,
                                    text = textInputText
                                )
                            )
                        }
                        showTextDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("যুক্ত করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextDialog = false }) {
                    Text("বাতিল", color = Color.Gray)
                }
            }
        )
    }

    if (showStickyDialog) {
        AlertDialog(
            onDismissRequest = { showStickyDialog = false },
            title = { Text("স্টিকি নোট যোগ করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = stickyInputText,
                    onValueChange = { stickyInputText = it },
                    placeholder = { Text("আপনার নোটটি লিখুন...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pos = stickyInputPosition
                        if (pos != null && stickyInputText.isNotBlank()) {
                            val localWidth = bitmap?.width?.toFloat() ?: 1f
                            val localHeight = bitmap?.height?.toFloat() ?: 1f
                            onAddAnnotation(
                                PdfAnnotation(
                                    id = java.util.UUID.randomUUID().toString(),
                                    pageIndex = pageIndex,
                                    type = "sticky",
                                    color = activeColor.toArgb(),
                                    strokeWidth = strokeWidth,
                                    startX = pos.x / localWidth,
                                    startY = pos.y / localHeight,
                                    text = stickyInputText
                                )
                            )
                        }
                        showStickyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("যুক্ত করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStickyDialog = false }) {
                    Text("বাতিল", color = Color.Gray)
                }
            }
        )
    }
}

fun isAnnotationTapped(ann: PdfAnnotation, tap: PointF): Boolean {
    val threshold = 0.05f
    when (ann.type) {
        "text", "sticky" -> {
            return Math.hypot((ann.startX - tap.x).toDouble(), (ann.startY - tap.y).toDouble()) < threshold
        }
        "line", "arrow", "highlight", "underline", "strikethrough" -> {
            val p1 = PointF(ann.startX, ann.startY)
            val p2 = PointF(ann.endX, ann.endY)
            return distanceToSegment(tap, p1, p2) < threshold
        }
        "rectangle", "circle" -> {
            val x1 = minOf(ann.startX, ann.endX)
            val y1 = minOf(ann.startY, ann.endY)
            val x2 = maxOf(ann.startX, ann.endX)
            val y2 = maxOf(ann.startY, ann.endY)
            return tap.x >= x1 - threshold && tap.x <= x2 + threshold &&
                   tap.y >= y1 - threshold && tap.y <= y2 + threshold
        }
        "draw" -> {
            return ann.points.any { p ->
                Math.hypot((p.x - tap.x).toDouble(), (p.y - tap.y).toDouble()) < threshold
            }
        }
    }
    return false
}

fun distanceToSegment(p: PointF, v: PointF, w: PointF): Float {
    val l2 = (v.x - w.x) * (v.x - w.x) + (v.y - w.y) * (v.y - w.y)
    if (l2 == 0f) return Math.hypot((p.x - v.x).toDouble(), (p.y - v.y).toDouble()).toFloat()
    var t = ((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2
    t = maxOf(0f, minOf(1f, t))
    val projectionX = v.x + t * (w.x - v.x)
    val projectionY = v.y + t * (w.y - v.y)
    return Math.hypot((p.x - projectionX).toDouble(), (p.y - projectionY).toDouble()).toFloat()
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

fun sharePdfFile(context: Context, file: File, title: String) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, title)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "পিডিএফ শেয়ার করুন"))
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "শেয়ার করতে সমস্যা হয়েছে", android.widget.Toast.LENGTH_SHORT).show()
    }
}

fun printPdfFile(context: Context, file: File, title: String) {
    try {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? android.print.PrintManager
        if (printManager != null) {
            val jobName = "Shikkhaloy - $title"
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val printAdapter = object : android.print.PrintDocumentAdapter() {
                override fun onWrite(
                    pages: Array<out android.print.PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        val input = java.io.FileInputStream(pfd.fileDescriptor)
                        val output = java.io.FileOutputStream(destination?.fileDescriptor)
                        val buf = ByteArray(1024)
                        var bytesRead: Int
                        while (input.read(buf).also { bytesRead = it } > 0) {
                            output.write(buf, 0, bytesRead)
                        }
                        callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    } finally {
                        pfd.close()
                    }
                }

                override fun onLayout(
                    oldAttributes: android.print.PrintAttributes?,
                    newAttributes: android.print.PrintAttributes?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: android.os.Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = android.print.PrintDocumentInfo.Builder(title)
                        .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }
            }
            printManager.print(jobName, printAdapter, null)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "প্রিন্ট করতে সমস্যা হয়েছে", android.widget.Toast.LENGTH_SHORT).show()
    }
}

fun downloadPdfFile(context: Context, file: File, title: String) {
    try {
        val resolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "$title.pdf")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
        }
        val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { output ->
                file.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            android.widget.Toast.makeText(context, "ডাউনলোড সম্পন্ন হয়েছে (Downloads ফোল্ডারে দেখুন)", android.widget.Toast.LENGTH_LONG).show()
        } else {
            val destFile = File(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                "$title.pdf"
            )
            file.copyTo(destFile, overwrite = true)
            android.widget.Toast.makeText(context, "ডাউনলোড সম্পন্ন হয়েছে: ${destFile.absolutePath}", android.widget.Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "ডাউনলোড করতে সমস্যা হয়েছে", android.widget.Toast.LENGTH_SHORT).show()
    }
}
