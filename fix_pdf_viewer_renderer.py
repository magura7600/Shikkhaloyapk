import re

with open('app/src/main/java/com/example/PdfViewer.kt', 'r') as f:
    content = f.read()

content = content.replace("pdfRenderer = pdfRenderer!!,", "pdfRenderer = pdfRenderer,")
content = re.sub(r'\} else if \(pageCount > 0 && pdfRenderer != null\) \{(\s*val pagerState)', r'} else if (pageCount > 0 && pdfRenderer != null) {\n            pdfRenderer?.let { validRenderer ->\1', content)

with open('app/src/main/java/com/example/PdfViewer.kt', 'w') as f:
    f.write(content)
