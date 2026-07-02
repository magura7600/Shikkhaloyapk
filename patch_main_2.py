import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Add logic for navigating to purchase_course
# CourseDetailScreen call is around line 2058.
course_detail_call_regex = re.compile(r"(CourseDetailScreen\([\s\S]*?onEnroll = \{ purchasedQuarters ->[\s\S]*?\}\n\s*\))")

def insert_purchase_click(m):
    return m.group(1).replace("onEnroll = { purchasedQuarters ->", "pendingRequest = enrollmentRequests.find { it.course_id == selectedCourse!!.id && it.user_id == profile.user_id },\n                    onPurchaseClick = { currentScreen = \"purchase_course\" },\n                    onEnroll = { purchasedQuarters ->")

content = re.sub(course_detail_call_regex, insert_purchase_click, content)

# Add the new screens in the if-else blocks of currentScreen
new_screens = """
            } else if (currentScreen == "purchase_course" && selectedCourse != null) {
                PurchaseCourseScreen(
                    course = selectedCourse!!,
                    profile = profile,
                    accentColor = accentColor,
                    onBack = { currentScreen = "course_detail" },
                    onPurchaseSubmitted = {
                        // Refresh requests
                        coroutineScope.launch {
                            try {
                                enrollmentRequests = supabase.from("enrollment_requests").select().decodeList<EnrollmentRequest>()
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                        currentScreen = "course_detail"
                    }
                )
            } else if (currentScreen == "enrollment_requests" && teacherChannel != null) {
                EnrollmentRequestsScreen(
                    teacherChannel = teacherChannel!!,
                    requests = enrollmentRequests,
                    courses = courses,
                    accentColor = accentColor,
                    onBack = { currentScreen = "dashboard" },
                    onUpdateRequests = {
                        coroutineScope.launch {
                            try {
                                enrollmentRequests = supabase.from("enrollment_requests").select().decodeList<EnrollmentRequest>()
                                enrollments = supabase.from("enrollments").select().decodeList<Enrollment>()
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    }
                )
            } else if (currentScreen == "my_enrollments") {
                MyEnrollmentsScreen(
                    profile = profile,
                    enrollments = enrollments,
                    requests = enrollmentRequests,
                    courses = courses,
                    accentColor = accentColor,
                    onBack = { currentScreen = "dashboard" }
                )
"""

# Find `} else if (currentScreen == "settings") {` and insert before it
content = content.replace("} else if (currentScreen == \"settings\") {", new_screens + "\n            } else if (currentScreen == \"settings\") {")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
