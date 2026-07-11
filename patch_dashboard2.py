import re

with open('app/src/main/java/com/example/DashboardScreen.kt', 'r') as f:
    content = f.read()

target = """                    onLikeToggle = {
                        coroutineScope.launch {
                            try {
                                val currentCourse = selectedCourse ?: return@launch
                                val existing = courseInteractions.find { it.course_id == currentCourse.id && it.user_id == profile.user_id && it.is_like }
                                if (existing != null) {
                                    withContext(Dispatchers.IO) {
                                        supabase.from("course_interactions").delete {
                                            filter { eq("id", existing.id) }
                                        }
                                    }
                                    courseInteractions = courseInteractions.filter { it.id != existing.id }
                                } else {
                                    val newInteraction = CourseInteraction(user_id = profile.user_id, course_id = currentCourse.id, is_like = true)
                                    withContext(Dispatchers.IO) {
                                        supabase.from("course_interactions").insert(newInteraction)
                                    }
                                    courseInteractions = courseInteractions + newInteraction
                                }
                            } catch(e: Exception) {
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onCourseUpdate = { updatedCourse ->
                        coroutineScope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    supabase.from("courses").update(updatedCourse) {
                                        filter { eq("id", updatedCourse.id) }
                                    }
                                }
                                courses = courses.map { if (it.id == updatedCourse.id) updatedCourse else it }
                                selectedCourse = updatedCourse
                                Toast.makeText(context, "Course updated!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                val msg = e.message ?: ""
                                if (msg.contains("subjects") || msg.contains("quarters") || msg.contains("isQuarterOn")) {
                                    Toast.makeText(context, "Error: Supabase-এ কলাম অনুপস্থিত বা ত্রুটি! (${e.message})", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },"""

content = content.replace(target, "")

with open('app/src/main/java/com/example/DashboardScreen.kt', 'w') as f:
    f.write(content)
