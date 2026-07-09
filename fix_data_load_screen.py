import sys

def main():
    file_path = "app/src/main/java/com/example/MainActivity.kt"
    with open(file_path, "r") as f:
        content = f.read()

    target = """            // 5. Course Interactions & Mentors (ONLY needed for Course Detail screen)
            if (currentScreen == "courseDetail" && selectedCourse != null) {"""

    replacement = """            // 5. Course Interactions & Mentors (ONLY needed for Course Detail screen)
            if (currentScreen == "course_detail" && selectedCourse != null) {"""

    if target in content:
        content = content.replace(target, replacement)
        with open(file_path, "w") as f:
            f.write(content)
        print("Replaced currentScreen successfully!")
    else:
        print("Target not found.")

if __name__ == "__main__":
    main()
