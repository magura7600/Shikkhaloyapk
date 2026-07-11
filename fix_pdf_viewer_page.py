import re

with open('app/src/main/java/com/example/PdfViewer.kt', 'r') as f:
    content = f.read()

content = content.replace("PdfPageViewer(", "ZoomablePdfPage(")
content = content.replace("fun ZoomablePdfPage(", "fun ZoomablePdfPage(")

with open('app/src/main/java/com/example/PdfViewer.kt', 'w') as f:
    f.write(content)
