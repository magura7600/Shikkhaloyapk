import re

with open('app/src/main/java/com/example/DashboardScreen.kt', 'r') as f:
    content = f.read()

old_block = """            } else if (currentScreen == "manage_students" && selectedCourse != null) {
                ManageStudentsScreen(
                    course = selectedCourse!!,
                    accentColor = accentColor,
                    onBack = { 
                        currentScreen = "dashboard"
                        selectedCourse = null
                    }
                )"""

new_block = """            } else if (currentScreen == "manage_students" && selectedCourse != null) {
                val currentSelectedCourse = selectedCourse
                if (currentSelectedCourse != null) {
                    ManageStudentsScreen(
                        course = currentSelectedCourse,
                        accentColor = accentColor,
                        onBack = { 
                            currentScreen = "dashboard"
                            selectedCourse = null
                        }
                    )
                }"""

if old_block in content:
    content = content.replace(old_block, new_block)
    with open('app/src/main/java/com/example/DashboardScreen.kt', 'w') as f:
        f.write(content)
    print("Fixed DashboardScreen manage_students")
else:
    print("Block not found in DashboardScreen manage_students")
