package com.example

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import io.github.jan.supabase.postgrest.from

@Composable
fun EnrollmentRequestsScreen(
    teacherChannel: UserProfile,
    requests: List<EnrollmentRequest>,
    courses: List<CourseItem>,
    accentColor: Color,
    onBack: () -> Unit,
    onUpdateRequests: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Filter requests for courses belonging to this teacher
    val teacherCourseIds = courses.filter { it.channel_id == teacherChannel.user_id }.map { it.id }
    val teacherRequests = requests.filter { it.course_id in teacherCourseIds }.sortedByDescending { it.created_at }

    var showRejectDialogFor by remember { mutableStateOf<EnrollmentRequest?>(null) }
    var rejectReason by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFBF8F1))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("কোর্স কেনা রিকোয়েস্ট", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFFFBF8F1)
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (teacherRequests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("কোনো রিকোয়েস্ট নেই", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(teacherRequests) { request ->
                        val course = courses.find { it.id == request.course_id }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Course: ${course?.title ?: "Unknown"}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("User ID: ${request.user_id}", fontSize = 12.sp, color = Color.Gray)
                                Text("Requested: ${request.requested_quarters}", fontSize = 14.sp)
                                Text("Amount: ৳${request.amount} (${request.payment_method})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                Text("Sender: ${request.sender_number}", fontSize = 14.sp)
                                Text("TrxID: ${request.transaction_id}", fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val statusColor = when (request.status) {
                                    "PENDING" -> Color(0xFFEAB308)
                                    "APPROVED" -> Color(0xFF22C55E)
                                    else -> Color(0xFFEF4444)
                                }
                                Text("Status: ${request.status}", color = statusColor, fontWeight = FontWeight.Bold)

                                if (request.status == "PENDING") {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    isProcessing = true
                                                    try {
                                                        // 1. Create Enrollment
                                                        val enrollment = Enrollment(
                                                            user_id = request.user_id,
                                                            course_id = request.course_id,
                                                            price_paid = request.amount,
                                                            purchased_quarters = if (request.requested_quarters == "FULL") "" else request.requested_quarters
                                                        )
                                                        supabase.from("enrollments").insert(enrollment)
                                                        
                                                        // 2. Update Request Status
                                                        supabase.from("enrollment_requests").update(
                                                            {
                                                                set("status", "APPROVED")
                                                            }
                                                        ) {
                                                            filter { eq("id", request.id) }
                                                        }
                                                        Toast.makeText(context, "Approved!", Toast.LENGTH_SHORT).show()
                                                        onUpdateRequests()
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                                    } finally {
                                                        isProcessing = false
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                            enabled = !isProcessing,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Approve")
                                        }
                                        
                                        OutlinedButton(
                                            onClick = { showRejectDialogFor = request },
                                            enabled = !isProcessing,
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                        ) {
                                            Text("Reject")
                                        }
                                    }
                                } else if (request.status == "REJECTED" && request.rejection_reason.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Reason: ${request.rejection_reason}", color = Color.Red, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRejectDialogFor != null) {
        AlertDialog(
            onDismissRequest = { showRejectDialogFor = null },
            title = { Text("Reject Request") },
            text = {
                Column {
                    Text("Please provide a reason for rejection:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val req = showRejectDialogFor!!
                        coroutineScope.launch {
                            isProcessing = true
                            try {
                                supabase.from("enrollment_requests").update(
                                    {
                                        set("status", "REJECTED")
                                        set("rejection_reason", rejectReason)
                                    }
                                ) {
                                    filter { eq("id", req.id) }
                                }
                                Toast.makeText(context, "Rejected!", Toast.LENGTH_SHORT).show()
                                showRejectDialogFor = null
                                rejectReason = ""
                                onUpdateRequests()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isProcessing = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Reject")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialogFor = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
