with open('.github/workflows/build-apk.yml', 'r') as f:
    content = f.read()

target = """    - name: Build Release APK"""

replacement = """    - name: Run Unit Tests
      run: ./gradlew test

    - name: Build Release APK"""

content = content.replace(target, replacement)

with open('.github/workflows/build-apk.yml', 'w') as f:
    f.write(content)
