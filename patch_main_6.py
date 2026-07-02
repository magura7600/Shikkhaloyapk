import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

filter_regex = re.compile(r"courses = courses\.filter \{ it\.id != course\.id \}")
new_filter = "courses = courses.filter { it.id != course.id }\n                                        enrollmentRequests = enrollmentRequests.filter { it.course_id != course.id }"
content = content.replace("courses = courses.filter { it.id != course.id }", new_filter)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
