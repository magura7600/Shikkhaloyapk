package com.example.utils

import com.example.CourseItem
import com.example.CourseQuarter

object BusinessLogic {
    /**
     * Calculates the total price for a course purchase.
     */
    fun calculateTotalPrice(
        course: CourseItem,
        selectFullCourse: Boolean,
        selectedQuarters: List<CourseQuarter>
    ): Double {
        if (course.pricingOption == "Fully Free") {
            return 0.0
        }
        return if (selectFullCourse) {
            val discountPrice = course.discountPrice.toDoubleOrNull()
            val mainPrice = course.mainPrice.toDoubleOrNull() ?: 0.0
            
            if (discountPrice != null && discountPrice > 0.0) {
                discountPrice
            } else {
                mainPrice
            }
        } else {
            selectedQuarters.sumOf { it.price.toDoubleOrNull() ?: 0.0 }
        }
    }

    /**
     * Checks if a user has access to a specific chapter based on enrollment.
     */
    fun canAccessChapter(
        enrollmentQuarters: String?,
        quarterId: String,
        isQuarterOn: Boolean,
        isTeacher: Boolean
    ): Boolean {
        if (isTeacher) return true
        if (!isQuarterOn) return true
        if (enrollmentQuarters.isNullOrBlank()) return true // Full course purchased

        val purchasedQuartersList = enrollmentQuarters.split(",").map { it.trim() }
        return purchasedQuartersList.contains(quarterId.trim())
    }
    
    /**
     * Resolves the user's role for app access logic.
     */
    fun resolveUserRole(roleString: String?): String {
        val trimmed = roleString?.trim()?.lowercase() ?: "student"
        return if (trimmed in listOf("student", "teacher", "admin")) {
            trimmed
        } else {
            "student"
        }
    }
    
    /**
     * Calculates the discount percentage if applicable.
     */
    fun calculateDiscountPercentage(mainPrice: String, discountPrice: String): Int {
        val original = mainPrice.toDoubleOrNull() ?: 0.0
        val discount = discountPrice.toDoubleOrNull() ?: 0.0
        
        if (original > 0 && discount > 0 && original > discount) {
            return (((original - discount) / original) * 100).toInt()
        }
        return 0
    }
}
