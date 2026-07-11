import re

with open('app/src/main/java/com/example/StudentDashboard.kt', 'r') as f:
    content = f.read()

content = content.replace("items(classesForDate.size) { index ->", "items(classesForDate.size, key = { index -> classesForDate[index].first.id }) { index ->")
content = content.replace("items(classesWithHomework.size) { index ->", "items(classesWithHomework.size, key = { index -> classesWithHomework[index].first.id }) { index ->")
content = content.replace("items(classesWithExams.size) { index ->", "items(classesWithExams.size, key = { index -> classesWithExams[index].first.id }) { index ->")

with open('app/src/main/java/com/example/StudentDashboard.kt', 'w') as f:
    f.write(content)
