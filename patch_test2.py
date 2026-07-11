import re

with open('app/src/test/java/com/example/ExampleRobolectricTest.kt', 'r') as f:
    content = f.read()

content = content.replace('"Shikkhaloy", appName', '"শিক্ষালয়", appName')

with open('app/src/test/java/com/example/ExampleRobolectricTest.kt', 'w') as f:
    f.write(content)
