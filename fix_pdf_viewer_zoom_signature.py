import re

with open('app/src/main/java/com/example/PdfViewer.kt', 'r') as f:
    content = f.read()

content = content.replace("    }\n}\n\n@Composable\nfun ZoomablePdfPage(", "}\n\n@Composable\nfun ZoomablePdfPage(")

with open('app/src/main/java/com/example/PdfViewer.kt', 'w') as f:
    f.write(content)
