import re

with open('app/src/main/java/com/example/ClassDetailView.kt', 'r') as f:
    content = f.read()

content = content.replace("videoOptions = videoOptions ?: emptyList(),", "videoOptions = it,")
content = content.replace("if (videoOptions != null) {", "videoOptions?.let {")
content = content.replace("VideoPlayer(", "VideoPlayer(")

with open('app/src/main/java/com/example/ClassDetailView.kt', 'w') as f:
    f.write(content)

