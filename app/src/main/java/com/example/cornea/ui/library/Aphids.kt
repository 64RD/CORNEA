package com.example.cornea.ui.library

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import com.example.cornea.databinding.ActivityAphidsBinding
import com.example.cornea.R

class Aphids : AppCompatActivity() {

    private lateinit var binding: ActivityAphidsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAphidsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Hide ActionBar permanently
        supportActionBar?.hide()

        //Hide status and navigation bars
        hideSystemBars()

        val uri = intent.getParcelableExtra<Uri>("annotated_image_uri")
        val avgConfidence = intent.getFloatExtra("avg_confidence", 0f)
        val inferenceTime = intent.getLongExtra("inference_time", 0L)

        if (uri != null) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                binding.imageView2.setImageBitmap(bmp)
            } catch (e: Exception) {
                e.printStackTrace()
                binding.imageView2.setImageResource(R.drawable.aphids)
            }
        } else {
            binding.imageView2.setImageResource(R.drawable.aphids)
        }

        //Show detection metadata
        binding.textConfidence.text =
            "Average Confidence: ${(avgConfidence * 100).toInt()}% | Inference Time: ${inferenceTime}ms"
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    )
        }
    }
}
