import re

# app/src/main/java/com/example/StudentDashboard.kt
with open('app/src/main/java/com/example/StudentDashboard.kt', 'r') as f:
    content = f.read()

content = content.replace("onClick = { onClassClick(courseClass, chapter, subject, focusCourse!!) }", 
                          "onClick = { focusCourse?.let { onClassClick(courseClass, chapter, subject, it) } }")
with open('app/src/main/java/com/example/StudentDashboard.kt', 'w') as f:
    f.write(content)

# app/src/main/java/com/example/DashboardScreen.kt
with open('app/src/main/java/com/example/DashboardScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('try { supabase.from("course_interactions").select { filter { eq("course_id", selectedCourse!!.id) } }.decodeList<CourseInteraction>() } catch(e: Exception) { courseInteractions }',
                          'try { selectedCourse?.id?.let { cid -> supabase.from("course_interactions").select { filter { eq("course_id", cid) } }.decodeList<CourseInteraction>() } ?: courseInteractions } catch(e: Exception) { courseInteractions }')

with open('app/src/main/java/com/example/DashboardScreen.kt', 'w') as f:
    f.write(content)

# app/src/main/java/com/example/ClassDetailView.kt
with open('app/src/main/java/com/example/ClassDetailView.kt', 'r') as f:
    content = f.read()

content = content.replace("text = { Text(\"আপনি কি নিশ্চিতভাবে এই ফাইলটি ('${recordToDelete!!.title}') ডিলিট করতে চান?\") },",
                          "text = { Text(\"আপনি কি নিশ্চিতভাবে এই ফাইলটি ('${recordToDelete?.title}') ডিলিট করতে চান?\") },")
content = content.replace("OfflineDownloadManager.deleteDownload(context, recordToDelete!!)",
                          "recordToDelete?.let { OfflineDownloadManager.deleteDownload(context, it) }")
content = content.replace("videoOptions = videoOptions!!,",
                          "videoOptions = videoOptions ?: emptyList(),")
content = content.replace("file = activePdfToView!!,",
                          "file = activePdfToView ?: File(\"\"),")
content = content.replace("activePdfToView = File(downloadedRecord!!.localPath)",
                          "downloadedRecord?.localPath?.let { activePdfToView = File(it) }")

with open('app/src/main/java/com/example/ClassDetailView.kt', 'w') as f:
    f.write(content)

# app/src/main/java/com/example/CourseOverview.kt
with open('app/src/main/java/com/example/CourseOverview.kt', 'r') as f:
    content = f.read()

content = content.replace("OfflineDownloadManager.deleteDownload(context, recordToDelete!!)",
                          "recordToDelete?.let { OfflineDownloadManager.deleteDownload(context, it) }")
content = content.replace("file = activePdfToView!!,",
                          "file = activePdfToView ?: File(\"\"),")
content = content.replace("activePdfToView = File(downloadedRecord!!.localPath)",
                          "downloadedRecord?.localPath?.let { activePdfToView = File(it) }")

with open('app/src/main/java/com/example/CourseOverview.kt', 'w') as f:
    f.write(content)

