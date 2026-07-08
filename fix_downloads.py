import re

with open('app/src/main/java/com/example/OfflineDownloadsDialog.kt', 'r') as f:
    content = f.read()

# Remove the 3rd tab
tab3_regex = r"""                    Tab\(\s*selected = selectedTab == 2,[\s\S]*?text = \{[\s\S]*?Text\("পছন্দ ও সাম্প্রতিক", fontWeight = FontWeight\.Bold, fontSize = 14\.sp\)[\s\S]*?unselectedContentColor = Color\(0xFF64748B\)\s*\)"""
content = re.sub(tab3_regex, "", content)

# Remove the content of the 3rd tab (if selectedTab == 2 ... )
# Instead of complex regex, let's just find the `if (selectedTab == 2) {` block and replace it if we can.
# Actually, it's easier to just do it via sed or just replace the whole section if needed.
