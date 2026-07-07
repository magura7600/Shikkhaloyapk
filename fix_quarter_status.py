import re

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

def replace_status(match):
    prefix = match.group(1)
    return prefix + """
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
"""

content = re.sub(r'(\s+val today = java\.time\.LocalDate\.now\(\)\s+)if \(today\.isBefore\(start\)\) \{\s+quarterStatus = "পড়ানো হবে"\s+statusBgColor = [^\n]+\s+statusTextColor = [^\n]+\s+\} else if \(today\.isAfter\(end\)\) \{', replace_status, content)

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)
