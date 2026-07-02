import re

with open("app/src/main/java/com/example/CourseDetailScreen.kt", "r") as f:
    content = f.read()

# We want to remove the 'Pricing & Quarters Selection' block and 'Payment Instructions' block from CourseDetailScreen.
# They are under `item { CourseContentSection(...) }`

pattern = r"// Pricing & Quarters Selection.*?// Close else for isUserBanned"

# Let's replace everything from `// Pricing & Quarters Selection` up to the closing brace before `} // Close else for isUserBanned`
new_content = re.sub(pattern, "        } // Close else for isUserBanned", content, flags=re.DOTALL)

with open("app/src/main/java/com/example/CourseDetailScreen.kt", "w") as f:
    f.write(new_content)

print(new_content != content)
