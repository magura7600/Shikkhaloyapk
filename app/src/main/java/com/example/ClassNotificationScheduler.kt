package com.example

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import org.json.JSONObject

object ClassNotificationScheduler {

    private val client by lazy { OkHttpClient() }

    fun parseClassTime(dateStr: String, timeStr: String): Long {
        if (dateStr.isBlank()) return 0L
        try {
            val dateParts = dateStr.trim().split("/")
            if (dateParts.size != 3) return 0L
            val day = dateParts[0].toIntOrNull() ?: return 0L
            val month = (dateParts[1].toIntOrNull() ?: return 0L) - 1
            val year = dateParts[2].toIntOrNull() ?: return 0L

            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, day)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.YEAR, year)

            if (timeStr.isNotBlank()) {
                val timeLower = timeStr.trim().uppercase()
                val amPm = if (timeLower.contains("PM")) Calendar.PM else Calendar.AM
                val cleanTime = timeLower.replace("AM", "").replace("PM", "").trim()
                val timeParts = cleanTime.split(":")
                if (timeParts.size >= 2) {
                    val hour12 = timeParts[0].toIntOrNull() ?: 12
                    val minute = timeParts[1].toIntOrNull() ?: 0
                    cal.set(Calendar.HOUR, if (hour12 == 12) 0 else hour12)
                    cal.set(Calendar.AM_PM, amPm)
                    cal.set(Calendar.MINUTE, minute)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                } else {
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                }
            } else {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        } catch (e: Exception) {
            e.printStackTrace()
            return 0L
        }
    }

    fun scheduleLocalNotification(
        context: Context,
        classId: String,
        subjectTitle: String,
        chapterTitle: String,
        classTitle: String,
        mentorName: String,
        dateStr: String,
        timeStr: String
    ) {
        val triggerTime = parseClassTime(dateStr, timeStr)
        if (triggerTime <= System.currentTimeMillis()) {
            Log.d("Scheduler", "Time is in the past, skipping local schedule.")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ClassNotificationReceiver::class.java).apply {
            putExtra("subjectTitle", subjectTitle)
            putExtra("chapterTitle", chapterTitle)
            putExtra("classTitle", classTitle)
            putExtra("mentorName", mentorName)
        }

        // Use a unique requestCode per class using hash of classId
        val requestCode = classId.hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d("Scheduler", "Successfully scheduled local notification for class $classId at $triggerTime")
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    // Call OneSignal REST API to schedule a push notification to users enrolled in this course/quarter
    fun scheduleOneSignalPushNotification(
        context: Context,
        courseId: String,
        quarterId: String,
        subjectTitle: String,
        chapterTitle: String,
        classTitle: String,
        mentorName: String,
        dateStr: String,
        timeStr: String,
        restApiKey: String = "" // Optional if configured via UI/code
    ) {
        val finalApiKey = restApiKey.ifBlank {
            try {
                // Safely grab if defined in build configuration
                BuildConfig::class.java.getField("ONESIGNAL_REST_API_KEY").get(null) as? String ?: ""
            } catch (e: Throwable) {
                ""
            }
        }

        if (finalApiKey.isBlank() || finalApiKey == "null") {
            Log.d("Scheduler", "OneSignal REST API Key is missing. Skipping push scheduling.")
            return
        }

        val triggerTime = parseClassTime(dateStr, timeStr)
        if (triggerTime <= System.currentTimeMillis()) return

        val cal = Calendar.getInstance().apply { timeInMillis = triggerTime }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val second = cal.get(Calendar.SECOND)

        // Generate timezone offset string
        val tz = cal.timeZone
        val offset = tz.getOffset(triggerTime)
        val offsetHours = Math.abs(offset / 3600000)
        val offsetMinutes = Math.abs((offset / 60000) % 60)
        val sign = if (offset >= 0) "+" else "-"
        val offsetStr = String.format("GMT%s%02d%02d", sign, offsetHours, offsetMinutes)

        val sendAfterStr = String.format("%04d-%02d-%02d %02d:%02d:%02d %s", year, month, day, hour, minute, second, offsetStr)

        val mediaType = "application/json; charset=utf-8".toMediaType()

        val jsonBody = JSONObject().apply {
            put("app_id", "9b18010c-9761-4d89-abfc-ae8a437f4943")
            
            // Filter by course tag and optionally quarter tag so only users who bought this course/quarter get notified
            val filtersArray = org.json.JSONArray().apply {
                if (quarterId.isNotBlank()) {
                    // Filter: course_<courseId> = true OR quarter_<quarterId> = true
                    val fullCourseFilter = JSONObject().apply {
                        put("field", "tag")
                        put("key", "course_$courseId")
                        put("relation", "=")
                        put("value", "true")
                    }
                    val orOperator = JSONObject().apply {
                        put("operator", "OR")
                    }
                    val quarterFilter = JSONObject().apply {
                        put("field", "tag")
                        put("key", "quarter_$quarterId")
                        put("relation", "=")
                        put("value", "true")
                    }
                    put(fullCourseFilter)
                    put(orOperator)
                    put(quarterFilter)
                } else {
                    // Filter: course_<courseId> = true
                    val fullCourseFilter = JSONObject().apply {
                        put("field", "tag")
                        put("key", "course_$courseId")
                        put("relation", "=")
                        put("value", "true")
                    }
                    put(fullCourseFilter)
                }
            }
            put("filters", filtersArray)
            
            val headings = JSONObject().apply {
                put("en", "লাইভ ক্লাস শুরু হচ্ছে! 🚀")
            }
            val contents = JSONObject().apply {
                put("en", "বিষয়: $subjectTitle\nঅধ্যায়: $chapterTitle\nক্লাস: $classTitle\nশিক্ষক: $mentorName")
            }
            put("headings", headings)
            put("contents", contents)
            put("send_after", sendAfterStr)
        }

        val request = Request.Builder()
            .url("https://onesignal.com/api/v1/notifications")
            .post(jsonBody.toString().toRequestBody(mediaType))
            .addHeader("Authorization", "Basic $finalApiKey")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Scheduler", "OneSignal API call failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        Log.d("Scheduler", "Successfully scheduled OneSignal push for $sendAfterStr")
                    } else {
                        Log.e("Scheduler", "OneSignal API returned error: ${response.code} ${response.message}")
                    }
                }
            }
        })
    }
}
