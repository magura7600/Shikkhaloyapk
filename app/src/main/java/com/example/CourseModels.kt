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
data class EnrollmentRequest(
    val id: String = UUID.randomUUID().toString(),
    val user_id: String,
    val course_id: String,
    val requested_quarters: String = "",
    val amount: String = "",
    val payment_method: String = "",
    val sender_number: String = "",
    val transaction_id: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val rejection_reason: String = "",
    val created_at: String? = null
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
    val sourceCourseId: String? = null,
    val learningResources: List<PdfLink> = emptyList()
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
    @kotlinx.serialization.Transient val studentsCount: Int = 0, 
    @kotlinx.serialization.Transient val rating: Float = 0f
) {
    val routineUrl: String
        get() {
            if (paymentDetails.contains("|||ROUTINE_DATA:")) {
                return paymentDetails.substringAfter("|||ROUTINE_DATA:")
            }
            return ""
        }

    val cleanPaymentDetails: String
        get() {
            if (paymentDetails.contains("|||ROUTINE_DATA:")) {
                return paymentDetails.substringBefore("|||ROUTINE_DATA:")
            }
            return paymentDetails
        }

    val bannerUrl: String
        get() {
            if (routineUrl.startsWith("v2;")) {
                val parts = routineUrl.substring(3).split(";")
                return if (parts.isNotEmpty()) parts[0] else ""
            }
            return if (routineUrl.contains(";")) routineUrl.substringBefore(";") else ""
        }

    val startDate: String
        get() {
            if (routineUrl.startsWith("v2;")) {
                val parts = routineUrl.substring(3).split(";")
                return if (parts.size >= 2) parts[1] else ""
            }
            return ""
        }

    val endDate: String
        get() {
            if (routineUrl.startsWith("v2;")) {
                val parts = routineUrl.substring(3).split(";")
                return if (parts.size >= 3) parts[2] else ""
            }
            return ""
        }

    val realRoutineUrl: String
        get() {
            if (routineUrl.startsWith("v2;")) {
                val parts = routineUrl.substring(3).split(";")
                return if (parts.size >= 4) parts.subList(3, parts.size).joinToString(";") else ""
            }
            return if (routineUrl.contains(";")) routineUrl.substringAfter(";") else routineUrl
        }
}
