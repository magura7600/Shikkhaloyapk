import re

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

# Fix the comma issue
content = content.replace("    onBack: () -> Unit\n) {,", "    onBack: () -> Unit,")

# Wait, were there others?
# Let's check for any `) {,` just in case.
content = content.replace(") {,", ",")

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)
