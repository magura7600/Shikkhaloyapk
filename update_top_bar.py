import re

with open("app/src/main/java/com/example/CourseDetailScreen.kt", "r") as f:
    content = f.read()

# 1. Update Top Bar Padding and remove Share and Add Subject
pattern1 = re.compile(r"(\s*Row\(\n\s*modifier = Modifier\n\s*\.fillMaxWidth\(\)\n\s*\.background\(Color\(0xFFFBF8F1\)\)\n\s*\.padding\(horizontal = 16\.dp\)\n\s*)\.statusBarsPadding\(\)\.padding\(bottom = 4\.dp\),(\n\s*verticalAlignment = Alignment\.CenterVertically,\n\s*horizontalArrangement = Arrangement\.SpaceBetween\n\s*\} \{\n\s*// Back Button\n\s*Card\([\s\S]*?Icon\([\s\S]*?Modifier\.size\(20\.dp\)\n\s*\)\n\s*\}\n\s*\})[\s\S]*?(// Course / Subject Title[\s\S]*?TextOverflow\.Ellipsis\n\s*\))\n\s*Row\(\n\s*horizontalArrangement = Arrangement\.spacedBy\(8\.dp\),\n\s*verticalAlignment = Alignment\.CenterVertically\n\s*\) \{\n\s*// Share Button[\s\S]*?// Add Subject Button \(For Teachers\)[\s\S]*?\}\n\s*\}\n\s*\}\n\s*\}")

def replacement1(match):
    return f"{match.group(1)}.padding(top = 16.dp, bottom = 4.dp),{match.group(2)}\n                    {match.group(3)}\n                    // Right side empty placeholder if needed, or just let SpaceBetween push title to center\n                    Spacer(modifier = Modifier.size(42.dp))\n                }}"

new_content = re.sub(pattern1, replacement1, content)

# 2. Remove the Add Chapter Row
pattern2 = re.compile(r"(\s*\} else \{\n\s*val subject = currentSubject \?: selectedSubjectForView!!\n\s*Column\(modifier = Modifier\.fillMaxWidth\(\)\) \{)\n\s*Row\(\n\s*modifier = Modifier\.fillMaxWidth\(\),\n\s*horizontalArrangement = Arrangement\.SpaceBetween,\n\s*verticalAlignment = Alignment\.CenterVertically\n\s*\) \{\n\s*val isMainCourse = subject\.sourceCourseId == null \|\| subject\.sourceCourseId == course\.id\n\s*if \(isTeacher && isMainCourse\) \{\n\s*IconButton\(onClick = \{ subjectToAddChapterTo = subject \}\) \{\n\s*Icon\(Icons\.Default\.Add, contentDescription = \"Add Chapter\", tint = accentColor\)\n\s*\}\n\s*\}\n\s*\}\n\s*Spacer\(modifier = Modifier\.height\(12\.dp\)\)")

new_content = re.sub(pattern2, r"\1", new_content)

if new_content == content:
    print("No changes made. Check regex.")
else:
    with open("app/src/main/java/com/example/CourseDetailScreen.kt", "w") as f:
        f.write(new_content)
    print("Updated successfully.")

