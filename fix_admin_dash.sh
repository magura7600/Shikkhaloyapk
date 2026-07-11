#!/bin/bash
sed -i 's/Color(0xFF1E293B)/MaterialTheme.colorScheme.onBackground/g' app/src/main/java/com/example/AdminDashboardScreen.kt
sed -i 's/Color(0xFFE2E8F0)/MaterialTheme.colorScheme.outlineVariant/g' app/src/main/java/com/example/AdminDashboardScreen.kt
sed -i 's/Color(0xFFF8FAFC)/MaterialTheme.colorScheme.surfaceVariant/g' app/src/main/java/com/example/AdminDashboardScreen.kt
sed -i 's/Color.White/MaterialTheme.colorScheme.surface/g' app/src/main/java/com/example/AdminDashboardScreen.kt
sed -i 's/Color(0xFFFFFBEB)/MaterialTheme.colorScheme.secondaryContainer/g' app/src/main/java/com/example/AdminDashboardScreen.kt
sed -i 's/Color(0xFFFDE68A)/MaterialTheme.colorScheme.secondary/g' app/src/main/java/com/example/AdminDashboardScreen.kt
