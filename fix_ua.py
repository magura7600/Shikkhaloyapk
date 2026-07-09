import sys

def main():
    file_path = "app/src/main/java/com/example/OfflineDownloadManager.kt"
    with open(file_path, "r") as f:
        content = f.read()

    target = "val request = Request.Builder().url(sanitizedUrl).build()"
    replacement = 'val request = Request.Builder().url(sanitizedUrl).header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36").build()'

    if target in content:
        content = content.replace(target, replacement)
        with open(file_path, "w") as f:
            f.write(content)
        print("Replaced successfully!")
    else:
        print("Target not found.")

if __name__ == "__main__":
    main()
