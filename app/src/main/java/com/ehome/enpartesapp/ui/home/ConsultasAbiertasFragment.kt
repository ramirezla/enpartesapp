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

//TODO:
// Buscador para Obtener el vehiculo por placa o serial (todos las veces que aparece el vehiculo, se selecciona uno)
//  -> todos reclamos (y se selecciona 1) getVehicleClaim con el id del vehiculo
//      -> Las solicitudes o reclamos seleccionar el reclamo, ejemplo "claimId": 260 y "snumerosolicitud": "E3F74736-BA3A-4BC0-9FAA-09C16FB290F9",
//          -> Leer las partes del reclamo y mostrarlos (getParts) GETPARTSCLAIM con clainnumber, separar por tab: vehiculo, piezas y destinos.

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
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

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
        restoreResultContainerState()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the state of your views here
        outState.putString("licensePlate", licensePlateEditText.text.toString())
//        outState.putString("results", resultTextView.text.toString())
//        outState.putStringArrayList("resultContainerState", ArrayList(currentResultContainerState))
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
            licensePlateInputLayout.error = "Ingrese la placa o el serial"
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
                            resultTextView.text = "No se encontraron resultados."
                            resultTextView.visibility = View.VISIBLE
                            resultContainer.removeAllViews()
                            currentResults = "No se encontraron resultados."
                            currentResultContainerState.clear()
                        }
                    } else {
                        resultTextView.text = "Error en la solicitud: ${response.code()}"
                        resultTextView.visibility = View.VISIBLE
                        resultContainer.removeAllViews()
                        currentResults = "Error en la solicitud: ${response.code()}"
                        currentResultContainerState.clear()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    resultTextView.text = "Error: ${e.message}"
                    resultTextView.visibility = View.VISIBLE
                    resultContainer.removeAllViews()
                    currentResults = "Error: ${e.message}"
                    currentResultContainerState.clear()
                }
            }
        }
    }

    private fun displayVehicleData(vehicleResponse: Map<String, VehicleData>) {
        resultTextView.visibility = View.VISIBLE
        resultContainer.removeAllViews()
        currentResultContainerState.clear()

        // Define the list of characteristics you want to display
        val caracteristicasMostrar = listOf("Clase", "cylinder", "Categoría", "liters", "Carrocería", "transmission/Transmisión","Transmisión/Tracción", "Tipo de Carrocería") // Only these three
        val caracteristicasNombres = listOf("Clase", "Cilindro", "Categoría", "Litros", "Carrocería", "Caja/Transmisión","Transmisión/Tracción", "Tipo de Carrocería") // Only these three

        // Iterate through the Map using entries
        for (entry in vehicleResponse.entries) {
            // val vehicleKey = entry.key
            // addTextViewToContainer("Vehicle Key: $vehicleKey")
            val vehicleData = entry.value

            // Display VehicleInfo
            val vehicleInfo = vehicleData.datos
            if (vehicleInfo != null) {
                val vehicleCarac = vehicleData.carac
                if (vehicleCarac !=null) {
                    for (carac in vehicleCarac) {
                        // addTextViewToContainer("---------------------------------------------")
//                        addTextViewToContainer(" ")
                        // addTextViewToContainer("vehicleId: ${vehicleInfo.vehicleId}")
                        addTextViewToContainer("Marca: ${vehicleInfo.brandName}")
                        addTextViewToContainer("Modelo: ${vehicleInfo.modelName}")
                        addTextViewToContainer("Año: ${vehicleInfo.year}")
                        addTextViewToContainer("vehicleId: ${carac.vehicleId}")

                        // Parse the inner JSON string
                        val caracJson = JSONObject(carac.carac)

                        // Build the characteristics string
                        val characteristicsLine = StringBuilder()

                        // Se crea la linea de caracteristicas.
                        caracteristicasMostrar.forEachIndexed { index, characteristic ->
                            if (caracJson.has(characteristic)) {
                                val value = caracJson.getString(characteristic)
                                val nombreCarac = caracteristicasNombres[index]
                                characteristicsLine.append("$nombreCarac: $value, ")
                            }
                        }
                        // Add the complete line to the container
                        addTextViewToContainer(characteristicsLine.toString())

                        addTextViewToContainer("Placa: ${vehicleInfo.licensePlate}")
                        addTextViewToContainer("serial: ${vehicleInfo.serialNumber}")
                        addTextViewToContainer("vin: ${vehicleInfo.vin}")

                        currentResultContainerState.add(" ")

                        // Create and add the button
                        val selectButton = Button(requireContext())
                        selectButton.text = "Seleccionar"
                        selectButton.setOnClickListener {
                            // Handle button click (e.g., log the characteristic)
                            addTextViewToContainer("vehicleId: ${carac.vehicleId}")

                            // TODO: ir al fragment para solicitar
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
                        resultContainer.addView(selectButton)
                    }
                }
            }
        }
    }

    private fun addTextViewToContainer(text: String) {
        val textView = TextView(requireContext())
        textView.text = text
        resultContainer.addView(textView)
        currentResultContainerState.add(text)
    }

    private fun restoreResultContainerState() {
        resultContainer.removeAllViews()
        val stateCopy = currentResultContainerState.toList()
        if (stateCopy.isNotEmpty()) { // Only add buttons if there are results
            var addButton = false // Flag to control button addition
            for (text in stateCopy) {
                if (text == " ") {
                    addButton = true // Set the flag to add a button after the next text view
                } else {
                    addTextViewToContainer(text)
                    if (addButton) {
                        // Create and add the button
                        val selectButton = Button(requireContext())
                        selectButton.text = "Seleccionar"
                        // ... (Add your button click listener here) ...
//                        resultContainer.addView(selectButton)
//                        addButton = false // Reset the flag
                        // Add the button click listener here
                        selectButton.setOnClickListener {
                            // Handle button click (e.g., log the characteristic)
                            // ... (Your existing button click logic) ...
                        }
                        resultContainer.addView(selectButton)
                        addButton = false // Reset the flag
                    }
                }
            }
        }
    }
}

