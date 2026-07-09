import re

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

# Remove the Aspect Ratio Toggle button
button_regex = r'// Aspect Ratio Toggle\s*Button\(\s*onClick = \{\s*interactionCount\+\+\s*scale = 1f\s*offset = androidx\.compose\.ui\.geometry\.Offset\.Zero\s*resizeMode = when \(resizeMode\) \{[\s\S]*?Text\(modeText, color = Color\.White, fontSize = 12\.sp, fontWeight = FontWeight\.Bold\)\s*\}'

content = re.sub(button_regex, '', content)

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)
