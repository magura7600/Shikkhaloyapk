import sys

def main():
    file_path = "app/src/main/java/com/example/MainActivity.kt"
    with open(file_path, "r") as f:
        content = f.read()
    
    target = """    LaunchedEffect(Unit) {
        try {
            val fetchedChannels = withContext(Dispatchers.IO) {
                supabase.from("profiles").select().decodeList<UserProfile>()
            }
            allChannels = fetchedChannels.filter { it.handle != null && it.handle.isNotBlank() }
            
            val fetchedCourses = withContext(Dispatchers.IO) {
                supabase.from("courses").select().decodeList<CourseItem>()
            }
            courses = fetchedCourses
            
            val fetchedEnrollments = withContext(Dispatchers.IO) {
                try {
                    val enrolls = supabase.from("enrollments").select().decodeList<Enrollment>()
                    try {
                        enrollmentRequests = supabase.from("enrollment_requests").select().decodeList<EnrollmentRequest>()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    enrolls
                } catch(e:Exception) { emptyList<Enrollment>() }
            }
            enrollments = fetchedEnrollments
            
            val fetchedInteractions = withContext(Dispatchers.IO) {
                try {
                    supabase.from("course_interactions").select().decodeList<CourseInteraction>()
                } catch(e:Exception) { emptyList() }
            }
            courseInteractions = fetchedInteractions
            
            try {
                val fetchedMentors = withContext(Dispatchers.IO) {
                    supabase.from("mentors").select().decodeList<Mentor>()
                }
                mentors = fetchedMentors
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error loading courses: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }"""

    replacement = """    // Data Loading Logic optimized to only load what's needed for the current screen
    LaunchedEffect(currentScreen, selectedTab) {
        try {
            // Load enrollments
            if (enrollments.isEmpty()) {
                val fetchedEnrollments = withContext(Dispatchers.IO) {
                    try {
                        val enrolls = if (isTeacher || isAdmin) {
                            supabase.from("enrollments").select().decodeList<Enrollment>()
                        } else {
                            supabase.from("enrollments").select { filter { eq("user_id", profile.user_id) } }.decodeList<Enrollment>()
                        }
                        try {
                            enrollmentRequests = if (isTeacher || isAdmin) {
                                supabase.from("enrollment_requests").select().decodeList<EnrollmentRequest>()
                            } else {
                                supabase.from("enrollment_requests").select { filter { eq("user_id", profile.user_id) } }.decodeList<EnrollmentRequest>()
                            }
                        } catch (e: Exception) {}
                        enrolls
                    } catch (e: Exception) { emptyList<Enrollment>() }
                }
                enrollments = fetchedEnrollments
            }

            // Load courses
            if (courses.isEmpty() || (selectedTab == 1 || selectedTab == 3 || currentScreen != "dashboard")) {
                val fetchedCourses = withContext(Dispatchers.IO) {
                    if ((selectedTab == 0 && currentScreen == "dashboard") && !isTeacher && !isAdmin) {
                        // Just need enrolled courses
                        val myCourseIds = enrollments.map { it.course_id }
                        if (myCourseIds.isNotEmpty()) {
                            supabase.from("courses").select { filter { isIn("id", myCourseIds) } }.decodeList<CourseItem>()
                        } else {
                            emptyList()
                        }
                    } else {
                        supabase.from("courses").select().decodeList<CourseItem>()
                    }
                }
                if (fetchedCourses.isNotEmpty()) {
                    courses = fetchedCourses
                }
            }

            // Load channels (Explore screen only or if needed)
            if (allChannels.isEmpty() && (selectedTab == 3 || currentScreen == "explore")) {
                val fetchedChannels = withContext(Dispatchers.IO) {
                    supabase.from("profiles").select().decodeList<UserProfile>()
                }
                allChannels = fetchedChannels.filter { it.handle != null && it.handle.isNotBlank() }
            }

            // Load course interactions
            if (courseInteractions.isEmpty()) {
                val fetchedInteractions = withContext(Dispatchers.IO) {
                    try {
                        if (isTeacher || isAdmin) {
                            supabase.from("course_interactions").select().decodeList<CourseInteraction>()
                        } else {
                            supabase.from("course_interactions").select { filter { eq("user_id", profile.user_id) } }.decodeList<CourseInteraction>()
                        }
                    } catch(e: Exception) { emptyList() }
                }
                courseInteractions = fetchedInteractions
            }

            // Load mentors
            if (mentors.isEmpty() && (currentScreen == "courseDetail" || showMentorsDialog)) {
                try {
                    val fetchedMentors = withContext(Dispatchers.IO) {
                        supabase.from("mentors").select().decodeList<Mentor>()
                    }
                    mentors = fetchedMentors
                } catch (e: Exception) {}
            }
        } catch (e: Exception) {
            // handle error silently to not annoy user
        }
    }"""

    if target in content:
        content = content.replace(target, replacement)
        with open(file_path, "w") as f:
            f.write(content)
        print("Replaced successfully!")
    else:
        print("Target not found.")

if __name__ == "__main__":
    main()
