#!/bin/bash
sed -i 's/Color(0xFF0F766E)/MaterialTheme.colorScheme.primary/g' app/src/main/java/com/example/VideoPlayer.kt
sed -i 's/Color(0xFF1E293B)/MaterialTheme.colorScheme.onSurface/g' app/src/main/java/com/example/VideoPlayer.kt
sed -i 's/Color(0xFFF1F5F9)/MaterialTheme.colorScheme.surfaceVariant/g' app/src/main/java/com/example/VideoPlayer.kt
sed -i 's/Color(0xFF64748B)/MaterialTheme.colorScheme.onSurfaceVariant/g' app/src/main/java/com/example/VideoPlayer.kt
sed -i 's/Color(0xFFE2E8F0)/MaterialTheme.colorScheme.outlineVariant/g' app/src/main/java/com/example/VideoPlayer.kt
sed -i 's/Color(0xFF0F172A)/MaterialTheme.colorScheme.background/g' app/src/main/java/com/example/VideoPlayer.kt
