import re

with open('app/src/main/java/com/example/ExploreFeedScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'val filteredCourses = if (searchQuery.isBlank()) {',
    'val filteredCourses = remember(courses, searchQuery) { if (searchQuery.isBlank()) {'
)
content = content.replace(
    'courses.filter { it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }\n    }',
    'courses.filter { it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }\n    } }'
)

with open('app/src/main/java/com/example/ExploreFeedScreen.kt', 'w') as f:
    f.write(content)


with open('app/src/main/java/com/example/CourseListScreen.kt', 'r') as f:
    content = f.read()

if 'val filteredCourses = if' in content:
    content = content.replace(
        'val filteredCourses = if (searchQuery.isBlank()) {',
        'val filteredCourses = remember(courses, searchQuery) { if (searchQuery.isBlank()) {'
    )
    content = content.replace(
        'courses.filter { it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }\n    }',
        'courses.filter { it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }\n    } }'
    )
    with open('app/src/main/java/com/example/CourseListScreen.kt', 'w') as f:
        f.write(content)

