import re

with open('app/src/main/java/com/example/DashboardScreen.kt', 'r') as f:
    content = f.read()

# Replace CourseDetailScreen arguments
target_call = """                    CourseDetailScreen(
                        course = activeCourse,
                        profile = profile,
                        mentors = mentors,
                        userEnrollment = enrollments.find { it.course_id == activeCourse.id && it.user_id == profile.user_id },
                        isLiked = courseInteractions.any { it.course_id == activeCourse.id && it.user_id == profile.user_id && it.is_like },
                        courseInteractions = courseInteractions,
                        initialSubjectId = initialSubjectId,
                        initialChapterId = initialChapterId,
                        initialClassId = initialClassId,
                        onClearInitialNavigation = {
                            initialSubjectId = null
                            initialChapterId = null
                            initialClassId = null
                        },
                        pendingRequest = enrollmentRequests.find { it.course_id == activeCourse.id && it.user_id == profile.user_id },
                    onPurchaseClick = { currentScreen = "purchase_course" },
                    onEnroll = { purchasedQuarters ->"""

replacement_call = """                    CourseDetailScreen(
                        initialCourse = activeCourse,
                        profile = profile,
                        userEnrollment = enrollments.find { it.course_id == activeCourse.id && it.user_id == profile.user_id },
                        initialSubjectId = initialSubjectId,
                        initialChapterId = initialChapterId,
                        initialClassId = initialClassId,
                        onClearInitialNavigation = {
                            initialSubjectId = null
                            initialChapterId = null
                            initialClassId = null
                        },
                        pendingRequest = enrollmentRequests.find { it.course_id == activeCourse.id && it.user_id == profile.user_id },
                    onPurchaseClick = { currentScreen = "purchase_course" },
                    onEnroll = { purchasedQuarters ->"""

content = content.replace(target_call, replacement_call)

# Now remove the onCourseUpdate and onLikeToggle blocks from DashboardScreen.kt
# Actually, wait, there's no onLikeToggle in DashboardScreen.kt because I see `onCourseUpdate = ...`
# Let's write a regex to remove `onCourseUpdate = { ... },`
# Because it's multi-line, it's safer to just do a string replacement if we know the exact string.

# Let's search for the exact onCourseUpdate string block in DashboardScreen.kt
