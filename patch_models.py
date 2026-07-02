import re

with open("app/src/main/java/com/example/CourseModels.kt", "r") as f:
    content = f.read()

new_model = """
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
"""

if "EnrollmentRequest" not in content:
    content = content.replace("data class Enrollment(", new_model + "\n@Serializable\ndata class Enrollment(")

with open("app/src/main/java/com/example/CourseModels.kt", "w") as f:
    f.write(content)
