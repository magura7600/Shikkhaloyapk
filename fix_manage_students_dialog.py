import re

with open('app/src/main/java/com/example/ManageStudentsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("val currentEnrollment = selectedEnrollment", "val currentEnrollment = selectedEnrollment\n            if (currentEnrollment != null) {")
content = content.replace("e.printStackTrace()\n                        }\n                    }\n                }\n            )\n        }", "e.printStackTrace()\n                        }\n                    }\n                }\n            )\n            }\n        }")
content = content.replace("filter { eq(\"id\", currentEnrollment.id) }", "filter { eq(\"id\", currentEnrollment?.id ?: \"\") }")
content = content.replace("if (it.id == currentEnrollment.id)", "if (it.id == currentEnrollment?.id)")

with open('app/src/main/java/com/example/ManageStudentsScreen.kt', 'w') as f:
    f.write(content)
