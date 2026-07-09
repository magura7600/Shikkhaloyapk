import re

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

# Remove the 'else' block for the share button
old_code = """                    }
                } else {
                    // This is live class play, show the Share on Facebook button!
                    Button(
                        onClick = {
                            try {
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, "আমাদের লাইভ ক্লাসে জয়েন করুন!\\nক্লাসের লিঙ্ক: ${clazz.liveLink}")
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "লাইভ ক্লাস লিংক শেয়ার করুন"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "শেয়ার করা যায়নি", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1877F2), // Facebook Blue
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("ফেসবুকে লাইভ ক্লাস লিংক শেয়ার করুন", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        } else if (clazz.liveLink.isNotBlank() || clazz.recordedLink.isBlank()) {"""

new_code = """                    }
                }
            }
        } else if (clazz.liveLink.isNotBlank() || clazz.recordedLink.isBlank()) {"""

content = content.replace(old_code, new_code)

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)
