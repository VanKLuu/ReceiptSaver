package com.example.receiptsaver

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.receiptsaver.db.MyDatabaseRepository
import com.example.receiptsaver.db.Receipts
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executors

private val TAG = "MainActivity"
private val REQUEST_IMAGE_CAPTURE = 1
private var currentPhotoPath: String? = null

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
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            Log.e(TAG, "Error creating photo file: ${ex.message}")
            null
        }
        photoFile?.also {
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                it
            )
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile(
            "JPEG_${System.currentTimeMillis()}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
        currentPhotoPath = imageFile.absolutePath // Save the file path
        return imageFile
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                // Load the full-size image from the file
                val imageFile = File(currentPhotoPath)
                val imageBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                if (imageBitmap != null) {
                    // Convert the Bitmap to a byte array
                    val imageByteArray = bitmapToByteArray(imageBitmap, quality = 100)

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
                } else {
                    // Handle the case when the image bitmap is null
                    Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // Handle the case when the user cancels the image capture process
                Toast.makeText(this, "Image capture canceled", Toast.LENGTH_SHORT).show()
            }
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