package com.example
import android.content.Context
import android.view.WindowManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.FirebaseException
import java.util.concurrent.TimeUnit
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.example.ui.CustomBottomNavigation
import com.example.ui.BottomNavItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun TeacherDashboardContent(accentColor: Color, onChannelClick: () -> Unit, onAddCourseClick: () -> Unit, onMentorsClick: () -> Unit, onManageStudentsClick: () -> Unit, onAddClassLinkClick: () -> Unit, onEnrollmentRequestsClick: () -> Unit) {
    val items = listOf(
        Pair("সকল কোর্স", Icons.Default.LibraryBooks),
        Pair("ক্লাস যোগ", Icons.Default.AddBox),
        Pair("ক্লাস লিংক যোগ", Icons.Default.AddLink),
        Pair("চ্যানেল", Icons.Default.LiveTv),
        Pair("হোম ওয়ার্ক", Icons.Default.Assignment),
        Pair("শিক্ষার্থীদের পরিচালনা", Icons.Default.People),
        Pair("মেন্টর তালিকা", Icons.Default.GroupAdd),
        Pair("কোর্স কেনা রিকোয়েস্ট", Icons.Outlined.Notifications)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
    ) {
        item {
            Text(
                "শিক্ষক ড্যাশবোর্ড".t(),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "আপনার ক্লাসরুম ও কোর্সের কাজগুলো পরিচালনা করুন".t(),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                for (i in items.indices step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TeacherDashboardCard(
                            title = items[i].first.t(),
                            icon = items[i].second,
                            accentColor = accentColor,
                            onClick = { 
                                if (items[i].first == "চ্যানেল") onChannelClick()
                                else if (items[i].first == "ক্লাস যোগ") onAddCourseClick()
                                else if (items[i].first == "মেন্টর তালিকা") onMentorsClick()
                                else if (items[i].first == "শিক্ষার্থীদের পরিচালনা") onManageStudentsClick()
                                else if (items[i].first == "ক্লাস লিংক যোগ") onAddClassLinkClick()
                                else if (items[i].first == "কোর্স কেনা রিকোয়েস্ট") onEnrollmentRequestsClick()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (i + 1 < items.size) {
                            TeacherDashboardCard(
                                title = items[i + 1].first.t(),
                                icon = items[i + 1].second,
                                accentColor = accentColor,
                                onClick = { 
                                    if (items[i + 1].first == "চ্যানেল") onChannelClick()
                                    else if (items[i + 1].first == "ক্লাস যোগ") onAddCourseClick()
                                    else if (items[i + 1].first == "মেন্টর তালিকা") onMentorsClick()
                                    else if (items[i + 1].first == "শিক্ষার্থীদের পরিচালনা") onManageStudentsClick()
                                    else if (items[i + 1].first == "ক্লাস লিংক যোগ") onAddClassLinkClick()
                                    else if (items[i + 1].first == "কোর্স কেনা রিকোয়েস্ট") onEnrollmentRequestsClick()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TeacherDashboardCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray,
                textAlign = TextAlign.Center
            )
        }
    }
}

