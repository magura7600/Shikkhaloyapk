import re

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

# Replace the DisposableEffect around line 2850
target_disposable = """    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            if (activity != null) {
                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }"""

replacement_disposable = """    DisposableEffect(Unit) {
        val originalBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        onDispose {
            exoPlayer.release()
            if (activity != null) {
                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                // Restore original brightness
                val lp = activity.window.attributes
                lp.screenBrightness = originalBrightness
                activity.window.attributes = lp
            }
        }
    }"""

content = content.replace(target_disposable, replacement_disposable)

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)
