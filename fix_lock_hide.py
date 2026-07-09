import re

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

# Fix 1: Allow controlsVisible to hide when locked
content = re.sub(
    r'if \(controlsVisible && !isLocked && !isDraggingSlider && !showSpeedDialog && !showQualitySelector\) \{',
    'if (controlsVisible && !isDraggingSlider && !showSpeedDialog && !showQualitySelector) {',
    content
)

# Fix 2: Allow tap to show controls when locked
old_gesture = """fun Modifier.playerGestureDetector(
    isLocked: Boolean,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit
): Modifier = this.pointerInput(isLocked) {
    if (isLocked) return@pointerInput
    detectTapGestures(
        onTap = { onTap() },
        onDoubleTap = { onDoubleTap() }
    )
}"""

new_gesture = """fun Modifier.playerGestureDetector(
    isLocked: Boolean,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit
): Modifier = this.pointerInput(isLocked) {
    if (isLocked) {
        detectTapGestures(
            onTap = { onTap() }
        )
    } else {
        detectTapGestures(
            onTap = { onTap() },
            onDoubleTap = { onDoubleTap() }
        )
    }
}"""

content = content.replace(old_gesture, new_gesture)

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)
