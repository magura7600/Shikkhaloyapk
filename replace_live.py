import sys

def main():
    file_path = "app/src/main/java/com/example/CourseDetailScreen.kt"
    with open(file_path, "r") as f:
        content = f.read()
    
    target = """        // Video Player Placeholder or Actual Player or Live Countdown Timer
        if (isClassUpcoming(clazz)) {
            val targetTime = getClassCalendar(clazz.date, clazz.time)?.timeInMillis ?: 0L
            var timeRemainingMillis by remember { mutableStateOf(0L) }
            LaunchedEffect(targetTime) {
                while (true) {
                    val now = System.currentTimeMillis()
                    timeRemainingMillis = (targetTime - now).coerceAtLeast(0L)
                    if (timeRemainingMillis <= 0L) {
                        break
                    }
                    kotlinx.coroutines.delay(1000)
                }
            }
            val days = timeRemainingMillis / (1000 * 60 * 60 * 24)
            val hours = (timeRemainingMillis / (1000 * 60 * 60)) % 24
            val minutes = (timeRemainingMillis / (1000 * 60)) % 60
            val seconds = (timeRemainingMillis / 1000) % 60

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                            .border(1.dp, accentColor, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(accentColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "লাইভ ক্লাস শুরু হতে বাকি",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CountdownUnit(value = days, label = "দিন")
                        CountdownDivider()
                        CountdownUnit(value = hours, label = "ঘণ্টা")
                        CountdownDivider()
                        CountdownUnit(value = minutes, label = "মিনিট")
                        CountdownDivider()
                        CountdownUnit(value = seconds, label = "সেকেন্ড")
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Surface(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${if (clazz.date.isNotBlank()) clazz.date else "5 May 2026"} • ${if (clazz.time.isNotBlank()) clazz.time else "11:01 AM"}",
                                color = Color.LightGray,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    if (clazz.liveLink.isNotBlank()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        val isLiveActive = timeRemainingMillis <= 0L
                        Button(
                            onClick = {
                                if (clazz.liveLink.startsWith("http://") || clazz.liveLink.startsWith("https://")) {
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(clazz.liveLink))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "লিঙ্ক ওপেন করা যায়নি", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "সঠিক লিঙ্ক পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLiveActive) accentColor else Color.Gray.copy(alpha = 0.3f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isLiveActive) "লাইভ ক্লাসে যোগ দিন" else "নির্দিষ্ট সময়ে জয়েন বাটন সচল হবে",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        } else if (clazz.recordedLink.isNotBlank()) {"""

    with open("patch_course_detail.txt", "r") as f:
        replacement = f.read().strip()
    
    # We will strip the whitespace from both ends for matching, since exact indentation could be tricky
    target_lines = [l.strip() for l in target.split("\n") if l.strip()]
    content_lines = content.split("\n")
    
    # Simple search
    start_idx = -1
    for i in range(len(content_lines)):
        match = True
        for j in range(len(target_lines)):
            if i+j >= len(content_lines) or target_lines[j] != content_lines[i+j].strip():
                match = False
                break
        if match:
            start_idx = i
            break
            
    if start_idx != -1:
        end_idx = start_idx + len(target_lines)
        new_content = "\n".join(content_lines[:start_idx]) + "\n" + replacement + "\n" + "\n".join(content_lines[end_idx:])
        with open(file_path, "w") as f:
            f.write(new_content)
        print("Replaced successfully")
    else:
        print("Target not found. Let's dump nearby.")
        for i, l in enumerate(content_lines):
            if "if (isClassUpcoming(clazz)) {" in l:
                for j in range(-5, 120):
                    print(content_lines[i+j])
                break

if __name__ == "__main__":
    main()
