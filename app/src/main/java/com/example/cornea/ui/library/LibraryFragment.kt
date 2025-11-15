package com.example.cornea.ui.library

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.cornea.databinding.FragmentLibraryBinding

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //Hide ActionBar permanently
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()

        //Make fullscreen
        makeFullscreen()

        //Button click logic
        binding.button1.setOnClickListener { startActivity(Intent(requireContext(), CornEarworm::class.java)) }
        binding.button2.setOnClickListener { startActivity(Intent(requireContext(), FallArmyworm::class.java)) }
        binding.button3.setOnClickListener { startActivity(Intent(requireContext(), CornPlantHopper::class.java)) }
        binding.button4.setOnClickListener { startActivity(Intent(requireContext(), Aphids::class.java)) }
        binding.button5.setOnClickListener { startActivity(Intent(requireContext(), Healthy::class.java)) }
        binding.button6.setOnClickListener { startActivity(Intent(requireContext(), GrayLeafSpot::class.java)) }
        binding.button7.setOnClickListener { startActivity(Intent(requireContext(), NorthernLeafBlight::class.java)) }
        binding.button8.setOnClickListener { startActivity(Intent(requireContext(), BrownSpot::class.java)) }
        binding.button9.setOnClickListener { startActivity(Intent(requireContext(), BandedleafandSheathBlight::class.java)) }
        binding.button10.setOnClickListener { startActivity(Intent(requireContext(), DownyMildew::class.java)) }

        return root
    }

    private fun makeFullscreen() {
        val window = requireActivity().window

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //Modern approach for Android 11+
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            //Legacy approach
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        //ActionBar and fullscreen remain hidden
    }
}
