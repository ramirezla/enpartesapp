package com.ehome.enpartesapp.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ehome.enpartesapp.R
import com.ehome.enpartesapp.databinding.FragmentConsultasabiertasBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

//TODO: Arreglar la vista
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

// API interface
interface ApiService {
    @GET("/integracion")
    suspend fun findVehicle(
        @Query("q") query: String
    ): Response<Map<String, VehicleData>> // Changed return type to Map<String, VehicleData>
}

// Retrofit client
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
    private lateinit var searchButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var resultContainer: LinearLayout

    // Variables to hold the state
    private var currentLicensePlate: String? = null
    private var currentResults: String? = null
    private val currentResultContainerState = mutableListOf<String>()

    private var savedQuery: String? = null //Variable para guardar la consulta

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentConsultasabiertasBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize views
        licensePlateInputLayout = binding.licensePlateInputLayout
        licensePlateEditText = binding.licensePlateEditText
        searchButton = binding.searchButton
        resultTextView = binding.resultTextView
        resultContainer = binding.resultContainer

        // Set up the search button click listener
        searchButton.setOnClickListener {
            searchVehicle()
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Restaurar la consulta y ejecutar la búsqueda
        if (savedQuery != null) {
            licensePlateEditText.setText(savedQuery)
            searchButton.performClick() // Simular el clic del botón al regresar de los reclamos
        }

        licensePlateEditText.setText(currentLicensePlate)
        resultTextView.text = currentResults
        resultTextView.visibility = if (currentResults.isNullOrEmpty()) View.GONE else View.VISIBLE

        // Restore the resultContainer state
        //restoreResultContainerState()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the state of your views here
        outState.putString("licensePlate", licensePlateEditText.text.toString())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun searchVehicle() {
        val licensePlate = licensePlateEditText.text.toString().trim()
        currentLicensePlate = licensePlate

        // Validate input (one or the other, not both)
        if (licensePlate.isEmpty()) {
            licensePlateInputLayout.error = getString(R.string.ingrese_la_placa_o_el_serial)
            return
        } else {
            licensePlateInputLayout.error = null
        }

        // Prepare the query
        val query = "{\"action\":\"FINDVEHICLE\",\"accessCode\":\"123456\",\"cKey\":\"12345\",\"licensePlate\":\"$licensePlate\",\"serialNumber\":\"$licensePlate\"}"

        // Guardar la consulta
        savedQuery = licensePlate // Guardar solo el texto ingresado

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
                            resultTextView.text = getString(R.string.no_se_encontraron_resultados)
                            resultTextView.visibility = View.VISIBLE
                            resultContainer.removeAllViews()
                            currentResults = getString(R.string.no_se_encontraron_resultados)
                            currentResultContainerState.clear()
                        }
                    } else {
                        //currentResults = "Error en la solicitud: ${response.code()}"
                        resultTextView.text = getString(R.string.error_en_la_solicitud, response.code(), "")
                        resultTextView.visibility = View.VISIBLE
                        resultContainer.removeAllViews()
                        currentResults = getString(R.string.error_en_la_solicitud, response.code(), "")
                        //currentResults = "Error en la solicitud: ${response.code()}"
                        currentResultContainerState.clear()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    resultTextView.text = getString(R.string.error, e.message)
                    resultTextView.visibility = View.VISIBLE
                    resultContainer.removeAllViews()
                    currentResults = getString(R.string.error, e.message)
                    currentResultContainerState.clear()
                }
            }
        }
    }

    private fun displayVehicleData(vehicleResponse: Map<String, VehicleData>) {
        resultTextView.visibility = View.VISIBLE
        resultContainer.removeAllViews()

        // Define the list of characteristics you want to display
        val caracteristicasMostrar = listOf("Clase", "cylinder", "Categoría", "liters", "Carrocería", "transmission/Transmisión", "Transmisión/Tracción", "Tipo de Carrocería")
        val caracteristicasNombres = listOf("Clase", "Cilindro", "Categoría", "Litros", "Carrocería", "Caja/Transmisión", "Transmisión/Tracción", "Tipo de Carrocería")

        // Iterate through the Map using entries
        if (vehicleResponse.isNotEmpty()) {
            for (entry in vehicleResponse.entries) {
                val vehicleData = entry.value

                // Display VehicleInfo
                val vehicleInfo = vehicleData.datos
                if (vehicleInfo != null) {
                    val vehicleCarac = vehicleData.carac
                    if (vehicleCarac != null) {
                        for (carac in vehicleCarac) {
                            // Create a container for each vehicle's info
                            val vehicleInfoContainer = LinearLayout(requireContext()).apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    setMargins(0, 0, 0, 16) // Add some bottom margin between vehicles
                                }
                                orientation = LinearLayout.VERTICAL
                                setBackgroundResource(R.drawable.vehicle_info_background) // Set the background
                                setPadding(16, 16, 16, 16) // Add some padding
                            }

                            // Add vehicle info to the container
                            addTextViewToContainer(vehicleInfoContainer, "Marca: ${vehicleInfo.brandName}")
                            addTextViewToContainer(vehicleInfoContainer, "Modelo: ${vehicleInfo.modelName}")
                            addTextViewToContainer(vehicleInfoContainer, "Año: ${vehicleInfo.year}")
                            addTextViewToContainer(vehicleInfoContainer, "vehicleId: ${carac.vehicleId}")

                            // Build the characteristics string
                            val characteristicsLine = StringBuilder()

                            // Parse the inner JSON string safely
                            carac.carac?.let { caracString ->
                                val caracJson = JSONObject(caracString)

                                // Create the characteristics line.
                                caracteristicasMostrar.forEachIndexed { index, characteristic ->
                                    if (caracJson.has(characteristic)) {
                                        val value = caracJson.getString(characteristic)
                                        val nombreCarac = caracteristicasNombres[index]
                                        characteristicsLine.append("$nombreCarac: $value, ")
                                    }
                                }
                            }
                            // Add the complete line to the container
                            addTextViewToContainer(vehicleInfoContainer, characteristicsLine.toString())

                            addTextViewToContainer(vehicleInfoContainer, "Placa: ${vehicleInfo.licensePlate}")
                            addTextViewToContainer(vehicleInfoContainer, "serial: ${vehicleInfo.serialNumber}")
                            addTextViewToContainer(vehicleInfoContainer, "vin: ${vehicleInfo.vin}")

                            // Create and add the button
                            val selectButton = Button(requireContext()).apply {
                                text = getString(R.string.seleccionar)
                                // Apply the custom style here
                                setTextAppearance(R.style.SelectButtonStyle)
                                setOnClickListener {

                                    // Navigate to ReclamosFragment and pass the vehicleId
                                    val bundle = Bundle().apply {
                                        putString("brandName", vehicleInfo.brandName)
                                        putString("modelName", vehicleInfo.modelName)
                                        putString("year", vehicleInfo.year)
                                        putString("vehicleId", carac.vehicleId)
                                        putString("licensePlate", vehicleInfo.licensePlate)
                                        putString("serialNumber", vehicleInfo.serialNumber)
                                        putString("vin", vehicleInfo.vin)
                                        // Add VehicleCarac data (characteristics)
                                        putString("characteristicsLine", characteristicsLine.toString())
                                    }
                                    findNavController().navigate(R.id.action_nav_consultas_abiertas_to_nav_reclamos, bundle)
                                }
                            }
                            vehicleInfoContainer.addView(selectButton)

                            resultContainer.addView(vehicleInfoContainer)
                        }
                    }
                }
            }
        } else {
            showDialog(getString(R.string.no_se_encontraron_resultados))
        }
    }

    private fun showDialog(message: String) {
        AlertDialog.Builder(requireContext()) // Usar requireContext() directamente
            .setTitle("Información") // Título del diálogo
            .setMessage(message)     // Mensaje a mostrar
            .setIcon(R.drawable.analyze_list_logs_search_icon) // Ícono personalizado
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()     // Cerrar el diálogo al hacer clic en "OK"
            }
            .show() // Mostrar el diálogo
    }

    private fun addTextViewToContainer(container: LinearLayout, text: String) {val textView = TextView(requireContext()).apply {
        this.text = text
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }
        container.addView(textView)
    }
}

