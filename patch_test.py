import re

with open('app/src/test/java/com/example/BusinessLogicTest.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'val quarters = listOf(CourseQuarter("q1", "Q1", "200"), CourseQuarter("q2", "Q2", "300"))',
    'val quarters = listOf(CourseQuarter("q1", "200"), CourseQuarter("q2", "300"))'
)

with open('app/src/test/java/com/example/BusinessLogicTest.kt', 'w') as f:
    f.write(content)
