#!/bin/bash
sed -i 's/Color(0xFF1E293B)/MaterialTheme.colorScheme.onBackground/g' app/src/main/java/com/example/ClassDetailView.kt
sed -i 's/Color(0xFF0F172A)/MaterialTheme.colorScheme.surfaceVariant/g' app/src/main/java/com/example/ClassDetailView.kt
sed -i 's/Color(0xFFF8FAFC)/MaterialTheme.colorScheme.surface/g' app/src/main/java/com/example/ClassDetailView.kt
sed -i 's/Color(0xFFE2E8F0)/MaterialTheme.colorScheme.outlineVariant/g' app/src/main/java/com/example/ClassDetailView.kt
sed -i 's/Color(0xFF0F766E)/MaterialTheme.colorScheme.primary/g' app/src/main/java/com/example/ClassDetailView.kt
sed -i 's/Color(0xFF10B981)/MaterialTheme.colorScheme.secondary/g' app/src/main/java/com/example/ClassDetailView.kt
