package com.example.receiptsaver

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import com.example.receiptsaver.db.MyDatabaseRepository
import com.example.receiptsaver.db.Receipts
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.Executors

private val TAG = "MainActivity"
private val REQUEST_IMAGE_CAPTURE = 1

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var dbRepo: MyDatabaseRepository


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbRepo = MyDatabaseRepository.getInstance(applicationContext)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    navigateToDashboard()
                    true
                }
                R.id.navigation_scan-> {
                    navigateToScan()
                    true
                }
                R.id.navigation_expenses -> {
                    navigateToExpenses()
                    true
                }
                R.id.navigation_account -> {
                    navigateToAccount()
                    true
                }
                else -> false
            }
        }

        val activityFragment = this.supportFragmentManager.findFragmentById(R.id.fragment_container)
        if(activityFragment == null){
            val fragment = DashboardFragment.newInstance()
            this.supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit()

        }
    }

    private fun navigateToDashboard() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, DashboardFragment())
            .commit()
        bottomNavigationView.menu.findItem(R.id.navigation_home).isChecked = true
    }

    private fun navigateToScan() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            // Get the captured image as a Bitmap
            val imageBitmap = data?.extras?.get("data") as? Bitmap

            // Convert the Bitmap to a byte array
            val imageByteArray = bitmapToByteArray(imageBitmap!!, quality = 100)

            // Create a Receipts object with the photo data
            val receipts = Receipts(
                id = UUID.randomUUID(), // Generate a unique ID for the receipt
                name = "Costco",    // Provide the store name
                date = "2024-02-17",    // Provide the date
                totalAmount = 100.0,      // Provide the total amount
                image = imageByteArray  // Set the photo data
            )
            saveReceipt(receipts)
            navigateToDashboard()
        }
    }

    private fun saveReceipt(receipts: Receipts) {

        Log.d(TAG, "saveReceipt called")
        Executors.newSingleThreadExecutor().execute {
            // Insert the Receipts object into the Room database
            dbRepo.addReceipt(receipts)
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap, quality: Int): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }

    private fun navigateToExpenses() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ExpensesFragment())
            .commit()
    }

    private fun navigateToAccount() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, AccountFragment())
            .commit()
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart called")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop called")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause called")

    }

}