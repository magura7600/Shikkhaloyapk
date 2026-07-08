import sys

def main():
    file_path = "app/src/main/java/com/example/MainActivity.kt"
    with open(file_path, "r") as f:
        content = f.read()

    target_start = "    var focusCourseId by remember { mutableStateOf<String?>(null) }"
    
    target_end = "    LaunchedEffect(isTeacher) {"

    start_idx = content.find(target_start)
    end_idx = content.find(target_end, start_idx)
    
    if start_idx != -1 and end_idx != -1:
        replacement = """    var focusCourseId by remember { mutableStateOf<String?>(null) }
    var hasLoadedAllCourses by remember { mutableStateOf(false) }

    // Sync OneSignal User identity and purchased course tags so they only receive notifications of courses they bought
    LaunchedEffect(enrollments, profile.user_id) {
        val myEnrolledCourseIds = enrollments.filter { it.user_id == profile.user_id }.map { it.course_id }
        if (focusCourseId == null && myEnrolledCourseIds.isNotEmpty()) {
            focusCourseId = myEnrolledCourseIds.firstOrNull()
        }
        
        try {
            // Set User Identity in OneSignal to track this user
            OneSignal.login(profile.user_id)
            
            // Get user's enrolled courses
            val myEnrollments = enrollments.filter { it.user_id == profile.user_id }
            val tags = mutableMapOf<String, String>()
            myEnrollments.forEach { enrollment ->
                // Tag for the main course
                tags["course_${enrollment.course_id}"] = "true"
                
                // Also tag for individual purchased quarters
                if (enrollment.purchased_quarters.isNotBlank()) {
                    enrollment.purchased_quarters.split(",").forEach { qId ->
                        val trimmed = qId.trim()
                        if (trimmed.isNotBlank()) {
                            tags["quarter_$trimmed"] = "true"
                        }
                    }
                }
            }
            if (tags.isNotEmpty()) {
                OneSignal.User.addTags(tags)
                android.util.Log.d("OneSignalSync", "Synced course enrollment tags to OneSignal: $tags")
            }
        } catch (e: Exception) {
            android.util.Log.e("OneSignalSync", "Failed to sync tags to OneSignal: ${e.message}")
        }
    }

    var enteredWithNetwork by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        var elapsedSeconds = 0
        while (true) {
            val hasInternet = NetworkUtils.hasActualInternetAccess(context)
            isOffline = !hasInternet
            if (hasInternet) {
                enteredWithNetwork = true
                hasPromptedOffline = false
            } else {
                if (!enteredWithNetwork) {
                    if (elapsedSeconds >= 120) {
                        if (!hasPromptedOffline) {
                            showOfflineDownloadsGlobal = true
                            hasPromptedOffline = true
                            Toast.makeText(context, "কোনো ইন্টারনেট সংযোগ নেই! অফলাইন মোড চালু করা হয়েছে।", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        elapsedSeconds += 8
                    }
                }
            }
            kotlinx.coroutines.delay(8000) // check every 8 seconds
        }
    }

    // Data Loading Logic optimized to truly only load what's needed for the current screen
    LaunchedEffect(currentScreen, selectedTab, focusCourseId, selectedCourse?.id) {
        try {
            // 1. My Enrollments (needed globally for a student to know what they own)
            if (!isTeacher && !isAdmin && enrollments.isEmpty()) {
                 enrollments = withContext(Dispatchers.IO) {
                     try {
                         supabase.from("enrollments").select { filter { eq("user_id", profile.user_id) } }.decodeList<Enrollment>()
                     } catch(e: Exception) { emptyList() }
                 }
            }

            // 2. All Enrollments & Requests (ONLY needed if Teacher/Admin goes to Management)
            if ((isTeacher || isAdmin) && selectedTab == 2) {
                 if (enrollments.isEmpty()) {
                     enrollments = withContext(Dispatchers.IO) {
                         try { supabase.from("enrollments").select().decodeList<Enrollment>() } catch(e: Exception) { emptyList() }
                     }
                 }
                 if (enrollmentRequests.isEmpty()) {
                     enrollmentRequests = withContext(Dispatchers.IO) {
                         try { supabase.from("enrollment_requests").select().decodeList<EnrollmentRequest>() } catch(e: Exception) { emptyList() }
                     }
                 }
            }

            // 3. Courses
            if (!hasLoadedAllCourses) {
                 if (selectedTab == 0 && !isTeacher && !isAdmin) {
                     // Home tab: only load enrolled courses
                     val myCourseIds = enrollments.map { it.course_id }
                     if (myCourseIds.isNotEmpty() && courses.isEmpty()) {
                         courses = withContext(Dispatchers.IO) {
                             try { supabase.from("courses").select { filter { isIn("id", myCourseIds) } }.decodeList<CourseItem>() } catch(e: Exception) { emptyList() }
                         }
                     }
                 } else if (selectedTab == 1 || selectedTab == 3 || currentScreen == "select_course_for_students") {
                     // Course list or Explore tab: load all courses
                     courses = withContext(Dispatchers.IO) {
                         try { supabase.from("courses").select().decodeList<CourseItem>() } catch(e: Exception) { emptyList() }
                     }
                     if (courses.isNotEmpty()) hasLoadedAllCourses = true
                 } else if (isTeacher && courses.isEmpty()) {
                     // Teacher home: load their courses
                     courses = withContext(Dispatchers.IO) {
                         try { supabase.from("courses").select { filter { eq("channel_id", profile.user_id) } }.decodeList<CourseItem>() } catch(e: Exception) { emptyList() }
                     }
                 }
            }

            // 4. Channels (ONLY needed for Explore tab)
            if (selectedTab == 3 && allChannels.isEmpty()) {
                 allChannels = withContext(Dispatchers.IO) {
                     try { supabase.from("profiles").select().decodeList<UserProfile>().filter { it.handle != null && it.handle.isNotBlank() } } catch(e: Exception) { emptyList() }
                 }
            }

            // 5. Course Interactions & Mentors (ONLY needed for Course Detail screen)
            if (currentScreen == "courseDetail" && selectedCourse != null) {
                 courseInteractions = withContext(Dispatchers.IO) {
                     try { supabase.from("course_interactions").select { filter { eq("course_id", selectedCourse!!.id) } }.decodeList<CourseInteraction>() } catch(e: Exception) { emptyList() }
                 }
                 if (mentors.isEmpty()) {
                     mentors = withContext(Dispatchers.IO) {
                         try { supabase.from("mentors").select().decodeList<Mentor>() } catch(e: Exception) { emptyList() }
                     }
                 }
            }
        } catch (e: Exception) {
            // silent
        }
    }

"""
        content = content[:start_idx] + replacement + content[end_idx:]
        with open(file_path, "w") as f:
            f.write(content)
        print("Replaced successfully!")
    else:
        print("Target not found.")

if __name__ == "__main__":
    main()
