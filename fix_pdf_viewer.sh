#!/bin/bash
sed -i 's/Color(0xFF0F172A)/MaterialTheme.colorScheme.background/g' app/src/main/java/com/example/PdfViewer.kt
sed -i 's/Color(0xFF1E293B)/MaterialTheme.colorScheme.surface/g' app/src/main/java/com/example/PdfViewer.kt
sed -i 's/Color(0xFFF1F5F9)/MaterialTheme.colorScheme.onSurface/g' app/src/main/java/com/example/PdfViewer.kt
sed -i 's/Color(0xFF94A3B8)/MaterialTheme.colorScheme.onSurfaceVariant/g' app/src/main/java/com/example/PdfViewer.kt
sed -i 's/Color(0xFF334155)/MaterialTheme.colorScheme.outlineVariant/g' app/src/main/java/com/example/PdfViewer.kt
