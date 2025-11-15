package com.example.cornea.ui.library

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import com.example.cornea.databinding.ActivityHealthyBinding
import com.example.cornea.R

class Healthy : AppCompatActivity() {

    private lateinit var binding: ActivityHealthyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHealthyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Hide ActionBar permanently
        supportActionBar?.hide()

        //Hide system bars for fullscreen
        hideSystemBars()

        //Receive the URI instead of byte array
        val imageUri: Uri? = intent.getParcelableExtra("annotated_image_uri")
        val avgConfidence = intent.getFloatExtra("avg_confidence", 0f)
        val inferenceTime = intent.getLongExtra("inference_time", 0L)

        if (imageUri != null) {
            binding.imageView2.setImageURI(imageUri)
        } else {
            binding.imageView2.setImageResource(R.drawable.healthy)
        }

        // Show detection metadata
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
