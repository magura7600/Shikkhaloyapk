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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreFeedScreen(
    accentColor: Color, 
    profile: UserProfile, 
    actingChannel: UserProfile? = null,
    courses: List<CourseItem> = emptyList(),
    allChannels: List<UserProfile> = emptyList(),
    onChannelClick: (UserProfile) -> Unit = {},
    onCourseClick: (CourseItem) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val mockPosts = emptyList<FeedItem>()
    
    val filteredCourses = if (searchQuery.isBlank()) {
        courses
    } else {
        courses.filter { it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
    }
    
    val filteredChannels = if (searchQuery.isBlank()) {
        allChannels
    } else {
        allChannels.filter { it.full_name.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = 80.dp, start = 16.dp, end = 16.dp)) {
        // Channels Story-like row
        if (searchQuery.isBlank() && allChannels.isNotEmpty()) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(allChannels) { channel ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onChannelClick(channel) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (channel.profile_image_url != null) {
                                AsyncImage(
                                    model = channel.profile_image_url,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(channel.full_name.first().toString(), color = accentColor, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(channel.full_name, fontSize = 12.sp, maxLines = 1, color = Color.Gray, modifier = Modifier.width(64.dp))
                    }
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("সার্চ করুন (ভিডিও, কোর্স, চ্যানেল)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color.LightGray,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        // Feed Categories
        var selectedCategory by remember { mutableStateOf("All") }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == "All",
                onClick = { selectedCategory = "All" },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentColor, selectedLabelColor = Color.White)
            )
            FilterChip(
                selected = selectedCategory == "Courses",
                onClick = { selectedCategory = "Courses" },
                label = { Text("Courses") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentColor, selectedLabelColor = Color.White)
            )
        }

        // Feed List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            if (filteredCourses.isNotEmpty()) {
                items(filteredCourses) { course ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCourseClick(course) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            // Thumbnail placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .background(Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                 if (course.bannerUrl.isNotBlank()) {
                                     AsyncImage(
                                         model = course.bannerUrl,
                                         contentDescription = "Course Banner",
                                         modifier = Modifier.fillMaxSize(),
                                         contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                     )
                                 } else {
                                     Icon(
                                         imageVector = Icons.Default.MenuBook,
                                         contentDescription = "Course",
                                         modifier = Modifier.size(64.dp),
                                         tint = Color.Gray
                                     )
                                 }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Course", color = Color.White, fontSize = 12.sp)
                                }
                            }

                            // Details
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(course.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2, color = Color(0xFF2D3748))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(course.description, fontSize = 14.sp, color = Color.Gray, maxLines = 2)
                                if (course.startDate.isNotBlank() || course.endDate.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = "Duration",
                                            modifier = Modifier.size(14.dp),
                                            tint = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("সময়কাল: ${course.startDate} - ${course.endDate}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(course.pricingOption, fontSize = 14.sp, color = accentColor, fontWeight = FontWeight.Bold)
                                    if (course.pricingOption != "Fully Free" && course.mainPrice.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("৳${course.mainPrice}", fontSize = 14.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (searchQuery.isNotEmpty()) {
                item {
                    Text("No results found", color = Color.Gray, modifier = Modifier.padding(16.dp))
                }
            } else {
                item {
                    Text("No courses available yet", color = Color.Gray, modifier = Modifier.padding(16.dp))
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

data class FeedItem(
    val id: String,
    val type: String,
    val title: String,
    val channelName: String,
    val stats: String,
    val thumbnailUrl: String?
)

@Composable
fun FeedItemCard(item: FeedItem, accentColor: Color, profile: UserProfile, actingChannel: UserProfile? = null, onChannelClick: (String) -> Unit = {}) {
    var isLiked by remember { mutableStateOf(false) }
    var isDisliked by remember { mutableStateOf(false) }
    var isSubscribed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val actingName = actingChannel?.full_name ?: profile.full_name

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.LightGray)
            ) {
                if (item.thumbnailUrl != null) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = "Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = when(item.type) {
                            "Video" -> Icons.Default.PlayCircleOutline
                            "Course" -> Icons.Default.MenuBook
                            else -> Icons.Default.Article
                        },
                        contentDescription = "Type",
                        modifier = Modifier.align(Alignment.Center).size(64.dp),
                        tint = Color.Gray
                    )
                }
                // Type badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(item.type, color = Color.White, fontSize = 12.sp)
                }
            }

            // Details
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    // Channel Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f))
                            .clickable { onChannelClick(item.channelName) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.channelName.first().toString(), color = accentColor, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Title and Channel
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2)
                        Text(
                            "${item.channelName} • ${item.stats}", 
                            color = Color.Gray, 
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { onChannelClick(item.channelName) }
                        )
                    }
                    
                    // Subscribe or Edit Button
                    if (actingChannel?.user_id == profile.user_id) {
                        Button(
                            onClick = { Toast.makeText(context, "Edit option clicked", Toast.LENGTH_SHORT).show() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.LightGray,
                                contentColor = Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = { 
                                isSubscribed = !isSubscribed
                                if (isSubscribed) {
                                    Toast.makeText(context, "$actingName subscribed to ${item.channelName}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "$actingName unsubscribed from ${item.channelName}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSubscribed) Color.LightGray else accentColor,
                                contentColor = if (isSubscribed) Color.Black else Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(if (isSubscribed) "Subscribed" else "Subscribe", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions (Like, Dislike, Share, Report)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { 
                            isLiked = !isLiked
                            if (isLiked) {
                                isDisliked = false
                                Toast.makeText(context, "$actingName liked this", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(
                                if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp, 
                                contentDescription = "Like",
                                tint = if (isLiked) accentColor else Color.Gray
                            )
                        }
                        IconButton(onClick = { 
                            isDisliked = !isDisliked
                            if (isDisliked) {
                                isLiked = false
                                Toast.makeText(context, "$actingName disliked this", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(
                                if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown, 
                                contentDescription = "Dislike",
                                tint = if (isDisliked) accentColor else Color.Gray
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { Toast.makeText(context, "$actingName shared this", Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Outlined.Share, contentDescription = "Share", tint = Color.Gray)
                        }
                        IconButton(onClick = { Toast.makeText(context, "$actingName reported this", Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Outlined.Flag, contentDescription = "Report", tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
