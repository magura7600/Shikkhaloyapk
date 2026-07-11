import re
import os
def replace_in_file(filepath):
    if not os.path.exists(filepath):
        return
    with open(filepath, 'r') as f:
        content = f.read()
    # If the build is going to fail for Icons.AutoMirrored.Filled.Logout without import
    # we can just use Icons.Outlined.Logout
    content = re.sub(r'Icons\.AutoMirrored\.Filled\.Logout', 'Icons.Outlined.Logout', content)
    with open(filepath, 'w') as f:
        f.write(content)
replace_in_file('app/src/main/java/com/example/SettingsScreen.kt')
