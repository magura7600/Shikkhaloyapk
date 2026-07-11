package com.example

import android.app.DatePickerDialog
import androidx.compose.ui.res.stringResource
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditClassDialog(
    existingClass: CourseClass?,
    course: CourseItem,
    mentors: List<Mentor>,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSave: (CourseClass, String) -> Unit // returns the updated/new CourseClass, and the selected mentor name
) {
    val mContext = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var selectedType by remember { mutableStateOf(existingClass?.type ?: "লেকচার ক্লাস") }
    val types = listOf("লেকচার ক্লাস", "সলভিং ক্লাস", "গাইডলাইন ক্লাস", "অতিরিক্ত ক্লাস", "লেকচার শিট/PDF")
    
    var newTitle by remember { mutableStateOf(existingClass?.title ?: "") }
    var newDescription by remember { mutableStateOf(existingClass?.description ?: "") }
    var newDate by remember { mutableStateOf(existingClass?.date ?: "") }
    var newTime by remember { mutableStateOf(existingClass?.time ?: "") }
    var newQuarter by remember { mutableStateOf(existingClass?.quarterId ?: "") }
    
    val initialMentorName = existingClass?.mentorId?.let { mId -> mentors.find { it.id == mId }?.name } ?: ""
    var newMentor by remember { mutableStateOf(initialMentorName) }
    var newLiveLink by remember { mutableStateOf(existingClass?.liveLink ?: "") }
    var newRecordedLink by remember { mutableStateOf(existingClass?.recordedLink ?: "") }
    var newHomeworkLink by remember { mutableStateOf(existingClass?.homeworkLink ?: "") }
    var pdfLinks by remember { mutableStateOf(existingClass?.pdfLinks ?: listOf<PdfLink>()) }
    var newPdfTitle by remember { mutableStateOf("") }
    var newPdfUrl by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF1E3A8A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = if (existingClass != null) stringResource(R.string.edit_class_title) else stringResource(R.string.add_class_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(stringResource(R.string.resource_type_label), fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        types.take(2).forEach { type ->
                            OutlinedButton(
                                onClick = { selectedType = type },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 4.dp),
                                border = BorderStroke(1.dp, if (selectedType == type) Color.White else Color.White.copy(alpha = 0.3f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White,
                                    containerColor = if (selectedType == type) Color.White.copy(alpha = 0.2f) else Color.Transparent
                                )
                            ) {
                                Text(type, fontSize = 12.sp)
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        types.drop(2).take(2).forEach { type ->
                            OutlinedButton(
                                onClick = { selectedType = type },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 4.dp, top = 4.dp),
                                border = BorderStroke(1.dp, if (selectedType == type) Color.White else Color.White.copy(alpha = 0.3f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White,
                                    containerColor = if (selectedType == type) Color.White.copy(alpha = 0.2f) else Color.Transparent
                                )
                            ) {
                                Text(type, fontSize = 12.sp)
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        types.drop(4).forEach { type ->
                            OutlinedButton(
                                onClick = { selectedType = type },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 4.dp, top = 4.dp),
                                border = BorderStroke(1.dp, if (selectedType == type) Color.White else Color.White.copy(alpha = 0.3f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White,
                                    containerColor = if (selectedType == type) Color.White.copy(alpha = 0.2f) else Color.Transparent
                                )
                            ) {
                                Text(type, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text(stringResource(R.string.resource_name_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newDescription,
                        onValueChange = { newDescription = it },
                        label = { Text(stringResource(R.string.resource_desc_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp)
                            .clickable {
                                val calendar = Calendar.getInstance()
                                DatePickerDialog(
                                    mContext,
                                    { _, year, month, day ->
                                        newDate = String.format(java.util.Locale.US, "%02d/%02d/%04d", day, month + 1, year)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                        ) {
                            OutlinedTextField(
                                value = newDate, 
                                onValueChange = {}, 
                                label = { Text(stringResource(R.string.date_label)) }, 
                                readOnly = true,
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color.White,
                                    disabledBorderColor = Color.White.copy(alpha = 0.5f),
                                    disabledLabelColor = Color.White.copy(alpha = 0.7f)
                                )
                            )
                        }
                        Box(modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                            .clickable {
                                val calendar = Calendar.getInstance()
                                TimePickerDialog(
                                    mContext,
                                    { _, hour, minute ->
                                        val amPm = if (hour >= 12) "PM" else "AM"
                                        val hour12 = if (hour % 12 == 0) 12 else hour % 12
                                        val hourStr = if (hour12 == 0) 12 else hour12
                                        newTime = String.format("%02d:%02d %s", hourStr, minute, amPm)
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    false
                                ).show()
                            }
                        ) {
                            OutlinedTextField(
                                value = newTime, 
                                onValueChange = {}, 
                                label = { Text(stringResource(R.string.time_label)) }, 
                                readOnly = true,
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color.White,
                                    disabledBorderColor = Color.White.copy(alpha = 0.5f),
                                    disabledLabelColor = Color.White.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }

                    if (course.isQuarterOn && course.quarters.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        var quarterDropdownExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier
                                .fillMaxWidth()
                                .clickable { quarterDropdownExpanded = true }) {
                                OutlinedTextField(
                                    value = newQuarter,
                                    onValueChange = {},
                                    label = { Text(stringResource(R.string.select_quarter_label)) },
                                    readOnly = true,
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = Color.White,
                                        disabledBorderColor = Color.White.copy(alpha = 0.5f),
                                        disabledLabelColor = Color.White.copy(alpha = 0.7f),
                                        disabledTrailingIconColor = Color.White
                                    )
                                )
                            }
                            DropdownMenu(
                                expanded = quarterDropdownExpanded,
                                onDismissRequest = { quarterDropdownExpanded = false }
                            ) {
                                course.quarters.forEach { quarter ->
                                    DropdownMenuItem(
                                        text = { Text(quarter.name) },
                                        onClick = {
                                            newQuarter = quarter.name
                                            quarterDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    var mentorDropdownExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mentorDropdownExpanded = true }) {
                            OutlinedTextField(
                                value = newMentor,
                                onValueChange = {},
                                label = { Text(stringResource(R.string.select_mentor_label)) },
                                readOnly = true,
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color.White,
                                    disabledBorderColor = Color.White.copy(alpha = 0.5f),
                                    disabledLabelColor = Color.White.copy(alpha = 0.7f),
                                    disabledTrailingIconColor = Color.White
                                )
                            )
                        }
                        DropdownMenu(
                            expanded = mentorDropdownExpanded,
                            onDismissRequest = { mentorDropdownExpanded = false }
                        ) {
                            mentors.forEach { mentor ->
                                DropdownMenuItem(
                                    text = { Text(mentor.name) },
                                    onClick = {
                                        newMentor = mentor.name
                                        mentorDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newLiveLink,
                        onValueChange = { newLiveLink = it },
                        label = { Text(stringResource(R.string.live_link_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newRecordedLink,
                        onValueChange = { newRecordedLink = it },
                        label = { Text(stringResource(R.string.recorded_link_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newHomeworkLink,
                        onValueChange = { newHomeworkLink = it },
                        label = { Text(stringResource(R.string.homework_link_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.pdf_links_title), fontWeight = FontWeight.Bold, color = Color.White)
                    pdfLinks.forEachIndexed { index, pdf ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${index + 1}. ${pdf.title}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = pdf.url,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        newPdfTitle = pdf.title
                                        newPdfUrl = pdf.url
                                        pdfLinks = pdfLinks.filterIndexed { idx, _ -> idx != index }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit PDF link",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        pdfLinks = pdfLinks.filterIndexed { idx, _ -> idx != index }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete PDF link",
                                        tint = Color.Red,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newPdfTitle, 
                            onValueChange = { newPdfTitle = it }, 
                            label = { Text(stringResource(R.string.pdf_title_label)) }, 
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                            )
                        )
                        OutlinedTextField(
                            value = newPdfUrl, 
                            onValueChange = { newPdfUrl = it }, 
                            label = { Text(stringResource(R.string.pdf_url_label)) }, 
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 4.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                    TextButton(
                        onClick = {
                            if (newPdfTitle.isNotBlank() && newPdfUrl.isNotBlank()) {
                                pdfLinks = pdfLinks + PdfLink(newPdfTitle, newPdfUrl)
                                newPdfTitle = ""
                                newPdfUrl = ""
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    ) {
                        Text(stringResource(R.string.add_more_pdf_button))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.8f))) {
                        Text(stringResource(R.string.cancel_button))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            
                            if (newTitle.isNotBlank()) {
                                val selectedQuarterId = newQuarter
                                val selectedMentorId = mentors.find { it.name == newMentor }?.id ?: ""
                                
                                val finalPdfLinks = if (newPdfTitle.isNotBlank() && newPdfUrl.isNotBlank()) {
                                    pdfLinks + PdfLink(newPdfTitle, newPdfUrl)
                                } else {
                                    pdfLinks
                                }

                                val savedClass = existingClass?.copy(
                                    type = selectedType,
                                    title = newTitle,
                                    description = newDescription,
                                    date = newDate,
                                    time = newTime,
                                    quarterId = selectedQuarterId,
                                    mentorId = selectedMentorId,
                                    liveLink = newLiveLink,
                                    recordedLink = newRecordedLink,
                                    homeworkLink = newHomeworkLink,
                                    pdfLinks = finalPdfLinks
                                ) ?: CourseClass(
                                    type = selectedType,
                                    title = newTitle,
                                    description = newDescription,
                                    date = newDate,
                                    time = newTime,
                                    quarterId = selectedQuarterId,
                                    mentorId = selectedMentorId,
                                    liveLink = newLiveLink,
                                    recordedLink = newRecordedLink,
                                    homeworkLink = newHomeworkLink,
                                    pdfLinks = finalPdfLinks
                                )
                                onSave(savedClass, newMentor)
                            }
                        }, 
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text(if (existingClass != null) stringResource(R.string.save_button) else stringResource(R.string.create_button))
                    }
                }
            }
        }
    }
}
