package com.example.cornea

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.cornea.ui.library.*
import java.io.File
import java.io.FileOutputStream

object DetectionNavigator {

    fun openDetectedActivity(
        context: Context,
        rawLabel: String,
        annotatedBitmap: Bitmap?,
        avgConfidence: Float = 0f,
        inferenceTime: Long = 0L
    ) {
        val normalized = rawLabel.trim().lowercase().replace(" ", "")

        val intent = when (normalized) {
            "aphids" -> Intent(context, Aphids::class.java)
            "bandedleafandsheathblight" -> Intent(context, BandedleafandSheathBlight::class.java)
            "brownspot" -> Intent(context, BrownSpot::class.java)
            "cornearworm" -> Intent(context, CornEarworm::class.java)
            "cornplanthopper" -> Intent(context, CornPlantHopper::class.java)
            "fallarmyworm" -> Intent(context, FallArmyworm::class.java)
            "healthy" -> Intent(context, Healthy::class.java)
            "northernleafblight" -> Intent(context, NorthernLeafBlight::class.java)
            "downymildew" -> Intent(context, DownyMildew::class.java)
            "grayleafspots" -> Intent(context, GrayLeafSpot::class.java)
            else -> null
        }

        if (intent != null) {

            //Pass annotated bitmap safely using FileProvider
            annotatedBitmap?.let { bitmap ->
                try {
                    val file = File(context.cacheDir, "annotated_image.png")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    val uri: Uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    intent.putExtra("annotated_image_uri", uri)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Failed to attach annotated image", Toast.LENGTH_SHORT).show()
                }
            }


            //Pass confidence and inference time
            intent.putExtra("avg_confidence", avgConfidence)
            intent.putExtra("inference_time", inferenceTime)

            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Unknown detection: $rawLabel", Toast.LENGTH_SHORT).show()
        }
    }
}
