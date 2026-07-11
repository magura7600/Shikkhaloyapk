package com.example

import com.example.utils.BusinessLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessLogicTest {

    @Test
    fun `calculateTotalPrice - Fully Free course`() {
        val course = CourseItem(id = "1", channel_id = "1", title = "Test", pricingOption = "Fully Free", mainPrice = "1000", discountPrice = "500")
        val price = BusinessLogic.calculateTotalPrice(course, true, emptyList())
        assertEquals(0.0, price, 0.001)
    }

    @Test
    fun `calculateTotalPrice - Full Course with discount`() {
        val course = CourseItem(id = "1", channel_id = "1", title = "Test", pricingOption = "Fully Paid", mainPrice = "1000", discountPrice = "500")
        val price = BusinessLogic.calculateTotalPrice(course, true, emptyList())
        assertEquals(500.0, price, 0.001)
    }

    @Test
    fun `calculateTotalPrice - Full Course without discount`() {
        val course = CourseItem(id = "1", channel_id = "1", title = "Test", pricingOption = "Fully Paid", mainPrice = "1000", discountPrice = "")
        val price = BusinessLogic.calculateTotalPrice(course, true, emptyList())
        assertEquals(1000.0, price, 0.001)
    }

    @Test
    fun `calculateTotalPrice - Quarters selected`() {
        val course = CourseItem(id = "1", channel_id = "1", title = "Test", pricingOption = "Fully Paid", mainPrice = "1000", discountPrice = "500")
        val quarters = listOf(CourseQuarter("q1", "200"), CourseQuarter("q2", "300"))
        val price = BusinessLogic.calculateTotalPrice(course, false, quarters)
        assertEquals(500.0, price, 0.001)
    }

    @Test
    fun `canAccessChapter - Teacher always has access`() {
        val access = BusinessLogic.canAccessChapter(enrollmentQuarters = "q1", quarterId = "q2", isQuarterOn = true, isTeacher = true)
        assertTrue(access)
    }

    @Test
    fun `canAccessChapter - Course is not quarter based`() {
        val access = BusinessLogic.canAccessChapter(enrollmentQuarters = "", quarterId = "q2", isQuarterOn = false, isTeacher = false)
        assertTrue(access)
    }

    @Test
    fun `canAccessChapter - Full course enrollment (empty quarters)`() {
        val access = BusinessLogic.canAccessChapter(enrollmentQuarters = "", quarterId = "q2", isQuarterOn = true, isTeacher = false)
        assertTrue(access)
    }

    @Test
    fun `canAccessChapter - Specific quarter purchased`() {
        val access = BusinessLogic.canAccessChapter(enrollmentQuarters = "q1, q2", quarterId = "q2", isQuarterOn = true, isTeacher = false)
        assertTrue(access)
    }

    @Test
    fun `canAccessChapter - Specific quarter NOT purchased`() {
        val access = BusinessLogic.canAccessChapter(enrollmentQuarters = "q1, q3", quarterId = "q2", isQuarterOn = true, isTeacher = false)
        assertFalse(access)
    }

    @Test
    fun `resolveUserRole - returns student by default`() {
        assertEquals("student", BusinessLogic.resolveUserRole(null))
        assertEquals("student", BusinessLogic.resolveUserRole(""))
        assertEquals("student", BusinessLogic.resolveUserRole("invalid_role"))
    }

    @Test
    fun `resolveUserRole - correctly resolves valid roles`() {
        assertEquals("teacher", BusinessLogic.resolveUserRole("teacher"))
        assertEquals("teacher", BusinessLogic.resolveUserRole("  TEACHER  "))
        assertEquals("admin", BusinessLogic.resolveUserRole("Admin"))
        assertEquals("student", BusinessLogic.resolveUserRole("student"))
    }
    
    @Test
    fun `calculateDiscountPercentage - standard discount`() {
        assertEquals(50, BusinessLogic.calculateDiscountPercentage("1000", "500"))
        assertEquals(20, BusinessLogic.calculateDiscountPercentage("100", "80"))
    }
    
    @Test
    fun `calculateDiscountPercentage - no discount`() {
        assertEquals(0, BusinessLogic.calculateDiscountPercentage("1000", "1000"))
        assertEquals(0, BusinessLogic.calculateDiscountPercentage("1000", "1500"))
        assertEquals(0, BusinessLogic.calculateDiscountPercentage("1000", ""))
        assertEquals(0, BusinessLogic.calculateDiscountPercentage("", "500"))
    }
}
