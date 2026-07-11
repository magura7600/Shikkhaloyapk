import re

with open('app/src/main/java/com/example/AdminDashboardScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("v${update.version_name}", "v${latestAppUpdate?.version_name}")
content = content.replace("AppUpdateManager.deleteUpdate(update.id ?: 0)", "AppUpdateManager.deleteUpdate(latestAppUpdate?.id ?: 0)")

with open('app/src/main/java/com/example/AdminDashboardScreen.kt', 'w') as f:
    f.write(content)
