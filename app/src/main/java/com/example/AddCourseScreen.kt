package com.example

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourseScreen(
    profile: UserProfile,
    accentColor: Color,
    initialCourse: CourseItem? = null,
    onBack: () -> Unit,
    onCourseAdded: (CourseItem) -> Unit
) {
    var courseTitle by remember { mutableStateOf(initialCourse?.title ?: "") }
    var courseDescription by remember { mutableStateOf(initialCourse?.description ?: "") }
    
    var pricingOption by remember { mutableStateOf(initialCourse?.pricingOption ?: "Fully Paid") }
    val pricingOptions = listOf("Fully Free", "Limited Free (Trial)", "Fully Paid")
    
    var mainPrice by remember { mutableStateOf(initialCourse?.mainPrice ?: "") }
    var discountPrice by remember { mutableStateOf(initialCourse?.discountPrice ?: "") }
    
    var bkashNumber by remember { mutableStateOf(initialCourse?.bkashNumber ?: "") }
    var nagadNumber by remember { mutableStateOf(initialCourse?.nagadNumber ?: "") }
    var rocketNumber by remember { mutableStateOf(initialCourse?.rocketNumber ?: "") }
    var paymentDetails by remember { mutableStateOf(initialCourse?.cleanPaymentDetails ?: "") }
    
    var isQuarterOn by remember { mutableStateOf(initialCourse?.isQuarterOn ?: false) }
    var quarters by remember { mutableStateOf(if (initialCourse != null && initialCourse.quarters.isNotEmpty()) initialCourse.quarters else listOf(CourseQuarter())) }
    
    var fullCourseStartDate by remember { mutableStateOf(initialCourse?.startDate ?: "") }
    var fullCourseEndDate by remember { mutableStateOf(initialCourse?.endDate ?: "") }
    
    val context = LocalContext.current
    
    fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = java.util.Calendar.getInstance()
        val datePickerDialog = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formattedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                onDateSelected(formattedDate)
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
    val isEditing = initialCourse != null
    val coroutineScope = rememberCoroutineScope()
    var bannerUrl by remember { mutableStateOf(initialCourse?.bannerUrl ?: "") }
    var isUploadingBanner by remember { mutableStateOf(false) }

    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isUploadingBanner = true
                Toast.makeText(context, "কভার ফটো আপলোড হচ্ছে...", Toast.LENGTH_SHORT).show()
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    if (bytes != null) {
                        val uploadedUrl = ImgBBClient.uploadImage(bytes)
                        if (uploadedUrl != null) {
                            bannerUrl = uploadedUrl
                            Toast.makeText(context, "কভার ফটো সফলভাবে আপলোড হয়েছে!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "আপলোড ব্যর্থ হয়েছে।", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "ত্রুটি: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isUploadingBanner = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "কোর্স এডিট করুন" else "নতুন কোর্স যোগ করুন", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFFBF8F1)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Course Banner
            item {
                Text("Course Cover Photo", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.LightGray.copy(alpha = 0.3f))
                        .clickable(enabled = !isUploadingBanner) {
                            bannerPickerLauncher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (bannerUrl.isNotBlank()) {
                        AsyncImage(
                            model = bannerUrl,
                            contentDescription = "Course Cover Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Change Cover Photo", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else if (isUploadingBanner) {
                        CircularProgressIndicator(color = accentColor)
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Upload Cover", tint = Color.Gray, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Upload Course Cover Photo", color = Color.Gray)
                        }
                    }
                }
            }

            // Course Name
            item {
                OutlinedTextField(
                    value = courseTitle,
                    onValueChange = { courseTitle = it },
                    label = { Text("Course Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Course Description
            item {
                OutlinedTextField(
                    value = courseDescription,
                    onValueChange = { courseDescription = it },
                    label = { Text("Course Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3
                )
            }

            // Pricing Options
            item {
                Text("Select Course Pricing Options", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    pricingOptions.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { pricingOption = option }
                        ) {
                            RadioButton(
                                selected = pricingOption == option,
                                onClick = { pricingOption = option },
                                colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                            )
                            Text(option)
                        }
                    }
                }
            }
            
            if (pricingOption != "Fully Free") {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = mainPrice,
                            onValueChange = { mainPrice = it },
                            label = { Text("Main Course Price (৳)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = discountPrice,
                            onValueChange = { discountPrice = it },
                            label = { Text("Discount Price (৳)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
                
                // Payment Methods
                item {
                    Text("Enter Payment Methods", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bkashNumber,
                        onValueChange = { bkashNumber = it },
                        label = { Text("bKash Number") },
                        placeholder = { Text("e.g. 017XXXXXXXX (Personal)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nagadNumber,
                        onValueChange = { nagadNumber = it },
                        label = { Text("Nagad Number") },
                        placeholder = { Text("e.g. 019XXXXXXXX (Personal)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rocketNumber,
                        onValueChange = { rocketNumber = it },
                        label = { Text("Rocket Number") },
                        placeholder = { Text("e.g. 018XXXXXXXX (Personal)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = paymentDetails,
                        onValueChange = { paymentDetails = it },
                        label = { Text("Payment details / instructions") },
                        placeholder = { Text("e.g. Please provide Sender number & TxID after sending Send Money.") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Quarters
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Quarter Details", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isQuarterOn) "Quarter On" else "Quarter Off", color = if (isQuarterOn) accentColor else Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isQuarterOn,
                            onCheckedChange = { isQuarterOn = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor, checkedTrackColor = accentColor.copy(alpha = 0.5f))
                        )
                    }
                }
            }
            
            if (isQuarterOn) {
                itemsIndexed(quarters) { index, quarter ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Quarter ${index + 1}", fontWeight = FontWeight.Bold, color = accentColor)
                                if (quarters.size > 1) {
                                    IconButton(onClick = { 
                                        val newQuarters = quarters.toMutableList()
                                        newQuarters.removeAt(index)
                                        quarters = newQuarters
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                    }
                                }
                            }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedTextField(
                                    value = quarter.name,
                                    onValueChange = { 
                                        val newQuarters = quarters.toMutableList()
                                        newQuarters[index] = quarter.copy(name = it)
                                        quarters = newQuarters
                                    },
                                    label = { Text("Quarter Name") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = quarter.price,
                                    onValueChange = { 
                                        val newQuarters = quarters.toMutableList()
                                        newQuarters[index] = quarter.copy(price = it)
                                        quarters = newQuarters
                                    },
                                    label = { Text("Price (৳)") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                            
                             Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                 // Quarter Start Date
                                 Box(
                                     modifier = Modifier
                                         .weight(1f)
                                         .clickable {
                                             showDatePicker { selectedDate ->
                                                 val newQuarters = quarters.toMutableList()
                                                 newQuarters[index] = quarter.copy(startDate = selectedDate)
                                                 quarters = newQuarters
                                             }
                                         }
                                 ) {
                                     OutlinedTextField(
                                         value = quarter.startDate,
                                         onValueChange = {},
                                         label = { Text("শুরুর তারিখ") },
                                         placeholder = { Text("dd/mm/yyyy") },
                                         readOnly = true,
                                         enabled = false,
                                         modifier = Modifier.fillMaxWidth(),
                                         shape = RoundedCornerShape(12.dp),
                                         colors = OutlinedTextFieldDefaults.colors(
                                             disabledTextColor = Color.Black,
                                             disabledBorderColor = Color.Gray,
                                             disabledLabelColor = Color.Gray,
                                             disabledPlaceholderColor = Color.Gray
                                         )
                                     )
                                 }

                                 // Quarter End Date
                                 Box(
                                     modifier = Modifier
                                         .weight(1f)
                                         .clickable {
                                             showDatePicker { selectedDate ->
                                                 val newQuarters = quarters.toMutableList()
                                                 newQuarters[index] = quarter.copy(endDate = selectedDate)
                                                 quarters = newQuarters
                                             }
                                         }
                                 ) {
                                     OutlinedTextField(
                                         value = quarter.endDate,
                                         onValueChange = {},
                                         label = { Text("শেষের তারিখ") },
                                         placeholder = { Text("dd/mm/yyyy") },
                                         readOnly = true,
                                         enabled = false,
                                         modifier = Modifier.fillMaxWidth(),
                                         shape = RoundedCornerShape(12.dp),
                                         colors = OutlinedTextFieldDefaults.colors(
                                             disabledTextColor = Color.Black,
                                             disabledBorderColor = Color.Gray,
                                             disabledLabelColor = Color.Gray,
                                             disabledPlaceholderColor = Color.Gray
                                         )
                                     )
                                 }
                             }
                        }
                    }
                }
                
                item {
                    Button(
                        onClick = { 
                            val newQuarters = quarters.toMutableList()
                            newQuarters.add(CourseQuarter())
                            quarters = newQuarters
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add Another Quarter")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (courseTitle.isBlank()) {
                            Toast.makeText(context, "Please enter course name", Toast.LENGTH_SHORT).show()
                        } else {
                            val originalRoutinePart = initialCourse?.realRoutineUrl ?: ""
                            val finalRoutineUrl = "v2;$bannerUrl;$fullCourseStartDate;$fullCourseEndDate;$originalRoutinePart"
                            val finalPaymentDetails = if (paymentDetails.isNotBlank()) {
                                "$paymentDetails|||ROUTINE_DATA:$finalRoutineUrl"
                            } else {
                                "|||ROUTINE_DATA:$finalRoutineUrl"
                            }
                            val newCourse = CourseItem(
                                id = initialCourse?.id ?: java.util.UUID.randomUUID().toString(),
                                channel_id = initialCourse?.channel_id ?: "",
                                title = courseTitle,
                                description = courseDescription,
                                pricingOption = pricingOption,
                                mainPrice = mainPrice,
                                discountPrice = discountPrice,
                                bkashNumber = bkashNumber,
                                nagadNumber = nagadNumber,
                                rocketNumber = rocketNumber,
                                paymentDetails = finalPaymentDetails,
                                isQuarterOn = isQuarterOn,
                                quarters = quarters,
                                subjects = initialCourse?.subjects ?: emptyList(),
                                studentsCount = initialCourse?.studentsCount ?: 0
                            )
                            onCourseAdded(newCourse)
                            Toast.makeText(context, if (isEditing) "Course updated successfully!" else "Course added successfully!", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isEditing) "আপডেট করুন (Update Course)" else "যোগ করুন (Add Course)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
