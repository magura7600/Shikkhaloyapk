package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.CourseItem
import com.example.Enrollment
import com.example.EnrollmentRequest
import com.example.CourseInteraction
import com.example.Mentor
import com.example.UserProfile
import com.example.data.SupabaseRepository
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
    private val repository = SupabaseRepository()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun initializeCachedData(cachedCourses: List<CourseItem>, cachedEnrollments: List<Enrollment>) {
        if (!_uiState.value.isInitialLoadComplete) {
            _uiState.value = _uiState.value.copy(
                courses = cachedCourses,
                enrollments = cachedEnrollments,
                isInitialLoadComplete = cachedCourses.isNotEmpty()
            )
        }
    }

    fun setCourses(newCourses: List<CourseItem>) {
        _uiState.value = _uiState.value.copy(courses = newCourses)
    }

    fun setEnrollments(newEnrollments: List<Enrollment>) {
        _uiState.value = _uiState.value.copy(enrollments = newEnrollments)
    }

    fun setEnrollmentRequests(newRequests: List<EnrollmentRequest>) {
        _uiState.value = _uiState.value.copy(enrollmentRequests = newRequests)
    }

    fun setCourseInteractions(newInteractions: List<CourseInteraction>) {
        _uiState.value = _uiState.value.copy(courseInteractions = newInteractions)
    }

    fun setAllChannels(newChannels: List<UserProfile>) {
        _uiState.value = _uiState.value.copy(allChannels = newChannels)
    }

    fun setMentors(newMentors: List<Mentor>) {
        _uiState.value = _uiState.value.copy(mentors = newMentors)
    }

    fun setInitialLoadComplete(complete: Boolean) {
        _uiState.value = _uiState.value.copy(isInitialLoadComplete = complete)
    }

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
                    newEnrollments = try {
                        repository.getEnrollmentsForUser(profile.user_id)
                    } catch(e: Exception) { _uiState.value.enrollments }
                    
                    newRequests = try {
                        repository.getEnrollmentRequestsForUser(profile.user_id)
                    } catch(e: Exception) { _uiState.value.enrollmentRequests }
                }

                // 2. All Enrollments & Requests
                if (isTeacher || isAdmin) {
                    newEnrollments = try {
                        repository.getAllEnrollments()
                    } catch(e: Exception) { _uiState.value.enrollments }
                    
                    if (selectedTab == 2 || currentScreen == "enrollment_requests") {
                        newRequests = try {
                            repository.getAllEnrollmentRequests()
                        } catch(e: Exception) { _uiState.value.enrollmentRequests }
                    }
                }

                // 3. Courses
                val newCourses = try {
                    repository.getAllCourses()
                } catch(e: Exception) { _uiState.value.courses }
                
                val mappedCourses = if (isTeacher || isAdmin) {
                    newCourses.map { c -> c.copy(studentsCount = newEnrollments.count { it.course_id == c.id }) }
                } else {
                    newCourses
                }

                // 4. Channels
                val allChannels = try {
                    repository.getAllChannels()
                } catch(e: Exception) { _uiState.value.allChannels }

                // 5. Course Interactions & Mentors
                var courseInteractions = _uiState.value.courseInteractions
                var mentors = _uiState.value.mentors
                if (currentScreen == "course_detail" && selectedCourse != null) {
                    courseInteractions = try {
                        repository.getCourseInteractions(selectedCourse.id)
                    } catch(e: Exception) { _uiState.value.courseInteractions }
                    
                    mentors = try {
                        repository.getMentorsForChannel(selectedCourse.channel_id)
                    } catch(e: Exception) { _uiState.value.mentors }
                } else if (isTeacher) {
                    mentors = try {
                        repository.getMentorsForChannel(profile.user_id)
                    } catch(e: Exception) { _uiState.value.mentors }
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

    fun saveCourse(courseToSave: CourseItem, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                if (courseToSave.id.isNotBlank()) {
                    repository.updateCourse(courseToSave)
                    _uiState.value = _uiState.value.copy(
                        courses = _uiState.value.courses.map { if (it.id == courseToSave.id) courseToSave else it }
                    )
                } else {
                    val newId = java.util.UUID.randomUUID().toString()
                    val newCourse = courseToSave.copy(id = newId)
                    repository.addCourse(newCourse)
                    _uiState.value = _uiState.value.copy(
                        courses = _uiState.value.courses + newCourse
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun deleteCourse(courseId: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteEnrollmentsForCourse(courseId)
                repository.deleteInteractionsForCourse(courseId)
                repository.deleteRequestsForCourse(courseId)
                repository.deleteCourse(courseId)
                _uiState.value = _uiState.value.copy(
                    courses = _uiState.value.courses.filter { it.id != courseId },
                    enrollmentRequests = _uiState.value.enrollmentRequests.filter { it.course_id != courseId }
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun enrollInCourse(enrollment: Enrollment, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                repository.addEnrollment(enrollment)
                val updatedCourses = _uiState.value.courses.map {
                    if (it.id == enrollment.course_id) it.copy(studentsCount = it.studentsCount + 1) else it
                }
                _uiState.value = _uiState.value.copy(
                    enrollments = _uiState.value.enrollments + enrollment,
                    courses = updatedCourses
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun toggleLike(courseId: String, userId: String) {
        viewModelScope.launch {
            try {
                val existing = _uiState.value.courseInteractions.find { it.course_id == courseId && it.user_id == userId && it.is_like }
                if (existing != null) {
                    repository.removeCourseInteraction(existing.id)
                    _uiState.value = _uiState.value.copy(
                        courseInteractions = _uiState.value.courseInteractions.filter { it.id != existing.id }
                    )
                } else {
                    val newInteraction = CourseInteraction(course_id = courseId, user_id = userId, is_like = true)
                    repository.addCourseInteraction(newInteraction)
                    _uiState.value = _uiState.value.copy(
                        courseInteractions = _uiState.value.courseInteractions + newInteraction
                    )
                }
            } catch (e: Exception) {
                // handle error silently
            }
        }
    }
    
    fun addViewInteraction(courseId: String, userId: String) {
        viewModelScope.launch {
            try {
                val hasViewed = _uiState.value.courseInteractions.any { it.course_id == courseId && it.user_id == userId && !it.is_like }
                if (!hasViewed) {
                    val newView = CourseInteraction(course_id = courseId, user_id = userId, is_like = false)
                    repository.addCourseInteraction(newView)
                    _uiState.value = _uiState.value.copy(
                        courseInteractions = _uiState.value.courseInteractions + newView
                    )
                }
            } catch (e: Exception) {
                // handle error silently
            }
        }
    }

    fun approveEnrollment(request: EnrollmentRequest) {
        viewModelScope.launch {
            try {
                val enrollment = Enrollment(
                    user_id = request.user_id,
                    course_id = request.course_id,
                    price_paid = request.amount,
                    purchased_quarters = if (request.requested_quarters == "FULL") "" else request.requested_quarters
                )
                repository.addEnrollment(enrollment)
                repository.deleteEnrollmentRequest(request.id)
                _uiState.value = _uiState.value.copy(
                    enrollmentRequests = _uiState.value.enrollmentRequests.filter { it.id != request.id },
                    enrollments = _uiState.value.enrollments + enrollment
                )
            } catch(e: Exception) { }
        }
    }

    fun rejectEnrollment(request: EnrollmentRequest) {
        viewModelScope.launch {
            try {
                repository.deleteEnrollmentRequest(request.id)
                _uiState.value = _uiState.value.copy(
                    enrollmentRequests = _uiState.value.enrollmentRequests.filter { it.id != request.id }
                )
            } catch(e: Exception) { }
        }
    }
    
    fun requestEnrollment(request: EnrollmentRequest, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                repository.addEnrollmentRequest(request)
                onSuccess()
            } catch(e: Exception) {
                onError(e)
            }
        }
    }
    
    fun saveMentor(mentor: Mentor, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                if (mentor.id.isNotBlank()) {
                    repository.updateMentor(mentor)
                    _uiState.value = _uiState.value.copy(
                        mentors = _uiState.value.mentors.map { if(it.id == mentor.id) mentor else it }
                    )
                } else {
                    val newId = java.util.UUID.randomUUID().toString()
                    val newMentor = mentor.copy(id = newId)
                    repository.addMentor(newMentor)
                    _uiState.value = _uiState.value.copy(
                        mentors = _uiState.value.mentors + newMentor
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
    
    fun deleteMentor(mentorId: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteMentor(mentorId)
                _uiState.value = _uiState.value.copy(
                    mentors = _uiState.value.mentors.filter { it.id != mentorId }
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun updateProfile(profile: UserProfile, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                repository.updateProfile(profile)
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun reloadEnrollmentsAndRequests(profile: UserProfile, isTeacher: Boolean, isAdmin: Boolean) {
        viewModelScope.launch {
            try {
                if (!isTeacher && !isAdmin) {
                    val enrollments = repository.getEnrollmentsForUser(profile.user_id)
                    val requests = repository.getEnrollmentRequestsForUser(profile.user_id)
                    _uiState.value = _uiState.value.copy(enrollments = enrollments, enrollmentRequests = requests)
                } else {
                    val enrollments = repository.getAllEnrollments()
                    val requests = repository.getAllEnrollmentRequests()
                    _uiState.value = _uiState.value.copy(enrollments = enrollments, enrollmentRequests = requests)
                }
            } catch(e: Exception) { }
        }
    }
}
