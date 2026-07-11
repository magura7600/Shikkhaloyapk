package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

fun isDirectApkUrl(url: String): Boolean {
    val lower = url.trim().lowercase()
    if (!lower.startsWith("http")) return false
    
    // Known cloud storage domains or web portals
    if (lower.contains("mega.nz") || 
        lower.contains("drive.google.com") || 
        lower.contains("docs.google.com") || 
        lower.contains("dropbox.com") || 
        lower.contains("mediafire.com") || 
        lower.contains("facebook.com") || 
        lower.contains("l.facebook.com") || 
        lower.contains("t.me") || 
        lower.contains("telegram.org") || 
        lower.contains("play.google.com") ||
        (lower.contains("github.com") && !lower.contains("/releases/download/"))
    ) {
        return false
    }
    
    val cleanUrl = url.split("?")[0]
    return cleanUrl.endsWith(".apk", ignoreCase = true)
}

@Composable
fun UpdatePromptDialog(
    update: AppUpdate,
    accentColor: Color,
    isAdmin: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloadState by AppUpdateManager.downloadState.collectAsState()
    
    val isForce = update.is_force_update && !isAdmin
    val isDirect = remember(update.apk_url) { isDirectApkUrl(update.apk_url) }

    AlertDialog(
        onDismissRequest = {
            if (!isForce && downloadState !is UpdateDownloadState.Downloading) {
                onDismiss()
            }
        },
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = "Update",
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "নতুন আপডেট উপলব্ধ! 🚀",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "বর্তমান ভার্সন: v${AppUpdateManager.getCurrentVersionName(context)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "নতুন ভার্সন: v${update.version_name}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "আপডেটে যা থাকছে:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = update.changelog.ifBlank { "রিলিজ নোট বা কোনো বিবরণ দেওয়া হয়নি।" },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    lineHeight = 20.sp
                )

                if (isForce) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "⚠️ এই আপডেটটি করা বাধ্যতামূলক!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                }

                // Telegram channel join card
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/ShikkhaloyAi"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "টেলিগ্রাম লিংক ওপেন করা সম্ভব হয়নি।", Toast.LENGTH_SHORT).show()
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Telegram Channel",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "টেলিগ্রাম চ্যানেলে যুক্ত হোন 📢",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "নতুন ভার্সন ও সকল আপডেট সবার আগে পেতে আমাদের অফিশিয়াল চ্যানেলে যোগ দিন।",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                if (!isDirect) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Cloud Link Info",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "এটি একটি এক্সটার্নাল/ক্লাউড লিংক (যেমন: Mega, Drive, Telegram)। বাটনটিতে ক্লিক করলে এটি আপনার ব্রাউজারে বা সংশ্লিষ্ট অ্যাপে ওপেন হবে এবং সেখান থেকে আপনি নতুন ভার্সনটি নামাতে পারবেন।",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Progress UI based on Download State (only if direct APK)
                if (isDirect) {
                    when (val state = downloadState) {
                        is UpdateDownloadState.Downloading -> {
                            val pct = (state.progress * 100).toInt()
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "ডাউনলোড হচ্ছে... $pct%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = accentColor,
                                    trackColor = accentColor.copy(alpha = 0.2f)
                                )
                            }
                        }
                        is UpdateDownloadState.Success -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ডাউনলোড সফল হয়েছে!",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        is UpdateDownloadState.Error -> {
                            Text(
                                text = "ত্রুটি: ${state.message}",
                                fontSize = 12.sp,
                                color = Color.Red,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        else -> {}
                    }
                }
            }
        },
        confirmButton = {
            val state = downloadState
            Button(
                onClick = {
                    if (!isDirect) {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(update.apk_url))
                            context.startActivity(intent)
                            Toast.makeText(context, "ক্লাউড/ড্রাইভ লিংক ব্রাউজারে ওপেন করা হচ্ছে...", Toast.LENGTH_SHORT).show()
                            if (!isForce) {
                                onDismiss()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "লিংক ওপেন করা সম্ভব হয়নি।", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        when (state) {
                            is UpdateDownloadState.Success -> {
                                AppUpdateManager.installApk(context, state.apkFile)
                            }
                            is UpdateDownloadState.Downloading -> {
                                // Do nothing, downloading
                            }
                            else -> {
                                scope.launch {
                                    AppUpdateManager.downloadApk(context, update.apk_url, update.sha256_checksum)
                                }
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state is UpdateDownloadState.Success && isDirect) MaterialTheme.colorScheme.secondary else accentColor
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (!isDirect) {
                    if (update.apk_url.contains("t.me") || update.apk_url.contains("telegram")) {
                        Text("টেলিগ্রাম থেকে আপডেট করুন 📢")
                    } else {
                        Text("ব্রাউজারে আপডেট করুন 🌐")
                    }
                } else {
                    when (state) {
                        is UpdateDownloadState.Downloading -> {
                            Text("ডাউনলোড হচ্ছে...")
                        }
                        is UpdateDownloadState.Success -> {
                            Text("ইনস্টল করুন ⚙️")
                        }
                        is UpdateDownloadState.Error -> {
                            Text("আবার চেষ্টা করুন 🔄")
                        }
                        else -> {
                            Text("আপডেট করুন 📥")
                        }
                    }
                }
            }
        },
        dismissButton = {
            val state = downloadState
            if (!isForce && state !is UpdateDownloadState.Downloading) {
                TextButton(onClick = {
                    AppUpdateManager.resetState()
                    onDismiss()
                }) {
                    Text("পরে করুন", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    )
}

@Composable
fun PublishUpdateDialog(
    accentColor: Color,
    existingUpdate: AppUpdate? = null,
    onDismiss: () -> Unit,
    onPublished: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var versionCodeInput by remember { mutableStateOf(existingUpdate?.version_code?.toString() ?: "") }
    var versionNameInput by remember { mutableStateOf(existingUpdate?.version_name ?: "") }
    var apkUrlInput by remember { mutableStateOf(existingUpdate?.apk_url ?: "") }
    var changelogInput by remember { mutableStateOf(existingUpdate?.changelog ?: "") }
    var isForceUpdate by remember { mutableStateOf(existingUpdate?.is_force_update ?: false) }
    
    var isSubmitting by remember { mutableStateOf(false) }
    var showSqlInstructions by remember { mutableStateOf(false) }

    val sqlSchema = """
-- Supabase SQL Editor এ এই কোডটি রান করুন:
CREATE TABLE IF NOT EXISTS app_updates (
    id SERIAL PRIMARY KEY,
    version_code INT NOT NULL,
    version_name TEXT NOT NULL,
    apk_url TEXT NOT NULL,
    changelog TEXT DEFAULT '',
    is_force_update BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW())
);

-- RLS (Row Level Security) পলিসি
ALTER TABLE app_updates ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow public read app_updates" ON app_updates;
DROP POLICY IF EXISTS "Allow authenticated insert app_updates" ON app_updates;
DROP POLICY IF EXISTS "Allow all app_updates" ON app_updates;

CREATE POLICY "Allow all app_updates" ON app_updates
    FOR ALL TO public USING (true) WITH CHECK (true);
    """.trimIndent()

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = "Publish",
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = if (existingUpdate != null) "আপডেট এডিট করুন ✏️" else "নতুন আপডেট রিলিজ করুন 📣",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = versionCodeInput,
                    onValueChange = { versionCodeInput = it },
                    label = { Text("ভার্সন কোড (Version Code)") },
                    placeholder = { Text("যেমন: 2") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = versionNameInput,
                    onValueChange = { versionNameInput = it },
                    label = { Text("ভার্সন নাম (Version Name)") },
                    placeholder = { Text("যেমন: 2.0") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = apkUrlInput,
                    onValueChange = { apkUrlInput = it },
                    label = { Text("এপিকে ডাউনলোড লিংক (APK Download URL)") },
                    placeholder = { Text("https://example.com/app.apk") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = changelogInput,
                    onValueChange = { changelogInput = it },
                    label = { Text("আপডেট বিবরণ / চ্যাঞ্জেলগ") },
                    placeholder = { Text("১. নতুন ফিচার যোগ করা হয়েছে।\n২. বাগ ফিক্স করা হয়েছে।") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "বাধ্যতামূলক আপডেট (Force Update)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "শিক্ষার্থীদের অবশ্যই আপডেট করতে হবে",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Switch(
                        checked = isForceUpdate,
                        onCheckedChange = { isForceUpdate = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Database Table Setup Instructions inside popup
                Button(
                    onClick = { showSqlInstructions = !showSqlInstructions },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (showSqlInstructions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle SQL"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Supabase ডাটাবেজ টেবিল সেটআপ গাইড 🛠️", fontSize = 12.sp)
                }

                if (showSqlInstructions) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SQL Script",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(sqlSchema))
                                    Toast.makeText(context, "SQL কপি হয়েছে!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sqlSchema,
                            color = MaterialTheme.colorScheme.surfaceVariant,
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
                    val vCode = versionCodeInput.toIntOrNull()
                    if (vCode == null || vCode <= 0) {
                        Toast.makeText(context, "দয়া করে সঠিক ভার্সন কোড দিন!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (versionNameInput.isBlank()) {
                        Toast.makeText(context, "দয়া করে ভার্সন নাম দিন!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (apkUrlInput.isBlank() || !apkUrlInput.startsWith("http")) {
                        Toast.makeText(context, "সঠিক APK URL লিংক দিন!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isSubmitting = true
                    scope.launch {
                        val updateRecord = AppUpdate(
                            id = existingUpdate?.id,
                            version_code = vCode,
                            version_name = versionNameInput,
                            apk_url = apkUrlInput,
                            changelog = changelogInput,
                            is_force_update = isForceUpdate
                        )
                        val result = if (existingUpdate != null) {
                            AppUpdateManager.updateUpdate(updateRecord)
                        } else {
                            AppUpdateManager.publishUpdate(updateRecord)
                        }
                        isSubmitting = false
                        if (result.isSuccess) {
                            val successMsg = if (existingUpdate != null) "আপডেট সফলভাবে এডিট হয়েছে! ✏️" else "আপডেট সফলভাবে পাবলিশ হয়েছে! 🎉"
                            Toast.makeText(context, successMsg, Toast.LENGTH_LONG).show()
                            onPublished()
                            onDismiss()
                        } else {
                            val errMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                            val failMsg = if (existingUpdate != null) "এডিট ব্যর্থ হয়েছে: $errMsg" else "পাবলিশ ব্যর্থ হয়েছে: $errMsg"
                            Toast.makeText(context, failMsg, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (existingUpdate != null) "সংরক্ষণ করুন 💾" else "পাবলিশ করুন 🚀")
                }
            }
        },
        dismissButton = {
            if (!isSubmitting) {
                TextButton(onClick = onDismiss) {
                    Text("বাতিল", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    )
}
