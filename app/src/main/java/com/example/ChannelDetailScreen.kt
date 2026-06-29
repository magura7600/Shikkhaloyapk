package com.example

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelDetailScreen(
    channel: UserProfile,
    profile: UserProfile,
    accentColor: Color,
    courses: List<CourseItem>,
    onBack: () -> Unit,
    onCourseClick: (CourseItem) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    var isEditing by remember { mutableStateOf(false) }
    var currentChannel by remember { mutableStateOf(channel) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    if (isEditing) {
        EditChannelDialog(
            channel = currentChannel,
            accentColor = accentColor,
            onDismiss = { isEditing = false },
            onUpdate = { updatedName ->
                coroutineScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            supabase.from("profiles").update(
                                {
                                    set("full_name", updatedName)
                                }
                            ) {
                                filter { eq("user_id", currentChannel.user_id) }
                            }
                        }
                        currentChannel = currentChannel.copy(full_name = updatedName)
                        Toast.makeText(context, "নাম পরিবর্তন হয়েছে", Toast.LENGTH_SHORT).show()
                        isEditing = false
                    } catch (e: Exception) {
                        Toast.makeText(context, "ত্রুটি: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentChannel.full_name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (profile.user_id == currentChannel.user_id) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Channel")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFFBF8F1)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Channel Header (Cover and Profile)
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                // Cover Image
                Box(modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.LightGray)) {
                    if (currentChannel.cover_image_url != null) {
                        AsyncImage(
                            model = currentChannel.cover_image_url,
                            contentDescription = "Cover",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Profile Image overlapping
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 25.dp)
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentChannel.profile_image_url != null) {
                            AsyncImage(
                                model = currentChannel.profile_image_url,
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(currentChannel.full_name.firstOrNull()?.toString() ?: "C", fontSize = 32.sp, color = Color.Gray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Channel Info
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(currentChannel.full_name, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Text("@${currentChannel.handle}", color = Color.Gray, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = accentColor
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("কোর্স", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("মিডিয়া", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            // Tab Content
            if (selectedTab == 0) {
                CourseTabContent(accentColor, profile, currentChannel, courses, onCourseClick)
            } else {
                MediaTabContent(accentColor, profile, currentChannel)
            }
        }
    }
}

@Composable
fun EditChannelDialog(
    channel: UserProfile,
    accentColor: Color,
    onDismiss: () -> Unit,
    onUpdate: (String) -> Unit
) {
    var newName by remember { mutableStateOf(channel.full_name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("চ্যানেলের নাম পরিবর্তন") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("নতুন নাম") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onUpdate(newName) }, colors = ButtonDefaults.buttonColors(containerColor = accentColor)) {
                Text("সেভ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = Color.Gray)
            }
        }
    )
}

@Composable
fun CourseTabContent(
    accentColor: Color, 
    profile: UserProfile, 
    channel: UserProfile, 
    courses: List<CourseItem>,
    onCourseClick: (CourseItem) -> Unit = {}
) {
    val channelCourses = courses.filter { it.channel_id == channel.user_id }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(channelCourses) { course ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCourseClick(course) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Book, contentDescription = "Course", tint = accentColor, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(course.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2D3748))
                        Text(course.description, fontSize = 12.sp, color = Color(0xFF718096), maxLines = 1)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(course.pricingOption, fontSize = 12.sp, color = accentColor, fontWeight = FontWeight.Bold)
                            if (course.pricingOption != "Fully Free" && course.mainPrice.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("৳${course.mainPrice}", fontSize = 12.sp, color = Color.Gray)
                            }
                            if (course.isQuarterOn && course.quarters.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("${course.quarters.size} Quarters", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun MediaTabContent(accentColor: Color, profile: UserProfile, channel: UserProfile) {
    val mockMedia = emptyList<FeedItem>()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(mockMedia) { media ->
            FeedItemCard(item = media, accentColor = accentColor, profile = profile, actingChannel = channel)
        }
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
