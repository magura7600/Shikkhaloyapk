package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MyEnrollmentsScreen(
    profile: UserProfile,
    enrollments: List<Enrollment>,
    requests: List<EnrollmentRequest>,
    courses: List<CourseItem>,
    accentColor: Color,
    onBack: () -> Unit
) {
    val myRequests = requests.filter { it.user_id == profile.user_id }.sortedByDescending { it.created_at }
    val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("আমার ভর্তি তথ্য", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (myRequests.isEmpty() && enrollments.none { it.user_id == profile.user_id }) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("আপনি কোনো কোর্সে এনরোল করেননি।", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(myRequests) { req ->
                        val course = courses.find { it.id == req.course_id }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(course?.title ?: "Unknown Course", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text("প্যাকেজ: ${if (req.requested_quarters == "FULL") "সম্পূর্ণ কোর্স" else req.requested_quarters}", fontSize = 14.sp)
                                Text("পরিশোধিত মূল্য: ৳${req.amount}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("পেমেন্ট মেথড: ${req.payment_method}", fontSize = 14.sp)
                                Text("TrxID: ${req.transaction_id}", fontSize = 14.sp)
                                
                                val statusColor = when (req.status) {
                                    "PENDING" -> MaterialTheme.colorScheme.error
                                    "APPROVED" -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.error
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("স্ট্যাটাস: ", fontSize = 14.sp)
                                    Text(req.status, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                if (req.status == "REJECTED" && req.rejection_reason.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("কারণ: ${req.rejection_reason}", color = Color.Red, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
