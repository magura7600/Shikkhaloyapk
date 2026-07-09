with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

# Remove the extra `}` before `if (activePdfToView != null)`
content = content.replace("} // Close bottom padded column", "")

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)
