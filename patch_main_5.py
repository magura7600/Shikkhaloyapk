import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

delete_regex = re.compile(r"// Delete the course itself\n\s*supabase\.from\(\"courses\"\)\.delete \{")

new_delete = """// Delete related enrollment_requests
                                            try {
                                                supabase.from("enrollment_requests").delete {
                                                    filter { eq("course_id", course.id) }
                                                }
                                            } catch (e: Exception) { e.printStackTrace() }

                                            // Delete the course itself
                                            supabase.from("courses").delete {"""

content = re.sub(delete_regex, new_delete, content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
