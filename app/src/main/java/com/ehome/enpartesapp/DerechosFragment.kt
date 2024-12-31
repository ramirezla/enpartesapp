package com.ehome.enpartesapp

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.ehome.enpartesapp.databinding.FragmentDerechosBinding

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class DerechosFragment : Fragment() {

    private var _binding: FragmentDerechosBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // Aceptar la licencia, los derechos y terminos.
    private lateinit var checkBoxAccept: CheckBox

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDerechosBinding.inflate(inflater, container, false)
        val view = inflater.inflate(R.layout.fragment_derechos, container, false)

        checkBoxAccept = view.findViewById(R.id.checkBoxAccept)

        // Format the license textval licenseTextView: TextView = view.findViewById(R.id.textViewLicense)
        val formattedLicenseText = formatLicenseText(getString(R.string.licencia_derechos))

        val textLicencia: TextView = view.findViewById(R.id.textViewLicense)
        textLicencia.text = formattedLicenseText

        // Handle back button press
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (checkBoxAccept.isChecked) {
                    // Navigate back to the previous fragment
                    findNavController().popBackStack()
                } else {
                    // Optionally, show a message to the user
                    // that they must accept the license
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        binding.buttonSecond.setOnClickListener {
//            findNavController().navigate(R.id.action_DerechosFragment_to_RegistrarFragment)
//        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun formatLicenseText(licenseText: String): SpannableString {
        val spannableString = SpannableString(licenseText)

        // Example formatting:
        // Make the first 10 characters bold and underlined
        spannableString.setSpan(StyleSpan(Typeface.BOLD), 0, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(UnderlineSpan(), 0, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Change the color of characters 15-25 to red
        spannableString.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)),
            15,
            25,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Make characters 30-40 larger
        spannableString.setSpan(RelativeSizeSpan(1.5f), 30, 40, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Add more formatting as needed...

        return spannableString
    }

}