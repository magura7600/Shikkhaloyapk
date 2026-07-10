package com.example

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- LOGIN & SIGNUP SCREEN WITH MOBILE NUMBER VERIFICATION ---
@OptIn(ExperimentalMaterial3Api::class)
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
    var showVerificationSentDialog by remember { mutableStateOf(false) }

    fun handleAuthSuccess(email: String, uid: String) {
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
                    var user = supabase.auth.currentUserOrNull() ?: supabase.auth.currentSessionOrNull()?.user
                    var attempts = 0
                    while (user == null && attempts < 10) {
                        kotlinx.coroutines.delay(200)
                        user = supabase.auth.currentUserOrNull() ?: supabase.auth.currentSessionOrNull()?.user
                        attempts++
                    }
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
                    var user = supabase.auth.currentUserOrNull() ?: supabase.auth.currentSessionOrNull()?.user
                    var attempts = 0
                    while (user == null && attempts < 10) {
                        kotlinx.coroutines.delay(200)
                        user = supabase.auth.currentUserOrNull() ?: supabase.auth.currentSessionOrNull()?.user
                        attempts++
                    }
                    if (user != null) {
                        handleAuthSuccess(user.email ?: emailAddress.trim(), user.id)
                    } else {
                        showVerificationSentDialog = true
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
            painter = painterResource(id = R.drawable.custom_logo),
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

        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = {
                coroutineScope.launch {
                    try {
                        // Use Chrome Custom Tabs for a modern in-app Google Sign-In experience
                        val url = supabase.auth.getOAuthUrl(provider = Google, redirectUrl = "shikkhaloy://login-callback")
                        val builder = androidx.browser.customtabs.CustomTabsIntent.Builder()
                        val customTabsIntent = builder.build()
                        customTabsIntent.launchUrl(context, android.net.Uri.parse(url))
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

    }

    if (showVerificationSentDialog) {
        AlertDialog(
            onDismissRequest = { 
                showVerificationSentDialog = false
                isLoginTab = true 
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Email",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "ইমেইল ভেরিফিকেশন প্রয়োজন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1E293B)
                )
            },
            text = {
                Text(
                    text = "আপনার অ্যাকাউন্ট তৈরি সফল হয়েছে! আমরা '${emailAddress.trim()}' ইমেইলে একটি ভেরিফিকেশন লিঙ্ক পাঠিয়েছি। অনুগ্রহ করে আপনার ইনবক্স (বা স্প্যাম ফোল্ডার) চেক করুন এবং অ্যাকাউন্টটি সক্রিয় করতে লিঙ্কটিতে ক্লিক করুন। এরপর লগইন করুন।",
                    fontSize = 14.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showVerificationSentDialog = false
                        isLoginTab = true 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("ঠিক আছে", color = Color.White)
                }
            }
        )
    }
}

// --- ONBOARDING & PROFILE COMPLETION SCREEN ---
@Composable
fun OnboardingScreen(
    email: String,
    userId: String,
    onProfileComplete: (UserProfile) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedRole by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) } // "teacher" or "student"
    var isSavingProfile by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

    val defaultName = remember(email) {
        val user = try { supabase.auth.currentUserOrNull() } catch(e: Exception) { null }
        // Attempt to extract the Google Display Name or metadata name
        val metaName = user?.userMetadata?.get("full_name")?.toString()?.replace("\"", "")
            ?: user?.userMetadata?.get("name")?.toString()?.replace("\"", "")
        if (!metaName.isNullOrBlank() && metaName != "null") {
            metaName
        } else {
            val part = email.substringBefore("@")
            part.split(".", "_", "-").joinToString(" ") { segment ->
                segment.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Navigation/Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = "Verified",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "প্রোফাইল সম্পন্ন করুন",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1B4B)
                )
                Text(
                    text = "সংযুক্ত অ্যাকাউন্ট: $email",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Logout",
                    tint = Color.Red
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "আপনার ভূমিকা নির্বাচন করুন *",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color.DarkGray,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Text(
            text = "সতর্কতা: আপনার ভূমিকা (শিক্ষক/শিক্ষার্থী) একবার নির্বাচন করার পর আর পরিবর্তন করা যাবে না।",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Elegant Card Selection for Role
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Teacher Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedRole = "teacher" },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (selectedRole == "teacher") 2.dp else 1.dp,
                    color = if (selectedRole == "teacher") MaterialTheme.colorScheme.primary else Color.LightGray
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedRole == "teacher") MaterialTheme.colorScheme.primaryContainer else Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (selectedRole == "teacher") MaterialTheme.colorScheme.primary else Color(0xFFF3F4F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CoPresent,
                            contentDescription = "Teacher",
                            tint = if (selectedRole == "teacher") Color.White else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "শিক্ষক (Teacher)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (selectedRole == "teacher") MaterialTheme.colorScheme.primary else Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "ক্লাস তৈরি, কুইজ প্রদান",
                        fontSize = 8.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Student Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedRole = "student" },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (selectedRole == "student") 2.dp else 1.dp,
                    color = if (selectedRole == "student") MaterialTheme.colorScheme.secondary else Color.LightGray
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedRole == "student") MaterialTheme.colorScheme.secondaryContainer else Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (selectedRole == "student") MaterialTheme.colorScheme.secondary else Color(0xFFF3F4F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = "Student",
                            tint = if (selectedRole == "student") Color.White else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "শিক্ষার্থী (Student)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (selectedRole == "student") MaterialTheme.colorScheme.secondary else Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "ক্লাসে যোগদান ও পড়া",
                        fontSize = 8.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Complete Profile Button
        Button(
            onClick = {
                if (selectedRole == null) {
                    Toast.makeText(context, "অনুগ্রহ করে ভূমিকা নির্বাচন করুন!", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                coroutineScope.launch {
                    isSavingProfile = true
                    val generatedUid = "SL-" + (100000..999999).random()
                    val resolvedRole = selectedRole ?: "student"
                    val profile = UserProfile(
                        user_id = userId,
                        email = email,
                        role = resolvedRole,
                        full_name = defaultName,
                        institution = "",
                        contact = "",
                        uid_code = generatedUid,
                        handle = null // NULL by default, they don't have a channel setup yet
                    )

                    try {
                        withContext(Dispatchers.IO) {
                            // Try inserting into Supabase profiles table
                            supabase.from("profiles").insert(profile)
                        }
                        Toast.makeText(context, "প্রোফাইল সফলভাবে তৈরি হয়েছে! UID: $generatedUid", Toast.LENGTH_SHORT).show()
                        onProfileComplete(profile)
                    } catch (e: Exception) {
                        Toast.makeText(context, "ত্রুটি: ${e.message ?: "প্রোফাইল সংরক্ষণ ব্যর্থ হয়েছে।"}", Toast.LENGTH_LONG).show()
                    } finally {
                        isSavingProfile = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isSavingProfile,
            colors = ButtonDefaults.buttonColors(
                containerColor = when (selectedRole) {
                    "student" -> MaterialTheme.colorScheme.secondary
                    "admin" -> Color.Red
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        ) {
            if (isSavingProfile) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = "প্রোফাইল সম্পন্ন করুন",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
