import re

with open("app/src/main/java/com/example/CourseDetailScreen.kt", "r") as f:
    content = f.read()

# 1. Add onPurchaseClick to CourseContentSection signature
sig_target = """    onExternalAddHandled: () -> Unit = {}
) {"""
sig_replace = """    onExternalAddHandled: () -> Unit = {},
    onPurchaseClick: () -> Unit = {}
) {"""
content = content.replace(sig_target, sig_replace)

# 2. Add the locking logic variables at the top of CourseContentSection
top_target = """    var isAddingSubject by remember { mutableStateOf(false) }"""
top_replace = """    var isAddingSubject by remember { mutableStateOf(false) }
    
    val isFullyPurchased = userEnrollment?.purchased_quarters.isNullOrBlank() && userEnrollment != null
    val purchasedQuartersList = userEnrollment?.purchased_quarters?.split(",")?.map { it.trim() } ?: emptyList()
    val isQuarterLocked = fun(qName: String): Boolean {
        if (isTeacher) return false
        if (course.pricingOption == "Fully Free") return false
        if (userEnrollment == null) return true
        if (isFullyPurchased) return false
        return qName !in purchasedQuartersList
    }
"""
content = content.replace(top_target, top_replace)

# 3. Use isCurrentQuarterLocked to hide the subjects grid and show locked message
grid_target = """            // 3. Subjects Grid with Video and PDF details and Premium Overlays
            if (course.subjects.isEmpty()) {
                Text("এখনো কোনো বিষয়বস্তু যোগ করা হয়নি।", color = Color.Gray, fontSize = 14.sp)
            } else {"""
grid_replace = """            // 3. Subjects Grid with Video and PDF details and Premium Overlays
            val isCurrentQuarterLocked = isQuarterLocked(selectedQuarterName)
            if (isCurrentQuarterLocked) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("এই কোয়ার্টারটি লক করা আছে", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("বিস্তারিত দেখতে কোয়ার্টারটি কিনুন বা আনলক করুন।", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onPurchaseClick,
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("আনলক করুন")
                        }
                    }
                }
            } else if (course.subjects.isEmpty()) {
                Text("এখনো কোনো বিষয়বস্তু যোগ করা হয়নি।", color = Color.Gray, fontSize = 14.sp)
            } else {"""
content = content.replace(grid_target, grid_replace)

with open("app/src/main/java/com/example/CourseDetailScreen.kt", "w") as f:
    f.write(content)

