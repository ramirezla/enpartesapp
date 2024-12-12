package com.ehome.enpartesapp.ui.consultasabiertas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ehome.enpartesapp.databinding.FragmentConsultasabiertasBinding

class ConsultasabiertasFragment : Fragment() {

    private var _binding: FragmentConsultasabiertasBinding? = null
    val username = arguments?.getString("username")

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val consultasabiertasViewModel =
            ViewModelProvider(this).get(ConsultasabiertasViewModel::class.java)

        _binding = FragmentConsultasabiertasBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Retrieve username and password from arguments
        val username = arguments?.getString("username")
        //val password = arguments?.getString("password")

        val textView: TextView = binding.ConsAbiEmailAddress

        // Update the TextView with the received values
        textView.text = username // \nPassword: $password"

        consultasabiertasViewModel.text.observe(viewLifecycleOwner) {
            // You can use the ViewModel to further process or display the data if needed
            // textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}