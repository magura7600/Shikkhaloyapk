import re

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

# Fix onCourseUpdate = onCourseUpdate
content = content.replace("onCourseUpdate = onCourseUpdate,", "onCourseUpdate = { viewModel.updateCourse(it) },")

# Fix onLikeToggle = onLikeToggle
content = content.replace("onLikeToggle = onLikeToggle,", "onLikeToggle = { viewModel.toggleLike(profile.user_id) },")
# But I already removed onLikeToggle from UnenrolledCourseOverview call above because it had `onLikeToggle = onLikeToggle,` probably. Let's add it back.
