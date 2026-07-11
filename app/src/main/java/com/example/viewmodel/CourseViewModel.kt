package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.CourseItem
import com.example.CourseSubject
import com.example.data.SupabaseRepository
import kotlinx.coroutines.launch

class CourseViewModel : ViewModel() {
    private val repository = SupabaseRepository()

    fun syncSubjectToAllCourses(
        course: CourseItem,
        updatedSubject: CourseSubject,
        onMultipleCoursesUpdate: (List<CourseItem>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val courses = repository.getCoursesByChannel(course.channel_id ?: "")
                
                val updatedOtherCourses = mutableListOf<CourseItem>()
                courses.forEach { otherCourse ->
                    if (otherCourse.id != course.id) {
                        val otherCourseSubjects = otherCourse.subjects.toMutableList()
                        val existingIdx = otherCourseSubjects.indexOfFirst { it.id == updatedSubject.id }
                        if (existingIdx != -1) {
                            otherCourseSubjects[existingIdx] = updatedSubject
                            val updatedOtherCourse = otherCourse.copy(subjects = otherCourseSubjects)
                            repository.updateCourse(updatedOtherCourse)
                            updatedOtherCourses.add(updatedOtherCourse)
                        }
                    }
                }
                if (updatedOtherCourses.isNotEmpty()) {
                    onMultipleCoursesUpdate(updatedOtherCourses)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun applySubjectToSelectedCourses(
        course: CourseItem,
        updatedSubject: CourseSubject,
        selectedOtherCourses: List<CourseItem>,
        onMultipleCoursesUpdate: (List<CourseItem>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val allCourses = repository.getCoursesByChannel(course.channel_id ?: "")
                
                val updatedOtherCourses = mutableListOf<CourseItem>()
                val selectedIds = selectedOtherCourses.map { it.id }.toSet()
                
                allCourses.forEach { otherCourse ->
                    if (otherCourse.id != course.id) {
                        val otherCourseSubjects = otherCourse.subjects.toMutableList()
                        val existingIdx = otherCourseSubjects.indexOfFirst { it.id == updatedSubject.id }
                        
                        var changed = false
                        if (selectedIds.contains(otherCourse.id)) {
                            if (existingIdx != -1) {
                                otherCourseSubjects[existingIdx] = updatedSubject
                                changed = true
                            } else {
                                otherCourseSubjects.add(updatedSubject)
                                changed = true
                            }
                        } else {
                            if (existingIdx != -1) {
                                otherCourseSubjects.removeAt(existingIdx)
                                changed = true
                            }
                        }
                        
                        if (changed) {
                            val updatedOtherCourse = otherCourse.copy(subjects = otherCourseSubjects)
                            repository.updateCourse(updatedOtherCourse)
                            updatedOtherCourses.add(updatedOtherCourse)
                        }
                    }
                }
                if (updatedOtherCourses.isNotEmpty()) {
                    onMultipleCoursesUpdate(updatedOtherCourses)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateCourseInfo(
        course: CourseItem,
        onSuccess: (CourseItem) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.updateCourse(course)
                onSuccess(course)
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            }
        }
    }
}
