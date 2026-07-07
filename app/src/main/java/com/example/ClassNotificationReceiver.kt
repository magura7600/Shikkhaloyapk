package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.UUID

class ClassNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val className = intent.getStringExtra("CLASS_NAME") ?: "একটি ক্লাস"
        val classTime = intent.getStringExtra("CLASS_TIME") ?: ""
        val type = intent.getStringExtra("TYPE") ?: "STARTING_SOON" // STARTING_SOON or STARTED
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "CLASS_REMINDER_CHANNEL"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Class Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming classes"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            UUID.randomUUID().hashCode(),
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val title: String
        val content: String
        
        if (type == "STARTING_SOON") {
            title = "ক্লাস শুরু হতে যাচ্ছে!"
            content = "আপনার '$className' ক্লাসটি $classTime এ শুরু হবে। প্রস্তুতি নিয়ে নিন।"
        } else {
            title = "ক্লাস শুরু হয়েছে!"
            content = "আপনার '$className' ক্লাসটি এখন শুরু হয়েছে। ক্লাসে যোগ দিন।"
        }
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback icon
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
            
        notificationManager.notify(UUID.randomUUID().hashCode(), notification)
    }
}
