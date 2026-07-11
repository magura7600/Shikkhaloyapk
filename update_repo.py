with open('app/src/main/java/com/example/data/SupabaseRepository.kt', 'r') as f:
    content = f.read()

new_methods = """
    suspend fun getAllChannels(): List<UserProfile> = withContext(Dispatchers.IO) {
        try {
            supabase.from("public_channels").select().decodeList<UserProfile>()
        } catch (e: Exception) {
            try {
                supabase.from("profiles").select().decodeList<UserProfile>().filter { it.handle != null && it.handle.isNotBlank() }
            } catch (ex: Exception) {
                emptyList()
            }
        }
    }

    suspend fun getEnrollmentsForUser(userId: String): List<Enrollment> = withContext(Dispatchers.IO) {
        supabase.from("enrollments").select { filter { eq("user_id", userId) } }.decodeList<Enrollment>()
    }

    suspend fun getAllEnrollments(): List<Enrollment> = withContext(Dispatchers.IO) {
        supabase.from("enrollments").select().decodeList<Enrollment>()
    }

    suspend fun getEnrollmentRequestsForUser(userId: String): List<EnrollmentRequest> = withContext(Dispatchers.IO) {
        supabase.from("enrollment_requests").select { filter { eq("user_id", userId) } }.decodeList<EnrollmentRequest>()
    }

    suspend fun getAllEnrollmentRequests(): List<EnrollmentRequest> = withContext(Dispatchers.IO) {
        supabase.from("enrollment_requests").select().decodeList<EnrollmentRequest>()
    }

    suspend fun getMentorsForChannel(channelId: String?): List<Mentor> = withContext(Dispatchers.IO) {
        if (!channelId.isNullOrBlank()) {
            supabase.from("mentors").select { filter { eq("channel_id", channelId) } }.decodeList<Mentor>()
        } else {
            supabase.from("mentors").select().decodeList<Mentor>()
        }
    }

    suspend fun getCourseInteractions(courseId: String): List<CourseInteraction> = withContext(Dispatchers.IO) {
        supabase.from("course_interactions").select { filter { eq("course_id", courseId) } }.decodeList<CourseInteraction>()
    }

    suspend fun addCourseInteraction(interaction: CourseInteraction) = withContext(Dispatchers.IO) {
        supabase.from("course_interactions").insert(interaction)
    }

    suspend fun removeCourseInteraction(interactionId: String) = withContext(Dispatchers.IO) {
        supabase.from("course_interactions").delete { filter { eq("id", interactionId) } }
    }

    suspend fun addCourse(course: CourseItem) = withContext(Dispatchers.IO) {
        supabase.from("courses").insert(course)
    }

    suspend fun deleteCourse(courseId: String) = withContext(Dispatchers.IO) {
        supabase.from("courses").delete { filter { eq("id", courseId) } }
    }
    
    suspend fun deleteEnrollmentsForCourse(courseId: String) = withContext(Dispatchers.IO) {
        supabase.from("enrollments").delete { filter { eq("course_id", courseId) } }
    }
    
    suspend fun deleteInteractionsForCourse(courseId: String) = withContext(Dispatchers.IO) {
        supabase.from("course_interactions").delete { filter { eq("course_id", courseId) } }
    }
    
    suspend fun deleteRequestsForCourse(courseId: String) = withContext(Dispatchers.IO) {
        supabase.from("enrollment_requests").delete { filter { eq("course_id", courseId) } }
    }

    suspend fun getMentors(): List<Mentor> = withContext(Dispatchers.IO) {
        supabase.from("mentors").select().decodeList<Mentor>()
    }

    suspend fun addMentor(mentor: Mentor) = withContext(Dispatchers.IO) {
        supabase.from("mentors").insert(mentor)
    }

    suspend fun updateMentor(mentor: Mentor) = withContext(Dispatchers.IO) {
        supabase.from("mentors").update(mentor) { filter { eq("id", mentor.id) } }
    }

    suspend fun deleteMentor(mentorId: String) = withContext(Dispatchers.IO) {
        supabase.from("mentors").delete { filter { eq("id", mentorId) } }
    }
"""

content = content.replace("}", new_methods + "\n}")
with open('app/src/main/java/com/example/data/SupabaseRepository.kt', 'w') as f:
    f.write(content)
