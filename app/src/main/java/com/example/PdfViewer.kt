package com.example

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
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

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            try {
                if (file.exists() && file.length() > 0) {
                    val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(fd)
                    withContext(Dispatchers.Main) {
                        fileDescriptor = fd
                        pdfRenderer = renderer
                        pageCount = renderer.pageCount
                    }
                } else {
                    error = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                error = true
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                pdfRenderer?.close()
                fileDescriptor?.close()
            } catch (e: Exception) {}
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

        val listState = rememberLazyListState()
        val firstVisibleItemIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(end = 48.dp) // Offset for back button to center text
                        ) {
                            Text(
                                text = "ক্লাসের লেকচার ফাইল:", 
                                fontSize = 18.sp, 
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            if (pageCount > 0) {
                                Text(
                                    text = "পৃষ্ঠা: ${firstVisibleItemIndex + 1} / $pageCount",
                                    fontSize = 14.sp,
                                    color = Color(0xFF1E3A8A)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF1E293B))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    ),
                    modifier = Modifier
                )
            },
            containerColor = Color(0xFFE8EDF1)
        ) { paddingValues ->
            if (error) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("পিডিএফ লোড করা যায়নি।", color = Color.Red, fontSize = 16.sp)
                }
            } else if (pageCount > 0 && pdfRenderer != null) {
                var scale by remember { mutableStateOf(1f) }
                var offsetX by remember { mutableStateOf(0f) }
                var offsetY by remember { mutableStateOf(0f) }

                val state = rememberTransformableState { zoomChange, offsetChange, _ ->
                    scale = (scale * zoomChange).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        offsetX += offsetChange.x
                        offsetY += offsetChange.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .clipToBounds()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                )
                            }
                            .transformable(state = state)
                    ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            ),
                        contentPadding = PaddingValues(0.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        userScrollEnabled = true // Always allow scrolling to load new pages
                    ) {
                        items(pageCount) { index ->
                            PdfPageSimple(pdfRenderer!!, index)
                        }
                    }
                }
                
                // Floating Page Indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .background(
                            color = Color(0xAA000000),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "পৃষ্ঠা: ${firstVisibleItemIndex + 1} / $pageCount",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun PdfPageSimple(pdfRenderer: PdfRenderer, pageIndex: Int) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pageError by remember { mutableStateOf(false) }

    LaunchedEffect(pageIndex) {
        withContext(Dispatchers.IO) {
            try {
                var bmp: Bitmap? = null
                synchronized(pdfRenderer) {
                    val page = pdfRenderer.openPage(pageIndex)
                    val width = (page.width * 2f).toInt()
                    val height = (page.height * 2f).toInt()
                    
                    try {
                        bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    } catch (e: OutOfMemoryError) {
                        System.gc()
                        bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    }
                    
                    if (bmp != null) {
                        bmp!!.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp!!, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                    page.close()
                }
                if (bmp != null) {
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

    Card(
        modifier = Modifier.fillMaxWidth().clipToBounds(),
        shape = androidx.compose.ui.graphics.RectangleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
        } else if (pageError) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = "Error",
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("পৃষ্ঠাটি লোড করা যায়নি", color = Color.Red, fontSize = 14.sp)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
