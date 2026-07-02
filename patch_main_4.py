import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

item_regex = re.compile(r"(SettingItem\([\s\S]*?icon = Icons\.Outlined\.Info,\s*title = \"ভর্তির তথ্য\"\.t\(\),\s*subtitle = \"আপনার ভর্তি হওয়া কোর্সের বিবরণ দেখুন\"\.t\(\),\s*accentColor = accentColor,\s*onClick = \{ showAdmissionInfoDialog = true \}\n\s*\))")

new_item = r"""SettingItem(
                            icon = Icons.Outlined.Info,
                            title = "ভর্তির তথ্য".t(),
                            subtitle = "আপনার ভর্তি হওয়া কোর্সের বিবরণ দেখুন".t(),
                            accentColor = accentColor,
                            onClick = onNavigateToMyEnrollments
                        )
                        if (isTeacher || isAdmin) {
                            SettingItem(
                                icon = Icons.Outlined.Assignment,
                                title = "কোর্স কেনা রিকোয়েস্ট".t(),
                                subtitle = "শিক্ষার্থীদের রিকোয়েস্ট দেখুন ও এপ্রুভ করুন".t(),
                                accentColor = accentColor,
                                onClick = onNavigateToEnrollmentRequests
                            )
                        }"""

content = re.sub(item_regex, new_item, content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
