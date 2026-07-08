import re

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

# Fix 1: Guard transformState with isLocked and bound it a bit? Or just reset it properly.
target_transform = """    val transformState = androidx.compose.foundation.gestures.rememberTransformableState { zoomChange, offsetChange, _ ->
        if (!VideoPipState.isInPip) {
            scale = (scale * zoomChange).coerceIn(1f, 5f)
            if (scale > 1f) {
                offset += offsetChange
            } else {
                offset = androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }"""

replacement_transform = """    val transformState = androidx.compose.foundation.gestures.rememberTransformableState { zoomChange, offsetChange, _ ->
        if (!VideoPipState.isInPip && !isLocked) {
            scale = (scale * zoomChange).coerceIn(1f, 5f)
            if (scale > 1f) {
                offset += offsetChange
                // Roughly bound offset to prevent dragging completely out (max 1000px per scale factor for simplicity)
                val maxOffset = 1500f * (scale - 1f)
                offset = androidx.compose.ui.geometry.Offset(
                    x = offset.x.coerceIn(-maxOffset, maxOffset),
                    y = offset.y.coerceIn(-maxOffset, maxOffset)
                )
            } else {
                offset = androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }"""
content = content.replace(target_transform, replacement_transform)

# Fix 2: Reset scale and offset when resizeMode changes
target_resize = """                        Button(
                            onClick = {
                                resizeMode = when (resizeMode) {
                                    androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT -> {
                                        statusMessage = "ভিডিও মোড: জুম"
                                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    }
                                    androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> {
                                        statusMessage = "ভিডিও মোড: ফুল স্ক্রিন"
                                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                                    }
                                    else -> {
                                        statusMessage = "ভিডিও মোড: ফিট"
                                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                }
                            },"""

replacement_resize = """                        Button(
                            onClick = {
                                scale = 1f
                                offset = androidx.compose.ui.geometry.Offset.Zero
                                resizeMode = when (resizeMode) {
                                    androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT -> {
                                        statusMessage = "ভিডিও মোড: জুম"
                                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    }
                                    androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> {
                                        statusMessage = "ভিডিও মোড: ফুল স্ক্রিন"
                                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                                    }
                                    else -> {
                                        statusMessage = "ভিডিও মোড: ফিট"
                                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                }
                            },"""
content = content.replace(target_resize, replacement_resize)

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)
