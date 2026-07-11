import re

with open('app/src/main/java/com/example/DashboardScreen.kt', 'r') as f:
    content = f.read()

# Replace empty catch blocks with Log.e
content = re.sub(r'\} catch\s*\(\s*e:\s*Exception\s*\)\s*\{\s*\}', '} catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) }', content)
content = re.sub(r'catch\s*\(e:\s*Exception\)\s*\{\s*//\s*silent\s*\}', 'catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) }', content)
content = re.sub(r'catch\s*\(\s*e:\s*Exception\s*\)\s*\{\s*enrollments\s*\}', 'catch (e: Exception) { android.util.Log.e("SilentCatch", "Error enrollments", e); enrollments }', content)
content = re.sub(r'catch\s*\(\s*e:\s*Exception\s*\)\s*\{\s*enrollmentRequests\s*\}', 'catch (e: Exception) { android.util.Log.e("SilentCatch", "Error enrollmentsReq", e); enrollmentRequests }', content)
content = re.sub(r'catch\s*\(\s*e:\s*Exception\s*\)\s*\{\s*courses\s*\}', 'catch (e: Exception) { android.util.Log.e("SilentCatch", "Error courses", e); courses }', content)
content = re.sub(r'catch\s*\(\s*e:\s*Exception\s*\)\s*\{\s*courseInteractions\s*\}', 'catch (e: Exception) { android.util.Log.e("SilentCatch", "Error interactions", e); courseInteractions }', content)
content = re.sub(r'catch\s*\(\s*e:\s*Exception\s*\)\s*\{\s*mentors\s*\}', 'catch (e: Exception) { android.util.Log.e("SilentCatch", "Error mentors", e); mentors }', content)
content = re.sub(r'catch\s*\(\s*e:\s*Exception\s*\)\s*\{\s*e.printStackTrace\(\)\s*\}', 'catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) }', content)


with open('app/src/main/java/com/example/DashboardScreen.kt', 'w') as f:
    f.write(content)


def replace_in_file(filepath):
    with open(filepath, 'r') as f:
        c = f.read()
    c = re.sub(r'catch\s*\(\s*e:\s*Exception\s*\)\s*\{\s*e.printStackTrace\(\)\s*\}', 'catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) }', c)
    c = re.sub(r'\} catch\s*\(\s*e:\s*Exception\s*\)\s*\{\s*\}', '} catch (e: Exception) { android.util.Log.e("SilentCatch", "Error", e) }', c)
    
    with open(filepath, 'w') as f:
        f.write(c)

replace_in_file('app/src/main/java/com/example/AppUpdateUI.kt')
replace_in_file('app/src/main/java/com/example/AppNoticeScreen.kt')
replace_in_file('app/src/main/java/com/example/EnrollmentRequestsScreen.kt')
replace_in_file('app/src/main/java/com/example/VideoPlayer.kt')
replace_in_file('app/src/main/java/com/example/ClassDetailView.kt')
replace_in_file('app/src/main/java/com/example/CourseContentSection.kt')
replace_in_file('app/src/main/java/com/example/AdminDashboardScreen.kt')
replace_in_file('app/src/main/java/com/example/CourseOverview.kt')

