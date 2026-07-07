import re

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

# Replace the "Quarter Status Badge" UI code
# There are two places it appears: one in course subjects (around line 682) and one in the main view (around line 1063)

target_badge = """                                    // Dynamic Quarter Status Badge
                                    var quarterStatus = "আনলক"
                                    var statusBgColor = if (isSelected) Color.White.copy(alpha = 0.2f) else Color(0xFFD1FAE5)
                                    var statusTextColor = if (isSelected) Color.White else Color(0xFF065F46)

                                    if (quarterObj != null && quarterObj.startDate.isNotBlank() && quarterObj.endDate.isNotBlank()) {
                                        try {
                                            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                                            val start = java.time.LocalDate.parse(quarterObj.startDate, formatter)
                                            val end = java.time.LocalDate.parse(quarterObj.endDate, formatter)
                                            val today = java.time.LocalDate.now()
                                            
                                            val hasClasses = course.subjects.flatMap { it.chapters }.filter { (it.quarter.ifBlank { "Quarter 1" }) == qName }.flatMap { it.classes }.isNotEmpty()
                                            if (today.isBefore(start)) {
                                                if (hasClasses) {
                                                    quarterStatus = "পড়ানো হচ্ছে"
                                                    statusBgColor = if (isSelected) Color(0xFFFEF08A).copy(alpha = 0.3f) else Color(0xFFFEF9C3)
                                                    statusTextColor = if (isSelected) Color.White else Color(0xFF854D0E)
                                                } else {
                                                    quarterStatus = "পড়ানো হবে"
                                                    statusBgColor = if (isSelected) Color(0xFF93C5FD).copy(alpha = 0.3f) else Color(0xFFDBEAFE)
                                                    statusTextColor = if (isSelected) Color.White else Color(0xFF1E3A8A)
                                                }
                                            } else if (today.isAfter(end)) {
                                                quarterStatus = "পড়ানো শেষ"
                                                statusBgColor = if (isSelected) Color(0xFFD1FAE5).copy(alpha = 0.3f) else Color(0xFFD1FAE5)
                                                statusTextColor = if (isSelected) Color.White else Color(0xFF065F46)
                                            } else {
                                                quarterStatus = "পড়ানো হচ্ছে"
                                                statusBgColor = if (isSelected) Color(0xFFFEF08A).copy(alpha = 0.3f) else Color(0xFFFEF9C3)
                                                statusTextColor = if (isSelected) Color.White else Color(0xFF854D0E)
                                            }
                                        } catch (e: Exception) { }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(statusBgColor, shape = RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = quarterStatus,
                                            color = statusTextColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }"""

replacement_badge = """                                    // Dynamic Quarter Status Badge
                                    var quarterStatus = "আনলক"
                                    var statusBgColor = if (isSelected) Color.White.copy(alpha = 0.2f) else Color(0xFFD1FAE5)
                                    var statusTextColor = if (isSelected) Color.White else Color(0xFF065F46)
                                    var statusIcon: androidx.compose.ui.graphics.vector.ImageVector? = null

                                    if (quarterObj != null && quarterObj.startDate.isNotBlank() && quarterObj.endDate.isNotBlank()) {
                                        try {
                                            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                                            val start = java.time.LocalDate.parse(quarterObj.startDate, formatter)
                                            val end = java.time.LocalDate.parse(quarterObj.endDate, formatter)
                                            val today = java.time.LocalDate.now()
                                            
                                            val hasClasses = course.subjects.flatMap { it.chapters }.filter { (it.quarter.ifBlank { "Quarter 1" }) == qName }.flatMap { it.classes }.isNotEmpty()
                                            if (today.isBefore(start)) {
                                                if (hasClasses) {
                                                    quarterStatus = "পড়ানো হচ্ছে"
                                                    statusBgColor = if (isSelected) Color(0xFFFEF08A).copy(alpha = 0.3f) else Color(0xFFFEF9C3)
                                                    statusTextColor = if (isSelected) Color.White else Color(0xFF854D0E)
                                                    statusIcon = androidx.compose.material.icons.Icons.Default.PlayCircle
                                                } else {
                                                    quarterStatus = "পড়ানো হবে"
                                                    statusBgColor = if (isSelected) Color(0xFF93C5FD).copy(alpha = 0.3f) else Color(0xFFDBEAFE)
                                                    statusTextColor = if (isSelected) Color.White else Color(0xFF1E3A8A)
                                                    statusIcon = androidx.compose.material.icons.Icons.Default.Schedule
                                                }
                                            } else if (today.isAfter(end)) {
                                                quarterStatus = "পড়ানো শেষ"
                                                statusBgColor = if (isSelected) Color(0xFFD1FAE5).copy(alpha = 0.3f) else Color(0xFFD1FAE5)
                                                statusTextColor = if (isSelected) Color.White else Color(0xFF065F46)
                                                statusIcon = androidx.compose.material.icons.Icons.Default.CheckCircle
                                            } else {
                                                quarterStatus = "পড়ানো হচ্ছে"
                                                statusBgColor = if (isSelected) Color(0xFFFEF08A).copy(alpha = 0.3f) else Color(0xFFFEF9C3)
                                                statusTextColor = if (isSelected) Color.White else Color(0xFF854D0E)
                                                statusIcon = androidx.compose.material.icons.Icons.Default.PlayCircle
                                            }
                                        } catch (e: Exception) { }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(statusBgColor, shape = RoundedCornerShape(12.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        if (statusIcon != null) {
                                            Icon(
                                                imageVector = statusIcon,
                                                contentDescription = null,
                                                tint = statusTextColor,
                                                modifier = Modifier.size(12.dp).padding(end = 4.dp)
                                            )
                                        }
                                        Text(
                                            text = quarterStatus,
                                            color = statusTextColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }"""

content = content.replace(target_badge, replacement_badge)

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)
