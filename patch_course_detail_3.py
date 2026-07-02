import re

with open("app/src/main/java/com/example/CourseDetailScreen.kt", "r") as f:
    content = f.read()

# 1. Replace the entire bottomBar
bottom_bar_regex = re.compile(r"bottomBar = \{[\s\S]*?containerColor = Color\(0xFFFBF8F1\)")
new_bottom_bar = """bottomBar = {
            if (!isClassActive && !isChapterActive && userEnrollment == null && !isTeacher) {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = {
                                if (course.pricingOption == "Fully Free") {
                                    onEnroll("")
                                } else {
                                    onPurchaseClick()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (pendingRequest?.status == "PENDING") Color.Gray else accentColor),
                            shape = RoundedCornerShape(12.dp),
                            enabled = pendingRequest?.status != "PENDING"
                        ) {
                            Text(
                                text = if (pendingRequest?.status == "PENDING") "পেন্ডিং..." else if (course.pricingOption == "Fully Free") "এনরোল করুন (Free)" else "কোর্স কিনুন",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFFBF8F1)"""
content = re.sub(bottom_bar_regex, new_bottom_bar, content)

# 2. Inside the Scaffold, use UnenrolledCourseOverview if userEnrollment == null && !isTeacher
content_regex = re.compile(r"val isInnerActive = isClassActive \|\| isChapterActive\n\s*if \(isInnerActive\) \{")
new_content = """val isInnerActive = isClassActive || isChapterActive
            if (userEnrollment == null && !isTeacher) {
                UnenrolledCourseOverview(course = course, accentColor = accentColor)
            } else if (isInnerActive) {"""
content = re.sub(content_regex, new_content, content)

with open("app/src/main/java/com/example/CourseDetailScreen.kt", "w") as f:
    f.write(content)
