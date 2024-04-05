package com.example.receiptsaver
import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.receiptsaver.db.MyDatabaseRepository
import java.text.SimpleDateFormat
import android.provider.Settings
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.startActivity
import java.util.*

class DailyNotificationWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    private lateinit var dbRepo: MyDatabaseRepository


    override fun doWork(): Result {
        dbRepo = MyDatabaseRepository.getInstance(applicationContext)
        // Fetch total amount for the current day from the database
        val totalAmount = fetchTotalAmountForToday()

        // Check if the total amount meets the notification criteria
        if (totalAmount >= THRESHOLD_AMOUNT) {
            // Create and send notification
            sendNotification(totalAmount)
        } else {
            // Open notification settings if conditions are not met
            openNotificationSettings(applicationContext)
        }

        return Result.success()
    }

    private fun openNotificationSettings(context: Context) {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    private fun fetchTotalAmountForToday(): Double {
        // Get current date in the required format (MM/dd/yyyy)
        val currentDate = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date())
        val totalAmount =  dbRepo.fetchTotalAmountForToday(currentDate)
        Log.d("FetchTotalAmount", "Total amount for today: $totalAmount")
        return totalAmount
    }

    private fun sendNotification(totalAmount: Double) {
        // Create a notification channel
        createNotificationChannel()

        // Build the notification
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Daily Total Amount")
            .setContentText("Today's total amount: $totalAmount")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Show the notification
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Handle the SecurityException here
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        // Create a notification channel (for Android Oreo and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Total Amount"
            val descriptionText = "Shows daily total amount notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            // Register the notification channel with the system
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "daily_notification_channel"
        private const val NOTIFICATION_ID = 123
        private const val THRESHOLD_AMOUNT = 100.0 // Example threshold amount

        fun isNotificationPolicyAccessGranted(context: Context): Boolean {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.isNotificationPolicyAccessGranted
            } else {
                true
            }
        }
    }
}