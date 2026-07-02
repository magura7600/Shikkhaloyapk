with open("app/src/main/java/com/example/CourseModels.kt", "r") as f:
    content = f.read()
content = content.replace("@Serializable\n@Serializable", "@Serializable")
with open("app/src/main/java/com/example/CourseModels.kt", "w") as f:
    f.write(content)
