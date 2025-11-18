package com.example.cornea.ui.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity  // <-- Add this
import com.example.cornea.databinding.FragmentGalleryBinding
import com.example.cornea.BoundingBox
import com.example.cornea.Constants.LABELS_PATH
import com.example.cornea.Constants.MODEL_PATH
import com.example.cornea.Detector
import android.os.Bundle
import com.example.cornea.R
import com.example.cornea.DetectionNavigator


class GalleryFragment : Fragment(R.layout.fragment_gallery), Detector.DetectorListener {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private var detector: Detector? = null
    private var currentBitmap: Bitmap? = null

    companion object {
        private const val REQ_GALLERY_PERMISSION = 101
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentGalleryBinding.bind(view)

        // --- Hide ActionBar ---
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()

        initDetector()
        registerGalleryLauncher()
        bindButtons()

        binding.overlay.visibility = View.VISIBLE

        // Auto-launch gallery
        binding.root.post {
            ensureGalleryPermission { pickImageLauncher.launch("image/*") }
        }
    }

    private fun initDetector() {
        Thread {
            detector = Detector(
                context = requireContext(),
                modelPath = MODEL_PATH,
                labelPath = LABELS_PATH,
                detectorListener = this
            ) { msg ->
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun registerGalleryLauncher() {
        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) {
                    val bmp = requireContext().contentResolver.safeLoadBitmap(uri)
                    bmp?.let { handleSelectedBitmap(it) }
                        ?: Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun bindButtons() {
        binding.btnPickImage.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnClear.setOnClickListener {
            currentBitmap = null
            binding.overlay.clear()
            binding.overlay.setBackgroundBitmap(null)
            binding.inferenceTime.text = ""
            binding.overlay.visibility = View.INVISIBLE
        }
    }

    private fun ensureGalleryPermission(onGranted: () -> Unit) {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), perm) == PackageManager.PERMISSION_GRANTED) {
            onGranted()
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(perm), REQ_GALLERY_PERMISSION)
        }
    }

    private fun handleSelectedBitmap(bitmap: Bitmap) {
        val scaled = bitmap.downscale(640)
        currentBitmap = scaled

        binding.overlay.setBackgroundBitmap(scaled)
        binding.overlay.clear()
        binding.overlay.visibility = View.VISIBLE

        detector?.detect(scaled)
    }

    private fun Bitmap.downscale(maxDim: Int = 640): Bitmap {
        val ratio = minOf(maxDim.toFloat() / width, maxDim.toFloat() / height, 1f)
        val newW = (width * ratio).toInt()
        val newH = (height * ratio).toInt()
        return if (newW == width && newH == height) this else Bitmap.createScaledBitmap(this, newW, newH, true)
    }

    override fun onEmptyDetect() {
        activity?.runOnUiThread {
            binding.inferenceTime.text = ""
            binding.overlay.clear()
            Toast.makeText(requireContext(), "Corn plant not detected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        activity?.runOnUiThread {
            binding.inferenceTime.text = "${inferenceTime}ms"
            binding.overlay.setResults(boundingBoxes)

            if (boundingBoxes.isNotEmpty()) {
                //Compute average confidence
                val avgConfidence = boundingBoxes.map { it.cnf }.average().toFloat()

                //Get top detection to determine which activity to open
                val topDetection = boundingBoxes.maxByOrNull { it.cnf }

                //Only open if top detection confidence is reasonable
                if (topDetection != null && topDetection.cnf > 0.3f) {
                    val annotatedBitmap = binding.overlay.getAnnotatedBitmap()

                    //Send the annotated image, average confidence, and inference time
                    DetectionNavigator.openDetectedActivity(
                        requireContext(),
                        topDetection.clsName,
                        annotatedBitmap,
                        avgConfidence,
                        inferenceTime
                    )
                } else {
                    Toast.makeText(requireContext(), "Low confidence detection", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "No pest or disease detected", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        detector?.close()
        _binding = null
    }
}

//Extension to safely load bitmap from gallery
private fun android.content.ContentResolver.safeLoadBitmap(uri: android.net.Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val src = ImageDecoder.createSource(this, uri)
            ImageDecoder.decodeBitmap(src) { decoder, _, _ -> decoder.isMutableRequired = true }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(this, uri)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
