package com.example

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardContent(
    accentColor: Color,
    onPublishUpdateClick: () -> Unit,
    onPublishNoticeClick: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var searchQuery by remember { mutableStateOf("") }
    var allProfiles by remember { mutableStateOf(listOf<UserProfile>()) }
    var isLoadingProfiles by remember { mutableStateOf(false) }
    var latestNotice by remember { mutableStateOf<AppNotice?>(null) }
    var latestAppUpdate by remember { mutableStateOf<AppUpdate?>(null) }
    var showSqlDialog by remember { mutableStateOf(false) }
    var showEditUpdateDialog by remember { mutableStateOf(false) }
    var showDeleteUpdateConfirm by remember { mutableStateOf(false) }
    var isDeletingUpdate by remember { mutableStateOf(false) }

    // Fetch all profiles & active notice on load
    val fetchAllData = {
        isLoadingProfiles = true
        scope.launch {
            try {
                val fetched = withContext(Dispatchers.IO) {
                    supabase.from("profiles").select().decodeList<UserProfile>()
                }
                allProfiles = fetched
                
                val notice = AppNoticeManager.getActiveNotice()
                latestNotice = notice

                // Fetch latest app update
                val update = AppUpdateManager.getLatestUpdate()
                latestAppUpdate = update
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingProfiles = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchAllData()
    }

    val filteredProfiles = allProfiles.filter {
        it.full_name.contains(searchQuery, ignoreCase = true) ||
        it.email.contains(searchQuery, ignoreCase = true) ||
        it.uid_code.contains(searchQuery, ignoreCase = true)
    }

    val sqlDbUpdate = """
-- ১. Profiles টেবিলে is_banned কলাম যোগ করতে রান করুন:
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS is_banned BOOLEAN DEFAULT FALSE;

-- ২. Courses টেবিলে কোয়ার্টার ও বিষয়সমূহ যোগ করতে রান করুন:
ALTER TABLE courses ADD COLUMN IF NOT EXISTS "isQuarterOn" BOOLEAN DEFAULT FALSE;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS "quarters" JSONB DEFAULT '[]'::jsonb;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS "subjects" JSONB DEFAULT '[]'::jsonb;

-- ৩. Enrollments টেবিলে প্রয়োজনীয় কলামসমূহ যোগ করতে রান করুন:
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS price_paid TEXT DEFAULT '';
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS purchased_quarters TEXT DEFAULT '';
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS banned_until BIGINT;
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS ban_reason TEXT;
    """.trimIndent()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (onBack != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.DarkGray
                        )
                    }
                    Text(
                        text = "ফিরে যান (Back)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            // Admin Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = accentColor),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "প্রশাসক প্যানেল (Admin Panel)",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "অ্যাপের সার্বিক নিয়ন্ত্রণ ও তদারকি",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        // Quick Stats/Overview Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatsCard(
                    title = "মোট ইউজার",
                    value = "${allProfiles.size}",
                    icon = Icons.Outlined.People,
                    color = accentColor,
                    modifier = Modifier.weight(1f)
                )
                StatsCard(
                    title = "ব্যানড অ্যাকাউন্ট",
                    value = "${allProfiles.count { it.is_banned }}",
                    icon = Icons.Outlined.Block,
                    color = Color.Red,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Database Fix SQL card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSqlDialog = true },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Fix DB",
                        tint = Color(0xFFD97706)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ডাটাবেজ টেবিল আপডেট গাইড",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "ইউজার ব্যান সিস্টেম সচল করতে প্রয়োজনীয় SQL রান করুন",
                            fontSize = 11.sp,
                            color = Color(0xFFB45309)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "More",
                        tint = Color(0xFFD97706)
                    )
                }
            }
        }

        // Management Controls Section
        item {
            Text(
                text = "জরুরি নিয়ন্ত্রণ (Emergency Actions)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF1E293B)
            )
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Notice Control Button & Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = "Notice",
                                    tint = accentColor
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "জরুরি নোটিশ প্রচার",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF1E293B)
                                )
                            }
                            Button(
                                onClick = onPublishNoticeClick,
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("নতুন নোটিশ", fontSize = 11.sp)
                            }
                        }

                        if (latestNotice != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = latestNotice!!.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF334155)
                                        )
                                        Text(
                                            text = "সক্রিয় 🟢",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = latestNotice!!.content,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                val res = AppNoticeManager.clearActiveNotice()
                                                if (res.isSuccess) {
                                                    Toast.makeText(context, "নোটিশটি নিষ্ক্রিয় করা হয়েছে", Toast.LENGTH_SHORT).show()
                                                    fetchAllData()
                                                } else {
                                                    Toast.makeText(context, "ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("নিষ্ক্রিয় করুন 🛑", fontSize = 10.sp)
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "বর্তমানে কোনো সক্রিয় নোটিশ নেই।",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // App Update Release Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = "Update",
                                    tint = Color(0xFF10B981)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "নতুন এপ সংস্করণ রিলিজ",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "এপিকে ফাইল আপডেট ও ফোর্সমোড পরিচালনা",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Button(
                                onClick = onPublishUpdateClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = "Upload", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("আপডেট দিন", fontSize = 11.sp)
                            }
                        }

                        if (latestAppUpdate != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "সর্বশেষ রিলিজ: v${latestAppUpdate!!.version_name}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF334155)
                                            )
                                            Text(
                                                text = "ভার্সন কোড: ${latestAppUpdate!!.version_code}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                        Text(
                                            text = if (latestAppUpdate!!.is_force_update) "বাধ্যতামূলক ⚠️" else "ঐচ্ছিক 🟢",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (latestAppUpdate!!.is_force_update) Color.Red else Color(0xFF10B981)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "চ্যাঞ্জেলগ: ${latestAppUpdate!!.changelog.ifBlank { "কোন বিবরণ নেই" }}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF475569)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "লিংক: ${latestAppUpdate!!.apk_url}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF0284C7),
                                        maxLines = 1
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = { showEditUpdateDialog = true },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F766E)),
                                            border = BorderStroke(1.dp, Color(0xFF0F766E).copy(alpha = 0.5f)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("এডিট করুন ✏️", fontSize = 10.sp)
                                        }

                                        OutlinedButton(
                                            onClick = { showDeleteUpdateConfirm = true },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("ডিলিট করুন 🗑️", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "বর্তমানে কোনো অ্যাপ আপডেট রেকর্ড নেই।",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // User Accounts & Ban Management
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ইউজার অ্যাকাউন্ট নিয়ন্ত্রণ (Ban/Unban)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1E293B)
                )
                IconButton(onClick = { fetchAllData() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = accentColor)
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("নাম, ইমেইল বা UID কোড দিয়ে খুঁজুন") },
                placeholder = { Text("উদা: রাইহান তানভীর বা SL-...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                }
            )
        }

        if (isLoadingProfiles) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accentColor)
                }
            }
        } else {
            if (filteredProfiles.isEmpty()) {
                item {
                    Text(
                        text = "কোনো অ্যাকাউন্ট পাওয়া যায়নি।",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }
            } else {
                items(filteredProfiles) { user ->
                    UserManagementRow(
                        user = user,
                        accentColor = accentColor,
                        onStatusChanged = { fetchAllData() }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showSqlDialog) {
        AlertDialog(
            onDismissRequest = { showSqlDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Build, contentDescription = "SQL", tint = accentColor)
                    Text("ডাটাবেজ কলাম আপডেট", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "অ্যাপের নতুন ফিচারসমূহ (যেমন: ইউজার ব্যান, কোয়ার্টার, ও বিষয়ভিত্তিক ক্লাসসমূহ) সঠিকভাবে কাজ করার জন্য নিচের SQL কোডটি কপি করে আপনার Supabase SQL Editor-এ রান করে নিন:",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
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
                                    clipboardManager.setText(AnnotatedString(sqlDbUpdate))
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
                            text = sqlDbUpdate,
                            color = Color(0xFFE2E8F0),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSqlDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = accentColor)) {
                    Text("ঠিক আছে")
                }
            }
        )
    }

    if (showEditUpdateDialog && latestAppUpdate != null) {
        PublishUpdateDialog(
            accentColor = accentColor,
            existingUpdate = latestAppUpdate,
            onDismiss = { showEditUpdateDialog = false },
            onPublished = {
                fetchAllData()
            }
        )
    }

    if (showDeleteUpdateConfirm && latestAppUpdate != null) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingUpdate) showDeleteUpdateConfirm = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    Text("আপডেট ডিলিট করুন 🗑️", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text("আপনি কি নিশ্চিতভাবে সর্বশেষ রিলিজ হওয়া আপডেটটি (v${latestAppUpdate!!.version_name}) ডিলিট করতে চান? এটি করার পর ইউজাররা আর আপডেট চেক বা ডাউনলোড করতে পারবেন না।")
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeletingUpdate = true
                        scope.launch {
                            val res = AppUpdateManager.deleteUpdate(latestAppUpdate!!.id ?: 0)
                            isDeletingUpdate = false
                            showDeleteUpdateConfirm = false
                            if (res.isSuccess) {
                                Toast.makeText(context, "আপডেট সফলভাবে ডিলিট করা হয়েছে! 🗑️", Toast.LENGTH_SHORT).show()
                                fetchAllData()
                            } else {
                                Toast.makeText(context, "ডিলিট করতে ব্যর্থ হয়েছে।", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    enabled = !isDeletingUpdate,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isDeletingUpdate) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("ডিলিট করুন 🗑️")
                    }
                }
            },
            dismissButton = {
                if (!isDeletingUpdate) {
                    TextButton(onClick = { showDeleteUpdateConfirm = false }) {
                        Text("বাতিল", color = Color(0xFF64748B))
                    }
                }
            }
        )
    }
}

