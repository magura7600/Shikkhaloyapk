package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Clean, lightweight, and memory-safe PDF Renderer wrapper.
 */
class SimplePdfRenderer(private val file: File) {
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    var pageCount by mutableStateOf(0)
        private set

    init {
        try {
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor!!)
            pageCount = pdfRenderer?.pageCount ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Renders a page directly in ARGB_8888 format (required by native PdfRenderer).
     */
    fun renderPage(pageIndex: Int): Bitmap? {
        val renderer = pdfRenderer ?: return null
        synchronized(this) {
            if (pageIndex < 0 || pageIndex >= pageCount) return null
            var page: PdfRenderer.Page? = null
            try {
                page = renderer.openPage(pageIndex)
                // Use a standard clear rendering scale (1.5f provides crisp text without huge memory)
                val width = (page.width * 1.5f).toInt().coerceAtLeast(100)
                val height = (page.height * 1.5f).toInt().coerceAtLeast(100)
                
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return bitmap
            } catch (oom: OutOfMemoryError) {
                System.gc()
                return null
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            } finally {
                try {
                    page?.close()
                } catch (e: Exception) {}
            }
        }
    }

    fun close() {
        synchronized(this) {
            try {
                pdfRenderer?.close()
            } catch (e: Exception) {}
            try {
                fileDescriptor?.close()
            } catch (e: Exception) {}
            pdfRenderer = null
            fileDescriptor = null
        }
    }
}

/**
 * Bengali digits helper for localization.
 */
private fun toBengaliDigits(input: String): String {
    val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    val builder = StringBuilder()
    for (char in input) {
        if (char in '0'..'9') {
            builder.append(bengaliDigits[char - '0'])
        } else {
            builder.append(char)
        }
    }
    return builder.toString()
}

/**
 * Complete replacement for PdfViewerDialog.
 * Beautiful, ultra-simple, vertically scrollable pages with buttery rendering and custom double-tap + pinch-to-zoom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerDialog(
    file: File,
    title: String,
    url: String = "",
    onClose: () -> Unit
) {
    val context = LocalContext.current
    
    var renderer by remember(file) { mutableStateOf<SimplePdfRenderer?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var loadError by remember { mutableStateOf(false) }
    var isLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            try {
                if (!file.exists() || file.length() == 0L) {
                    throw Exception("File is empty or missing")
                }
                val r = SimplePdfRenderer(file)
                if (r.pageCount > 0) {
                    renderer = r
                    pageCount = r.pageCount
                    isLoaded = true
                } else {
                    loadError = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                loadError = true
            }
        }
    }

    DisposableEffect(renderer) {
        onDispose {
            renderer?.close()
        }
    }

    // High performance localized bitmap cache (retains last 8 pages for fast scrolling)
    val bitmapCache = remember { LruCache<Int, Bitmap>(8) }
    val lazyListState = rememberLazyListState()

    val visiblePageIndex by remember {
        derivedStateOf {
            val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) 0 else visibleItems.first().index
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
        // Force full screen window
        val dialogWindow = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        LaunchedEffect(dialogWindow) {
            dialogWindow?.let { window ->
                window.setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                window.setBackgroundDrawableResource(android.R.color.transparent)
            }
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (pageCount > 0) {
                                Text(
                                    text = "পৃষ্ঠা: ${toBengaliDigits((visiblePageIndex + 1).toString())} / ${toBengaliDigits(pageCount.toString())}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            containerColor = Color(0xFFE2E8F0) // Clean slate-gray neutral backdrop to emphasize the white paper pages
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (loadError) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Corrupted File",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "পিডিএফ ভিউ করা যাচ্ছে না",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ফাইলটি ক্ষতিগ্রস্থ বা খালি হতে পারে।",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onClose,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("ফিরে যান")
                        }
                    }
                } else if (!isLoaded) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "পিডিএফ লোড হচ্ছে...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val activeRenderer = renderer
                    if (activeRenderer != null) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(pageCount) { index ->
                                PdfPageCard(
                                    renderer = activeRenderer,
                                    pageIndex = index,
                                    bitmapCache = bitmapCache
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Beautiful, reliable PDF Page container.
 * Renders on a worker thread and supports intuitive pinch-to-zoom & double tap zoom.
 */
@Composable
fun PdfPageCard(
    renderer: SimplePdfRenderer,
    pageIndex: Int,
    bitmapCache: LruCache<Int, Bitmap>
) {
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(bitmapCache.get(pageIndex)) }
    var isLoading by remember(pageIndex) { mutableStateOf(bitmap == null) }
    var hasError by remember(pageIndex) { mutableStateOf(false) }

    // Load page bitmap asynchronously
    LaunchedEffect(pageIndex) {
        if (bitmap == null) {
            isLoading = true
            hasError = false
            withContext(Dispatchers.IO) {
                try {
                    val rendered = renderer.renderPage(pageIndex)
                    if (rendered != null) {
                        bitmapCache.put(pageIndex, rendered)
                        withContext(Dispatchers.Main) {
                            bitmap = rendered
                            isLoading = false
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            hasError = true
                            isLoading = false
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        hasError = true
                        isLoading = false
                    }
                }
            }
        }
    }

    // Zoom and translation gestures
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val aspectRatio = if (bitmap != null) {
        bitmap!!.width.toFloat() / bitmap!!.height.toFloat()
    } else {
        0.707f // A4 standard ratio fallback
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .background(Color.White)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4f)
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                            
                            // Prevent dragging page completely off the screen
                            val maxOffsetX = (size.width * (scale - 1)) / 2
                            val maxOffsetY = (size.height * (scale - 1)) / 2
                            offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                            offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                }
                .pointerInput(Unit) {
                    // Quick double-tap zoom
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                scale = 2.5f
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                }
            } else if (hasError || bitmap == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "পৃষ্ঠাটি লোড করা যায়নি",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        ),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
