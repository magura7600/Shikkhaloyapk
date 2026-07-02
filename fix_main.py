import re
with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

bad_block = """            val fetchedEnrollments = withContext(Dispatchers.IO) {
                try {
                    supabase.from("enrollments").select().decodeList<Enrollment>()

                    try {
                        enrollmentRequests = supabase.from("enrollment_requests").select().decodeList<EnrollmentRequest>()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } catch(e:Exception) { emptyList() }
            }"""

# Since regex is safer with varied whitespace
pattern = re.compile(r"val fetchedEnrollments = withContext\(Dispatchers\.IO\) \{[\s\S]*?catch\(e:Exception\) \{ emptyList\(\) \}\n\s*\}")

new_block = """val fetchedEnrollments = withContext(Dispatchers.IO) {
                try {
                    val enrolls = supabase.from("enrollments").select().decodeList<Enrollment>()
                    try {
                        enrollmentRequests = supabase.from("enrollment_requests").select().decodeList<EnrollmentRequest>()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    enrolls
                } catch(e:Exception) { emptyList<Enrollment>() }
            }"""

content = re.sub(pattern, new_block, content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
