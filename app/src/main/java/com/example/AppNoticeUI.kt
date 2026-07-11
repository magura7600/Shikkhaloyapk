package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun EmergencyNoticeDialog(
    notice: AppNotice,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    // Determine color schemes and icons based on Notice type
    val colorScheme = MaterialTheme.colorScheme
    val themeColors = remember(notice.type, colorScheme) {
        when (notice.type.lowercase()) {
            "warning" -> NoticeThemeColors(
                primary = Color(0xFFEF4444),
                background = Color(0xFFF3F4F6),
                border = Color(0xFFEF4444),
                onBg = Color(0xFFEF4444),
                icon = Icons.Default.Warning
            )
            "offer" -> NoticeThemeColors(
                primary = Color(0xFF1E3A8A),
                background = Color(0xFFF3F4F6),
                border = Color(0xFFEFF6FF),
                onBg = Color(0xFF1E3A8A),
                icon = Icons.Default.Star
            )
            "exam" -> NoticeThemeColors(
                primary = Color(0xFFEF4444),
                background = Color(0xFF1E3A8A),
                border = Color(0xFFF59E0B),
                onBg = Color(0xFFEF4444),
                icon = Icons.Default.MenuBook
            )
            else -> NoticeThemeColors(
                primary = accentColor,
                background = Color(0xFFF3F4F6),
                border = Color(0xFFF3F4F6),
                onBg = Color(0xFF1E3A8A),
                icon = Icons.Default.Campaign
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(themeColors.primary.copy(alpha = 0.12f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = themeColors.icon,
                    contentDescription = notice.type,
                    tint = themeColors.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        },
        title = {
            Text(
                text = L.translateNotice(notice.title),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF1E3A8A),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Image preview if present
                if (!notice.image_url.isNullOrBlank()) {
                    AsyncImage(
                        model = notice.image_url,
                        contentDescription = "Notice Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(14.dp))
                            .background(Color(0xFFF3F4F6)),
                        contentScale = ContentScale.Crop
                    )
                }

                // Styled Message Content Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = themeColors.background),
                    border = BorderStroke(1.dp, themeColors.border),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = L.translateNotice(notice.content),
                        fontSize = 14.sp,
                        color = themeColors.onBg,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Call to action button if action URL is configured
                if (!notice.action_url.isNullOrBlank()) {
                    Button(
                        onClick = {
                            try {
                                uriHandler.openUri(notice.action_url.trim())
                            } catch (e: Exception) {
                                // Do nothing
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Link",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "বিস্তারিত দেখুন 🔗",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ঠিক আছে, বুঝতে পেরেছি".t(),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A),
                    fontSize = 14.sp
                )
            }
        }
    )
}

data class NoticeThemeColors(
    val primary: Color,
    val background: Color,
    val border: Color,
    val onBg: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun PublishNoticeDialog(
    accentColor: Color,
    onDismiss: () -> Unit,
    onPublished: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var titleInput by remember { mutableStateOf("") }
    var contentInput by remember { mutableStateOf("") }
    var imageUrlInput by remember { mutableStateOf("") }
    var actionUrlInput by remember { mutableStateOf("") }
    var scheduledTimeInput by remember { mutableStateOf("") }
    
    // Notice Type Selection
    var selectedType by remember { mutableStateOf("general") } // general, warning, offer, exam

    var isUploadingImage by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showSqlInstructions by remember { mutableStateOf(false) }

    // Media Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isUploadingImage = true
                Toast.makeText(context, "ছবি আপলোড হচ্ছে...", Toast.LENGTH_SHORT).show()
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    if (bytes != null) {
                        val uploadedUrl = ImgBBClient.uploadImage(bytes)
                        if (uploadedUrl != null) {
                            imageUrlInput = uploadedUrl
                            Toast.makeText(context, "ছবি সফলভাবে আপলোড হয়েছে! 📸", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "ছবি আপলোড ব্যর্থ হয়েছে।", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isUploadingImage = false
                }
            }
        }
    }

    val sqlSchema = """
-- Supabase SQL Editor এ এই কোডটি রান করে নোটিফিকেশন ডাটাবেজ আপডেট করুন:
ALTER TABLE public.app_notices ADD COLUMN IF NOT EXISTS image_url TEXT;
ALTER TABLE public.app_notices ADD COLUMN IF NOT EXISTS type TEXT DEFAULT 'general';
ALTER TABLE public.app_notices ADD COLUMN IF NOT EXISTS action_url TEXT;
ALTER TABLE public.app_notices ADD COLUMN IF NOT EXISTS scheduled_time TEXT;
ALTER TABLE public.app_notices ADD COLUMN IF NOT EXISTS target_course_id TEXT;
    """.trimIndent()

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = "Publish Notice",
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "উন্নত নোটিশ পাবলিশ 📢",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1E3A8A)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Title Input
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("নোটিশের শিরোনাম (Title)") },
                    placeholder = { Text("যেমন: ভর্তি সংক্রান্ত জরুরি নোটিশ") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // 2. Content Input
                OutlinedTextField(
                    value = contentInput,
                    onValueChange = { contentInput = it },
                    label = { Text("নোটিশের বিস্তারিত বিবরণ (Content)") },
                    placeholder = { Text("প্রিয় শিক্ষার্থীরা, আমাদের ভর্তি কার্যক্রম আগামী... ") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3
                )

                // 3. Notice Type Selector (Category)
                Text(
                    text = "নোটিশের ধরণ (Notice Category)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF1E3A8A),
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf(
                        Triple("general", "সাধারণ", Color(0xFF1E3A8A)),
                        Triple("warning", "জরুরি", Color(0xFFEF4444)),
                        Triple("offer", "অফার/প্রোমো", Color(0xFF1E3A8A)),
                        Triple("exam", "পরীক্ষা", Color(0xFFEF4444))
                    )
                    types.forEach { (typeKey, typeLabel, color) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (selectedType == typeKey) color.copy(alpha = 0.15f) else Color(0xFFF3F4F6),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (selectedType == typeKey) color else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedType = typeKey }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = typeLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedType == typeKey) color else Color(0xFF1E3A8A)
                            )
                        }
                    }
                }

                // 4. Image Upload Block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "নোটিফিকেশন ইমেজ (Optional)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF1E3A8A)
                        )
                        Text(
                            text = if (imageUrlInput.isBlank()) "কোনো ছবি যুক্ত করা হয়নি" else "ছবি সফলভাবে যুক্ত করা হয়েছে",
                            fontSize = 11.sp,
                            color = if (imageUrlInput.isBlank()) Color(0xFF1E3A8A) else Color(0xFF10B981)
                        )
                    }
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        enabled = !isUploadingImage,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (imageUrlInput.isBlank()) accentColor.copy(alpha = 0.1f) else Color(0xFFF3F4F6),
                            contentColor = if (imageUrlInput.isBlank()) accentColor else Color(0xFFEF4444)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        if (isUploadingImage) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = accentColor, strokeWidth = 1.5.dp)
                        } else {
                            Icon(
                                imageVector = if (imageUrlInput.isBlank()) Icons.Default.Image else Icons.Default.Delete,
                                contentDescription = "Image Action",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (imageUrlInput.isBlank()) "আপলোড" else "মুছুন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (imageUrlInput.isNotBlank()) {
                    AsyncImage(
                        model = imageUrlInput,
                        contentDescription = "Uploaded Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                            .clickable { imageUrlInput = "" },
                        contentScale = ContentScale.Crop
                    )
                }

                // 5. Action Link Input
                OutlinedTextField(
                    value = actionUrlInput,
                    onValueChange = { actionUrlInput = it },
                    label = { Text("অ্যাকশন লিংক বা বাটন লিংক (Optional)") },
                    placeholder = { Text("যেমন: https://facebook.com/groups/...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Link, contentDescription = "Link", modifier = Modifier.size(18.dp))
                    }
                )

                // 6. Schedule Time Input
                OutlinedTextField(
                    value = scheduledTimeInput,
                    onValueChange = { scheduledTimeInput = it },
                    label = { Text("সিডিউল পাবলিশ সময় (Optional)") },
                    placeholder = { Text("Format: YYYY-MM-DD HH:MM") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Schedule, contentDescription = "Schedule", modifier = Modifier.size(18.dp))
                    }
                )

                // Quick Scheduling Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    val now = LocalDateTime.now()
                    val presets = listOf(
                        Pair("এখনই", ""),
                        Pair("+১ ঘণ্টা", now.plusHours(1).format(formatter)),
                        Pair("+৬ ঘণ্টা", now.plusHours(6).format(formatter)),
                        Pair("+১ দিন", now.plusDays(1).format(formatter))
                    )
                    presets.forEach { (label, valStr) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                                .clickable { scheduledTimeInput = valStr }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFF3F4F6))

                // SQL Guide dropdown
                Button(
                    onClick = { showSqlInstructions = !showSqlInstructions },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF3F4F6),
                        contentColor = Color(0xFF1E3A8A)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (showSqlInstructions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle SQL"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Supabase নোটিশ ডাটাবেজ আপডেট নির্দেশিকা 🛠️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (showSqlInstructions) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E3A8A), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFF1E3A8A), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SQL Migration Script",
                                color = Color(0xFF1E3A8A),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(sqlSchema))
                                    Toast.makeText(context, "SQL স্ক্রিপ্ট কপি হয়েছে!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sqlSchema,
                            color = Color(0xFFF3F4F6),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titleInput.isBlank()) {
                        Toast.makeText(context, "দয়া করে নোটিশ শিরোনাম দিন!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (contentInput.isBlank()) {
                        Toast.makeText(context, "দয়া করে নোটিশ বিবরণ দিন!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isSubmitting = true
                    scope.launch {
                        val noticeRecord = AppNotice(
                            title = titleInput,
                            content = contentInput,
                            is_active = true,
                            image_url = if (imageUrlInput.isBlank()) null else imageUrlInput.trim(),
                            type = selectedType,
                            action_url = if (actionUrlInput.isBlank()) null else actionUrlInput.trim(),
                            scheduled_time = if (scheduledTimeInput.isBlank()) null else scheduledTimeInput.trim()
                        )
                        val result = AppNoticeManager.publishNotice(noticeRecord)
                        isSubmitting = false
                        if (result.isSuccess) {
                            Toast.makeText(context, "জরুরি নোটিশ সফলভাবে প্রচার করা হয়েছে! 🎉", Toast.LENGTH_LONG).show()
                            onPublished()
                            onDismiss()
                        } else {
                            val errMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                            Toast.makeText(context, "ব্যর্থ হয়েছে! ডাটাবেজ আপডেট করা আছে তো? গাইড দেখুন।", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = !isSubmitting && !isUploadingImage,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("পাবলিশ করুন 🚀", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isSubmitting) {
                TextButton(onClick = onDismiss) {
                    Text("বাতিল", color = Color(0xFF1E3A8A), fontWeight = FontWeight.Medium)
                }
            }
        }
    )
}

fun showNoticeNotification(context: Context, notice: AppNotice) {
    if (notice.id == null) return
    
    val sharedPrefs = PrefUtils.getSecurePrefs(context)
    val lastNotifiedId = sharedPrefs.getInt("last_notified_notice_id", -1)
    
    // Only notify if we haven't notified for this notice ID yet
    if (notice.id != lastNotifiedId) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "announcements_channel_v1"
        
        L.init(context)
        val isBn = L.currentLanguage == "bn"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                if (isBn) "জরুরি নোটিশ ও ঘোষণা" else "Announcements & Notices",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = if (isBn) "গুরুত্বপূর্ণ ঘোষণা এবং জরুরি নোটিশ" else "Important announcements and notices"
                enableVibration(true)
                val defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(defaultSoundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Intent to open app
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val displayTitle = L.translateNotice(notice.title)
        val displayContent = L.translateNotice(notice.content)
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(displayTitle)
            .setContentText(displayContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(displayContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(notice.id, notification)
        
        // Save to preferences to avoid double notification
        sharedPrefs.edit().putInt("last_notified_notice_id", notice.id).apply()
    }
}
