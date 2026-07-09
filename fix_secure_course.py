import re

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

# Add FLAG_SECURE at the top of CourseDetailScreen
secure_code = """
    val activity = LocalContext.current as? androidx.activity.ComponentActivity
    val isAdmin = LocalContext.current.getSharedPreferences("shikkhaloy_prefs", android.content.Context.MODE_PRIVATE).getString("user_role", "") == "admin"
    androidx.compose.runtime.DisposableEffect(isAdmin) {
        val window = activity?.window
        if (window != null && !isAdmin) {
            window.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            if (window != null && !isAdmin) {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
"""

content = re.sub(
    r'val isTeacher = course\.channel_id == profile\.user_id',
    'val isTeacher = course.channel_id == profile.user_id\n' + secure_code,
    content
)

# Also fix the Speed/Quality Dialogs in VideoPlayer to have FLAG_SECURE
dialog_secure_code = """
        val dialogWindow = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        androidx.compose.runtime.LaunchedEffect(dialogWindow) {
            if (dialogWindow != null && !isAdmin) {
                dialogWindow.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
"""

content = re.sub(
    r'(if \(showQualitySelector\) \{\s*AlertDialog\(\s*onDismissRequest = \{\s*showQualitySelector = false\s*\},\s*title = \{)',
    dialog_secure_code.strip() + '\n\\1',
    content
)

content = re.sub(
    r'(if \(showSpeedDialog\) \{\s*AlertDialog\(\s*onDismissRequest = \{\s*showSpeedDialog = false\s*\},\s*title = \{)',
    dialog_secure_code.strip() + '\n\\1',
    content
)

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)
