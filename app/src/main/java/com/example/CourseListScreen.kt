package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CourseListScreen(
    accentColor: Color, 
    courses: List<CourseItem>,
    onCourseClick: (CourseItem) -> Unit = {}
) {

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(
                "আমার কোর্সসমূহ",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF4A5568)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "আপনার তৈরি করা সকল কোর্স এখানে দেখুন",
                fontSize = 14.sp,
                color = Color(0xFF718096)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(courses.size) { index ->
            val course = courses[index]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCourseClick(course) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Book, contentDescription = "Course", tint = accentColor, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(course.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2D3748))
                        Text(course.description, fontSize = 12.sp, color = Color(0xFF718096), maxLines = 1)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(course.pricingOption, fontSize = 12.sp, color = accentColor, fontWeight = FontWeight.Bold)
                            if (course.pricingOption != "Fully Free" && course.mainPrice.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("৳${course.mainPrice}", fontSize = 12.sp, color = Color.Gray)
                            }
                            if (course.isQuarterOn && course.quarters.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("${course.quarters.size} Quarters", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}
