package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.CourseItem
import com.example.Enrollment
import com.example.EnrollmentRequest
import com.example.CourseInteraction
import com.example.Mentor
import com.example.UserProfile
import com.example.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DashboardUiState(
    val courses: List<CourseItem> = emptyList(),
    val enrollments: List<Enrollment> = emptyList(),
    val enrollmentRequests: List<EnrollmentRequest> = emptyList(),
    val allChannels: List<UserProfile> = emptyList(),
    val courseInteractions: List<CourseInteraction> = emptyList(),
    val mentors: List<Mentor> = emptyList(),
    val isInitialLoadComplete: Boolean = false
)

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun loadData(
        currentScreen: String,
        selectedTab: Int,
        selectedCourse: CourseItem?,
        profile: UserProfile,
        isTeacher: Boolean,
        isAdmin: Boolean
    ) {
        viewModelScope.launch {
            try {
                var newEnrollments = _uiState.value.enrollments
                var newRequests = _uiState.value.enrollmentRequests

                // 1. My Enrollments
                if (!isTeacher && !isAdmin) {
                    newEnrollments = withContext(Dispatchers.IO) {
                        try {
                            supabase.from("enrollments").select { filter { eq("user_id", profile.user_id) } }.decodeList<Enrollment>()
                        } catch(e: Exception) { _uiState.value.enrollments }
                    }
                    newRequests = withContext(Dispatchers.IO) {
                        try { supabase.from("enrollment_requests").select { filter { eq("user_id", profile.user_id) } }.decodeList<EnrollmentRequest>() } catch(e: Exception) { _uiState.value.enrollmentRequests }
                    }
                }

                // 2. All Enrollments & Requests
                if (isTeacher || isAdmin) {
                    newEnrollments = withContext(Dispatchers.IO) {
                        try { supabase.from("enrollments").select().decodeList<Enrollment>() } catch(e: Exception) { _uiState.value.enrollments }
                    }
                    if (selectedTab == 2 || currentScreen == "enrollment_requests") {
                        newRequests = withContext(Dispatchers.IO) {
                            try { supabase.from("enrollment_requests").select().decodeList<EnrollmentRequest>() } catch(e: Exception) { _uiState.value.enrollmentRequests }
                        }
                    }
                }

                // 3. Courses
                val newCourses = withContext(Dispatchers.IO) {
                    try { supabase.from("courses").select().decodeList<CourseItem>() } catch(e: Exception) { _uiState.value.courses }
                }
                val mappedCourses = if (isTeacher || isAdmin) {
                    newCourses.map { c -> c.copy(studentsCount = newEnrollments.count { it.course_id == c.id }) }
                } else {
                    newCourses
                }

                // 4. Channels
                val allChannels = withContext(Dispatchers.IO) {
                    try {
                        supabase.from("public_channels").select().decodeList<UserProfile>()
                    } catch (e: Exception) {
                        try {
                            supabase.from("profiles").select().decodeList<UserProfile>().filter { it.handle != null && it.handle.isNotBlank() }
                        } catch (ex: Exception) {
                            _uiState.value.allChannels
                        }
                    }
                }

                // 5. Course Interactions & Mentors
                var courseInteractions = _uiState.value.courseInteractions
                var mentors = _uiState.value.mentors
                if (currentScreen == "course_detail" && selectedCourse != null) {
                    courseInteractions = withContext(Dispatchers.IO) {
                        try { supabase.from("course_interactions").select { filter { eq("course_id", selectedCourse.id) } }.decodeList<CourseInteraction>() } catch(e: Exception) { _uiState.value.courseInteractions }
                    }
                    mentors = withContext(Dispatchers.IO) {
                        try {
                            val cid = selectedCourse.channel_id
                            if (!cid.isNullOrBlank()) {
                                supabase.from("mentors").select { filter { eq("channel_id", cid) } }.decodeList<Mentor>()
                            } else {
                                supabase.from("mentors").select().decodeList<Mentor>()
                            }
                        } catch(e: Exception) { _uiState.value.mentors }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    enrollments = newEnrollments,
                    enrollmentRequests = newRequests,
                    courses = mappedCourses,
                    allChannels = allChannels,
                    courseInteractions = courseInteractions,
                    mentors = mentors,
                    isInitialLoadComplete = true
                )

            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
