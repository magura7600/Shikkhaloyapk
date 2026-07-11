import re

with open('app/src/main/java/com/example/AdminDashboardScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("if (latestNotice != null) {", "latestNotice?.let { notice ->")
content = content.replace("latestNotice!!.", "notice.")

content = content.replace("if (latestAppUpdate != null) {", "latestAppUpdate?.let { update ->")
content = content.replace("latestAppUpdate!!.", "update.")

with open('app/src/main/java/com/example/AdminDashboardScreen.kt', 'w') as f:
    f.write(content)
