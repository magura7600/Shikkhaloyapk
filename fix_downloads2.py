import re

with open('app/src/main/java/com/example/OfflineDownloadsDialog.kt', 'r') as f:
    content = f.read()

target = """                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = "Favorites", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("পছন্দ ও সাম্প্রতিক", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        },
                        selectedContentColor = accentColor,
                        unselectedContentColor = Color(0xFF64748B)
                    )"""

content = content.replace(target, "")

with open('app/src/main/java/com/example/OfflineDownloadsDialog.kt', 'w') as f:
    f.write(content)
