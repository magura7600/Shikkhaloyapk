package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ArtTrack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

@Composable
fun CourseListScreen(
    accentColor: Color, 
    courses: List<CourseItem>,
    title: String = "আমার কোর্সসমূহ",
    subtitle: String = "আপনার তৈরি করা সকল কোর্স এখানে দেখুন",
    onCourseClick: (CourseItem) -> Unit = {}
) {

    // Helper to get diverse banner backgrounds
    val bannerColors = listOf(
        listOf(Color(0xFFE0F2F1), Color(0xFFB2DFDB)), // Teal
        listOf(Color(0xFFE8EAF6), Color(0xFFC5CAE9)), // Indigo
        listOf(Color(0xFFF3E5F5), Color(0xFFE1BEE7)), // Purple
        listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB)), // Blue
        listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2))  // Orange
    )
    val bannerIcons = listOf(
        Icons.Default.Science,
        Icons.Default.Calculate,
        Icons.Default.Computer,
        Icons.Default.Language,
        Icons.Default.ArtTrack
    )
    val bannerIconColors = listOf(
        Color(0xFF00796B),
        Color(0xFF303F9F),
        Color(0xFF7B1FA2),
        Color(0xFF1976D2),
        Color(0xFFF57C00)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC)),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 100.dp)
    ) {
        item {
            Text(
                title,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF2D3748)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                subtitle,
                fontSize = 14.sp,
                color = Color(0xFF718096)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(courses.size) { index ->
            val course = courses[index]
            val colorIndex = course.id.hashCode().absoluteValue % bannerColors.size
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(20.dp),
                        spotColor = Color(0x1A000000),
                        ambientColor = Color(0x0D000000)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onCourseClick(course) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column {
                    // Banner Image Placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(
                                Brush.horizontalGradient(bannerColors[colorIndex])
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = bannerIcons[colorIndex],
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = bannerIconColors[colorIndex].copy(alpha = 0.5f)
                        )
                    }

                    Column(modifier = Modifier.padding(20.dp)) {
                        // Title and Progress icon
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = course.title,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("0%", fontSize = 14.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Description
                        Text(
                            text = course.description.ifBlank { "No description available for this course." },
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 18.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Progress bar
                        LinearProgressIndicator(
                            progress = { 0.0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = accentColor,
                            trackColor = Color(0xFFF1F5F9)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Footer row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = course.pricingOption,
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            HorizontalDivider(
                                modifier = Modifier.height(12.dp).width(1.dp),
                                color = Color(0xFFE2E8F0)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF94A3B8))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${course.studentsCount} Enrollments",
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = { onCourseClick(course) },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 2.dp)
                            ) {
                                Text("Continue", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
