package com.ehome.enpartesapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ehome.enpartesapp.databinding.FragmentConsultasabiertasBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

//TODO:
// Buscador para Obtener el vehiculo por placa o serial (todos las veces que aparece el vehiculo, se selecciona 1)
//  -> todos reclamos (y se selecciona 1) getVehicleClaim con el id del vehiculo
//      -> Las solicitudes o reclamos seleccionar el reclamo
//          -> Leer las partes del reclamo y mostrarlos (getParts)

import retrofit2.http.GET
import retrofit2.http.Query

data class VehicleResponse(
    val infoVehicle: VehicleData?
)

data class VehicleData(
    val datos: VehicleInfo?,
    val carac: List<VehicleCarac>?
)

data class VehicleInfo(
    val vehicleId: String?,
    val brandId: Int?,
    val brandName: String?,
    val modelId: Int?,
    val modelName: String?,
    val licensePlate: String?,
    val vin: String?,
    val serialNumber: String?,
    val year: String?
)

data class VehicleCarac(
    val vehicleId: String?,
    val carac: String?
)

//interface ApiService {
//    @GET("/integracion")
//    suspend fun findVehicle(
//        @Query("q") query: String
//    ): Response<VehicleResponse>
//}

interface ApiService {
    @GET("/integracion")
    suspend fun findVehicle(
        @Query("q") query: String
    ): Response<Map<String, VehicleData>> // Changed return type to Map<String, VehicleData>
}

object RetrofitClient {
    private const val BASE_URL = "http://192.168.1.143"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}

class ConsultasAbiertasFragment : Fragment() {

    private var _binding: FragmentConsultasabiertasBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var licensePlateInputLayout: TextInputLayout
    private lateinit var licensePlateEditText: TextInputEditText
    private lateinit var serialCarroceriaInputLayout: TextInputLayout
    private lateinit var serialCarroceriaEditText: TextInputEditText
    private lateinit var searchButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var resultContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentConsultasabiertasBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize views
        licensePlateInputLayout = binding.licensePlateInputLayout
        licensePlateEditText = binding.licensePlateEditText
        serialCarroceriaInputLayout = binding.serialCarroceriaInputLayout
        serialCarroceriaEditText = binding.serialCarroceriaEditText
        searchButton = binding.searchButton
        resultTextView = binding.resultTextView
        resultContainer = binding.resultContainer

        // Set up the search button click listener
        searchButton.setOnClickListener {
            searchVehicle()
        }

        return root
    }

    private fun searchVehicle() {
        val licensePlate = licensePlateEditText.text.toString().trim()
        val serialCarroceria = serialCarroceriaEditText.text.toString().trim()

        // Validate input (one or the other, not both)
        if (licensePlate.isEmpty() && serialCarroceria.isEmpty()) {
            licensePlateInputLayout.error = "Ingrese la placa o el serial"
            serialCarroceriaInputLayout.error = "Ingrese la placa o el serial"
            return
        } else {
            licensePlateInputLayout.error = null
            serialCarroceriaInputLayout.error = null
        }

        if (licensePlate.isNotEmpty() && serialCarroceria.isNotEmpty()) {
            licensePlateInputLayout.error = "Ingrese solo la placa o el serial"
            serialCarroceriaInputLayout.error = "Ingrese solo la placa o el serial"
            return
        } else {
            licensePlateInputLayout.error = null
            serialCarroceriaInputLayout.error = null
        }

        // Prepare the query
        val query = if (licensePlate.isNotEmpty()) {
            "{\"action\":\"FINDVEHICLE\",\"accessCode\":\"123456\",\"cKey\":\"12345\",\"licensePlate\":\"$licensePlate\"}"
        } else {
            "{\"action\":\"FINDVEHICLE\",\"accessCode\":\"123456\",\"cKey\":\"12345\",\"serialNumber\":\"$serialCarroceria\"}"
        }

        // Make the API request
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.findVehicle(query)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val vehicleResponse = response.body()
                        if (vehicleResponse != null) {
                            displayVehicleData(vehicleResponse)
                        } else {
                            resultTextView.text = "No se encontraron resultados."
                            resultTextView.visibility = View.VISIBLE
                            resultContainer.removeAllViews()
                        }
                    } else {
                        resultTextView.text = "Error en la solicitud: ${response.code()}"
                        resultTextView.visibility = View.VISIBLE
                        resultContainer.removeAllViews()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    resultTextView.text = "Error: ${e.message}"
                    resultTextView.visibility = View.VISIBLE
                    resultContainer.removeAllViews()
                }
            }
        }
    }

