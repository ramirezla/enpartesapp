package com.ehome.enpartesapp.ui.reclamos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.ehome.enpartesapp.R
import com.ehome.enpartesapp.databinding.FragmentReclamosBinding

class ReclamosFragment : Fragment() {

    private var _binding: FragmentReclamosBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var resultContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val reclamosViewModel =
            ViewModelProvider(this).get(ReclamosViewModel::class.java)

        _binding = FragmentReclamosBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize the resultContainer
        resultContainer = binding.resultContainer
        //resultContainer = view.findViewById(R.id.resultContainer)

        // Get the bundle from the arguments
        val bundle = arguments
        if (bundle != null) {
            // Retrieve VehicleInfo data
            val brandName = bundle.getString("brandName")
            val modelName = bundle.getString("modelName")
            val year = bundle.getString("year")
            val vehicleId = bundle.getString("vehicleId")
            val licensePlate = bundle.getString("licensePlate")
            val serialNumber = bundle.getString("serialNumber")
            val vin = bundle.getString("vin")
            val characteristicsLine = bundle.getString("characteristicsLine")

            // Retrieve VehicleCarac data (characteristics)
//            val clase = bundle.getString("Clase")
//            val cylinder = bundle.getString("Cilindro")
//            val categoria = bundle.getString("Categoría")
//            val liters = bundle.getString("Litros")
//            val carroceria = bundle.getString("Carrocería")
//            val cajaTransmision = bundle.getString("Caja/Transmisión")
//            val transmisionTraccion = bundle.getString("Transmisión/Tracción")
//            val tipoCarroceria = bundle.getString("Tipo de Carrocería")
//            val characteristicsLine = bundle.getString("characteristicsLine")

            // Now you can use these values in your ReclamosFragment
            // For example, set them to TextViews:
//            view.findViewById<TextView>(R.id.brandNameTextView).text = "Brand: $brandName"
//            view.findViewById<TextView>(R.id.modelNameTextView).text = "Model: $modelName"
//            view.findViewById<TextView>(R.id.yearTextView).text = "Year: $year"
//            view.findViewById<TextView>(R.id.vehicleIdTextView).text = "Vehicle ID: $vehicleId"
//            view.findViewById<TextView>(R.id.licensePlateTextView).text = "License Plate: $licensePlate"
//            view.findViewById<TextView>(R.id.serialNumberTextView).text = "Serial Number: $serialNumber"
//            view.findViewById<TextView>(R.id.vinTextView).text = "Vin: $vin"
//            view.findViewById<TextView>(R.id.claseTextView).text = "Clase: $clase"
//            view.findViewById<TextView>(R.id.cylinderTextView).text = "Cilindro: $cylinder"
//            view.findViewById<TextView>(R.id.categoriaTextView).text = "Categoría: $categoria"
//            view.findViewById<TextView>(R.id.litersTextView).text = "Litros: $liters"
//            view.findViewById<TextView>(R.id.carroceriaTextView).text = "Carrocería: $carroceria"
//            view.findViewById<TextView>(R.id.cajaTransmisionTextView).text = "Caja/Transmisión: $cajaTransmision"
//            view.findViewById<TextView>(R.id.transmisionTraccionTextView).text = "Transmisión/Tracción: $transmisionTraccion"
//            view.findViewById<TextView>(R.id.tipoCarroceriaTextView).text = "Tipo de Carrocería: $tipoCarroceria"
            // Now you can use these values and add them to the container
            addTextViewToContainer("Brand: $brandName")
            addTextViewToContainer("Model: $modelName")
            addTextViewToContainer("Year: $year")
            addTextViewToContainer("Vehicle ID: $vehicleId")
            addTextViewToContainer("License Plate: $licensePlate")
            addTextViewToContainer("Serial Number: $serialNumber")
            addTextViewToContainer("Vin: $vin")
            addTextViewToContainer("characteristicsLine: $characteristicsLine")
        }

        // Now that the layout is inflated, we can find the button
        binding.backToMainButton.setOnClickListener {
            // Navigate back to MainActivity, popping up to ConsultasAbiertasFragment
            findNavController().navigate(R.id.action_nav_reclamos_to_nav_consultas_abiertas)
        }
        return root
    }

    private fun addTextViewToContainer(text: String) {
        val textView = TextView(requireContext())
        textView.text = text
        resultContainer.addView(textView)
    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
////        binding.buttonFirst.setOnClickListener {
////            findNavController().navigate(R.id.action_nav_consultas_abiertas_to_nav_reclamos)
////        }
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}