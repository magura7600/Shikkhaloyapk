import re

with open('app/src/main/java/com/example/PdfViewer.kt', 'r') as f:
    content = f.read()

content = content.replace("    }\n}\n\n@Composable\nfun ZoomablePdfPage(", "        }\n    }\n}\n\n@Composable\nfun ZoomablePdfPage(")
content = content.replace("CircleShape", "androidx.compose.foundation.shape.CircleShape")

with open('app/src/main/java/com/example/PdfViewer.kt', 'w') as f:
    f.write(content)
