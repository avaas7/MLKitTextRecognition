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
        // Define patterns to match various details
        val certNoPattern = Pattern.compile("\\b(\\d{2}-\\d{2}-\\d{2}-\\d{5})\\b")
        val nameAfterCertNoPattern =
            Pattern.compile("\\b(\\d{2}-\\d{2}-\\d{2}-\\d{5})\\s+([A-Z][a-zA-Z]*\\s[A-Z][a-zA-Z]*)")

        // Updated pattern to handle month and day extraction
        val monthDayPattern =
            Pattern.compile("Month\\s*([A-Za-z]{3})\\s*Day\\s*[:\\-]?\\s*(\\d{1,2})")
        val yearPattern = Pattern.compile("\\b(\\d{4})\\b")
        val districtPattern = Pattern.compile("District\\s+(\\w+)")
        val subMetroPattern = Pattern.compile("Sub-Metropolitan\\s*:?\\s*(\\w+)")
        val wardNoPattern = Pattern.compile("Ward\\s*No\\.\\s*(\\d+)")
        val sexPattern = Pattern.compile("Sex:\\s*(Male|Female)")

        // Extract certificate number and name
        val certNoMatcher = nameAfterCertNoPattern.matcher(text)
        val certificateNumber: String
        val fullName: String

        if (certNoMatcher.find()) {
            certificateNumber = certNoMatcher.group(1)
            fullName = certNoMatcher.group(2)
        } else {
            certificateNumber = "Not found"
            fullName = "Not found"
        }

        // Extract year, month, and day separately for date of birth
        val yearMatch = yearPattern.matcher(text)
        val monthDayMatch = monthDayPattern.matcher(text)
        var month = "Not found"
        var day = "Not found"

        if (monthDayMatch.find()) {
            month = monthDayMatch.group(1) // Extracted month
            day = monthDayMatch.group(2) // Extracted day
            Log.d("Details", "Extracted Month: $month")
            Log.d("Details", "Extracted Day: $day")
        }

        val year = if (yearMatch.find()) yearMatch.group(1) else "Not found"

        // Extract other details
        val district = extractPatternValue(districtPattern, text)
        val subMetropolitan = extractPatternValue(subMetroPattern, text)
        val wardNo = extractPatternValue(wardNoPattern, text)
        val sex = extractPatternValue(sexPattern, text)

        // Log or use extracted details
        Log.d("Details", "Citizenship Certificate No: $certificateNumber")
        Log.d("Details", "Full Name: $fullName")
        Log.d("Details", "Year: $year")
        Log.d("Details", "Month: $month")
        Log.d("Details", "Day: $day")
        Log.d("Details", "District: $district")
        Log.d("Details", "Sub Metropolitan: $subMetropolitan")
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
        Sub Metropolitan: $subMetropolitan
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


}
