import re

with open("app/src/main/java/com/example/CourseDetailScreen.kt", "r") as f:
    content = f.read()

# Replace UnenrolledCourseOverview function body with the new one
new_func = """@Composable
fun UnenrolledCourseOverview(
    course: CourseItem,
    accentColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())) {
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp).background(Color.LightGray, RoundedCornerShape(16.dp))
        ) {
            if (course.bannerUrl.isNotBlank()) {
                coil.compose.AsyncImage(
                    model = course.bannerUrl,
                    contentDescription = "Course Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(accentColor), contentAlignment = Alignment.Center) {
                    Text("No Banner", color = Color.White)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(course.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text(course.description, fontSize = 16.sp, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                Text("দাম", color = Color.Gray, fontSize = 12.sp)
                Text(if (course.pricingOption == "Fully Free") "ফ্রি" else "৳${course.mainPrice}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentColor)
            }
            Column {
                Text("মোট বিষয়", color = Color.Gray, fontSize = 12.sp)
                Text("${course.subjects.size} টি", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        if (course.isQuarterOn && course.quarters.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("কোয়ার্টার বা প্যাকেজ সমূহ", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            val currentDate = java.time.LocalDate.now()
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
            
            course.quarters.forEach { quarter ->
                var statusText = "অজানা"
                var statusColor = Color.Gray
                var statusIcon = Icons.Default.HelpOutline
                
                try {
                    val start = java.time.LocalDate.parse(quarter.startDate, formatter)
                    val end = java.time.LocalDate.parse(quarter.endDate, formatter)
                    
                    if (currentDate.isBefore(start)) {
                        statusText = "পড়ানো হবে"
                        statusColor = Color(0xFF3B82F6) // Blue
                        statusIcon = Icons.Default.Schedule
                    } else if (currentDate.isAfter(end)) {
                        statusText = "সম্পন্ন"
                        statusColor = Color(0xFF22C55E) // Green
                        statusIcon = Icons.Default.CheckCircle
                    } else {
                        statusText = "পড়ানো হচ্ছে"
                        statusColor = Color(0xFFEAB308) // Yellow/Orange
                        statusIcon = Icons.Default.PlayCircle
                    }
                } catch (e: Exception) { }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(quarter.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("${quarter.startDate} - ${quarter.endDate}", fontSize = 12.sp, color = Color.Gray)
                            }
                            Text("৳${quarter.price}", fontWeight = FontWeight.Bold, color = accentColor)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(statusText, color = statusColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}
"""

content = re.sub(r"@Composable\nfun UnenrolledCourseOverview\([\s\S]*?\}\n\}", new_func, content)

with open("app/src/main/java/com/example/CourseDetailScreen.kt", "w") as f:
    f.write(content)
