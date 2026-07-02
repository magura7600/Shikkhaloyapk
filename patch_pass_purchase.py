import re

with open("app/src/main/java/com/example/CourseDetailScreen.kt", "r") as f:
    content = f.read()

# Pass onPurchaseClick to CourseContentSection
target = """                    onExternalAddHandled = { isAddingSubjectTopBar = false }
                )"""
replace = """                    onExternalAddHandled = { isAddingSubjectTopBar = false },
                    onPurchaseClick = onPurchaseClick
                )"""

content = content.replace(target, replace)

with open("app/src/main/java/com/example/CourseDetailScreen.kt", "w") as f:
    f.write(content)

