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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
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

// Single-threaded dispatcher to ensure thread safety with Android's PdfRenderer
private val pdfDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
private val pdfMutex = Mutex()

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

    DisposableEffect(pdfRenderer, fileDescriptor) {
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
        onDispose {
            bitmapCache.evictAll()
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A)) // Immersive dark theme background (Sleek Dark Slate)
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
                        }
                    )
                }

                // Beautiful translucent floating overlay header at the very top (starts exactly from y = 0)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                modifier = Modifier.widthIn(max = 180.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF4F46E5), shape = RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
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
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF4F46E5))
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
    onZoomChanged: (Boolean) -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    var bitmap by remember { mutableStateOf<Bitmap?>(bitmapCache.get(pageIndex)) }
    var pageError by remember { mutableStateOf(false) }

    LaunchedEffect(pageIndex) {
        if (bitmap != null) return@LaunchedEffect

        withContext(pdfDispatcher) {
            pdfMutex.withLock {
                try {
                    var bmp: Bitmap? = null
                    val page = pdfRenderer.openPage(pageIndex)
                    try {
                        val width = (page.width * 2f).toInt()
                        val height = (page.height * 2f).toInt()

                        try {
                            bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        } catch (e: OutOfMemoryError) {
                            System.gc()
                            try {
                                val fallbackWidth = (page.width * 1.5f).toInt()
                                val fallbackHeight = (page.height * 1.5f).toInt()
                                bmp = Bitmap.createBitmap(fallbackWidth, fallbackHeight, Bitmap.Config.ARGB_8888)
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
                .padding(8.dp)
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
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            shape = RoundedCornerShape(8.dp),
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
                            .background(Color(0xFF1E293B))
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
                        color = Color(0xFF4F46E5),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
