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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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
    private lateinit var textRecognizer: TextRecognizer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize text recognizer and download the model if needed
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

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
                    // Perform OCR on the captured image
                    performOCR(imageBitmap)
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

                val extractedText = visionText.text
                Log.d(TAG, "Extracted Text: $extractedText")

                // Extract relevant information from the text
                val (name, date, totalAmount) = extractInformationFromText(extractedText)
                Log.d(TAG, "Store name: $name")
                Log.d(TAG, "date: $date")
                Log.d(TAG, "totalAmount: $totalAmount")

                // Convert Bitmap to byte array
                val imageByteArray = bitmapToByteArray(bitmap, 100)

                // Create a Receipts object with the extracted information
                val receipts = Receipts(
                    id = UUID.randomUUID(), // Generate a unique ID for the receipt
                    name = name ?: "",     // Set the store name
                    date = date ?: "",     // Set the date
                    totalAmount = totalAmount ?: 0.0, // Set the total amount
                    image = imageByteArray  // Set the photo data
                )

                // Save the receipt to the database
                saveReceipt(receipts)

                // Navigate to the dashboard screen
                navigateToDashboard()
            }
            .addOnFailureListener { e ->
                // Text recognition failed, handle the failure
                Log.e(TAG, "Text recognition failed: ${e.message}", e)
                handleTextRecognitionFailure()
            }
    }

    // Function to extract relevant information from the extracted text
    private fun extractInformationFromText(extractedText: String): Triple<String?, String?, Double?> {
        // Split the extracted text into lines
        val lines = extractedText.split("\n")

        // Initialize variables to store extracted information
        var storeName: String? = null
        var date: String? = null
        var totalAmount: Double? = null

        // Iterate through each line of the extracted text
        for (line in lines) {

            if (isDate(line)) {
                date = line
            }

            else if (storeName == null) {
                storeName = line
            }

            else if (isTotalAmount(line)) {
                totalAmount = extractTotalAmount(line)
            }
        }

        return Triple(storeName, date, totalAmount)
    }


    private fun isDate(text: String): Boolean {
        val datePatterns = arrayOf(
            """\b\d{1,2}/\d{1,2}/\d{4}\b""",  // Matches dates in MM/DD/YYYY format
            """\b\d{4}-\d{2}-\d{2}\b"""        // Matches dates in YYYY-MM-DD format
        )
        // Check if any of the date patterns match the text
        return datePatterns.any { pattern -> text.matches(pattern.toRegex()) }
    }

    // Check if the text contains any of the terms "Total", "Subtotal", or "Balance Due"
    private fun isTotalAmount(text: String): Boolean {
        return text.contains("Total", ignoreCase = true) ||
                text.contains("Subtotal", ignoreCase = true) ||
                text.contains("Balance Due", ignoreCase = true)
    }

    // Function to extract the total amount (balance due or total) from the text
    private fun extractTotalAmount(text: String): Double? {
        val keywords = listOf("Total", "Subtotal", "Balance Due")

        // Search for the keywords in the text
        val keywordIndex = keywords.indexOfFirst { text.contains(it, ignoreCase = true) }
        if (keywordIndex != -1) {
            // Find the position of the keyword
            val keyword = keywords[keywordIndex]
            val keywordIndex = text.indexOf(keyword, ignoreCase = true)

            // Extract the substring after the keyword
            val substring = text.substring(keywordIndex + keyword.length).trim()

            // Use regular expressions to extract the numerical value
            val amountRegex = """\d+(\.\d+)?""".toRegex()
            val matchResult = amountRegex.find(substring)
            if (matchResult != null) {
                // Extract the matched value and convert it to a Double
                val amountString = matchResult.value
                return amountString.toDoubleOrNull()
            }
        }

        return null // Return null if no amount is found
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