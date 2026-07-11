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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    var pdfUrlToView by remember { mutableStateOf("") }
    
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
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Tab Selection
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
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
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                }

                if (selectedTab == 2) {
                    var favorites by remember { mutableStateOf(emptyList<FavoritePdf>()) }
                    var recents by remember { mutableStateOf(emptyList<RecentPdf>()) }
                    
                    LaunchedEffect(Unit) {
                        favorites = PdfHistoryManager.getFavorites(context)
                        recents = PdfHistoryManager.getRecents(context)
                    }
                    
                    if (favorites.isEmpty() && recents.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Star, contentDescription = "No items", tint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("কোনো পছন্দের বা সাম্প্রতিক ফাইল নেই", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("পিডিএফ পড়ার সময় স্টার দিয়ে পছন্দ করতে পারেন।", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (favorites.isNotEmpty()) {
                                item {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = "Favorites", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("পছন্দের তালিকা (${favorites.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
                                    }
                                }
                                items(favorites) { fav ->
                                    val isDownloaded = File(fav.filePath).exists()
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            val file = File(fav.filePath)
                                            if (file.exists()) {
                                                pdfToView = file
                                                pdfTitleToView = fav.title
                                                pdfUrlToView = fav.url
                                            } else {
                                                android.widget.Toast.makeText(context, "ডাউনলোডকৃত ফাইলটি স্থানীয় স্টোরেজে পাওয়া যায়নি!", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Star, contentDescription = "Favorite", tint = MaterialTheme.colorScheme.error)
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(fav.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                                                Text(if (isDownloaded) "অফলাইন ডাউনলোডকৃত" else "অনলাইন / লিংক ড্যামেজ", fontSize = 11.sp, color = if (isDownloaded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error)
                                            }
                                            IconButton(onClick = {
                                                PdfHistoryManager.toggleFavorite(context, fav.title, fav.filePath, fav.url)
                                                favorites = PdfHistoryManager.getFavorites(context)
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.6f))
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (recents.isNotEmpty()) {
                                item {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                        Icon(Icons.Default.History, contentDescription = "Recents", tint = accentColor, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("সাম্প্রতিক পঠিত (${recents.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
                                    }
                                }
                                items(recents) { rec ->
                                    val isDownloaded = File(rec.filePath).exists()
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            val file = File(rec.filePath)
                                            if (file.exists()) {
                                                pdfToView = file
                                                pdfTitleToView = rec.title
                                                pdfUrlToView = rec.url
                                            } else {
                                                android.widget.Toast.makeText(context, "ডাউনলোডকৃত ফাইলটি স্থানীয় স্টোরেজে পাওয়া যায়নি!", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier.size(40.dp).background(accentColor.copy(alpha = 0.1f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.MenuBook, contentDescription = "Recent", tint = accentColor)
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(rec.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                                                Text(if (isDownloaded) "অফলাইন ডাউনলোডকৃত" else "অনলাইন / লিংক ড্যামেজ", fontSize = 11.sp, color = if (isDownloaded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error)
                                            }
                                            Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
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
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                                    pdfUrlToView = record.url
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
        }

        // Display PDF Viewer Dialog when a PDF is selected
        if (pdfToView != null) {
            PdfViewerDialog(
                file = pdfToView!!,
                title = pdfTitleToView,
                url = pdfUrlToView,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                if (record.className.isNotBlank()) {
                    Text(
                        text = "ক্লাস: ${record.className}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                tint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (tabIndex == 0) "ডাউনলোডকৃত কোনো ক্লাস ভিডিও পাওয়া যায়নি" else "ডাউনলোডকৃত কোনো পিডিএফ স্লাইড পাওয়া যায়নি",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ক্লাস প্যানেল থেকে ফাইল ডাউনলোড করলে এখানে দেখতে পাবেন।",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        var isManualFullscreen by remember(isLandscape) { mutableStateOf(isLandscape) }

        androidx.activity.compose.BackHandler(enabled = isManualFullscreen) {
            isManualFullscreen = false
            val activity = context.findActivity()
            if (activity != null) {
                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }

        Scaffold(
            topBar = {
                if (!VideoPipState.isInPip && !isLandscape) {
                    TopAppBar(
                        title = {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.surface
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onClose) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.surface)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Black,
                            titleContentColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            },
            containerColor = Color.Black
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (VideoPipState.isInPip || isLandscape) PaddingValues(0.dp) else paddingValues)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                VideoPlayer(
                    videoOptions = videoOptions,
                    modifier = if (isLandscape) {
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black)
                    },
                    isFullscreen = isManualFullscreen,
                    onFullscreenToggle = { fullscreen ->
                        isManualFullscreen = fullscreen
                        val activity = context.findActivity()
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
