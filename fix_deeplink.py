with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace("import io.github.jan.supabase.handleDeeplinks\n", "")
content = content.replace("supabase.handleDeeplinks(intent)", "supabase.auth.handleDeeplinks(intent)")

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
print("Updated deeplinks")
