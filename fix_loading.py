import re

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

target = """            Text(
                text = "ভিডিও লিংক খোঁজা হচ্ছে...",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "সেরা রেজুলেশনগুলো প্রস্তুত করা হচ্ছে, অনুগ্রহ করে অপেক্ষা করুন",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )"""

replacement = """            Text(
                text = "লোডিং হচ্ছে...",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ভিডিও লোড হচ্ছে, অনুগ্রহ করে অপেক্ষা করুন",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)
