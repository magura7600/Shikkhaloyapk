package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.CourseItem
import com.example.CourseInteraction
import com.example.Mentor
import com.example.data.SupabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CourseDetailUiState(
    val isLoading: Boolean = true,
    val course: CourseItem? = null,
    val mentors: List<Mentor> = emptyList(),
    val interactions: List<CourseInteraction> = emptyList(),
    val error: String? = null
)

class CourseDetailViewModel : ViewModel() {
    private val repository = SupabaseRepository()
    private val _uiState = MutableStateFlow(CourseDetailUiState())
    val uiState: StateFlow<CourseDetailUiState> = _uiState.asStateFlow()

    fun loadCourseDetails(course: CourseItem) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, course = course)
            try {
                val mentors = repository.getMentorsForChannel(course.channel_id)
                val interactions = repository.getCourseInteractions(course.id)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    mentors = mentors,
                    interactions = interactions
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun toggleLike(userId: String) {
        val currentState = _uiState.value
        val course = currentState.course ?: return
        viewModelScope.launch {
            try {
                val existing = currentState.interactions.find { it.user_id == userId && it.is_like }
                if (existing != null) {
                    repository.removeCourseInteraction(existing.id)
                    _uiState.value = currentState.copy(
                        interactions = currentState.interactions.filter { it.id != existing.id }
                    )
                } else {
                    val newInteraction = CourseInteraction(user_id = userId, course_id = course.id, is_like = true)
                    repository.addCourseInteraction(newInteraction)
                    _uiState.value = currentState.copy(
                        interactions = currentState.interactions + newInteraction
                    )
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun updateCourse(updatedCourse: CourseItem) {
        viewModelScope.launch {
            try {
                repository.updateCourse(updatedCourse)
                _uiState.value = _uiState.value.copy(course = updatedCourse)
            } catch (e: Exception) {
                // handle error
            }
        }
    }
}
