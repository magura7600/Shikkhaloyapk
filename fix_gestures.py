import re

with open('app/src/main/java/com/example/PdfViewer.kt', 'r') as f:
    content = f.read()

target = """                            .pointerInput(Unit) {
                                androidx.compose.foundation.gestures.detectDragGestures { change, dragAmount ->
                                    if (scale > 1f) {
                                        change.consume()
                                        offsetX += dragAmount.x
                                        offsetY += dragAmount.y
                                    }
                                }
                            }
                            .transformable(state = state)"""

replacement = """                            .transformable(state = state)"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/PdfViewer.kt', 'w') as f:
    f.write(content)
