import re

with open("app/src/main/java/com/example/CourseDetailScreen.kt", "r") as f:
    content = f.read()

imports_to_add = """
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
"""

if "import androidx.compose.material.icons.filled.Schedule" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.Block", "import androidx.compose.material.icons.filled.Block\n" + imports_to_add)

with open("app/src/main/java/com/example/CourseDetailScreen.kt", "w") as f:
    f.write(content)
