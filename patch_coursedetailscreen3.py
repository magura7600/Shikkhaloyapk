import re

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

# Replace onCourseUpdate = onCourseUpdate with onCourseUpdate = { viewModel.updateCourse(it) }
content = content.replace("onCourseUpdate = onCourseUpdate,", "onCourseUpdate = { viewModel.updateCourse(it) },")

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)
