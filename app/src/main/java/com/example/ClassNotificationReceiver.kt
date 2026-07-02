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
        L.init(context)
        val isBn = L.currentLanguage == "bn"

        val subjectTitle = intent.getStringExtra("subjectTitle") ?: (if (isBn) "অজানা বিষয়" else "Unknown Subject")
        val chapterTitle = intent.getStringExtra("chapterTitle") ?: (if (isBn) "অজানা অধ্যায়" else "Unknown Chapter")
        val classTitle = intent.getStringExtra("classTitle") ?: (if (isBn) "অজানা ক্লাস" else "Unknown Class")
        val mentorName = intent.getStringExtra("mentorName") ?: (if (isBn) "অজানা শিক্ষক" else "Unknown Teacher")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "class_alerts_v2"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                if (isBn) "আসন্ন ক্লাস এলার্ট" else "Upcoming Class Alert",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = if (isBn) "লাইভ ক্লাস শুরু হওয়ার এলার্ট" else "Alert for starting live class"
                enableVibration(true)
                val defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(defaultSoundUri, audioAttributes)
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
            .setContentTitle(if (isBn) "লাইভ ক্লাস শুরু হচ্ছে! 🚀" else "Live class is starting! 🚀")
            .setContentText("$subjectTitle • $chapterTitle")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                (if (isBn) "বিষয়: " else "Subject: ") + "$subjectTitle\n" +
                (if (isBn) "অধ্যায়: " else "Chapter: ") + "$chapterTitle\n" +
                (if (isBn) "ক্লাস: " else "Class: ") + "$classTitle\n" +
                (if (isBn) "শিক্ষক: " else "Teacher: ") + "$mentorName"
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
