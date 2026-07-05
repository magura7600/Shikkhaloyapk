import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# Find the start of LoginScreen
start_idx = content.find('@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun LoginScreen(')
if start_idx == -1:
    print("Could not find start")
    exit(1)

# Find the end of LoginScreen
# I'll count braces
open_braces = 0
in_function = False
end_idx = -1

for i in range(start_idx, len(content)):
    if content[i] == '{':
        open_braces += 1
        in_function = True
    elif content[i] == '}':
        open_braces -= 1
        if in_function and open_braces == 0:
            end_idx = i + 1
            break

if end_idx == -1:
    print("Could not find end")
    exit(1)

new_login = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String, String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = remember { context.getSharedPreferences("shikkhaloy_prefs", Context.MODE_PRIVATE) }

    var isLoginTab by remember { mutableStateOf(true) }
    var emailAddress by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    fun handleAuthSuccess(email: String, uid: String) {
        if (!isLoginTab) {
            sharedPrefs.edit().putString("temp_register_password", password).apply()
        }
        
        sharedPrefs.edit()
            .putString("user_id", uid)
            .putString("email", email)
            .apply()
            
        onLoginSuccess(email, uid)
    }

    fun submitForm() {
        if (emailAddress.isBlank() || password.isBlank()) {
            Toast.makeText(context, "ইমেইল এবং পাসওয়ার্ড দিন!", Toast.LENGTH_SHORT).show()
            return
        }
        loading = true
        coroutineScope.launch {
            try {
                if (isLoginTab) {
                    supabase.auth.signInWith(Email) {
                        email = emailAddress.trim()
                        this.password = password
                    }
                    val user = supabase.auth.currentUserOrNull()
                    if (user != null) {
                        handleAuthSuccess(user.email ?: emailAddress.trim(), user.id)
                    } else {
                        Toast.makeText(context, "লগইন ব্যর্থ হয়েছে!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    supabase.auth.signUpWith(Email) {
                        email = emailAddress.trim()
                        this.password = password
                    }
                    val user = supabase.auth.currentUserOrNull()
                    if (user != null) {
                        handleAuthSuccess(user.email ?: emailAddress.trim(), user.id)
                    } else {
                        Toast.makeText(context, "সাইন আপ সফল হয়েছে!", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "App Logo",
            modifier = Modifier.size(100.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(
                onClick = { isLoginTab = true },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isLoginTab) MaterialTheme.colorScheme.primary else Color.Gray
                )
            ) {
                Text(
                    "লগইন",
                    fontSize = 18.sp,
                    fontWeight = if (isLoginTab) FontWeight.Bold else FontWeight.Normal,
                    textDecoration = if (isLoginTab) TextDecoration.Underline else TextDecoration.None
                )
            }
            Spacer(modifier = Modifier.width(32.dp))
            TextButton(
                onClick = { isLoginTab = false },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (!isLoginTab) MaterialTheme.colorScheme.primary else Color.Gray
                )
            ) {
                Text(
                    "সাইন-আপ",
                    fontSize = 18.sp,
                    fontWeight = if (!isLoginTab) FontWeight.Bold else FontWeight.Normal,
                    textDecoration = if (!isLoginTab) TextDecoration.Underline else TextDecoration.None
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = emailAddress,
            onValueChange = { emailAddress = it },
            label = { Text("ইমেইল অ্যাড্রেস") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("পাসওয়ার্ড") },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Password"
                    )
                }
            }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { submitForm() },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !loading
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = if (isLoginTab) "লগইন করুন" else "অ্যাকাউন্ট তৈরি করুন",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}"""

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content[:start_idx] + new_login + content[end_idx:])

print("LoginScreen replaced successfully!")
