import sys

def main():
    file_path = "app/src/main/java/com/example/CourseDetailScreen.kt"
    with open(file_path, "r") as f:
        content_lines = f.readlines()
    
    with open("patch_course_detail.txt", "r") as f:
        replacement = f.read().rstrip()
    
    start_idx = -1
    end_idx = -1
    
    for i, line in enumerate(content_lines):
        if "if (isClassUpcoming(clazz)) {" in line:
            start_idx = i
            break
            
    if start_idx != -1:
        for i in range(start_idx, len(content_lines)):
            if "} else if (clazz.recordedLink.isNotBlank()) {" in content_lines[i]:
                end_idx = i
                break
                
    if start_idx != -1 and end_idx != -1:
        new_lines = content_lines[:start_idx] + [replacement + "\n"] + content_lines[end_idx+1:]
        with open(file_path, "w") as f:
            f.writelines(new_lines)
        print("Replaced successfully!")
    else:
        print("Failed to find start or end index.")

if __name__ == "__main__":
    main()
