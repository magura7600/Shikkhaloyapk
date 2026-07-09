import sys

def main():
    file_path = "app/src/main/java/com/example/PdfViewer.kt"
    with open(file_path, "r") as f:
        content = f.read()

    target = """import androidx.compose.ui.Modifier"""

    replacement = """import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext"""

    if target in content:
        content = content.replace(target, replacement)
        with open(file_path, "w") as f:
            f.write(content)
        print("Replaced pdf import successfully!")
    else:
        print("Target not found.")

if __name__ == "__main__":
    main()
