import re

with open("app/src/main/java/com/example/CourseDetailScreen.kt", "r") as f:
    content = f.read()

new_composable = """
@Composable
fun UnenrolledCourseOverview(
    course: CourseItem,
    accentColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
            course.quarters.forEach { quarter ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(quarter.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("${quarter.startDate} - ${quarter.endDate}", fontSize = 12.sp, color = Color.Gray)
                        }
                        Text("৳${quarter.price}", fontWeight = FontWeight.Bold, color = accentColor)
                    }
                }
            }
        }
    }
}
"""

if "fun UnenrolledCourseOverview" not in content:
    content += new_composable

with open("app/src/main/java/com/example/CourseDetailScreen.kt", "w") as f:
    f.write(content)
