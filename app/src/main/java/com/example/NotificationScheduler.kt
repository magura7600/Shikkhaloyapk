package com.example

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object NotificationScheduler {
    fun scheduleClassNotifications(context: Context, course: CourseItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        
        // Ensure we can schedule exact alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                return // Missing permission
            }
        }
        
        val dateFormatter = DateTimeFormatter.ofPattern("d/M/yyyy")
        val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a") // Ex: "10:30 AM" or "10:30 PM"
        
        val now = LocalDateTime.now()
        
        course.subjects.flatMap { it.chapters }.flatMap { it.classes }.forEach { classItem ->
            try {
                if (classItem.date.isNotBlank() && classItem.time.isNotBlank()) {
                    val date = LocalDate.parse(classItem.date.trim(), dateFormatter)
                    
                    // Parse time. Some might use "hh:mm a", some might use "HH:mm". Let's handle generic format safely if possible.
                    // First try "hh:mm a", if fails, try "h:mm a"
                    var time: LocalTime? = null
                    try {
                        time = LocalTime.parse(classItem.time.trim(), timeFormatter)
                    } catch (e: Exception) {
                        try {
                            time = LocalTime.parse(classItem.time.trim(), DateTimeFormatter.ofPattern("h:mm a"))
                        } catch (e2: Exception) {
                            // Fallback
                            time = LocalTime.parse(classItem.time.trim(), DateTimeFormatter.ofPattern("HH:mm"))
                        }
                    }
                    
                    if (time != null) {
                        val classDateTime = LocalDateTime.of(date, time)
                        
                        // Schedule "Starting Soon" (30 mins before)
                        val soonDateTime = classDateTime.minusMinutes(30)
                        if (soonDateTime.isAfter(now)) {
                            scheduleAlarm(context, alarmManager, classItem, soonDateTime, "STARTING_SOON")
                        }
                        
                        // Schedule "Started"
                        if (classDateTime.isAfter(now)) {
                            scheduleAlarm(context, alarmManager, classItem, classDateTime, "STARTED")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace() // Ignore parsing errors
            }
        }
    }
    
    private fun scheduleAlarm(
        context: Context, 
        alarmManager: AlarmManager, 
        classItem: CourseClass, 
        dateTime: LocalDateTime, 
        type: String
    ) {
        val intent = Intent(context, ClassNotificationReceiver::class.java).apply {
            putExtra("CLASS_NAME", classItem.title)
            putExtra("CLASS_TIME", classItem.time)
            putExtra("TYPE", type)
        }
        
        // Generate a unique ID based on class ID and type
        val requestCode = (classItem.id.hashCode() * 31) + type.hashCode()
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val timeInMillis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
