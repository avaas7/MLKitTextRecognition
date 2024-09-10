package com.av.mlkittextrecognition

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.av.mlkittextrecognition.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                try {
                    val bitmap: Bitmap =
                        MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                    val image = InputImage.fromBitmap(bitmap, 0)

                    recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            val resultText = visionText.text
                            Log.d("Av_Tag", "Result Text: $resultText")
                            extractDetails(resultText)
                        }
                        .addOnFailureListener { e ->
                            Log.e("Av_Tag", "Text recognition failed", e)
                        }
                } catch (e: IOException) {
                    Log.e("Av_Tag", "Error reading image", e)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun extractDetails(text: String) {
        // Updated pattern for certificate number and names (up to 3 words)
        val certNoPattern = Pattern.compile("\\b(\\d{2}-\\d{2}-\\d{2}-\\d{5}|\\d{4}/\\d{3})\\b")
        val nameAfterCertNoPattern = Pattern.compile(
            "(\\d{2}-\\d{2}-\\d{2}-\\d{5}|\\d{4}/\\d{3})\\s+([A-Z][a-zA-Z]*\\s[A-Z][a-zA-Z]*)(?:\\s[A-Z][a-zA-Z]*)?"
        )

        val certNoMatcher = nameAfterCertNoPattern.matcher(text)
        val certNoAltMatcher = certNoPattern.matcher(text)

        var certificateNumber = "Not found"
        var fullName = "Not found"

        if (certNoMatcher.find()) {
            certificateNumber = certNoMatcher.group(1)
            fullName = certNoMatcher.group(2) ?: "Not found"
        } else if (certNoAltMatcher.find()) {
            certificateNumber = certNoAltMatcher.group(1)
        }

        // Updated pattern for extracting month (3 uppercase letters)
        val monthPattern = Pattern.compile("Month\\s*:?\\s*([A-Z]{3})")

// Updated pattern to handle day extraction with optional colon and space
        val dayPattern = Pattern.compile("Day\\s*:?\\s*(\\d+)")

        val monthMatch = monthPattern.matcher(text)
        val dayMatch = dayPattern.matcher(text)


        // Updated pattern to handle year extraction, allowing for optional colon
        val yearPattern = Pattern.compile("Year\\s*:?(\\d{4})")
        val districtPattern = Pattern.compile("District\\s*:?\\s*([A-Za-z]+)")

        // Updated pattern to match VDC, Sub-Metropolitan, or Municipality and capture the following word
        val locationPattern = Pattern.compile("(VDC|Sub-Metropolitan|Municipality)\\s*:?\\s*([A-Za-z]+)")

        val wardNoPattern = Pattern.compile("Ward\\s*No\\.?\\s*(\\d+)")
        val sexPattern = Pattern.compile("Sex:?\\s*(Male|Female|Other)")

        // Extract year, month, and day
        val yearMatch = yearPattern.matcher(text)
        val month = if (monthMatch.find()) monthMatch.group(1) else "Not found" // Capture month
        val day = if (dayMatch.find()) dayMatch.group(1) else "Not found" // Capture day

        val year = if (yearMatch.find()) yearMatch.group(1) else "Not found"

        // Extract other details
        val district = extractPatternValue(districtPattern, text)
        val location = extractLocationValue(locationPattern, text) // Changed to new method
        val wardNo = extractPatternValue(wardNoPattern, text)
        val sex = extractPatternValue(sexPattern, text)

        // Log the extracted details
        Log.d("Details", "Citizenship Certificate No: $certificateNumber")
        Log.d("Details", "Full Name: $fullName")
        Log.d("Details", "Year: $year")
        Log.d("Details", "Month: $month")
        Log.d("Details", "Day: $day")
        Log.d("Details", "District: $district")
        Log.d("Details", "Location: $location")
        Log.d("Details", "Ward No: $wardNo")
        Log.d("Details", "Sex: $sex")

        // Format the extracted details
        val extractedDetails = """
        Citizenship Certificate No: $certificateNumber
        Full Name: $fullName
        Year: $year
        Month: $month
        Day: $day
        District: $district
        Location: $location
        Ward No: $wardNo
        Sex: $sex
    """.trimIndent()

        // Set the extracted details to the TextView
        binding.tvTextExtracted.text = extractedDetails
    }

    private fun extractPatternValue(pattern: Pattern, text: String): String {
        val matcher = pattern.matcher(text)
        return if (matcher.find()) matcher.group(1) else "Not found"
    }

    // Updated method to extract the location name (the word after VDC, Sub-Metropolitan, or Municipality)
    private fun extractLocationValue(pattern: Pattern, text: String): String {
        val matcher = pattern.matcher(text)
        return if (matcher.find()) matcher.group(2) else "Not found" // Return the second group (location name)
    }


}
