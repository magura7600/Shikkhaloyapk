import re

with open('app/src/main/java/com/example/ManageStudentsScreen.kt', 'r') as f:
    content = f.read()

old_block = """        if (showBanDialog && selectedEnrollment != null) {
            BanStudentDialog(
                enrollment = selectedEnrollment!!,
                accentColor = accentColor,
                onDismiss = { showBanDialog = false },
                onBanConfirm = { durationMillis, reason ->
                    val bannedUntil = if (durationMillis == -1L) -1L else System.currentTimeMillis() + durationMillis
                    coroutineScope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                supabase.from("enrollments").update(
                                    {
                                        set("banned_until", bannedUntil)
                                        set("ban_reason", reason)
                                    }
                                ) {
                                    filter { eq("id", selectedEnrollment!!.id) }
                                }
                            }
                            // Update local state
                            enrollments = enrollments.map {
                                if (it.id == selectedEnrollment!!.id) it.copy(banned_until = bannedUntil, ban_reason = reason) else it
                            }
                            showBanDialog = false
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            )
        }"""

new_block = """        if (showBanDialog && selectedEnrollment != null) {
            val currentEnrollment = selectedEnrollment
            BanStudentDialog(
                enrollment = currentEnrollment,
                accentColor = accentColor,
                onDismiss = { showBanDialog = false },
                onBanConfirm = { durationMillis, reason ->
                    val bannedUntil = if (durationMillis == -1L) -1L else System.currentTimeMillis() + durationMillis
                    coroutineScope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                supabase.from("enrollments").update(
                                    {
                                        set("banned_until", bannedUntil)
                                        set("ban_reason", reason)
                                    }
                                ) {
                                    filter { eq("id", currentEnrollment.id) }
                                }
                            }
                            // Update local state
                            enrollments = enrollments.map {
                                if (it.id == currentEnrollment.id) it.copy(banned_until = bannedUntil, ban_reason = reason) else it
                            }
                            showBanDialog = false
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            )
        }"""

if old_block in content:
    content = content.replace(old_block, new_block)
    with open('app/src/main/java/com/example/ManageStudentsScreen.kt', 'w') as f:
        f.write(content)
    print("Fixed ManageStudentsScreen")
else:
    print("Block not found in ManageStudentsScreen")
