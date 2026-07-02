import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# I will find the first insertion and revert it.
# The first insertion starts at: `} else if (currentScreen == "purchase_course" && selectedCourse != null) {`
# And ends before `            } else {` at the end of the insertion.

insertion = """            } else if (currentScreen == "purchase_course" && selectedCourse != null) {
                PurchaseCourseScreen(
                    course = selectedCourse!!,
                    profile = profile,
                    accentColor = accentColor,
                    onBack = { currentScreen = "course_detail" },
                    onPurchaseSubmitted = {
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
                    teacherChannel = profile,
                    requests = enrollmentRequests,
                    courses = courses,
                    accentColor = accentColor,
                    onBack = { currentScreen = "dashboard" },
                    onUpdateRequests = {
                        coroutineScope.launch {
                            try {
                                enrollmentRequests = supabase.from("enrollment_requests").select().decodeList<EnrollmentRequest>()
                                val fetched = supabase.from("enrollments").select().decodeList<Enrollment>()
                                enrollments = fetched
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

content = content.replace(insertion, "", 1)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
