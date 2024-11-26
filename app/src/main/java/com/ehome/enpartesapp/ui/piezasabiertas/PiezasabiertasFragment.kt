package com.ehome.enpartesapp.ui.piezasabiertas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ehome.enpartesapp.databinding.FragmentPiezasabiertasBinding

class PiezasabiertasFragment : Fragment() {

    private var _binding: FragmentPiezasabiertasBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val piezasabiertasViewModel =
            ViewModelProvider(this).get(PiezasabiertasViewModel::class.java)

        _binding = FragmentPiezasabiertasBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textGallery
        piezasabiertasViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}