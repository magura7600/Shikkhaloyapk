import re

with open('app/src/main/java/com/example/AdminDashboardScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("latestNotice!!.", "latestNotice?.")
content = content.replace("latestAppUpdate!!.", "latestAppUpdate?.")

with open('app/src/main/java/com/example/AdminDashboardScreen.kt', 'w') as f:
    f.write(content)
