import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target = """    var selectedRole by remember { mutableStateOf<String?>(null) } // "teacher" or "student"
    var isSavingProfile by remember { mutableStateOf(false) }"""

replacement = """    var selectedRole by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) } // "teacher" or "student"
    var isSavingProfile by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
