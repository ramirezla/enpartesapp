package com.ehome.enpartesapp.ui.reclamos

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private val binding get() = _binding!!
    private lateinit var claimAdapter: ClaimAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReclamosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Now that binding is inflated, we can use it to access views
        recyclerView = binding.recyclerViewClaims
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val bundle = arguments
        if (bundle != null) {
            val brandName = bundle.getString("brandName")
            val modelName = bundle.getString("modelName")
            val year = bundle.getString("year")
            val vehicleId = bundle.getString("vehicleId")
            val licensePlate = bundle.getString("licensePlate")
            val serialNumber = bundle.getString("serialNumber")
            val vin = bundle.getString("vin")
            val characteristicsLine = bundle.getString("characteristicsLine")

            val receivedDataString = StringBuilder()
                .append("Brand: $brandName\n")
                .append("Model: $modelName\n")
                .append("Year: $year\n")
                .append("Vehicle ID: $vehicleId\n")
                .append("License Plate: $licensePlate\n")
                .append("Serial Number: $serialNumber\n")
                .append("Vin: $vin\n")
                .append("Characteristics: $characteristicsLine\n")
                .toString()

            // Use binding to access the TextView
            binding.receivedDataVehicle.text = receivedDataString

            if (vehicleId != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val url =
                            "http://192.168.1.143/integracion?q={\"action\":\"GETVEHICLECLAIMS\",\"accessCode\":\"123456\",\"cKey\":\"12345\",\"vehicleId\":\"$vehicleId\"}"
                        val response = makeApiRequest(url)

                        withContext(Dispatchers.Main) {
                            val claimsList: List<Claim> = Gson().fromJson(
                                response,
                                object : TypeToken<List<Claim>>() {}.type
                            )
                            if (claimsList.isNotEmpty()) {
                                claimAdapter = ClaimAdapter(claimsList) { claim ->
                                    if (claim.claimId != 0) {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            val partsUrl =
                                                "http://192.168.1.143/integracion?q={\"action\":\"GETPARTSCLAIM\",\"accessCode\":\"123456\",\"cKey\":\"12345\",\"claimId\":\"${claim.claimId}\"}"
                                            val partsResponse = makeApiRequest(partsUrl)

                                            withContext(Dispatchers.Main) {
                                                val partsList: List<Part> =
                                                    Gson().fromJson(
                                                        partsResponse,
                                                        object : TypeToken<List<Part>>() {}.type
                                                    )
                                                showPartsPopup(partsList)
                                            }
                                        }
                                    } else {
                                        Toast.makeText(
                                            requireContext(),
                                            getString(R.string.id_del_reclamo_es_cero),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                recyclerView.adapter = claimAdapter
                            } else {
                                addTextViewToContainer(getString(R.string.no_se_encontraron_reclamos))
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(
                            "ReclamosFragment",
                            getString(R.string.error_en_la_solicitud_del_api, e.message)
                        )
                        withContext(Dispatchers.Main) {
                            addTextViewToContainer(getString(R.string.error_cargando_la_informacion_de_los_reclamos))
                        }
                    }
                }
            }
        }
    }

    // Function to show parts data in a popup
    private fun showPartsPopup(partsList: List<Part>) {
        val builder = AlertDialog.Builder(requireContext())

        val titleTextView = TextView(requireContext()).apply {
            text = getString(R.string.partes_y_piezas)
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 16)
        }
        builder.setCustomTitle(titleTextView)

        val recyclerViewParts = RecyclerView(requireContext())
        recyclerViewParts.layoutManager = LinearLayoutManager(requireContext())
        val partAdapter = PartAdapter(partsList)
        recyclerViewParts.adapter = partAdapter

        builder.setView(recyclerViewParts)
        builder.setPositiveButton(getString(R.string.ok_texto)) { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    // Function to make API request
    private fun makeApiRequest(url: String): String {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        return response.body?.string() ?: ""
    }

    private fun addTextViewToContainer(text: String) {
        val textView = TextView(requireContext())
        textView.text = text
        //resultContainer.addView(textView)
    }

    // Data class for Part (adjust fields as needed)
    data class Part(
        val className: String, // Renamed from __className
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

    // Data class for Claim (adjust fields as needed)
    data class Claim(
        val className: String, // Renamed from __className
        val claimId: Int,
        val policyNumber: String,
        val claimNumber: String,
        val certificateNumber: String,
        val snumerosolicitud: String,
        val claimDate: String,
        val sumInsured: String,val splaca: String,
        val scarroceria: String,
        val cliente: String
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}