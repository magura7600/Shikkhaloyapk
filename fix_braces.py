import re

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("    onBack: () -> Unit\n) {\n) {", "    onBack: () -> Unit\n) {")
content = content.replace("    onBack: () -> Unit\n) {\n) {", "    onBack: () -> Unit\n) {") # Just in case

# Fix PdfViewer Warning import
with open('app/src/main/java/com/example/PdfViewer.kt', 'r') as f:
    pdf_content = f.read()

pdf_content = pdf_content.replace("import androidx.compose.material.icons.filled.Warning", "import androidx.compose.material.icons.filled.Warning")
if "import androidx.compose.material.icons.Icons" not in pdf_content:
    pdf_content = pdf_content.replace("import androidx.compose.material.icons.filled.ArrowBack", "import androidx.compose.material.icons.filled.ArrowBack\nimport androidx.compose.material.icons.Icons")

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/PdfViewer.kt', 'w') as f:
    f.write(pdf_content)

