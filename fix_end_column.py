import re
with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

# Add the missing closing brace before activePdfToView
target = r"""        if \(activePdfToView != null\) \{
            PdfViewerDialog\("""

replacement = """        } // Close bottom padded column
        if (activePdfToView != null) {
            PdfViewerDialog("""

content = re.sub(target, replacement, content)

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)
