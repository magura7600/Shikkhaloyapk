import re

with open('app/src/main/java/com/example/PdfViewer.kt', 'r') as f:
    content = f.read()

content = content.replace("    onTap: () -> Unit = {}) {) {", "    onTap: () -> Unit = {}\n) {")
content = content.replace("androidx.compose.material.icons.Icons.Default.Close", "Icons.Default.Close")

with open('app/src/main/java/com/example/PdfViewer.kt', 'w') as f:
    f.write(content)
