package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

@Composable
fun UpdatePromptDialog(
    update: AppUpdate,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloadState by AppUpdateManager.downloadState.collectAsState()

    AlertDialog(
        onDismissRequest = {
            if (!update.is_force_update && downloadState !is UpdateDownloadState.Downloading) {
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
                    color = Color(0xFF1E293B)
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
                                color = Color(0xFF64748B)
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
                    color = Color(0xFF1E293B)
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = update.changelog.ifBlank { "রিলিজ নোট বা কোনো বিবরণ দেওয়া হয়নি।" },
                    fontSize = 13.sp,
                    color = Color(0xFF475569),
                    lineHeight = 20.sp
                )

                if (update.is_force_update) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "⚠️ এই আপডেটটি করা বাধ্যতামূলক!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Progress UI based on Download State
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
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ডাউনলোড সফল হয়েছে!",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
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
        },
        confirmButton = {
            val state = downloadState
            Button(
                onClick = {
                    when (state) {
                        is UpdateDownloadState.Success -> {
                            AppUpdateManager.installApk(context, state.apkFile)
                        }
                        is UpdateDownloadState.Downloading -> {
                            // Do nothing, downloading
                        }
                        else -> {
                            scope.launch {
                                AppUpdateManager.downloadApk(context, update.apk_url)
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state is UpdateDownloadState.Success) Color(0xFF10B981) else accentColor
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
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
        },
        dismissButton = {
            val state = downloadState
            if (!update.is_force_update && state !is UpdateDownloadState.Downloading) {
                TextButton(onClick = {
                    AppUpdateManager.resetState()
                    onDismiss()
                }) {
                    Text("পরে করুন", color = Color(0xFF64748B))
                }
            }
        }
    )
}

@Composable
fun PublishUpdateDialog(
    accentColor: Color,
    onDismiss: () -> Unit,
    onPublished: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var versionCodeInput by remember { mutableStateOf("") }
    var versionNameInput by remember { mutableStateOf("") }
    var apkUrlInput by remember { mutableStateOf("") }
    var changelogInput by remember { mutableStateOf("") }
    var isForceUpdate by remember { mutableStateOf(false) }
    
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
                    text = "নতুন আপডেট রিলিজ করুন 📣",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1E293B)
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
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "শিক্ষার্থীদের অবশ্যই আপডেট করতে হবে",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Switch(
                        checked = isForceUpdate,
                        onCheckedChange = { isForceUpdate = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                    )
                }

                Divider(color = Color(0xFFE2E8F0))

                // Database Table Setup Instructions inside popup
                Button(
                    onClick = { showSqlInstructions = !showSqlInstructions },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = Color(0xFF475569)
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
                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SQL Script",
                                color = Color(0xFF38BDF8),
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
                            color = Color(0xFFE2E8F0),
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
                            version_code = vCode,
                            version_name = versionNameInput,
                            apk_url = apkUrlInput,
                            changelog = changelogInput,
                            is_force_update = isForceUpdate
                        )
                        val result = AppUpdateManager.publishUpdate(updateRecord)
                        isSubmitting = false
                        if (result.isSuccess) {
                            Toast.makeText(context, "আপডেট সফলভাবে পাবলিশ হয়েছে! 🎉", Toast.LENGTH_LONG).show()
                            onPublished()
                            onDismiss()
                        } else {
                            val errMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                            Toast.makeText(context, "পাবলিশ ব্যর্থ হয়েছে: $errMsg", Toast.LENGTH_LONG).show()
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
                    Text("পাবলিশ করুন 🚀")
                }
            }
        },
        dismissButton = {
            if (!isSubmitting) {
                TextButton(onClick = onDismiss) {
                    Text("বাতিল", color = Color(0xFF64748B))
                }
            }
        }
    )
}
