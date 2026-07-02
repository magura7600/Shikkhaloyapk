import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Modify SettingsScreen signature
sig_regex = re.compile(r"(fun SettingsScreen\([\s\S]*?enrollments: List<Enrollment> = emptyList\(\))")
new_sig = r"\1,\n    onNavigateToMyEnrollments: () -> Unit = {},\n    onNavigateToEnrollmentRequests: () -> Unit = {}"
content = re.sub(sig_regex, new_sig, content)

# Remove showAdmissionInfoDialog code
content = re.sub(r"var showAdmissionInfoDialog by remember \{ mutableStateOf\(false\) \}", "", content)

# Remove the block if (showAdmissionInfoDialog) { ... }
dialog_regex = re.compile(r"if \(showAdmissionInfoDialog\) \{[\s\S]*?\}\n\n    if \(showAdminDashboardPanel\)")
content = re.sub(dialog_regex, "if (showAdminDashboardPanel)", content)

# Replace the onClick of Admission info item
item_regex = re.compile(r"SettingsItem\(icon = Icons\.Default\.School, title = \"Admission Information\", onClick = \{ showAdmissionInfoDialog = true \}\)")
new_item = """SettingsItem(icon = Icons.Default.School, title = "আমার ভর্তি তথ্য", onClick = onNavigateToMyEnrollments)
                if (isTeacher || isAdmin) {
                    SettingsItem(icon = Icons.Default.RequestPage, title = "কোর্স কেনা রিকোয়েস্ট", onClick = onNavigateToEnrollmentRequests)
                }"""
content = re.sub(item_regex, new_item, content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
