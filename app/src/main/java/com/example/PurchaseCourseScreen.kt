package com.example

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import io.github.jan.supabase.postgrest.from

@Composable
fun PurchaseCourseScreen(
    course: CourseItem,
    profile: UserProfile,
    accentColor: Color,
    onBack: () -> Unit,
    onRequestEnrollment: (EnrollmentRequest) -> Unit = {},
    onPurchaseSubmitted: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    
    var selectFullCourse by remember { mutableStateOf(true) }
    var selectedQuarters by remember { mutableStateOf(setOf<CourseQuarter>()) }
    
    var paymentMethod by remember { mutableStateOf("Bkash") }
    var senderNumber by remember { mutableStateOf("") }
    var transactionId by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val totalPrice = if (selectFullCourse) {
        course.discountPrice.toDoubleOrNull() ?: course.mainPrice.toDoubleOrNull() ?: 0.0
    } else {
        selectedQuarters.sumOf { it.price.toDoubleOrNull() ?: 0.0 }
    }

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
                Text("কোর্স কিনুন", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(course.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (course.isQuarterOn && course.quarters.isNotEmpty()) {
                Text("প্যাকেজ নির্বাচন করুন:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectFullCourse,
                        onClick = { selectFullCourse = true },
                        colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                    )
                    Text("সম্পূর্ণ কোর্স", modifier = Modifier.clickable { selectFullCourse = true })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !selectFullCourse,
                        onClick = { selectFullCourse = false },
                        colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                    )
                    Text("কোয়ার্টার নির্বাচন করুন", modifier = Modifier.clickable { selectFullCourse = false })
                }

                if (!selectFullCourse) {
                    Spacer(modifier = Modifier.height(8.dp))
                    course.quarters.forEach { quarter ->
                        val isSelected = selectedQuarters.contains(quarter)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 16.dp)
                                .clickable {
                                    selectedQuarters = if (isSelected) selectedQuarters - quarter else selectedQuarters + quarter
                                }
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedQuarters = if (checked) selectedQuarters + quarter else selectedQuarters - quarter
                                },
                                colors = CheckboxDefaults.colors(checkedColor = accentColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${quarter.name} - ৳${quarter.price}", fontSize = 14.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("পরিশোধ করতে হবে:", color = Color.Gray, fontSize = 14.sp)
                    Text("৳${totalPrice.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = accentColor)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("টাকা পাঠানোর নিয়ম:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            val methods = listOf("Bkash" to course.bkashNumber, "Nagad" to course.nagadNumber, "Rocket" to course.rocketNumber)
                .filter { it.second.isNotBlank() }

            if (methods.isEmpty()) {
                Text("দুঃখিত, কোনো পেমেন্ট নম্বর দেওয়া নেই।", color = Color.Red)
                return@Column
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                methods.forEach { (name, _) ->
                    FilterChip(
                        selected = paymentMethod == name,
                        onClick = { paymentMethod = name },
                        label = { Text(name) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentColor.copy(alpha = 0.2f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            val currentNumber = methods.find { it.first == paymentMethod }?.second ?: ""
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("$paymentMethod Personal Number:", color = Color.Gray, fontSize = 12.sp)
                        Text(currentNumber, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(currentNumber))
                        Toast.makeText(context, "নম্বর কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("উপরের নম্বরে ৳${totalPrice.toInt()} সেন্ড মানি করুন। তারপর নিচের তথ্যগুলো দিয়ে রিকোয়েস্ট জমা দিন।", color = Color.DarkGray, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = senderNumber,
                onValueChange = { senderNumber = it },
                label = { Text("যে নম্বর থেকে টাকা পাঠিয়েছেন") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = transactionId,
                onValueChange = { transactionId = it },
                label = { Text("Transaction ID (TrxID)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    if (totalPrice == 0.0) {
                        Toast.makeText(context, "মূল্য ০ টাকা হতে পারে না", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (senderNumber.isBlank() || transactionId.isBlank()) {
                        Toast.makeText(context, "নম্বর এবং TrxID প্রদান করুন", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!selectFullCourse && selectedQuarters.isEmpty()) {
                        Toast.makeText(context, "অন্তত একটি কোয়ার্টার নির্বাচন করুন", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    coroutineScope.launch {
                        isSubmitting = true
                        try {
                            val req = EnrollmentRequest(
                                user_id = profile.user_id,
                                course_id = course.id,
                                requested_quarters = if (selectFullCourse) "FULL" else selectedQuarters.joinToString(",") { it.name },
                                amount = totalPrice.toInt().toString(),
                                payment_method = paymentMethod,
                                sender_number = senderNumber,
                                transaction_id = transactionId,
                                status = "PENDING"
                            )
                            onRequestEnrollment(req)
                            Toast.makeText(context, "রিকোয়েস্ট সফলভাবে জমা হয়েছে!", Toast.LENGTH_LONG).show()
                            onPurchaseSubmitted()
                        } catch (e: Exception) {
                            Toast.makeText(context, "ত্রুটি: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isSubmitting = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("রিকোয়েস্ট জমা দিন", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
