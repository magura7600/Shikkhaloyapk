package com.example

import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class AppNotice(
    val id: Int? = null,
    val title: String,
    val content: String,
    val is_active: Boolean = true,
    val created_at: String? = null,
    val image_url: String? = null,
    val type: String = "general", // "general", "warning", "offer", "exam"
    val action_url: String? = null,
    val scheduled_time: String? = null, // Format: yyyy-MM-dd HH:mm
    val target_course_id: String? = null
)

object AppNoticeManager {

    /**
     * Checks Supabase for any active notices.
     * Returns the latest active [AppNotice] or null.
     */
    suspend fun getActiveNotice(): AppNotice? {
        return withContext(Dispatchers.IO) {
            try {
                val notices = supabase.from("app_notices").select {
                    filter {
                        eq("is_active", true)
                    }
                }.decodeList<AppNotice>()
                
                // Return the latest created active notice considering scheduled time
                val now = java.time.LocalDateTime.now()
                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                
                notices.filter { notice ->
                    if (notice.scheduled_time.isNullOrBlank()) {
                        true
                    } else {
                        try {
                            val schedTime = java.time.LocalDateTime.parse(notice.scheduled_time.trim(), formatter)
                            now.isAfter(schedTime) || now.isEqual(schedTime)
                        } catch (e: Exception) {
                            true // Fallback to displaying if formatting is unparseable
                        }
                    }
                }.maxByOrNull { it.id ?: 0 }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Publishes a new notice to Supabase app_notices table.
     */
    suspend fun publishNotice(notice: AppNotice): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // First deactivate other active notices so there's only one main active notice
                try {
                    supabase.from("app_notices").update(
                        {
                            set("is_active", false)
                        }
                    ) {
                        filter {
                            eq("is_active", true)
                        }
                    }
                } catch (e: Exception) {
                    // Ignore if no prior active notices
                }

                // Insert new notice
                supabase.from("app_notices").insert(notice)
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    /**
     * Deactivates all active notices.
     */
    suspend fun clearActiveNotice(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                supabase.from("app_notices").update(
                    {
                        set("is_active", false)
                    }
                ) {
                    filter {
                        eq("is_active", true)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}
