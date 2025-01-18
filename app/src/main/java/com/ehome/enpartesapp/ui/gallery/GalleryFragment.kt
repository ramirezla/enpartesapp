package com.ehome.enpartesapp.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.ehome.enpartesapp.R
import com.ehome.enpartesapp.databinding.FragmentGalleryBinding

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val galleryViewModel =
            ViewModelProvider(this).get(GalleryViewModel::class.java)

        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Now that the layout is inflated, we can find the button
        binding.backToMainButton.setOnClickListener {
            // Navigate back to MainActivity, popping up to ConsultasAbiertasFragment
            findNavController().navigate(R.id.action_nav_gallery_to_nav_consultas_abiertas)
        }
        return root
    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
////        binding.buttonFirst.setOnClickListener {
////            findNavController().navigate(R.id.action_nav_consultas_abiertas_to_nav_gallery)
////        }
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}