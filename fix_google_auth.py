import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# Import Google
import_str = "import io.github.jan.supabase.auth.providers.Google\nimport io.github.jan.supabase.handleDeeplinks"
content = content.replace("import io.github.jan.supabase.auth.providers.builtin.Email", 
                          "import io.github.jan.supabase.auth.providers.builtin.Email\n" + import_str)

# Add scheme to Auth init
auth_init_pattern = r'install\(Auth\) \{'
content = re.sub(auth_init_pattern, 'install(Auth) {\n            scheme = "shikkhaloy"\n            host = "login-callback"', content)

# Add handleDeeplinks to onCreate
oncreate_pattern = r'super\.onCreate\(savedInstanceState\)'
content = re.sub(oncreate_pattern, 'super.onCreate(savedInstanceState)\n        supabase.handleDeeplinks(intent)', content)

# Add Google Button to LoginScreen
btn_code = """
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = {
                coroutineScope.launch {
                    try {
                        supabase.auth.signInWith(Google)
                    } catch(e: Exception) {
                        Toast.makeText(context, "Google Sign-in error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Google দিয়ে সাইন-ইন করুন", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
"""

login_btn_pattern = r'if \(isLoginTab\) "লগইন করুন" else "অ্যাকাউন্ট তৈরি করুন",\n                    fontSize = 16\.sp,\n                    fontWeight = FontWeight\.Bold\n                \)\n            \}\n        \}'
content = re.sub(login_btn_pattern, r'if (isLoginTab) "লগইন করুন" else "অ্যাকাউন্ট তৈরি করুন",\n                    fontSize = 16.sp,\n                    fontWeight = FontWeight.Bold\n                )\n            }\n        }\n' + btn_code, content)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
print("Updated MainActivity.kt")
