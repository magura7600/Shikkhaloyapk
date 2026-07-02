import os

with open('/app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    lines = f.readlines()

if '2035' in str(len(lines)): # Just a check
    pass

# We want to remove line 2035 (1-indexed, so index 2034)
# if it is just a "}" and line 2037 is "@Composable"
if lines[2034].strip() == "}" and "@Composable" in lines[2036]:
    del lines[2034]
    with open('/app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
        f.writelines(lines)
    print("Removed line 2035")
else:
    print(f"Line 2034 is: '{lines[2034].strip()}'")
    print(f"Line 2036 is: '{lines[2036].strip()}'")
