import re

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("if (isUserBanned && activeEnrollment != null) {", "if (isUserBanned) {")

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)

