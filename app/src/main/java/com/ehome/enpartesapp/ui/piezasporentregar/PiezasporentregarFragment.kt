package com.ehome.enpartesapp.ui.piezasporentregar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ehome.enpartesapp.databinding.FragmentPiezasporentregarBinding

class PiezasporentregarFragment : Fragment() {

    private var _binding: FragmentPiezasporentregarBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val piezasporentregarViewModel =
            ViewModelProvider(this).get(PiezasporentregarViewModel::class.java)

        _binding = FragmentPiezasporentregarBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textSlideshow
        piezasporentregarViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}