@Composable
fun StatsCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
            }
            Text(text = title, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        }
    }
}

@Composable
fun UserManagementRow(
    user: UserProfile,
    accentColor: Color,
    onStatusChanged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isUpdating by remember { mutableStateOf(false) }

    val isSelfAdmin = user.role == "admin"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (user.is_banned) Color.Red.copy(alpha = 0.3f) else Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                ) {
                    if (!user.profile_image_url.isNullOrBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(user.profile_image_url),
                            contentDescription = "Profile Pic",
                            modifier = Modifier.fillMaxSize().statusBarsPadding(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Default Profile",
                            tint = Color.Gray,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.full_name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Role Badge
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when (user.role) {
                                    "admin" -> Color.Red.copy(alpha = 0.1f)
                                    "teacher" -> accentColor.copy(alpha = 0.1f)
                                    else -> Color.Gray.copy(alpha = 0.1f)
                                }
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = when (user.role) {
                                    "admin" -> "Admin"
                                    "teacher" -> "Teacher"
                                    else -> "Student"
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (user.role) {
                                    "admin" -> Color.Red
                                    "teacher" -> accentColor
                                    else -> Color.DarkGray
                                },
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "UID: ${user.uid_code} • ${user.email}",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    if (user.is_banned) {
                        Text(
                            text = "❌ নিষিদ্ধ অ্যাকাউন্ট (Banned)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (!isSelfAdmin) {
                Button(
                    onClick = {
                        isUpdating = true
                        scope.launch {
                            try {
                                val targetStatus = !user.is_banned
                                withContext(Dispatchers.IO) {
                                    supabase.from("profiles").update(
                                        {
                                            set("is_banned", targetStatus)
                                        }
                                    ) {
                                        filter { eq("user_id", user.user_id) }
                                    }
                                }
                                Toast.makeText(
                                    context,
                                    if (targetStatus) "${user.full_name} কে ব্যান করা হয়েছে।" else "${user.full_name} কে আনব্যান করা হয়েছে।",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onStatusChanged()
                            } catch (e: Exception) {
                                Toast.makeText(context, "ব্যর্থ হয়েছে: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isUpdating = false
                            }
                        }
                    },
                    enabled = !isUpdating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (user.is_banned) Color(0xFF10B981) else Color.Red
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (user.is_banned) "আনব্যান" else "ব্যান করুন",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
