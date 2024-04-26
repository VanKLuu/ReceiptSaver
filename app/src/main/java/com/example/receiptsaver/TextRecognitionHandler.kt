package com.example.receiptsaver

import android.util.Log
import com.google.mlkit.vision.text.Text
import java.text.SimpleDateFormat
import java.util.Locale
private const val TAG = "TextRecognitionHandler"
object TextRecognitionHandler {
    fun getWellFormattedText(blocks: List<Text.TextBlock>): String {
        val textElements = mutableListOf<Text.Element>()

        // Breaking boxes

        for (block in blocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    textElements.add(element)
                }
            }
        }

        // Sorting boxes
        val sortedElements = textElements.sortedWith(Comparator { t1, t2 ->
            val diffOfTops = (t1.boundingBox?.top ?: 0) - (t2.boundingBox?.top ?: 0)
            val diffOfLefts = (t1.boundingBox?.left ?: 0) - (t2.boundingBox?.left ?: 0)

            val height = ((t1.boundingBox?.height() ?: 0) + (t2.boundingBox?.height() ?: 0)) / 2
            val verticalDiff = (height * 0.35).toInt()

            var result = diffOfLefts
            if (Math.abs(diffOfTops) > verticalDiff) {
                result = diffOfTops
            }
            result
        })

        val formattedText = StringBuilder()

        // Organize sorted text elements into lines
        val lines = mutableListOf<MutableList<Text.Element>>()
        var currentLine = mutableListOf<Text.Element>()

        for (element in sortedElements) {
            if (currentLine.isNotEmpty() && !isSameLine(currentLine.last(), element)) {
                lines.add(currentLine)
                currentLine = mutableListOf()
            }
            currentLine.add(element)
        }
        lines.add(currentLine)

        // Concatenate text elements within each line and add newline between lines
        for (line in lines) {
            for (element in line) {
                formattedText.append(element.text).append(" ")
            }
            formattedText.append("\n")
        }

        return formattedText.toString()
    }

    // Function to check if two elements are on the same line
    private fun isSameLine(t1: Text.Element, t2: Text.Element): Boolean {
        val diffOfTops = (t1.boundingBox!!.top ?: 0) - (t2.boundingBox!!.top ?: 0)
        val height = ((t1.boundingBox!!.height() ?: 0) + (t2.boundingBox!!.height() ?: 0)) * 35 / 100

        return Math.abs(diffOfTops) <= height
    }


    // Function to extract relevant information from the extracted text
    fun extractInformationFromText(extractedText: String): Triple<String?, String?, Double?> {
        val lines = extractedText.split("\n")
        var storeName: String? = null
        var date: String? = null
        var totalAmount: Double? = null

        for (line in lines) {
            val trimmedLine = line.trim()
            val extractedDate = extractDateFromLine(trimmedLine)
            when {
                date == null && extractedDate != null -> {
                    date = formatDate(extractedDate)
                }
                storeName == null -> storeName = trimmedLine
                totalAmount == null && isTotalAmount(trimmedLine) -> totalAmount = extractTotalAmount(trimmedLine)
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
                text.contains("Balance Due", ignoreCase = true)||
                text.contains("Amount", ignoreCase = true)
    }

    // Function to extract the total amount (balance due or total) from the text
    private fun extractTotalAmount(text: String): Double? {
        val keywords = listOf("Balance Due","Total", "Amount")

        // Search for the keywords in the text
        val keywordIndex = keywords.indexOfFirst { text.contains(it, ignoreCase = true) }
        if (keywordIndex != -1) {
            // Find the position of the keyword
            val keyword = keywords[keywordIndex]
            val keywordIndex = text.indexOf(keyword, ignoreCase = true)

            // Extract the substring after the keyword
            val substring = text.substring(keywordIndex + keyword.length).trim()
            Log.d(TAG, "Substring: $substring")

            // Use regular expressions to extract the numerical value
            val amountRegex = """\d+(\.\d+)?""".toRegex()
            val matchResult = amountRegex.find(substring)
            if (matchResult != null) {
                // Extract the matched value and convert it to a Double
                val amountString = matchResult.value
                return amountString.toDoubleOrNull()
            }
        }

        return null
    }
}