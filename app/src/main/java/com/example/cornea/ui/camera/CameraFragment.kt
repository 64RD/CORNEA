package com.example.cornea.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity  // <-- Needed
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.cornea.databinding.FragmentCameraBinding
import com.example.cornea.BoundingBox
import com.example.cornea.Constants.LABELS_PATH
import com.example.cornea.Constants.MODEL_PATH
import com.example.cornea.Detector
import com.example.cornea.R
import com.example.cornea.DetectionNavigator

class CameraFragment : Fragment(R.layout.fragment_camera), Detector.DetectorListener {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private var detector: Detector? = null
    private lateinit var takePreviewLauncher: ActivityResultLauncher<Void?>
    private var currentBitmap: Bitmap? = null
    private var hasNavigated = false

    companion object {
        private const val REQ_CAMERA_PERMISSION = 100
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCameraBinding.bind(view)

        //Hide the ActionBar
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()

        initDetector()
        registerCameraLauncher()
        bindButtons()

        binding.overlay.visibility = View.VISIBLE

        //Auto-launch camera
        binding.root.post {
            ensureCameraPermission {
                takePreviewLauncher.launch(null)
            }
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

    private fun registerCameraLauncher() {
        takePreviewLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
                if (bmp != null) handleSelectedBitmap(bmp)
                else Toast.makeText(requireContext(), "Camera returned no image", Toast.LENGTH_SHORT).show()
            }
    }

    private fun bindButtons() {
        binding.btnTakePhoto.setOnClickListener { takePreviewLauncher.launch(null) }
        binding.btnClear.setOnClickListener {
            hasNavigated = false //reset navigation flag
            currentBitmap = null
            binding.overlay.clear()
            binding.overlay.setBackgroundBitmap(null)
            binding.inferenceTime.text = ""
            binding.overlay.visibility = View.INVISIBLE
        }
    }

    private fun ensureCameraPermission(onGranted: () -> Unit) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            onGranted()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                REQ_CAMERA_PERMISSION
            )
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
