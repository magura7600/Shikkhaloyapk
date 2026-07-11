import re

with open('app/src/main/java/com/example/AdminDashboardScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("latestNotice?.let { notice ->", "if (latestNotice != null) {\n                            val notice = latestNotice")
content = content.replace("latestAppUpdate?.let { update ->", "if (latestAppUpdate != null) {\n                            val update = latestAppUpdate")

with open('app/src/main/java/com/example/AdminDashboardScreen.kt', 'w') as f:
    f.write(content)
