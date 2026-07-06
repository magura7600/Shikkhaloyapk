package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInBrowser
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * A highly reliable and thread-safe wrapper around native Android [PdfRenderer].
 * Safely handles memory constraints, scaling, and OOM fallbacks.
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
     * Renders a given PDF page to a Bitmap on a background thread.
     * Scale 1.5f provides sharp, readable text on most screen sizes.
     */
    fun renderPage(pageIndex: Int, scale: Float = 1.5f): Bitmap? {
        val renderer = pdfRenderer ?: return null
        synchronized(this) {
            if (pageIndex < 0 || pageIndex >= pageCount) return null
            var page: PdfRenderer.Page? = null
            try {
                page = renderer.openPage(pageIndex)
                val width = (page.width * scale).toInt().coerceAtLeast(100)
                val height = (page.height * scale).toInt().coerceAtLeast(100)
                
                var bitmap: Bitmap? = null
                try {
                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                } catch (oom: OutOfMemoryError) {
                    System.gc()
                    try {
                        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                    } catch (t: Throwable) {
                        // Fallback to low-res safe scaling if still struggling
                        val lowerWidth = (width * 0.6f).toInt().coerceAtLeast(100)
                        val lowerHeight = (height * 0.6f).toInt().coerceAtLeast(100)
                        bitmap = Bitmap.createBitmap(lowerWidth, lowerHeight, Bitmap.Config.RGB_565)
                    }
                }
                
                bitmap?.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap!!, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return bitmap
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
 * Standalone clean utility to map numeric strings to Bengali localized digits.
 */
private fun convertToBengaliDigitsLocal(input: String): String {
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
 * Beautifully simplified, modern full-screen PDF Viewer Dialog.
 * Light, fast, supports vertical smooth scrolling, pinch-to-zoom, and memory-safe caching.
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
                    throw Exception("File empty or does not exist")
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

    // Safely release system files and handles
    DisposableEffect(renderer) {
        onDispose {
            renderer?.close()
        }
    }

    // Keep memory cache of rendered pages to ensure smooth butter scrolling
    val bitmapCache = remember { LruCache<Int, Bitmap>(10) }
    val lazyListState = rememberLazyListState()

    // Determine the first visible page index dynamically
    val visiblePageIndex = remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) 0
            else visibleItems.first().index
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
            }
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                    ),
                    actions = {
                        if (url.isNotBlank()) {
                            IconButton(onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "ব্রাউজারে ওপেন করা যায়নি", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = "Open in browser",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
            },
            containerColor = Color(0xFFF1F5F9) // Clean slate theme background to highlight sheet pages
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
                            contentDescription = "Corrupted",
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
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
                                PdfPageItem(
                                    renderer = activeRenderer,
                                    pageIndex = index,
                                    bitmapCache = bitmapCache
                                )
                            }
                        }

                        // Floating Pill Indicator for current page index
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .shadow(8.dp, RoundedCornerShape(24.dp)),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "Page Index Indicator",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${convertToBengaliDigitsLocal((visiblePageIndex.value + 1).toString())} / ${convertToBengaliDigitsLocal(pageCount.toString())}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
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
 * Individual PDF Page Composable.
 * Features background rendering, tap-to-zoom, dynamic pinch to zoom, and memory caching.
 */
@Composable
fun PdfPageItem(
    renderer: SimplePdfRenderer,
    pageIndex: Int,
    bitmapCache: LruCache<Int, Bitmap>,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(bitmapCache.get(pageIndex)) }
    var isLoading by remember(pageIndex) { mutableStateOf(bitmap == null) }
    var hasError by remember(pageIndex) { mutableStateOf(false) }

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

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        offset += offsetChange
        if (scale == 1f) {
            offset = androidx.compose.ui.geometry.Offset.Zero
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (bitmap != null) bitmap!!.width.toFloat() / bitmap!!.height.toFloat() else 0.707f)
                .background(Color.White)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            scale = if (scale > 1f) 1f else 2f
                            offset = androidx.compose.ui.geometry.Offset.Zero
                        }
                    )
                }
                .transformable(state = state),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
            } else if (hasError || bitmap == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
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
                        color = MaterialTheme.colorScheme.error
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
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
