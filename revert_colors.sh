#!/bin/bash

# Reverse Slate 800
find app/src/main/java/com/example -name "*.kt" -exec sed -i 's/MaterialTheme\.colorScheme\.onBackground/Color(0xFF1E293B)/g' {} +
# Reverse Slate 900
find app/src/main/java/com/example -name "*.kt" -exec sed -i 's/MaterialTheme\.colorScheme\.onSurface/Color(0xFF0F172A)/g' {} +
# Reverse Slate 500
find app/src/main/java/com/example -name "*.kt" -exec sed -i 's/MaterialTheme\.colorScheme\.onSurfaceVariant/Color(0xFF64748B)/g' {} +
# Reverse Slate 400
find app/src/main/java/com/example -name "*.kt" -exec sed -i 's/MaterialTheme\.colorScheme\.onSurface\.copy(alpha = 0\.5f)/Color(0xFF94A3B8)/g' {} +
# Reverse Slate 200
find app/src/main/java/com/example -name "*.kt" -exec sed -i 's/MaterialTheme\.colorScheme\.outlineVariant/Color(0xFFE2E8F0)/g' {} +
# Reverse Slate 100
find app/src/main/java/com/example -name "*.kt" -exec sed -i 's/MaterialTheme\.colorScheme\.surfaceVariant/Color(0xFFF1F5F9)/g' {} +
# Reverse Slate 50
find app/src/main/java/com/example -name "*.kt" -exec sed -i 's/MaterialTheme\.colorScheme\.background/Color(0xFFF8FAFC)/g' {} +
# Reverse White
find app/src/main/java/com/example -name "*.kt" -exec sed -i 's/MaterialTheme\.colorScheme\.surface/Color.White/g' {} +
# Reverse Red
find app/src/main/java/com/example -name "*.kt" -exec sed -i 's/MaterialTheme\.colorScheme\.error/Color(0xFFEF4444)/g' {} +
# Reverse Teal
find app/src/main/java/com/example -name "*.kt" -exec sed -i 's/MaterialTheme\.colorScheme\.secondary/Color(0xFF0F766E)/g' {} +
# Reverse Blue
find app/src/main/java/com/example -name "*.kt" -exec sed -i 's/MaterialTheme\.colorScheme\.primary/Color(0xFF1E3A8A)/g' {} +

