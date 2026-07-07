import re

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

target = """    LaunchedEffect(courseId) {
        if (courseId != null) {
            viewModel.fetchCourseDetails(courseId)
        }
    }"""

replacement = """    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(courseId) {
        if (courseId != null) {
            viewModel.fetchCourseDetails(courseId)
        }
    }
    
    LaunchedEffect(course) {
        if (course != null) {
            NotificationScheduler.scheduleClassNotifications(context, course)
        }
    }"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)
