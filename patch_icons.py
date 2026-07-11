import re
import os

def replace_in_file(filepath):
    if not os.path.exists(filepath):
        return
        
    with open(filepath, 'r') as f:
        content = f.read()

    # Revert Outlined auto mirrored
    content = re.sub(r'Icons\.AutoMirrored\.Outlined\.MenuBook', 'Icons.Outlined.MenuBook', content)
    content = re.sub(r'Icons\.AutoMirrored\.Outlined\.Logout', 'Icons.AutoMirrored.Filled.Logout', content) # Or revert to Icons.Outlined.Logout
    
    with open(filepath, 'w') as f:
        f.write(content)

replace_in_file('app/src/main/java/com/example/DashboardScreen.kt')
replace_in_file('app/src/main/java/com/example/SettingsScreen.kt')

