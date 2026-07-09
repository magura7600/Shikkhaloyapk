import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# Replace block
old_block = """            // 1. My Enrollments (needed globally for a student to know what they own)
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
            if (!hasLoadedAllCourses || courses.isEmpty()) {
                 courses = withContext(Dispatchers.IO) {
                     try { supabase.from("courses").select().decodeList<CourseItem>() } catch(e: Exception) { emptyList() }
                 }
                 hasLoadedAllCourses = true
            }

            // 4. Channels
            if (allChannels.isEmpty()) {
                 allChannels = withContext(Dispatchers.IO) {
                     try { supabase.from("profiles").select().decodeList<UserProfile>().filter { it.handle != null && it.handle.isNotBlank() } } catch(e: Exception) { emptyList() }
                 }
            }

            // 5. Course Interactions & Mentors (ONLY needed for Course Detail screen)
            if (currentScreen == "course_detail" && selectedCourse != null) {
                 courseInteractions = withContext(Dispatchers.IO) {
                     try { supabase.from("course_interactions").select { filter { eq("course_id", selectedCourse!!.id) } }.decodeList<CourseInteraction>() } catch(e: Exception) { emptyList() }
                 }
                 if (mentors.isEmpty()) {
                     mentors = withContext(Dispatchers.IO) {
                         try { supabase.from("mentors").select().decodeList<Mentor>() } catch(e: Exception) { emptyList() }
                     }
                 }
            }"""

new_block = """            // 1. My Enrollments (needed globally for a student to know what they own)
            if (!isTeacher && !isAdmin) {
                 val newEnrollments = withContext(Dispatchers.IO) {
                     try {
                         supabase.from("enrollments").select { filter { eq("user_id", profile.user_id) } }.decodeList<Enrollment>()
                     } catch(e: Exception) { enrollments }
                 }
                 enrollments = newEnrollments
            }

            // 2. All Enrollments & Requests (ONLY needed if Teacher/Admin goes to Management)
            if ((isTeacher || isAdmin) && selectedTab == 2) {
                 enrollments = withContext(Dispatchers.IO) {
                     try { supabase.from("enrollments").select().decodeList<Enrollment>() } catch(e: Exception) { enrollments }
                 }
                 enrollmentRequests = withContext(Dispatchers.IO) {
                     try { supabase.from("enrollment_requests").select().decodeList<EnrollmentRequest>() } catch(e: Exception) { enrollmentRequests }
                 }
            }

            // 3. Courses
            courses = withContext(Dispatchers.IO) {
                try { supabase.from("courses").select().decodeList<CourseItem>() } catch(e: Exception) { courses }
            }
            hasLoadedAllCourses = true

            // 4. Channels
            allChannels = withContext(Dispatchers.IO) {
                try { supabase.from("profiles").select().decodeList<UserProfile>().filter { it.handle != null && it.handle.isNotBlank() } } catch(e: Exception) { allChannels }
            }

            // 5. Course Interactions & Mentors (ONLY needed for Course Detail screen)
            if (currentScreen == "course_detail" && selectedCourse != null) {
                 courseInteractions = withContext(Dispatchers.IO) {
                     try { supabase.from("course_interactions").select { filter { eq("course_id", selectedCourse!!.id) } }.decodeList<CourseInteraction>() } catch(e: Exception) { courseInteractions }
                 }
                 mentors = withContext(Dispatchers.IO) {
                     try { supabase.from("mentors").select().decodeList<Mentor>() } catch(e: Exception) { mentors }
                 }
            }"""

content = content.replace(old_block, new_block)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
