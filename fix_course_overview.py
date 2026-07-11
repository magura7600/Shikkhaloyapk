import re

with open('app/src/main/java/com/example/CourseOverview.kt', 'r') as f:
    content = f.read()

content = content.replace("recordToDelete!!", "recordToDelete")
content = content.replace("if (showDeleteConfirmDialog && recordToDelete != null) {", "recordToDelete?.let { recordToDelete ->\n        if (showDeleteConfirmDialog) {")
content = content.replace("showDeleteConfirmDialog = false\n                        recordToDelete = null\n                    }\n                )\n            )\n        }", "showDeleteConfirmDialog = false\n                        this@let.recordToDelete = null\n                    }\n                )\n            )\n        }\n        }")

with open('app/src/main/java/com/example/CourseOverview.kt', 'w') as f:
    f.write(content)
