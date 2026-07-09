import re

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

# Remove the negative padding layout hack
content = re.sub(
    r'modifier = Modifier\.fillMaxWidth\(\)\.height\(250\.dp\)\.layout \{ measurable, constraints -> val pad = 16\.dp\.roundToPx\(\); val p = measurable\.measure\(constraints\.copy\(maxWidth = constraints\.maxWidth \+ pad \* 2\)\); layout\(p\.width, p\.height\) \{ p\.place\(-pad, 0\) \} \}',
    'modifier = Modifier.fillMaxWidth().height(250.dp)',
    content
)

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)
