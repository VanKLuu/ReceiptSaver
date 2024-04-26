package com.example.receiptsaver
import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.receiptsaver.db.MyDatabaseRepository
import java.text.SimpleDateFormat
import android.provider.Settings
import java.util.*

private const val TAG = "DailyNotificationWorker"
class DailyNotificationWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    private lateinit var dbRepo: MyDatabaseRepository
   // private lateinit var sharedPreferences: SharedPreferences


    override fun doWork(): Result {
        dbRepo = MyDatabaseRepository.getInstance(applicationContext)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        // Fetch total amount for the current day from the database
        val totalAmount = fetchTotalAmountForToday()

        // Load the saved threshold amount from SharedPreferences
        val thresholdAmount = sharedPreferences.getString("thresholdAmount", "0.0")?.toDouble() ?: 0.0
        Log.d("thresholdAmount", "Daily threshold Amount: $thresholdAmount")
        // Exit if thresholdAmount is 0 or not set
        if (thresholdAmount <= 0.0 ) {
            return Result.success()
        }

        // Check if the total amount meets the notification criteria
        if (totalAmount >= thresholdAmount && isNotificationPolicyAccessGranted(applicationContext)) {
            sendNotification(totalAmount)
        }

        return Result.success()
    }

    private fun fetchTotalAmountForToday(): Double {
        // Get current date in the required format (MM/dd/yyyy)
        val currentDate = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date())
        val totalAmount =  dbRepo.fetchTotalAmountForToday(currentDate)
        Log.d("FetchTotalAmount", "Total amount for today: $totalAmount")
        return totalAmount
    }


    private fun sendNotification(totalAmount: Double) {

        createNotificationChannel()

        // Build the notification
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Daily Total Amount")
            .setContentText("Today's total amount: $totalAmount")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = NotificationManagerCompat.from(applicationContext)
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
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

        fun isNotificationPolicyAccessGranted(context: Context): Boolean {
            // Check if notification permissions are granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                val isEnabled = notificationManager.areNotificationsEnabled()

                if (!isEnabled) {
                    // Open the app notification settings if notifications are not enabled
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    context.startActivity(intent)

                    return false
                }
            } else {
                val areEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()

                if (!areEnabled) {
                    // Open the app notification settings if notifications are not enabled
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    context.startActivity(intent)

                    return false
                }
            }

            // Permissions are granted
            return true
        }
    }
    override fun onStopped() {
        super.onStopped()

    }
}