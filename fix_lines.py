import re

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = lines[:139] + [
    "    accentColor: Color,\n",
    "    initialSubjectId: String? = null,\n",
    "    initialChapterId: String? = null,\n",
    "    initialClassId: String? = null,\n",
    "    onBack: () -> Unit\n",
    ") {\n",
    "    var selectedQuarters by remember { mutableStateOf(setOf<CourseQuarter>()) }\n",
    "    var selectFullCourse by remember { mutableStateOf(true) }\n",
    "    var isAddingSubjectTopBar by remember { mutableStateOf(false) }\n",
    "    val mContext = LocalContext.current\n",
    "    androidx.compose.runtime.LaunchedEffect(course) {\n",
    "        NotificationScheduler.scheduleClassNotifications(mContext, course)\n",
    "    }\n",
    "    val isTeacher = course.channel_id == profile.user_id\n",
    "        \n",
    "    var selectedChapterForView by remember { mutableStateOf<CourseChapter?>(null) }\n"
]

# Find where to resume
resume_idx = -1
for i, line in enumerate(lines[140:]):
    if "selectedClassForView" in line and "mutableStateOf" in line:
        resume_idx = 140 + i
        break

new_lines.extend(lines[resume_idx:])

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.writelines(new_lines)
