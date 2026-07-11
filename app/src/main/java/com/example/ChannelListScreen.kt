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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
fun ChannelListScreen(
    profile: UserProfile,
    accentColor: Color,
    onCreateChannel: () -> Unit,
    onChannelClick: (UserProfile) -> Unit,
    onBack: () -> Unit
) {
    var channels by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val fetchedChannels = withContext(Dispatchers.IO) {
                    supabase.from("profiles").select {
                        filter { eq("user_id", profile.user_id) }
                    }.decodeList<UserProfile>()
                }
                channels = fetchedChannels.filter { it.handle != null }
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("আমার চ্যানেলসমূহ") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateChannel,
                containerColor = accentColor,
                contentColor = Color.White,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Channel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accentColor)
            } else if (errorMessage != null) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: $errorMessage", color = Color.Red, modifier = Modifier.padding(16.dp))
                    Text("Please ensure the 'channels' table exists in Supabase.", color = Color.Gray)
                }
            } else if (channels.isEmpty()) {
                Text(
                    "কোনো চ্যানেল পাওয়া যায়নি। নতুন চ্যানেল তৈরি করুন।",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(channels, key = { it.user_id }) { channel ->
                        ChannelItemCard(channel = channel, onClick = { onChannelClick(channel) })
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelItemCard(channel: UserProfile, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (channel.profile_image_url != null) {
                    AsyncImage(
                        model = channel.profile_image_url,
                        contentDescription = "Channel Profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(channel.full_name.firstOrNull()?.toString() ?: "C", fontSize = 24.sp, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(channel.full_name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("@${channel.handle}", color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}
