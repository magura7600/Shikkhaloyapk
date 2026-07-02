
def check_braces(filename):
    with open(filename, 'r') as f:
        lines = f.readlines()
    
    stack = []
    for i, line in enumerate(lines):
        line_num = i + 1
        for j, char in enumerate(line):
            if char == '{':
                stack.append((line_num, j + 1))
            elif char == '}':
                if not stack:
                    print(f"Extra closing brace at line {line_num}, col {j+1}")
                else:
                    stack.pop()
    
    if stack:
        print(f"Unclosed braces:")
        for line_num, col in stack:
            print(f"  Line {line_num}, col {col}")

check_braces('app/src/main/java/com/example/CourseDetailScreen.kt')
