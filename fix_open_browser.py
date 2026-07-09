import sys

def main():
    file_path = "app/src/main/java/com/example/CourseDetailScreen.kt"
    with open(file_path, "r") as f:
        content = f.read()

    target = """                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(targetUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {"""

    replacement = """                            try {
                                val fixedUrl = if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) "https://$targetUrl" else targetUrl
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(fixedUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {"""

    if target in content:
        content = content.replace(target, replacement)
        with open(file_path, "w") as f:
            f.write(content)
        print("Replaced openBrowserIntent successfully!")
    else:
        print("Target not found.")

if __name__ == "__main__":
    main()
