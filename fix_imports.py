with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

imports_to_add = """
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
"""
# find the last import and insert
import_idx = content.rfind("import ")
end_of_line = content.find("\n", import_idx)

content = content[:end_of_line] + imports_to_add + content[end_of_line:]

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)

print("Imports fixed!")
