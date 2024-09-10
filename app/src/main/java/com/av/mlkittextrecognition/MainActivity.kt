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
    val NOT_FOUND = "Not found"
    var certificateNumber = NOT_FOUND
    var fullName = NOT_FOUND
    var month = NOT_FOUND
    var year = NOT_FOUND
    var day = NOT_FOUND
    var district = NOT_FOUND
    var location = NOT_FOUND
    var wardNo = NOT_FOUND
    var sex = NOT_FOUND

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                try {
                    val bitmap: Bitmap =
                        MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                    val image = InputImage.fromBitmap(bitmap, 0)
                    initializeData()
                    recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            val resultText = visionText.text
                            Log.d("Av_Tag", "Result Text: $resultText")
                            //  extractDetails(resultText)
                            var counter = 1
                            for (block in visionText.textBlocks) {
                                val blockText = block.text
//                                val blockCornerPoints = block.cornerPoints
//                                val blockFrame = block.boundingBox


                                extractSpecificDetails(blockText)
                                Log.d("Av_Tag", "$counter Block Text: $blockText")
                                /*       Log.d(
                                           "Av_Tag",
                                           "$counter Block Corner Points: ${blockCornerPoints?.joinToString()}"
                                       )
                                       Log.d("Av_Tag", "$counter Block Bounding Box: $blockFrame")*/
                                counter++
                            }

                            printData()
                        }
                        .addOnFailureListener { e ->
                            Log.e("Av_Tag", "Text recognition failed", e)
                        }
                } catch (e: IOException) {
                    Log.e("Av_Tag", "Error reading image", e)
                }
            }
        }

    private fun initializeData() {
        certificateNumber = NOT_FOUND
        fullName = NOT_FOUND
        month = NOT_FOUND
        year = NOT_FOUND
        day = NOT_FOUND
        district = NOT_FOUND
        location = NOT_FOUND
        wardNo = NOT_FOUND
        sex = NOT_FOUND
    }

    private fun extractSpecificDetails(text: String) {
        val certNoPattern = Pattern.compile("\\b(\\d{2}-\\d{2}-\\d{2}-\\d{5}|\\d{4}/\\d{3,5}|\\d{3,}/\\d{3,}/\\d{3,}|\\d{5})\\b");
      //  val certNoPattern = Pattern.compile("\\b(\\d{2}-\\d{2}-\\d{2}-\\d{5}|\\d{4}/\\d{3})\\b")
        val certNoAltMatcher = certNoPattern.matcher(text)

        val fullNamePattern = Pattern.compile("\\b([A-Z]+(?:\\s[A-Z]+)+)\\b")
        val fullNameMatcher = fullNamePattern.matcher(text)

        val monthPattern = Pattern.compile("Month\\s*:?\\s*([A-Z]{3})")
        val monthMatch = monthPattern.matcher(text)

        val dayPattern = Pattern.compile("Day\\s*:?\\s*(\\d+)")
        val dayMatch = dayPattern.matcher(text)

        val yearPattern = Pattern.compile("Year\\s*:?(\\d{4})")
        val yearMatch = yearPattern.matcher(text)

        val districtPattern = Pattern.compile("District\\s*:?\\s*([A-Za-z]+)")
        val locationPattern = Pattern.compile("(VDC|Sub-Metropolitan|Municipality)\\s*:?\\s*([A-Za-z]+)")
        val wardNoPattern = Pattern.compile("Ward\\s*No\\.?\\s*(\\d+)")
        val sexPattern = Pattern.compile("Sex:?\\s*(Male|Female|Other)")

        if (!ifValueFound(certificateNumber)) {
        if (certNoAltMatcher.find()) {
                certificateNumber = certNoAltMatcher.group(1)
            }
        }

        if (!ifValueFound(fullName)) {
        if (fullNameMatcher.find()) {
                fullName = fullNameMatcher.group(1)
            }
        }

        if (!ifValueFound(month)) {
            month = if (monthMatch.find()) monthMatch.group(1) else NOT_FOUND // Capture month
        }

        if (!ifValueFound(day)) {
            day = if (dayMatch.find()) dayMatch.group(1) else NOT_FOUND // Capture day
        }

        if (!ifValueFound(year)) {
            year = if (yearMatch.find()) yearMatch.group(1) else NOT_FOUND
        }

        if (!ifValueFound(district)) {
            district = extractPatternValue(districtPattern, text)
        }
        if (!ifValueFound(location)) {
            location = extractLocationValue(locationPattern, text)
        }

        if (!ifValueFound(wardNo)) {
            wardNo = extractPatternValue(wardNoPattern, text)
        }

        if (!ifValueFound(sex)) {
            sex = extractPatternValue(sexPattern, text)
        }
    }

    private fun extractPatternValue(pattern: Pattern, text: String): String {
        val matcher = pattern.matcher(text)
        return if (matcher.find()) matcher.group(1) else NOT_FOUND
    }

    private fun extractLocationValue(pattern: Pattern, text: String): String {
        val matcher = pattern.matcher(text)
        return if (matcher.find()) matcher.group(2) else NOT_FOUND
    }

    private fun ifValueFound(text: String): Boolean {
        if (!text.equals(NOT_FOUND)) {
            return true
        } else {
            return false
        }
    }


    private fun printData() {

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

  /*  private fun extractDetails(text: String) {
        // Updated pattern for certificate number and names (up to 3 words)
        val certNoPattern = Pattern.compile("\\b(\\d{2}-\\d{2}-\\d{2}-\\d{5}|\\d{4}/\\d{3})\\b")
        val nameAfterCertNoPattern = Pattern.compile(
            "(\\d{2}-\\d{2}-\\d{2}-\\d{5}|\\d{4}/\\d{3})\\s+([A-Z][a-zA-Z]*\\s[A-Z][a-zA-Z]*)(?:\\s[A-Z][a-zA-Z]*)?"
        )

        val certNoMatcher = nameAfterCertNoPattern.matcher(text)
        val certNoAltMatcher = certNoPattern.matcher(text)

        var certificateNumber = NOT_FOUND
        var fullName = NOT_FOUND

        if (certNoMatcher.find()) {
            certificateNumber = certNoMatcher.group(1)
            fullName = certNoMatcher.group(2) ?: NOT_FOUND
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
        val locationPattern =
            Pattern.compile("(VDC|Sub-Metropolitan|Municipality)\\s*:?\\s*([A-Za-z]+)")

        val wardNoPattern = Pattern.compile("Ward\\s*No\\.?\\s*(\\d+)")
        val sexPattern = Pattern.compile("Sex:?\\s*(Male|Female|Other)")

        // Extract year, month, and day
        val yearMatch = yearPattern.matcher(text)
        val month = if (monthMatch.find()) monthMatch.group(1) else NOT_FOUND // Capture month
        val day = if (dayMatch.find()) dayMatch.group(1) else NOT_FOUND // Capture day

        val year = if (yearMatch.find()) yearMatch.group(1) else NOT_FOUND

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
    }*/

}
