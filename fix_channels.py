import sys

def main():
    file_path = "app/src/main/java/com/example/MainActivity.kt"
    with open(file_path, "r") as f:
        content = f.read()

    target = """            // 4. Channels (ONLY needed for Explore tab)
            if (selectedTab == 3 && allChannels.isEmpty()) {"""
            
    replacement = """            // 4. Channels
            if (allChannels.isEmpty()) {"""

    if target in content:
        content = content.replace(target, replacement)
        with open(file_path, "w") as f:
            f.write(content)
        print("Replaced channels successfully!")
    else:
        print("Target not found for channels.")

if __name__ == "__main__":
    main()
