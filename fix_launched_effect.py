with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if "androidx.compose.runtime.LaunchedEffect(course) {" in line:
        skip = True
        continue
    if skip and "NotificationScheduler.scheduleClassNotifications" in line:
        continue
    if skip and "    }" in line:
        skip = False
        continue
    new_lines.append(line)

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.writelines(new_lines)
