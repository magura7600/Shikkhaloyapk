def check():
    with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
        lines = f.readlines()
    stack = []
    for i, line in enumerate(lines):
        for char in line:
            if char == '{':
                stack.append(i + 1)
            elif char == '}':
                if not stack:
                    print(f"Extra closing brace at line {i + 1}")
                    return
                stack.pop()
    if stack:
        print(f"Missing closing braces for lines: {stack}")
    else:
        print("Braces are balanced!")

check()
