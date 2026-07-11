import re

with open('app/src/main/java/com/example/DashboardScreen.kt', 'r') as f:
    content = f.read()

# Replace CourseDetailScreen arguments
# Find the call to CourseDetailScreen and its arguments
pattern = r'CourseDetailScreen\(\s*course\s*=\s*activeCourse,\s*profile\s*=\s*profile,\s*mentors\s*=\s*mentors,\s*userEnrollment\s*=\s*([^\n]+),\s*isLiked\s*=\s*([^\n]+),\s*courseInteractions\s*=\s*courseInteractions,'

replacement = r'CourseDetailScreen(\n                        initialCourse = activeCourse,\n                        profile = profile,\n                        userEnrollment = \1,'

content = re.sub(pattern, replacement, content, flags=re.MULTILINE)

with open('app/src/main/java/com/example/DashboardScreen.kt', 'w') as f:
    f.write(content)
