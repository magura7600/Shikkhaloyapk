import re

with open('app/src/main/java/com/example/ClassDetailView.kt', 'r') as f:
    content = f.read()

content = content.replace("} else videoOptions?.let {", "} else if (videoOptions != null) {")

with open('app/src/main/java/com/example/ClassDetailView.kt', 'w') as f:
    f.write(content)

