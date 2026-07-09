import re
with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()
class_view_end = r"""                \}
            \}
        \}
    \} else if \(selectedChapterForView != null\) \{"""
matches = re.findall(class_view_end, content)
print(f"Matches for class_view_end: {len(matches)}")

class_detail_end = r"""                \}
            \}
            \} // Close the padded column
        \} else if \(clazz\.liveLink\.isNotBlank\(\) \|\| clazz\.recordedLink\.isBlank\(\)\) \{"""
matches = re.findall(class_detail_end, content)
print(f"Matches for class_detail_end: {len(matches)}")
