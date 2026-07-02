import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Add enrollmentRequests state
state_code = """
    var enrollmentRequests by remember { mutableStateOf(listOf<EnrollmentRequest>()) }
"""
content = re.sub(r"(var enrollments by remember \{ mutableStateOf\(listOf<Enrollment>\(\)\) \})", r"\1\n" + state_code, content)

# Fetch enrollmentRequests
fetch_code = """
                    try {
                        enrollmentRequests = supabase.from("enrollment_requests").select().decodeList<EnrollmentRequest>()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
"""
content = re.sub(r"(supabase\.from\(\"enrollments\"\)\.select\(\)\.decodeList<Enrollment>\(\))", r"\1\n" + fetch_code, content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
