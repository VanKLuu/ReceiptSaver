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
import com.google.mlkit.vision.text.Text

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
                    // Proceed with camera operation
                } else {
                    // Permission denied
                    // Handle permission denied scenario
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

                val extractedText = concatenateTextBlocks(visionText)
                Log.d(TAG, "Extracted Text: $extractedText")

                // Extract relevant information from the text
                val (name, date, totalAmount) = extractInformationFromText(extractedText)
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
                val receipts = Receipts(
                    id = UUID.randomUUID(), // Generate a unique ID for the receipt
                    name = name ?: "",     // Set the store name
                    date = date ?: currentDate,     // Set the date
                    totalAmount = totalAmount ?: 0.0, // Set the total amount
                    image = imageByteArray,  // Set the photo data
                    thumbnail = thumbnailByteArray  // Set the thumbnail photo data
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
    private fun concatenateTextBlocks(text: Text): String {
        val stringBuilder = StringBuilder()

        // Iterate through each text block
        for (block in text.textBlocks) {
            // Append text from the current block to the StringBuilder
            stringBuilder.append(block.text)

            // Optionally, add a newline character between blocks
            stringBuilder.append("\n")
        }

        // Convert StringBuilder to a String and return
        return stringBuilder.toString()
    }

    // Function to extract relevant information from the extracted text
    private fun extractInformationFromText(extractedText: String): Triple<String?, String?, Double?> {
        val lines = extractedText.split("\n")
        var storeName: String? = null
        var date: String? = null
        var totalAmount: Double? = null

        for (line in lines) {
            val trimmedLine = line.trim()
            val extractedDate = extractDateFromLine(trimmedLine)
            when {
                extractedDate != null -> {
                    date = formatDate(extractedDate)
                }
                storeName == null -> storeName = trimmedLine
                isTotalAmount(trimmedLine) -> totalAmount = extractTotalAmount(trimmedLine)
            }
        }

        return Triple(storeName, date, totalAmount)
    }

    // Function to format the date as MM/DD/YYYY
    private fun formatDate(dateString: String): String {
        val inputFormat = SimpleDateFormat("M/d/yyyy", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        return outputFormat.format(date)
    }

    private fun extractDateFromLine(line: String): String? {
        val datePatterns = arrayOf(
            """^\d{1,2}/\d{1,2}/\d{4}\b""",    // Matches dates in MM/DD/YYYY format
            """^\d{1,2}/\d{1,2}/\d{2,4}\b"""   // Matches dates in M/D/YYYY or MM/DD/YYYY format with a two or four-digit year
        )

        // Iterate through each date pattern
        for (pattern in datePatterns) {
            val regex = pattern.toRegex()
            val matchResult = regex.find(line)
            if (matchResult != null) {
                // Return the matched date string
                return matchResult.value
            }
        }

        // No date found in the line
        return null
    }

    // Check if the text contains any of the terms "Total" or "Balance Due"
    private fun isTotalAmount(text: String): Boolean {
        return text.contains("Total", ignoreCase = true) ||
                text.contains("Balance Due", ignoreCase = true)
    }

    // Function to extract the total amount (balance due or total) from the text
    private fun extractTotalAmount(text: String): Double? {
        val keywords = listOf("Total", "Balance Due")

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