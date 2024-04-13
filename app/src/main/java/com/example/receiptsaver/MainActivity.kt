package com.example.receiptsaver

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.receiptsaver.db.MyDatabaseRepository
import com.example.receiptsaver.db.Receipts
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import android.Manifest
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager


private const val REQUEST_IMAGE_CAPTURE = 1
private var currentPhotoPath: String? = null

class MainActivity : AppCompatActivity() {

    private lateinit var dbRepo: MyDatabaseRepository
    private lateinit var textRecognizer: TextRecognizer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize text recognizer and download the model if needed
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        dbRepo = MyDatabaseRepository.getInstance(applicationContext)
        setupBottomNavigationView()
        if (savedInstanceState == null) {
            navigateToDashboard()
        }
        // Schedule daily work
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val dailyWorkRequest =
            OneTimeWorkRequestBuilder<DailyNotificationWorker>()
                .setConstraints(constraints)
                .addTag(DailyNotificationWorker::class.java.name)
                .build()

        WorkManager.getInstance(applicationContext).enqueue(dailyWorkRequest)

    }
    private fun setupBottomNavigationView() {
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    navigateToDashboard()
                    true
                }
                R.id.navigation_scan -> {
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
    }

    private fun navigateToDashboard() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, DashboardFragment())
            .commit()
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).menu.findItem(R.id.navigation_home)
            .isChecked = true
    }

    private fun navigateToScan() {
        checkCameraPermission()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted
                    openCamera()
                } else {
                    // Permission denied
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION)
        } else {
            openCamera()
        }
    }


    private fun openCamera() {
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
        currentPhotoPath = imageFile.absolutePath // Set the value of currentPhotoPath
        return imageFile
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                // Load the full-size image from the file
                if (currentPhotoPath != null) { // Ensure currentPhotoPath is not null
                    val imageFile = File(currentPhotoPath!!)
                    val imageBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                    if (imageBitmap != null) {
                        // Perform OCR on the captured image
                        performOCR(imageBitmap)
                    } else {
                        // Handle the case when the image bitmap is null
                        Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // Handle the case when the user cancels the image capture process
                Toast.makeText(this, "Image capture canceled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performOCR(bitmap: Bitmap) {
        // Check if the text recognizer is initialized
        if (!::textRecognizer.isInitialized) {
            Log.e(TAG, "Text recognizer is not initialized")
            return
        }

        // Convert Bitmap to InputImage
        val image = InputImage.fromBitmap(bitmap, 0)


        // Process the image for text recognition
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->

                val extractedText = TextRecognitionHandler.getWellFormattedText(visionText.textBlocks)
                Log.d(TAG, "Extracted Text: $extractedText")

                // Extract relevant information from the text
                val (name, date, totalAmount) = TextRecognitionHandler.extractInformationFromText(extractedText)
                Log.d(TAG, "Store name: $name")
                Log.d(TAG, "date: $date")
                Log.d(TAG, "totalAmount: $totalAmount")

                // Convert Bitmap to byte array
                val imageByteArray = bitmapToByteArray(bitmap, 100)
                // Generate thumbnail from the bitmap
                val thumbnailBitmap = Bitmap.createScaledBitmap(bitmap, 120, 160, false)
                // Convert thumbnail Bitmap to byte array
                val thumbnailByteArray = bitmapToByteArray(thumbnailBitmap, 100)
                val currentDate = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date())

                // Create a Receipts object with the extracted information
                // Generate a unique ID for the receipt
                val receiptId = UUID.randomUUID()
                val receipts = Receipts(
                    id = receiptId,
                    name = name ?: "",     // Set the store name
                    date = date ?: currentDate,     // Set the date
                    totalAmount = totalAmount ?: 0.0, // Set the total amount
                    image = imageByteArray,  // Set the photo data
                    thumbnail = thumbnailByteArray  // Set the thumbnail photo data
                )

                // Save the receipt to the database
                saveReceipt(receipts)

                // Navigate to the dashboard screen
                navigateToDetail(receiptId.toString())
            }
            .addOnFailureListener { e ->
                // Text recognition failed, handle the failure
                Log.e(TAG, "Text recognition failed: ${e.message}", e)
                handleTextRecognitionFailure()
            }
    }
    private fun handleTextRecognitionFailure() {
        // Log an error message
        Log.e(TAG, "Text recognition failed")

        // Display a toast message to the user
        Toast.makeText(this, "Text recognition failed. Please try again later.", Toast.LENGTH_SHORT).show()
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

    private fun navigateToDetail(id: String) {
        val fragment = ReceiptDetailFragment.newInstance(id)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
        textRecognizer.close()
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

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CAMERA_PERMISSION = 101
    }

}