//    private fun displayVehicleData(vehicleResponse: VehicleResponse) {
//        resultTextView.visibility = View.VISIBLE
//        resultContainer.removeAllViews()
//
//        val vehicleData = vehicleResponse.infoVehicle
//        if (vehicleData != null) {
//            val vehicleInfo = vehicleData.datos
//            if (vehicleInfo != null) {
//                addTextViewToContainer("vehicleId: ${vehicleInfo.vehicleId}")
//                addTextViewToContainer("brandId: ${vehicleInfo.brandId}")
//                addTextViewToContainer("brandName: ${vehicleInfo.brandName}")
//                addTextViewToContainer("modelId: ${vehicleInfo.modelId}")
//                addTextViewToContainer("modelName: ${vehicleInfo.modelName}")
//                addTextViewToContainer("licensePlate: ${vehicleInfo.licensePlate}")
//                addTextViewToContainer("vin: ${vehicleInfo.vin}")
//                addTextViewToContainer("serialNumber: ${vehicleInfo.serialNumber}")
//                addTextViewToContainer("year: ${vehicleInfo.year}")
//            }
//
//            val vehicleCarac = vehicleData.carac
//            if (vehicleCarac != null) {
//                for (carac in vehicleCarac) {
//                    addTextViewToContainer("vehicleId: ${carac.vehicleId}")
//                    addTextViewToContainer("carac: ${carac.carac}")
//                }
//            }
//        }
//    }

    private fun displayVehicleData(vehicleResponse: Map<String, VehicleData>) {
        resultTextView.visibility = View.VISIBLE
        resultContainer.removeAllViews()

        // Iterate through the Map using entries
        for (entry in vehicleResponse.entries) {
            val vehicleKey = entry.key
            val vehicleData = entry.value

            // Add a header for each vehicle
            addTextViewToContainer("--------------------")
            addTextViewToContainer("Vehicle Key: $vehicleKey")
            addTextViewToContainer("--------------------")

            // Display VehicleInfo
            val vehicleInfo = vehicleData.datos
            if (vehicleInfo != null) {
                addTextViewToContainer("vehicleId: ${vehicleInfo.vehicleId}")
                addTextViewToContainer("brandId: ${vehicleInfo.brandId}")
                addTextViewToContainer("brandName: ${vehicleInfo.brandName}")
                addTextViewToContainer("modelId: ${vehicleInfo.modelId}")
                addTextViewToContainer("modelName: ${vehicleInfo.modelName}")
                addTextViewToContainer("licensePlate: ${vehicleInfo.licensePlate}")
                addTextViewToContainer("vin: ${vehicleInfo.vin}")
                addTextViewToContainer("serialNumber: ${vehicleInfo.serialNumber}")
                addTextViewToContainer("year: ${vehicleInfo.year}")
            }

            // Display VehicleCarac
            val vehicleCarac = vehicleData.carac
            if (vehicleCarac !=null) {
                for (carac in vehicleCarac) {
                    addTextViewToContainer("  vehicleId: ${carac.vehicleId}")
                    addTextViewToContainer("carac: ${carac.carac}")
                }
            }
        }
    }

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

// Error: com.ehome.enpartesapp.ui.home.VehicleResponse cannot be cast to java.util.map