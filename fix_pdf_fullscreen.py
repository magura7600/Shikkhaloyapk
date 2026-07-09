import re

with open('app/src/main/java/com/example/PdfViewer.kt', 'r') as f:
    content = f.read()

# Add immersive mode in LaunchedEffect(dialogWindow)
immersive_code = """
                window.setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                window.setBackgroundDrawableResource(android.R.color.transparent)
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
"""

content = re.sub(
    r'window\.setLayout\([\s\S]*?androidx\.core\.view\.WindowCompat\.setDecorFitsSystemWindows\(window, false\)',
    immersive_code.strip(),
    content
)

# Remove .statusBarsPadding() and padding(horizontal = 16.dp, vertical = 12.dp) for the overlay header
content = re.sub(
    r'\.statusBarsPadding\(\)\s*\.padding\(horizontal = 16\.dp, vertical = 12\.dp\)',
    '.padding(horizontal = 16.dp, vertical = 16.dp)',
    content
)

with open('app/src/main/java/com/example/PdfViewer.kt', 'w') as f:
    f.write(content)
