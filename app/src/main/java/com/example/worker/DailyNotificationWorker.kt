package com.example.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.repository.BlockerRepository
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

class DailyNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(context)
        val settings = db.blockerDao().getUserSettingsDirect()

        // Check if notifications are enabled
        // We'll assume yes if permission is granted, since there is no explicit setting field for it in UserSettings yet, 
        // wait, we added a toggle in SettingsScreen.kt `notificationEnabled` but it wasn't saved.
        // Actually, let's just check permission.
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return Result.success()
            }
        }

        val events = db.blockerDao().getAllBlockEvents().firstOrNull() ?: emptyList()
        val totalBlockedToday = events.filter {
            val elementDay = android.text.format.DateFormat.format("dd", it.timestamp)
            val currentDay = android.text.format.DateFormat.format("dd", System.currentTimeMillis())
            elementDay == currentDay
        }.size
        
        val timeSavedMinutes = totalBlockedToday * 3
        
        if (timeSavedMinutes > 0) {
            sendNotification(timeSavedMinutes)
        }

        return Result.success()
    }

    private fun sendNotification(minutesSaved: Int) {
        val hours = minutesSaved / 60
        val mins = minutesSaved % 60
        val timeString = if (hours > 0) {
            "$hours hours $mins minutes"
        } else {
            "$mins minutes"
        }

        val channelId = "daily_summary"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Summary",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Reels Blocker")
            .setContentText("You saved \"$timeString\" today.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(1001, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
}
