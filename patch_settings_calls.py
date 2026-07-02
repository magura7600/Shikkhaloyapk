import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Replace SettingsScreen calls
# Note: we can just find `onProfileUpdate = onProfileUpdate,` and inject our two new callbacks if `courses = courses,` is present or not, but it's easier to just regex the whole call.

call_regex = re.compile(r"(SettingsScreen\(\s*profile = profile,\s*(teacherChannel = teacherChannel,)?\s*onLogout = onLogout,\s*accentColor = accentColor,\s*onProfileUpdate = onProfileUpdate,(\s*courses = courses,\s*enrollments = enrollments)?\s*\))")

def replacer(m):
    return m.group(0).replace(")", ",\n                            onNavigateToMyEnrollments = { currentScreen = \"my_enrollments\" },\n                            onNavigateToEnrollmentRequests = { currentScreen = \"enrollment_requests\" }\n                        )")

content = re.sub(call_regex, replacer, content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
