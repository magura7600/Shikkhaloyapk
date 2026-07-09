import re

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

# Add immersive mode logic for VideoPlayer
immersive_code = """
    // Apply immersive mode for full screen video
    androidx.compose.runtime.LaunchedEffect(isManualFullscreen, isDeviceLandscape) {
        val window = activity?.window
        if (window != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            if (isManualFullscreen || isDeviceLandscape) {
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    if ((isManualFullscreen || isDeviceLandscape) && isVideoPlayingActive && activePdfToView == null) {
"""

content = re.sub(
    r'if \(\(isManualFullscreen \|\| isDeviceLandscape\) && isVideoPlayingActive && activePdfToView == null\) \{',
    immersive_code.strip(),
    content
)

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)
