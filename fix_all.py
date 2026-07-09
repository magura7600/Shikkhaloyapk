import re
with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

# 1. Add `}` before `} else {` to close the `Column(padding)` in the `else if (live)` block.
target_else = r"""                        \}
                    \}
                \}
            \}
        \} else \{
            // Placeholder when no video"""

replacement_else = """                        }
                    }
                }
            }
            } // Close the padded column of the Live block
        } else {
            // Placeholder when no video"""

content = re.sub(target_else, replacement_else, content)

# 2. Add `Column` right after the `else` block
target_spacer = r"""                    Text\(clazz\.title, color = Color\.White, fontSize = 16\.sp, fontWeight = FontWeight\.Bold\)
                \}
            \}
        \}
        
        Spacer\(modifier = Modifier\.height\(16\.dp\)\)
        
        // Class Details Card"""

replacement_spacer = """                    Text(clazz.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { // Open bottom padded column
        Spacer(modifier = Modifier.height(16.dp))
        
        // Class Details Card"""

content = re.sub(target_spacer, replacement_spacer, content)

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)
