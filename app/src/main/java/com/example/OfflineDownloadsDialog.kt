package com.example

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineDownloadsDialog(
    onDismiss: () -> Unit,
    accentColor: Color
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var downloads by remember { mutableStateOf(emptyList<DownloadRecord>()) }
    
    // Dialog states
    var pdfToView by remember { mutableStateOf<File?>(null) }
    var pdfTitleToView by remember { mutableStateOf("") }
    
    var videoToPlay by remember { mutableStateOf<File?>(null) }
    var videoTitleToPlay by remember { mutableStateOf("") }

    // Load downloads initially
    LaunchedEffect(Unit) {
        downloads = OfflineDownloadManager.getDownloadRecords(context)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "অফলাইন ডাউনলোডসমূহ",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF1E293B))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF1E293B)
                    )
                )
            },
            containerColor = Color(0xFFF8FAFC)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Tab Selection
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = accentColor,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = accentColor
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VideoLibrary, contentDescription = "Videos", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ক্লাস ভিডিও", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        },
                        selectedContentColor = accentColor,
                        unselectedContentColor = Color(0xFF64748B)
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MenuBook, contentDescription = "PDFs", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("পিডিএফ ও শিট", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        },
                        selectedContentColor = accentColor,
                        unselectedContentColor = Color(0xFF64748B)
                    )
                }

                val filteredDownloads = remember(downloads, selectedTab) {
                    downloads.filter { 
                        if (selectedTab == 0) it.fileType == "video" else it.fileType == "pdf" 
                    }
                }

                if (filteredDownloads.isEmpty()) {
                    EmptyDownloadsView(selectedTab)
                } else {
                    // Group downloads by course
                    val groupedDownloads = remember(filteredDownloads) {
                        filteredDownloads.groupBy { it.courseName.ifBlank { "অন্যান্য" } }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        groupedDownloads.forEach { (courseName, records) ->
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Folder,
                                        contentDescription = "Course",
                                        tint = accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = courseName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }

                            items(records, key = { it.id }) { record ->
                                DownloadItemCard(
                                    record = record,
                                    onAction = {
                                        val file = File(record.localPath)
                                        if (file.exists()) {
                                            if (record.fileType == "pdf") {
                                                pdfToView = file
                                                pdfTitleToView = record.title
                                            } else {
                                                videoToPlay = file
                                                videoTitleToPlay = record.title
                                            }
                                        } else {
                                            android.widget.Toast.makeText(context, "ফাইলটি খুঁজে পাওয়া যায়নি!", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDelete = {
                                        OfflineDownloadManager.deleteDownload(context, record)
                                        downloads = OfflineDownloadManager.getDownloadRecords(context)
                                    },
                                    accentColor = accentColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // Display PDF Viewer Dialog when a PDF is selected
        if (pdfToView != null) {
            PdfViewerDialog(
                file = pdfToView!!,
                title = pdfTitleToView,
                onClose = { pdfToView = null }
            )
        }

        // Display offline video player Dialog when a video is selected
        if (videoToPlay != null) {
            OfflineVideoPlayerDialog(
                file = videoToPlay!!,
                title = videoTitleToPlay,
                onClose = { videoToPlay = null }
            )
        }
    }
}

@Composable
fun DownloadItemCard(
    record: DownloadRecord,
    onAction: () -> Unit,
    onDelete: () -> Unit,
    accentColor: Color
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val formattedDate = remember(record.downloadTime) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(record.downloadTime))
    }

    val fileSizeStr = remember(record.localPath) {
        val file = File(record.localPath)
        if (file.exists()) {
            val bytes = file.length()
            if (bytes < 1024 * 1024) {
                String.format(Locale.US, "%.1f KB", bytes.toFloat() / 1024f)
            } else {
                String.format(Locale.US, "%.1f MB", bytes.toFloat() / (1024f * 1024f))
            }
        } else {
            "Unknown size"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onAction() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(accentColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (record.fileType == "pdf") Icons.Default.MenuBook else Icons.Default.PlayArrow,
                    contentDescription = record.fileType,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1.0f)
            ) {
                Text(
                    text = record.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    maxLines = 1
                )
                if (record.className.isNotBlank()) {
                    Text(
                        text = "ক্লাস: ${record.className}",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = fileSizeStr,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { showDeleteConfirm = true }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red.copy(alpha = 0.8f)
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("ডিলিট নিশ্চিত করুন", fontWeight = FontWeight.Bold) },
            text = { Text("আপনি কি নিশ্চিতভাবে এই ডাউনলোড করা ফাইলটি মুছে ফেলতে চান? এটি আর পুনরায় ফিরে পাওয়া যাবে না।") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("হ্যাঁ, ডিলিট করুন", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("বাতিল", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun EmptyDownloadsView(tabIndex: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.DownloadDone,
                contentDescription = "No downloads",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (tabIndex == 0) "ডাউনলোডকৃত কোনো ক্লাস ভিডিও পাওয়া যায়নি" else "ডাউনলোডকৃত কোনো পিডিএফ স্লাইড পাওয়া যায়নি",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF475569)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ক্লাস প্যানেল থেকে ফাইল ডাউনলোড করলে এখানে দেখতে পাবেন।",
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineVideoPlayerDialog(
    file: File,
    title: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    // Set up dummy VideoOptions pointing to the local secure file
    val videoOptions = remember(file) {
        VideoOptions(
            title = title,
            description = "Offline class video",
            links = listOf(VideoLink(quality = "Offline Playback", url = file.absolutePath))
        )
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Scaffold(
            topBar = {
                if (!VideoPipState.isInPip) {
                    TopAppBar(
                        title = {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onClose) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Black,
                            titleContentColor = Color.White
                        )
                    )
                }
            },
            containerColor = Color.Black
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (VideoPipState.isInPip) PaddingValues(0.dp) else paddingValues)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                VideoPlayer(
                    videoOptions = videoOptions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                )
            }
        }
    }
}
