package com.example

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CourseQuarter(
    var name: String = "",
    var price: String = "",
    var startDate: String = "",
    var endDate: String = ""
)

@Serializable
data class Enrollment(
    val id: String = UUID.randomUUID().toString(),
    val user_id: String,
    val course_id: String,
    val price_paid: String = "",
    val purchased_quarters: String = "",
    val created_at: String? = null,
    val banned_until: Long? = null,
    val ban_reason: String? = null
)

@Serializable
data class CourseInteraction(
    val id: String = UUID.randomUUID().toString(),
    val user_id: String,
    val course_id: String,
    val is_like: Boolean
)

@Serializable
data class PdfLink(
    val title: String = "",
    val url: String = ""
)

@Serializable
data class CourseClass(
    val id: String = UUID.randomUUID().toString(),
    val type: String = "লেকচার ক্লাস",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val time: String = "",
    val quarterId: String = "",
    val mentorId: String = "",
    val liveLink: String = "",
    val recordedLink: String = "",
    val homeworkLink: String = "",
    val pdfLinks: List<PdfLink> = emptyList(),
    val isFree: Boolean = false
)

@Serializable
data class Mentor(
    val id: String = UUID.randomUUID().toString(),
    val channel_id: String = "",
    val name: String = "",
    val education: String = "",
    val subjects: String = "",
    val experience: String = "",
    val image_url: String = ""
)

@Serializable
data class CourseChapter(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val classes: List<CourseClass> = emptyList(),
    val quarter: String = "Quarter 1",
    val teachingStatus: String = "পড়ানো হবে"
)

@Serializable
data class CourseSubject(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val colorHex: String = "#FF6B6B",
    val iconUrl: String = "",
    val chapters: List<CourseChapter> = emptyList(),
    val sourceCourseId: String? = null
)

@Serializable
data class CourseItem(
    val id: String = UUID.randomUUID().toString(),
    val channel_id: String? = null,
    val title: String = "", 
    val description: String = "",
    val pricingOption: String = "Fully Paid",
    val mainPrice: String = "",
    val discountPrice: String = "",
    val bkashNumber: String = "",
    val nagadNumber: String = "",
    val rocketNumber: String = "",
    val paymentDetails: String = "",
    val isQuarterOn: Boolean = false,
    val quarters: List<CourseQuarter> = emptyList(),
    val subjects: List<CourseSubject> = emptyList(),
    val studentsCount: Int = 0, 
    val rating: Float = 0f,
    val routineUrl: String = ""
)
