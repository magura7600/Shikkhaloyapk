import re

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

target = """                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("এই কোয়ার্টারে কোনো অধ্যায় যোগ করা হয়নি।", color = Color.Gray, fontSize = 14.sp)
                }"""

replacement = """                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = "Empty",
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "এই কোয়ার্টারে ক্লাস শুরু হয়নি",
                            color = Color(0xFF475569),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "খুব শীঘ্রই এই কোয়ার্টারের ক্লাস শুরু হবে। প্রস্তুতি নিতে থাকুন!",
                            color = Color(0xFF64748B),
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }"""

# Fix EventAvailable import if needed
if "Icons.Default.EventAvailable" not in content and "import androidx.compose.material.icons.filled.EventAvailable" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.DateRange", "import androidx.compose.material.icons.filled.DateRange\nimport androidx.compose.material.icons.filled.EventAvailable")

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)
