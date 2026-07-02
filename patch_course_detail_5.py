
import re

file_path = "app/src/main/java/com/example/CourseDetailScreen.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Fix CourseDetailScreen closing braces
# Looking for the end of LazyColumn and Scaffold content
# LazyColumn starts around 344
# Scaffold content starts around 285
content = content.replace(
    'onPurchaseClick = onPurchaseClick\n                    )\n                }\n\n                    } // Close else for isUserBanned\n    }\n}\n\n    }\n}',
    'onPurchaseClick = onPurchaseClick\n                    )\n                }\n            } // Close else for isUserBanned\n        }\n    }\n}'
)

# 2. Fix Unresolved reference 'it' by providing explicit names
content = content.replace(
    'onSelectedSubjectChange = { selectedSubjectForView = it },',
    'onSelectedSubjectChange = { s -> selectedSubjectForView = s },'
)
content = content.replace(
    'onSelectedChapterChange = { selectedChapterForView = it },',
    'onSelectedChapterChange = { ch -> selectedChapterForView = ch },'
)
content = content.replace(
    'onSelectedClassChange = { selectedClassForView = it },',
    'onSelectedClassChange = { cl -> selectedClassForView = cl },'
)

# 3. Fix unresolved 'subject' in CourseContentSection by using a more unique name
# Line 974: val subject = currentSubject ?: selectedSubjectForView!!
content = content.replace(
    'val subject = currentSubject ?: selectedSubjectForView!!',
    'val activeSubject = currentSubject ?: selectedSubjectForView!!'
)

# Replace all usages of 'subject' within that block with 'activeSubject'
# We need to be careful not to replace 'subject' in other contexts.
# But since this is a patch, we can target specific lines if we know them.
# We'll use re.sub with a lookahead or just replace the common patterns.
content = content.replace('subject.chapters', 'activeSubject.chapters')
content = content.replace('subject.learningResources', 'activeSubject.learningResources')
content = content.replace('subject.copy', 'activeSubject.copy')
content = content.replace('if (it.id == subject.id)', 'if (it.id == activeSubject.id)')
content = content.replace('if (subj.id == subject.id)', 'if (subj.id == activeSubject.id)')
content = content.replace('subjectTitle = subject.title', 'subjectTitle = activeSubject.title')
content = content.replace('subject.title', 'activeSubject.title')
content = content.replace('Pair(subject, chapter)', 'Pair(activeSubject, chapter)')
content = content.replace('Triple(subject, chapter, clazz)', 'Triple(activeSubject, chapter, clazz)')

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
