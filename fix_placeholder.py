import re

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

# Fix placeholder Box
content = re.sub(
    r'modifier = Modifier\s*\.fillMaxWidth\(\)\s*\.height\(200\.dp\)\s*\.background',
    'modifier = Modifier.fillMaxWidth().height(250.dp).layout { measurable, constraints -> val pad = 16.dp.roundToPx(); val p = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + pad * 2)); layout(p.width, p.height) { p.place(-pad, 0) } }.background',
    content
)

# Also remove RoundedCornerShape(16.dp) from the gradient background to make it square
content = re.sub(
    r'brush = Brush\.verticalGradient\(\s*colors = listOf\(Color\(0xFF1E293B\), Color\(0xFF0F172A\)\)\s*\),\s*shape = RoundedCornerShape\(16\.dp\)',
    'brush = Brush.verticalGradient(colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)))',
    content
)

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)
