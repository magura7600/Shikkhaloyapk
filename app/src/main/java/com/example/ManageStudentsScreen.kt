package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageStudentsScreen(
    course: CourseItem,
    accentColor: Color,
    onBack: () -> Unit
) {
    var enrollments by remember { mutableStateOf<List<Enrollment>>(emptyList()) }
    var profiles by remember { mutableStateOf<Map<String, UserProfile>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    var showBanDialog by remember { mutableStateOf(false) }
    var selectedEnrollment by remember { mutableStateOf<Enrollment?>(null) }

    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(course.id) {
        coroutineScope.launch {
            try {
                val fetchedEnrollments = withContext(Dispatchers.IO) {
                    supabase.from("enrollments").select {
                        filter { eq("course_id", course.id) }
                    }.decodeList<Enrollment>()
                }
                enrollments = fetchedEnrollments

                val userIds = fetchedEnrollments.map { it.user_id }.distinct()
                if (userIds.isNotEmpty()) {
                    val fetchedProfiles = withContext(Dispatchers.IO) {
                        supabase.from("profiles").select {
                            filter { isIn("user_id", userIds) }
                        }.decodeList<UserProfile>()
                    }
                    profiles = fetchedProfiles.associateBy { it.user_id }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("শিক্ষার্থী পরিচালনা", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accentColor)
            }
        } else if (enrollments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("কোনো শিক্ষার্থী পাওয়া যায়নি", color = Color.Gray)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("শিক্ষার্থীর নাম বা ইমেইল দিয়ে খুঁজুন") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                val filteredEnrollments = enrollments.filter { enrollment ->
                    val profile = profiles[enrollment.user_id]
                    profile?.full_name?.contains(searchQuery, ignoreCase = true) == true ||
                    profile?.email?.contains(searchQuery, ignoreCase = true) == true
                }

                if (filteredEnrollments.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("কোনো শিক্ষার্থী পাওয়া যায়নি", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredEnrollments) { enrollment ->
                            val profile = profiles[enrollment.user_id]
                            StudentItem(
                                enrollment = enrollment,
                                profile = profile,
                                accentColor = accentColor,
                                onBanClick = {
                                    selectedEnrollment = enrollment
                                    showBanDialog = true
                                },
                                onUnbanClick = {
                                    coroutineScope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                supabase.from("enrollments").update(
                                                    {
                                                        set("banned_until", null as Long?)
                                                        set("ban_reason", null as String?)
                                                    }
                                                ) {
                                                    filter { eq("id", enrollment.id) }
                                                }
                                            }
                                            // Update local state
                                            enrollments = enrollments.map { 
                                                if (it.id == enrollment.id) it.copy(banned_until = null, ban_reason = null) else it
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showBanDialog && selectedEnrollment != null) {
            BanStudentDialog(
                enrollment = selectedEnrollment!!,
                accentColor = accentColor,
                onDismiss = { showBanDialog = false },
                onBanConfirm = { durationMillis, reason ->
                    val bannedUntil = if (durationMillis == -1L) -1L else System.currentTimeMillis() + durationMillis
                    coroutineScope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                supabase.from("enrollments").update(
                                    {
                                        set("banned_until", bannedUntil)
                                        set("ban_reason", reason)
                                    }
                                ) {
                                    filter { eq("id", selectedEnrollment!!.id) }
                                }
                            }
                            // Update local state
                            enrollments = enrollments.map {
                                if (it.id == selectedEnrollment!!.id) it.copy(banned_until = bannedUntil, ban_reason = reason) else it
                            }
                            showBanDialog = false
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun StudentItem(
    enrollment: Enrollment,
    profile: UserProfile?,
    accentColor: Color,
    onBanClick: () -> Unit,
    onUnbanClick: () -> Unit
) {
    val isBanned = enrollment.banned_until != null && (enrollment.banned_until == -1L || enrollment.banned_until > System.currentTimeMillis())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (profile?.profile_image_url != null) {
                AsyncImage(
                    model = profile.profile_image_url,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile?.full_name ?: "Unknown Student",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Text(
                    text = profile?.email ?: "",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                if (isBanned) {
                    val banText = if (enrollment.banned_until == -1L) "Lifetime Ban" else "Banned"
                    Text(
                        text = "$banText: ${enrollment.ban_reason ?: "No reason"}",
                        fontSize = 12.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                } else if (enrollment.purchased_quarters.isNotEmpty()) {
                    Text(
                        text = "Quarters: ${enrollment.purchased_quarters}",
                        fontSize = 12.sp,
                        color = accentColor
                    )
                }
            }

            if (isBanned) {
                IconButton(onClick = onUnbanClick) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Unban", tint = MaterialTheme.colorScheme.secondary)
                }
            } else {
                IconButton(onClick = onBanClick) {
                    Icon(Icons.Default.Block, contentDescription = "Ban", tint = Color.Red)
                }
            }
        }
    }
}

@Composable
fun BanStudentDialog(
    enrollment: Enrollment,
    accentColor: Color,
    onDismiss: () -> Unit,
    onBanConfirm: (Long, String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var selectedDurationIndex by remember { mutableStateOf(0) }

    val durations = listOf(
        "১ ঘণ্টা" to 1L * 60 * 60 * 1000,
        "১২ ঘণ্টা" to 12L * 60 * 60 * 1000,
        "১ দিন" to 1L * 24 * 60 * 60 * 1000,
        "৩ দিন" to 3L * 24 * 60 * 60 * 1000,
        "৭ দিন" to 7L * 24 * 60 * 60 * 1000,
        "১ মাস" to 30L * 24 * 60 * 60 * 1000,
        "সারাজীবন" to -1L
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("শিক্ষার্থীকে ব্যান করুন", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("কারন (Reason)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("সময়কাল (Duration):", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Using a simple list of buttons for duration selection since Dropdown can be complex inside dialog
                durations.forEachIndexed { index, pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = index == selectedDurationIndex,
                            onClick = { selectedDurationIndex = index },
                            colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(pair.first)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onBanConfirm(durations[selectedDurationIndex].second, reason) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("ব্যান করুন", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = Color.Gray)
            }
        }
    )
}
