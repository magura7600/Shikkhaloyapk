package com.example

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
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
import kotlinx.serialization.Serializable
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChannelScreen(
    profile: UserProfile,
    existingChannel: UserProfile? = null,
    accentColor: Color,
    onBack: () -> Unit
) {
    var channelName by remember { mutableStateOf(existingChannel?.full_name ?: profile.full_name) }
    var channelHandle by remember { mutableStateOf(existingChannel?.handle ?: "") }
    var channelDescription by remember { mutableStateOf(existingChannel?.description ?: "") }
    var handleError by remember { mutableStateOf<String?>(null) }
    var profileImageUrl by remember { mutableStateOf<String?>(existingChannel?.profile_image_url ?: profile.profile_image_url) }
    var coverImageUrl by remember { mutableStateOf<String?>(existingChannel?.cover_image_url) }
    var isSaving by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val profileImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isSaving = true
                Toast.makeText(context, "Uploading profile image...", Toast.LENGTH_SHORT).show()
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    if (bytes != null) {
                        val uploadedUrl = ImgBBClient.uploadImage(bytes)
                        if (uploadedUrl != null) {
                            profileImageUrl = uploadedUrl
                            Toast.makeText(context, "Profile Image uploaded!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Upload failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isSaving = false
                }
            }
        }
    }

    val coverImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isSaving = true
                Toast.makeText(context, "Uploading cover image...", Toast.LENGTH_SHORT).show()
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    if (bytes != null) {
                        val uploadedUrl = ImgBBClient.uploadImage(bytes)
                        if (uploadedUrl != null) {
                            coverImageUrl = uploadedUrl
                            Toast.makeText(context, "Cover Image uploaded!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Upload failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isSaving = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingChannel != null) "চ্যানেল এডিট করুন" else "চ্যানেল সেটআপ করুন") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Cover Image Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color.LightGray)
                    .clickable { if (!isSaving) coverImagePicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (coverImageUrl != null) {
                    AsyncImage(
                        model = coverImageUrl,
                        contentDescription = "Cover Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, contentDescription = "Upload Cover", tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Text("Upload Cover Photo", color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profile Image Section
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { if (!isSaving) profileImagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageUrl != null) {
                        AsyncImage(
                            model = profileImageUrl,
                            contentDescription = "Profile Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Upload Profile", tint = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Input Fields
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = channelName,
                    onValueChange = { channelName = it },
                    label = { Text("Channel Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        focusedLabelColor = accentColor
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = channelHandle,
                    onValueChange = { 
                        // Only allow alphanumeric and underscore
                        val filtered = it.filter { char -> char.isLetterOrDigit() || char == '_' }
                        channelHandle = filtered
                        handleError = null 
                    },
                    label = { Text("Channel Handle (e.g. math_with_fahim)") },
                    leadingIcon = { Text("@", modifier = Modifier.padding(start = 12.dp)) },
                    isError = handleError != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        focusedLabelColor = accentColor
                    )
                )
                handleError?.let { errorMsg ->
                    Text(
                        text = errorMsg,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = channelDescription,
                    onValueChange = { channelDescription = it },
                    label = { Text("Channel Description") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        focusedLabelColor = accentColor
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (channelName.isBlank()) {
                            Toast.makeText(context, "Please enter a channel name.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (channelHandle.isBlank()) {
                            handleError = "Handle cannot be empty"
                            return@Button
                        }
                        
                        coroutineScope.launch {
                            isSaving = true
                            try {
                                val handleExists = if (existingChannel != null && existingChannel.handle == channelHandle) false else withContext(Dispatchers.IO) {
                                    try {
                                        val existing = supabase.from("profiles").select {
                                            filter { eq("handle", channelHandle) }
                                        }.decodeList<UserProfile>()
                                        existing.isNotEmpty()
                                    } catch(e: Exception) {
                                        false
                                    }
                                }
                                
                                if (handleExists) {
                                    handleError = "This handle is already taken."
                                } else {
                                    withContext(Dispatchers.IO) {
                                        supabase.from("profiles").update(
                                            {
                                                set("full_name", channelName)
                                                set("handle", channelHandle)
                                                set("description", channelDescription.ifBlank { null })
                                                set("profile_image_url", profileImageUrl)
                                                set("cover_image_url", coverImageUrl)
                                            }
                                        ) { filter { eq("user_id", profile.user_id) } }
                                    }
                                    Toast.makeText(context, if (existingChannel != null) "চ্যানেল সফলভাবে আপডেট হয়েছে!" else "চ্যানেল সফলভাবে তৈরি হয়েছে!", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(25.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text(if (existingChannel != null) "সেভ করুন" else "চ্যানেল তৈরি করুন", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
