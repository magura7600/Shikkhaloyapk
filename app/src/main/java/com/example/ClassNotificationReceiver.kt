package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ClassNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val subjectTitle = intent.getStringExtra("subjectTitle") ?: "অজানা বিষয়"
        val chapterTitle = intent.getStringExtra("chapterTitle") ?: "অজানা অধ্যায়"
        val classTitle = intent.getStringExtra("classTitle") ?: "অজানা ক্লাস"
        val mentorName = intent.getStringExtra("mentorName") ?: "অজানা শিক্ষক"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "class_alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "আসন্ন ক্লাস এলার্ট",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "লাইভ ক্লাস শুরু হওয়ার এলার্ট"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Open app when clicked
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("লাইভ ক্লাস শুরু হচ্ছে! 🚀")
            .setContentText("$subjectTitle • $chapterTitle")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "বিষয়: $subjectTitle\n" +
                "অধ্যায়: $chapterTitle\n" +
                "ক্লাস: $classTitle\n" +
                "শিক্ষক: $mentorName"
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
