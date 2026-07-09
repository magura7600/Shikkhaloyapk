import sys

def main():
    file_path = "app/src/main/java/com/example/ExploreFeedScreen.kt"
    with open(file_path, "r") as f:
        content = f.read()

    target = """                            // Details
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(course.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2, color = Color(0xFF2D3748))
                                Spacer(modifier = Modifier.height(4.dp))"""
                                
    replacement = """                            // Details
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(course.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2, color = Color(0xFF2D3748))
                                
                                val courseChannel = allChannels.find { it.user_id == course.channel_id }
                                if (courseChannel != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.clickable { onChannelClick(courseChannel) }) {
                                        Box(
                                            modifier = Modifier.size(20.dp).clip(androidx.compose.foundation.shape.CircleShape).background(accentColor.copy(alpha=0.15f)),
                                            contentAlignment = androidx.compose.ui.Alignment.Center
                                        ) {
                                            Text(courseChannel.full_name.firstOrNull()?.toString() ?: "T", color = accentColor, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(courseChannel.full_name, fontSize = 13.sp, color = accentColor, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))"""

    if target in content:
        content = content.replace(target, replacement)
        with open(file_path, "w") as f:
            f.write(content)
        print("Replaced successfully in ExploreFeedScreen!")
    else:
        print("Target not found.")

if __name__ == "__main__":
    main()
