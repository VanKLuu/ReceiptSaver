package com.example.receiptsaver

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class Landing : AppCompatActivity() {
    // Duration of wait in milliseconds (e.g., 3000ms = 3s)
    private val splashDisplayLength: Long = 3000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        // New Handler to start the DashboardActivity and close the splash after some seconds.
        Handler(Looper.getMainLooper()).postDelayed({
            // Create an Intent that will start the DashboardActivity.
            val dashboardIntent = Intent(this, MainActivity::class.java)
            startActivity(dashboardIntent)
            finish() // Close the Landing
        }, splashDisplayLength)
    }
}