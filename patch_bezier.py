import re

with open('app/src/main/java/com/example/InteractiveBear.kt', 'r') as f:
    content = f.read()

content = content.replace('quadraticBezierTo(', 'quadraticTo(')

with open('app/src/main/java/com/example/InteractiveBear.kt', 'w') as f:
    f.write(content)
