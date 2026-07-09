import sys

def main():
    file_path = "app/src/main/java/com/example/PdfViewer.kt"
    with open(file_path, "r") as f:
        content = f.read()

    target = """    var error by remember { mutableStateOf(false) }"""

    replacement = """    var error by remember { mutableStateOf(false) }
    val context = LocalContext.current"""

    if target in content:
        content = content.replace(target, replacement)
        with open(file_path, "w") as f:
            f.write(content)
        print("Replaced pdf context successfully!")
    else:
        print("Target not found.")

if __name__ == "__main__":
    main()
