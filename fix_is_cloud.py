import sys
import re

def main():
    file_path = "app/src/main/java/com/example/CourseDetailScreen.kt"
    with open(file_path, "r") as f:
        content = f.read()

    # Find the pattern starting with val isCloudOrWebUrl = remember { up to the matching }
    # Since we know there are 2 occurrences and they end with the same structure, let's use regex
    
    # We will just replace all instances of:
    # { url: String ->
    # ...
    # }
    # inside remember with just { url: String -> false }
    
    pattern = r"val isCloudOrWebUrl = remember \{[^\}]+\{[^\}]+\}[^\}]+\}"
    replacement = "val isCloudOrWebUrl = remember { { url: String -> false } }"
    
    content = re.sub(pattern, replacement, content)

    with open(file_path, "w") as f:
        f.write(content)
    print("Fixed isCloudOrWebUrl with regex")

if __name__ == "__main__":
    main()
