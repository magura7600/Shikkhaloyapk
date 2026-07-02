import re

with open("app/src/main/java/com/example/CourseDetailScreen.kt", "r") as f:
    content = f.read()

sig_regex = re.compile(r"(fun CourseDetailScreen\([\s\S]*?isLiked: Boolean,)")
new_sig = r"\1\n    pendingRequest: EnrollmentRequest? = null,\n    onPurchaseClick: () -> Unit = {},"

content = re.sub(sig_regex, new_sig, content)

with open("app/src/main/java/com/example/CourseDetailScreen.kt", "w") as f:
    f.write(content)
