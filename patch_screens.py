import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Make sure we insert at the correct place. Let's find the closing brace for CourseDetailScreen
target = """                        initialClassId = null
                    }
                )"""

screens_to_add = """
            } else if (currentScreen == "purchase_course" && selectedCourse != null) {
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
                )"""

if screens_to_add not in content:
    content = content.replace(target, target + screens_to_add)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
