package com.ehome.enpartesapp.ui.reclamos

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ehome.enpartesapp.R
import com.ehome.enpartesapp.databinding.FragmentReclamosBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

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
        _binding = FragmentReclamosBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize resultContainer here
        resultContainer = binding.resultContainer

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

            // Build the string to display in the TextView
            val receivedDataString = StringBuilder()
                .append("Brand: $brandName\n")
                .append("Model: $modelName\n")
                .append("Year: $year\n")
                .append("Vehicle ID: $vehicleId\n")
                .append("License Plate: $licensePlate\n")
                .append("Serial Number: $serialNumber\n")
                .append("Vin: $vin\n")
                .append("Characteristics: $characteristicsLine\n") // Added characteristicsLine
                .toString()

            // Set the text to the TextView
            val receivedDataTextView: TextView = view.findViewById(R.id.receivedDataVehicle)
            receivedDataTextView.text = receivedDataString

            // Make API call and display data
            if (vehicleId != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // http://192.168.1.143/integracion?q={"action":"GETVEHICLECLAIMS","accessCode":"123456","cKey":"12345","vehicleId" : "12345-1"}
                        val url = "http://192.168.1.143/integracion?q={\"action\":\"GETVEHICLECLAIMS\",\"accessCode\":\"123456\",\"cKey\":\"12345\",\"vehicleId\":\"$vehicleId\"}"
                        val response = makeApiRequest(url)

                        withContext(Dispatchers.Main) {
                            // Parse and display JSON response
                            val claimsList: List<Claim> = Gson().fromJson(response, object : TypeToken<List<Claim>>() {}.type)

                            for (claim in claimsList) {
                                // Create a horizontal LinearLayout for each claim
                                val claimLayout = LinearLayout(requireContext()).apply {
                                    orientation = LinearLayout.VERTICAL
                                    layoutParams = LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                        )
                                }

                                // Add snumerosolicitud TextView
                                val snumerosolicitudTextView = TextView(requireContext()).apply {
                                    text = "ID Solicitud: ${claim.snumerosolicitud}"
                                }
                                claimLayout.addView(snumerosolicitudTextView)

                                // Add claimNumber TextView
                                val claimNumberTextView = TextView(requireContext()).apply {
                                    text = "Sin número"
                                    if (claim.claimNumber.isNotEmpty())
                                        text = "${claim.claimNumber}"
                                }
                                claimLayout.addView(claimNumberTextView)

                                // Add claimDate TextView
                                val claimDateTextView = TextView(requireContext()).apply {
                                    text = "${formatDate(claim.claimDate)}"
                                }
                                claimLayout.addView(claimDateTextView)

                                // Add selection button
                                val selectButton = Button(requireContext()).apply {
                                    text = "Seleccionar"
                                    // Set onClickListener for the button (handle selection logic here)
                                    setOnClickListener {// Check if claimNumber is empty
                                        if (claim.claimId != 0) {
                                            // Execute API call with claimNumber
                                            CoroutineScope(Dispatchers.IO).launch {
                                                val partsUrl = "http://192.168.1.143/integracion?q={\"action\":\"GETPARTSCLAIM\",\"accessCode\":\"123456\",\"cKey\":\"12345\",\"claimId\":\"${claim.claimId}\"}"
                                                val partsResponse = makeApiRequest(partsUrl)

                                                withContext(Dispatchers.Main) {// Parse and display parts data in popup
                                                    val partsList: List<Part> = Gson().fromJson(partsResponse, object : TypeToken<List<Part>>() {}.type)
                                                    showPartsPopup(partsList)
                                                }
                                            }
                                        } else {
                                            // Handle empty claimNumber (e.g., show a message to the user)
                                            Toast.makeText(requireContext(), "Claim Id is cero", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                claimLayout.addView(selectButton)

                                // Add the claimLayout to the resultContainer
                                resultContainer.addView(claimLayout)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ReclamosFragment", "Error making API request: ${e.message}")
                        withContext(Dispatchers.Main) {
                            addTextViewToContainer("Error loading claims data.")
                        }
                    }
                }
            }
        }
    }

    // Function to show parts data in a popup
    private fun showPartsPopup(partsList: List<Part>) {
        val builder = AlertDialog.Builder(requireContext())

        // Create a custom title TextView to center the title
        val titleTextView = TextView(requireContext()).apply {
            text = "Partes y Piezas"
            gravity = Gravity.CENTER // Center the text
            setPadding(0, 16, 0, 16) // Add padding for spacing
        }
        builder.setCustomTitle(titleTextView) // Set the custom title

        // Create a ScrollView to hold the parts data
        val scrollView = ScrollView(requireContext())
        val linearLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 0, 16, 0) // Add left and right padding to the main LinearLayout
        }
        scrollView.addView(linearLayout)

        // Add TextViews for each part to the LinearLayout
        for (part in partsList) {
            // Create a vertical LinearLayout for each part's details
            val partDetailsLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = FrameLayout.LayoutParams( // Use FrameLayout.LayoutParams for margins
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 8, 16, 8) // Add left, top, right, bottom margins
                }
            }

            // Add TextViews for each field
            val descriptionTextView = TextView(requireContext()).apply {
                text = "Description: ${part.description}"
            }
            partDetailsLayout.addView(descriptionTextView)

            val statusTextView = TextView(requireContext()).apply {
                text = "Status: ${part.status}"
            }
            partDetailsLayout.addView(statusTextView)

            val amountTextView = TextView(requireContext()).apply {
                text = "Amount: ${part.ammount}"
            }
            partDetailsLayout.addView(amountTextView)

            val conditionTextView = TextView(requireContext()).apply {
                text = "Condition: ${part.condition}"
            }
            partDetailsLayout.addView(conditionTextView)

            // Add the partDetailsLayout to the main LinearLayout
            linearLayout.addView(partDetailsLayout)

            // Add a separator (optional)
            val separator = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1 // 1dp height for the separator
                ).apply {
                    setMargins(0, 8,0, 8) // Add margins for spacing
                }
                setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            }
            linearLayout.addView(separator)
        }

        builder.setView(scrollView)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    // Data class for Part (adjust fields as needed)
    data class Part(
        val __className: String,
        val description: String,
        val code: String,
        val additionalinformation: String,
        val quality: String,
        val codstatus: String,
        val status: String,
        val ammount: String,
        val tax: String,
        val disponibility: Int,
        val condition: String
    )

    // Function to format date to DD/MM/YYYY
    private fun formatDate(inputDate: String): String {
        // Define input and output formats
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        try {
            // Parse the input date string val
            val date = inputFormat.parse(inputDate) // Remove 'Z' if present

            // Format the date to the desired output format
            return outputFormat.format(date)
        } catch (e: ParseException) {
            // Handle parsing errors (e.g., log the error or return a default value)
            Log.e("ReclamosFragment", "Error parsing date: ${e.message}")
            return "Invalid Date" // Or any other default value
        }
    }

    // Function to make API request
    private fun makeApiRequest(url: String): String {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        return response.body?.string() ?: ""
    }

    // Data class for Claim (adjust fields as needed)
    data class Claim(
        val __className: String,
        val claimId: Int,
        val policyNumber: String,
        val claimNumber: String,
        val certificateNumber: String,
        val snumerosolicitud: String,
        val claimDate: String,
        val sumInsured: String,
        val splaca: String,
        val scarroceria: String,
        val cliente: String
    )

    private fun addTextViewToContainer(text: String) {
        val textView = TextView(requireContext())
        textView.text = text
        resultContainer.addView(textView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}