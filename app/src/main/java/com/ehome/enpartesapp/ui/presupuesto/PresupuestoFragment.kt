package com.ehome.enpartesapp.ui.presupuesto

//import com.google.ai.client.generativeai.type.image // This import is correct for the image(Bitmap) extension function
import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ehome.enpartesapp.R
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import ReportGenerator
import android.widget.ProgressBar   // Importa la barra de progreso
import android.view.View.GONE       // Importa GONE para ocultar
import android.view.View.VISIBLE    // Importa VISIBLE para mostrar
import androidx.navigation.fragment.findNavController // Asegúrate de que esta línea esté presente

private const val BASE_URL = "http://209.126.106.199/"
private const val API_submit_case = "/solmovsa/ApiGestorSiniestros/api/MotionsCloud/submit-case"

// ADD YOUR GEMINI API KEY HERE! For production, consider more secure storage.  AIzaSyBwS4dsCanEu3cbGb1isD4Vh4KUSYLOM6Y
private const val GEMINI_API_KEY = "AIzaSyBwS4dsCanEu3cbGb1isD4Vh4KUSYLOM6Y"

data class FotoItem(
    var imagenUri: Uri? = null,
    var tipoFoto: String = "",
    var isFotoTomada: Boolean = false
)

class FotoAdapter(
    private val context: Context,
    private val fotoList: MutableList<FotoItem>,
    private val onAddClickListener: () -> Unit,
    private val onDeleteClickListener: (Int) -> Unit,
    private val onTakePhotoClickListener: (Int) -> Unit,
    private val onUploadPhotoClickListener: (Int) -> Unit
) : RecyclerView.Adapter<FotoAdapter.FotoViewHolder>() {

    var currentPosition: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_foto, parent, false)
        return FotoViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        val fotoItem = fotoList[position]

        val tiposFoto = context.resources.getStringArray(R.array.tipos_foto)
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, tiposFoto)
        holder.spinnerTipoFoto.adapter = adapter

        val selectedIndex = tiposFoto.indexOf(fotoItem.tipoFoto)
        if (selectedIndex >= 0) {
            holder.spinnerTipoFoto.setSelection(selectedIndex)
        }

        holder.spinnerTipoFoto.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                fotoItem.tipoFoto = tiposFoto[pos]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        holder.btnTomarFoto.setOnClickListener {
            currentPosition = position
            onTakePhotoClickListener(position)
        }

        holder.btnSubirFoto.setOnClickListener {
            currentPosition = position
            onUploadPhotoClickListener(position)
        }

        if (fotoItem.imagenUri != null) {
            holder.imgFoto.setImageURI(fotoItem.imagenUri)
        } else {
            holder.imgFoto.setImageResource(R.drawable.ic_menu_gallery)
        }

        holder.btnBorrar.setOnClickListener {
            onDeleteClickListener(position)
        }

        holder.btnAgregar.setOnClickListener {
            onAddClickListener()
        }
    }

    override fun getItemCount(): Int = fotoList.size

    class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val btnTomarFoto: ImageButton = itemView.findViewById(R.id.btnTomarFoto)
        val btnSubirFoto: ImageButton = itemView.findViewById(R.id.btnSubirFoto)
        val imgFoto: ImageView = itemView.findViewById(R.id.imgFoto)
        val spinnerTipoFoto: Spinner = itemView.findViewById(R.id.spinnerTipoFoto)
        val btnBorrar: ImageButton = itemView.findViewById(R.id.btnBorrar)
        val btnAgregar: ImageButton = itemView.findViewById(R.id.btnAgregar)
    }
}

class PresupuestoFragment : Fragment() {

    private var fotoList: MutableList<FotoItem> = mutableListOf()
    private var caseToken: String? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FotoAdapter
    private var currentPhotoUri: Uri? = null

    private lateinit var spinnerTipoFotoVin: Spinner
    private lateinit var spinnerTipoVehiculo: Spinner
    private lateinit var spinnerMarcaVehiculo: Spinner
    private lateinit var spinnerModeloVehiculo: Spinner
    private lateinit var spinnerVehicleColor: Spinner
    private lateinit var spinnerCountry: Spinner
    private lateinit var spinnerState: Spinner
    private lateinit var spinnerCity: Spinner

    private lateinit var progressBar: ProgressBar // <-- AÑADIR ESTA LÍNEA

    private lateinit var takePhotoLauncher: ActivityResultLauncher<Uri>
    private lateinit var uploadPhotoLauncher: ActivityResultLauncher<String>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private lateinit var etCaseNumber: TextInputEditText
    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etVINnumber: TextInputEditText
    private lateinit var etDateOfInspection: TextInputEditText
    private lateinit var etVehicleYear: TextInputEditText

    private lateinit var btnCancelar: Button
    private lateinit var btnAceptar: Button

    private lateinit var geminiModel: GenerativeModel

    private val modelosPorMarca: Map<String, Array<String>> = mapOf(
        "Seleccione una marca..." to arrayOf("Seleccione un modelo..."),
        "Acura" to arrayOf("ILX","MDX","RDX","RL","RLX","RSX","TL","TSX"),
        "BAIC" to arrayOf("Seleccione un modelo...", "X35", "X55", "BJ40"),
        "Chevrolet" to arrayOf("Seleccione un modelo...", "Astro", "Avalanche", "Aveo/Kalos/Sonic","Bolt","C/K Pickup",
            "Camaro","Caprice","Captiva","Cavalier","Cobalt","Colorado","Corvette","Cruze","D-Max","Epica/Tosca",
            "Equinox","Express","HHR","Impala","Joy","Lanus","Lumina","Malibu","Monte Carlo","Nexia/Cielo/Racer","N300",
            "N400","Optra/Lacetti/Nubira","Onix","Orlando","Prizm","S-10 Blazer/Jimmy","S10 Pickup/Sonoma","SS","SSR","Sail",
            "Silverado","Spark/Matiz/Beato","Suburban","Tacuma/Rezzo/Vivant","Tahoe","TrailBlazer","Tracker","Traverse",
            "Uplander","Venture","Volt"),
        "Chery" to arrayOf("Seleccione un modelo...", "A1","A11","A13","A15","A3/M11","A5","Eastar/B11","Exceed/Exceed TX",
            "IndiS","Karry Youya","QQ3/S11","QQ6/S21","Tiggo","Tiggo 2 Pro","Tiggo 3/3x/2 (A13T)", "Tiggo 4 Pro","Tiggo 5 (T21)",
            "Tiggo 7 Pro", "Tiggo 8 Pro", "Tiggo 8 Pro Max", "Arrizo 5","V5/B14"),
        "Ford" to arrayOf("Seleccione un modelo...", "Bronco","Bronco Sport","C-Max","Cargo","Crown Victoria"
            ,"E-Series/Econoline","Ecosport","Edge","Escape","Escort","Everest","Excursion","Expedition","Explorer",
            "F-Series","F-150","Falcon","Fiesta","Figo/Ka","Five Hundred","Flex","Focus","Freestart","Fusion",
            "Galaxy","Grand C-Max","Grand Tourneo Connect","Kuga","Laser","Mondeo","Mustang","Orion","Probe","Puma",
            "Ranger","S-Series","S-Max","Taurus","Taurus X/Freestyle","Tempo/Topaz","Territory (SY)","Thunderbird","Transit",
            "Transit Connect","Transit Courier/Turneo","Transit Courier/Turneo Custom","Windstar","ZX2/Escort ZX2"),
        "Great Wall" to arrayOf("Seleccione un modelo...", "Deer","Poer","Voleex C30","Wingle 5", "Wingle 7"),
        "Haval" to arrayOf("Seleccione un modelo...", "Dargo","H6","Haval F7","Haval H2","Haval H3/Hover H3","Haval H5/Hover H5",
            "Haval H6","Haval H8","Haval H9","Haval M4","H9","Jolion"),
        "Hyundai" to arrayOf("Seleccione un modelo...", "Tucson", "Creta", "Accent", "Grand i10", "Santa Fe", "Palisade", "Venue", "Staria"),
        "Jetour" to arrayOf("Seleccione un modelo...", "Dashing","X70","X70 Plus"),
        "Kia" to arrayOf("Seleccione un modelo...", "Sportage", "Picanto", "Rio", "Soluto", "Seltos", "Sonet", "Stonic", "Carnival", "K2500/K2700"),
        "Mazda" to arrayOf("Seleccione un modelo...", "BT-50", "CX-5", "CX-30", "Mazda2", "Mazda3", "CX-9"),
        "Nissan" to arrayOf("Seleccione un modelo...", "Frontier", "Kicks", "Versa", "X-Trail", "Pathfinder", "Qashqai", "Murano"),
        "Renault" to arrayOf("Seleccione un modelo...", "Duster", "Logan", "Sandero", "Stepway", "Kwid", "Oroch", "Captur"),
        "Suzuki" to arrayOf("Seleccione un modelo...", "Grand Vitara", "Jimny", "Swift", "S-Preso", "Baleno", "Vitara (nuevo)", "S-Cross"),
        "Toyota" to arrayOf("Seleccione un modelo...", "Hilux", "Fortuner", "RAV4", "Corolla", "Yaris", "Prado", "Land Cruiser", "Rush", "Agya", "Stout"),
        "Volkswagen" to arrayOf("Seleccione un modelo...", "Amarok", "T-Cross", "Nivus", "Virtus", "Polo", "Tiguan", "Saveiro", "Taos"),
        "Otra" to arrayOf("Seleccione un modelo...", "Otro Modelo")
    )

    private val colors = arrayOf("Seleccione un color...", "Blanco","Blanco Perlado","Blanco Hueso",
        "Negro","Negro Brillante","Negro Mate","Gris","Gris Plata","Gris Oscuro","Gris Grafito",
        "Gris Acero","Gris Cemento","Beige","Champán","Azul","Azul Marino","Azul Cielo",
        "Azul Eléctrico","Azul Rey","Azul Petróleo","Azul Noche","Rojo","Rojo Vino","Rojo Cereza",
        "Rojo Brillante","Rojo Naranja","Verde","Verde Oscuro","Verde Oliva","Verde Lima",
        "Verde Esmeralda","Verde Menta","Amarillo","Amarillo Patito","Naranja","Naranja Cobrizo",
        "Marrón","Marrón Chocolate","Bronce","Cobre","Púrpura","Morado","Lila","Rosado","Dorado",
        "Turquesa","Crema","Otro")

    private val countries = arrayOf("Seleccione un país...",
        "Ecuador", "Colombia", "Perú", "Chile", "Argentina")

    private val statesByCountry: Map<String, Array<String>> = mapOf(
        "Seleccione un país..." to arrayOf("Seleccione un estado..."),
        "Ecuador" to arrayOf(
            "Seleccione un estado...",
            "Azuay", "Bolívar", "Cañar", "Carchi", "Chimborazo", "Cotopaxi", "El Oro", "Esmeraldas",
            "Galápagos", "Guayas", "Imbabura", "Loja", "Los Ríos", "Manabí", "Morona Santiago",
            "Napo", "Orellana", "Pastaza", "Pichincha", "Santa Elena", "Santo Domingo de los Tsáchilas",
            "Sucumbíos", "Tungurahua", "Zamora Chinchipe"
        ),
        "Colombia" to arrayOf("Seleccione un estado...", "Cundinamarca", "Antioquia", "Valle del Cauca"),
        "Perú" to arrayOf("Seleccione un estado...", "Lima", "Arequipa", "Cusco"),
        "Chile" to arrayOf("Seleccione un estado...", "Región Metropolitana", "Valparaíso", "Biobío"),
        "Argentina" to arrayOf("Seleccione un estado...", "Buenos Aires", "Córdoba", "Santa Fe")
    )
    private val citiesByState: Map<String, Array<String>> = mapOf(
        "Seleccione un estado..." to arrayOf("Seleccione una ciudad..."),
        //Inicio Ecuador
        // Sierra
        "Azuay" to arrayOf("Seleccione una ciudad...", "Cuenca", "Gualaceo", "Paute", "Sígsig", "Girón"),
        "Bolívar" to arrayOf("Seleccione una ciudad...", "Guaranda", "Chimbo", "San Miguel", "Caluma"),
        "Cañar" to arrayOf("Seleccione una ciudad...", "Azogues", "Biblián", "Cañar", "La Troncal"),
        "Carchi" to arrayOf("Seleccione una ciudad...", "Tulcán", "San Gabriel", "El Ángel", "Mira"),
        "Chimborazo" to arrayOf("Seleccione una ciudad...", "Riobamba", "Guano", "Alausí", "Chambo", "Colta"),
        "Cotopaxi" to arrayOf("Seleccione una ciudad...", "Latacunga", "Salcedo", "Pujilí", "Saquisilí", "La Maná"),
        "Imbabura" to arrayOf("Seleccione una ciudad...", "Ibarra", "Otavalo", "Cotacachi", "Atuntaqui", "Pimampiro"),
        "Loja" to arrayOf("Seleccione una ciudad...", "Loja", "Catamayo", "Cariamanga", "Macará", "Alamor"),
        "Pichincha" to arrayOf("Seleccione una ciudad...", "Quito", "Cayambe", "Machachi", "Sangolquí", "Pedro Vicente Maldonado", "Tabacundo"), // Sangolquí es cabecera de Rumiñahui
        "Santo Domingo de los Tsáchilas" to arrayOf("Seleccione una ciudad...", "Santo Domingo", "La Concordia"),
        "Tungurahua" to arrayOf("Seleccione una ciudad...", "Ambato", "Baños de Agua Santa", "Pelileo", "Píllaro", "Patate"),
        // Costa
        "El Oro" to arrayOf("Seleccione una ciudad...", "Machala", "Pasaje", "Santa Rosa", "Huaquillas", "Zaruma", "Piñas"),
        "Esmeraldas" to arrayOf("Seleccione una ciudad...", "Esmeraldas", "Atacames", "Muisne", "Quinindé", "San Lorenzo"),
        "Guayas" to arrayOf("Seleccione una ciudad...", "Guayaquil", "Durán", "Daule", "Milagro", "Samborondón", "Playas", "El Triunfo", "Naranjal", "Yaguachi"),
        "Los Ríos" to arrayOf("Seleccione una ciudad...", "Babahoyo", "Quevedo", "Vinces", "Ventanas", "Puebloviejo", "Baba"),
        "Manabí" to arrayOf("Seleccione una ciudad...", "Portoviejo", "Manta", "Chone", "El Carmen", "Jipijapa", "Bahía de Caráquez", "Pedernales"),
        "Santa Elena" to arrayOf("Seleccione una ciudad...", "Santa Elena", "La Libertad", "Salinas", "Manglaralto"),
        // Amazonía
        "Morona Santiago" to arrayOf("Seleccione una ciudad...", "Macas", "Gualaquiza", "Sucúa", "Palora"),
        "Napo" to arrayOf("Seleccione una ciudad...", "Tena", "Archidona", "El Chaco", "Baeza"),
        "Orellana" to arrayOf("Seleccione una ciudad...", "Puerto Francisco de Orellana (Coca)", "La Joya de los Sachas", "Loreto"),
        "Pastaza" to arrayOf("Seleccione una ciudad...", "Puyo", "Mera", "Santa Clara", "Arajuno"),
        "Sucumbíos" to arrayOf("Seleccione una ciudad...", "Nueva Loja (Lago Agrio)", "Shushufindi", "Cascales", "Putumayo"),
        "Zamora Chinchipe" to arrayOf("Seleccione una ciudad...", "Zamora", "Yantzaza", "Zumba", "El Pangui"),
        // Región Insular
        "Galápagos" to arrayOf("Seleccione una ciudad...", "Puerto Baquerizo Moreno", "Puerto Ayora", "Puerto Villamil"),
        // Fin Ecuador
        // Inicio Colombia
        "Cundinamarca" to arrayOf("Seleccione una ciudad...", "Bogotá", "Soacha"),
        "Antioquia" to arrayOf("Seleccione una ciudad...", "Medellín", "Envigado"),
        // Fin Colombia
        // Inicio Perú
        "Lima" to arrayOf("Seleccione una ciudad...", "Lima Metropolitana", "Callao")
        // Fin Perú
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        geminiModel = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = GEMINI_API_KEY)

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                tomarFoto(adapter.currentPosition)
            } else {
                Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }

        takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentPhotoUri?.let { uri ->
                    fotoList[adapter.currentPosition].imagenUri = uri
                    fotoList[adapter.currentPosition].isFotoTomada = true
                    adapter.notifyItemChanged(adapter.currentPosition)
                }
            }
        }

        uploadPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                fotoList[adapter.currentPosition].imagenUri = it
                fotoList[adapter.currentPosition].isFotoTomada = false
                adapter.notifyItemChanged(adapter.currentPosition)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_presupuesto, container, false)

        etCaseNumber = view.findViewById(R.id.etCaseNumber)
        etFullName = view.findViewById(R.id.etFullName)
        etEmail = view.findViewById(R.id.etEmail)
        etDateOfInspection = view.findViewById(R.id.etDateOfInspection)
        etVINnumber = view.findViewById(R.id.etVINnumber)
        etVehicleYear = view.findViewById(R.id.etVehicleYear)
        spinnerTipoVehiculo = view.findViewById(R.id.spinnerTipoVehiculo)
        spinnerMarcaVehiculo = view.findViewById(R.id.spinnerMarcaVehiculo)
        spinnerModeloVehiculo = view.findViewById(R.id.spinnerModeloVehiculo)
        spinnerVehicleColor = view.findViewById(R.id.spinnerVehicleColor)
        spinnerCountry = view.findViewById(R.id.spinnerCountry)
        spinnerState = view.findViewById(R.id.spinnerState)
        spinnerCity = view.findViewById(R.id.spinnerCity)

        //spinnerTipoFotoVin = view.findViewById(R.id.spinnerTipoFotoVin) // Se mantiene si se usa en otra parte

        btnCancelar = view.findViewById(R.id.btnCancelar)
        btnAceptar = view.findViewById(R.id.btnAceptar)
        progressBar = view.findViewById(R.id.progressBar)

        btnCancelar.setOnClickListener {
            Log.d("PresupuestoFragment", "Cancelar button clicked.")
            limpiarFormulario()
        }

        btnAceptar.setOnClickListener {
            Log.d("PresupuestoFragment", "Aceptar button clicked.")
            validarYProcesarDatos()
        }

        etDateOfInspection.setOnClickListener { mostrarDatePicker() }
        etVehicleYear.setOnClickListener { mostrarYearPicker() }

        configurarSpinnerMarcasVehiculosEcuador()
        configurarSpinnerTipoVehiculo()
        configurarSpinnerVehicleColor()
        configurarSpinnerCountry()

        fotoList.add(FotoItem())
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = FotoAdapter(
            requireContext(),
            fotoList,
            onAddClickListener = {
                Log.d("PresupuestoFragment", "Add photo button clicked.")
                agregarLinea()
            },
            onDeleteClickListener = {
                Log.d("PresupuestoFragment", "Delete photo button clicked for position $it.")
                borrarLinea(it)
            },
            onTakePhotoClickListener = { position ->
                Log.d("PresupuestoFragment", "Take photo button clicked for position $position.")
                tomarFoto(position)
            },
            onUploadPhotoClickListener = { position ->
                Log.d("PresupuestoFragment", "Upload photo button clicked for position $position.")
                subirFoto(position)
            }
        )
        recyclerView.adapter = adapter
        return view
    }

    private fun limpiarFormulario() {
        etCaseNumber.text?.clear()
        etFullName.text?.clear()
        etEmail.text?.clear()
        etDateOfInspection.text?.clear()
        etVINnumber.text?.clear()
        etVehicleYear.text?.clear()

        spinnerTipoVehiculo.setSelection(0)
        spinnerMarcaVehiculo.setSelection(0)
        spinnerModeloVehiculo.setSelection(0)
        spinnerVehicleColor.setSelection(0)
        spinnerCountry.setSelection(0)
        spinnerState.setSelection(0)
        spinnerCity.setSelection(0)

        spinnerTipoFotoVin.setSelection(0)

        fotoList.clear()
        fotoList.add(FotoItem())
        adapter.notifyDataSetChanged()
        Log.d("PresupuestoFragment", "Formulario limpiado.")
    }

    private fun validarYProcesarDatos() {
        Log.d("PresupuestoFragment", "validarYProcesarDatos() called.")
        if (validarCampos() && validarFotosVehiculo()) {
            Log.d("PresupuestoFragment", "Validación exitosa.")
            // Comenta la siguiente línea para evitar la llamada a la API anterior
            // procesarDatos()
            Toast.makeText(requireContext(), "Campos validados correctamente (envío a API).", Toast.LENGTH_LONG).show()
            generateDamageReport() //Envia a la API

        } else {
            Log.w("PresupuestoFragment", "Validación fallida. Mostrando toast de error.")
            Toast.makeText(requireContext(), "Por favor, complete todos los campos y una foto del vehículo", Toast.LENGTH_LONG).show()
        }
    }

    private fun validarCampos(): Boolean {
        Log.d("Validacion", "Iniciando validación de campos.")
        val campos = listOf(
            etCaseNumber.text,
            etFullName.text,
            etEmail.text,
            etDateOfInspection.text,
            etVINnumber.text,
            etVehicleYear.text
        )
        val spinners = listOf(
            spinnerTipoVehiculo,
            spinnerMarcaVehiculo,
            spinnerModeloVehiculo,
            spinnerVehicleColor,
            spinnerCountry,
            spinnerState,
            spinnerCity
        )

        for (campo in campos) {
            if (campo.isNullOrBlank()) {
                Log.d("Validacion", "Campo vacío detectado.")
                return false
            }
        }

        for (spinner in spinners) {
            if (spinner.selectedItemPosition == 0) {
                Log.d("Validacion", "Spinner no seleccionado detectado.")
                return false
            }
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(etEmail.text.toString()).matches()) {
            Log.d("Validacion", "Formato de email inválido.")
            Toast.makeText(requireContext(), "Por favor, ingrese un email válido", Toast.LENGTH_SHORT).show()
            return false
        }
        Log.d("Validacion", "Validación de campos completada y exitosa.")
        return true
    }

    private fun validarFotosVehiculo(): Boolean {
        Log.d("Validacion", "Iniciando validación de fotos del vehículo.")
        if (fotoList.isEmpty()) {
            Log.d("Validacion", "No hay fotos en la lista.")
            Toast.makeText(requireContext(), "Por favor, agregue al menos una foto del vehículo", Toast.LENGTH_SHORT).show()
            return false
        }
        for (fotoItem in fotoList) {
            if (fotoItem.imagenUri == null) {
                Log.d("Validacion", "Falta imagen URI en un ítem de foto.")
                Toast.makeText(requireContext(), "Por favor, agregue al menos una foto del vehículo", Toast.LENGTH_SHORT).show()
                return false
            }
            if (fotoItem.tipoFoto == "Seleccione un tipo...") {
                Log.d("Validacion", "Tipo de foto de vehículo no seleccionado en un ítem.")
                Toast.makeText(requireContext(), "Por favor, seleccione un tipo para todas las fotos del vehículo", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        Log.d("Validacion", "Validación de fotos del vehículo completada y exitosa.")
        return true
    }

    private fun agregarLinea() {
        fotoList.add(FotoItem())
        adapter.notifyItemInserted(fotoList.size - 1)
        Log.d("PresupuestoFragment", "Nueva línea de foto agregada. Total: ${fotoList.size}")
    }

    private fun borrarLinea(position: Int) {
        if (fotoList.size > 1) {
            fotoList.removeAt(position)
            adapter.notifyItemRemoved(position)
            adapter.notifyItemRangeChanged(position, fotoList.size - position)
            Log.d("PresupuestoFragment", "Línea de foto borrada en posición $position. Total: ${fotoList.size}")
        } else {
            fotoList.clear()
            fotoList.add(FotoItem())
            adapter.notifyDataSetChanged()
            Log.d("PresupuestoFragment", "Última línea de foto borrada y restablecida a una por defecto.")
        }
    }

    private fun mostrarDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDayOfMonth ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(selectedYear, selectedMonth, selectedDayOfMonth)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            etDateOfInspection.setText(dateFormat.format(selectedDate.time))
            Log.d("PresupuestoFragment", "Fecha de inspección seleccionada: ${dateFormat.format(selectedDate.time)}")
        }, year, month, day)
        datePickerDialog.show()
    }

    private fun mostrarYearPicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)

        val yearPickerDialog = DatePickerDialog(
            requireContext(),
            AlertDialog.THEME_HOLO_LIGHT,
            { _, selectedYear, _, _ ->
                etVehicleYear.setText(selectedYear.toString())
                Log.d("PresupuestoFragment", "Año del vehículo seleccionado: $selectedYear")
            },
            year,
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        yearPickerDialog.datePicker.findViewById<View>(resources.getIdentifier("android:id/day", null, null))?.visibility = View.GONE
        yearPickerDialog.datePicker.findViewById<View>(resources.getIdentifier("android:id/month", null, null))?.visibility = View.GONE
        yearPickerDialog.show()
    }

    private fun tomarFoto(position: Int) {
        Log.d("PresupuestoFragment", "Attempting to take photo for position $position.")
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val photoFile: File? = try {
                crearArchivoTemporal(fotoList[position].tipoFoto)
            } catch (ex: IOException) {
                Log.e("PresupuestoFragment", "Error al crear el archivo temporal para la foto: ${ex.message}", ex)
                Toast.makeText(requireContext(), "Error al crear el archivo", Toast.LENGTH_SHORT).show()
                null
            }
            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    it
                )
                currentPhotoUri = photoURI
                takePhotoLauncher.launch(photoURI)
                Log.d("PresupuestoFragment", "Launched camera for URI: $photoURI")
            }
        } else {
            Log.d("PresupuestoFragment", "Requesting camera permission.")
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun subirFoto(position: Int) {
        Log.d("PresupuestoFragment", "Attempting to upload photo for position $position.")
        uploadPhotoLauncher.launch("image/*")
    }

    private fun crearArchivoTemporal(tipoFoto: String): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(null)
        val file = File.createTempFile(
            "JPEG_${tipoFoto}_${timeStamp}_",
            ".jpg",
            storageDir
        )
        Log.d("PresupuestoFragment", "Archivo temporal creado: ${file.absolutePath}")
        return file
    }

    private fun configurarSpinnerTipoVehiculo() {
        val tiposVehiculo = arrayOf(
            "Seleccione un tipo de vehículo...",
            "Automóvil", "Camioneta", "Camión", "Motocicleta", "Bus", "Tractor", "Remolque", "Otros"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tiposVehiculo)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipoVehiculo.adapter = adapter
        Log.d("PresupuestoFragment", "Spinner Tipo Vehículo configurado.")
    }

    private fun configurarSpinnerMarcasVehiculosEcuador() {
        val marcas = modelosPorMarca.keys.toTypedArray()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, marcas)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMarcaVehiculo.adapter = adapter

        spinnerMarcaVehiculo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val marcaSeleccionada = parent?.getItemAtPosition(position).toString()
                configurarSpinnerModeloVehiculo(marcaSeleccionada)
                Log.d("PresupuestoFragment", "Marca seleccionada: $marcaSeleccionada")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No hacer nada
            }
        }
        Log.d("PresupuestoFragment", "Spinner Marcas Vehículos configurado.")
    }

    private fun configurarSpinnerModeloVehiculo(marca: String) {
        val modelos = modelosPorMarca[marca] ?: arrayOf("Seleccione un modelo...")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, modelos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerModeloVehiculo.adapter = adapter
        Log.d("PresupuestoFragment", "Spinner Modelo Vehículo configurado para marca: $marca")
    }

    private fun configurarSpinnerVehicleColor() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, colors)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVehicleColor.adapter = adapter
        Log.d("PresupuestoFragment", "Spinner Vehicle Color configurado.")
    }

    private fun configurarSpinnerCountry() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, countries)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCountry.adapter = adapter

        spinnerCountry.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val countrySelected = parent?.getItemAtPosition(position).toString()
                configurarSpinnerState(countrySelected)
                Log.d("PresupuestoFragment", "País seleccionado: $countrySelected")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        Log.d("PresupuestoFragment", "Spinner Country configurado.")
    }

    private fun configurarSpinnerState(country: String) {
        val states = statesByCountry[country] ?: arrayOf("Seleccione un estado...")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, states)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerState.adapter = adapter

        spinnerState.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val stateSelected = parent?.getItemAtPosition(position).toString()
                configurarSpinnerCity(stateSelected)
                Log.d("PresupuestoFragment", "Estado seleccionado: $stateSelected")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        Log.d("PresupuestoFragment", "Spinner State configurado para país: $country")
    }

    private fun configurarSpinnerCity(state: String) {
        val cities = citiesByState[state] ?: arrayOf("Seleccione una ciudad...")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, cities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCity.adapter = adapter
        Log.d("PresupuestoFragment", "Spinner City configurado para estado: $state")
    }

    private fun procesarDatos() {
        Log.d("PresupuestoFragment", "procesarDatos() called. Iniciando solicitud de red.")
        val jsonObject = JSONObject().apply {
            put("CaseNumber", etCaseNumber.text.toString())
            put("FullName", etFullName.text.toString())
            put("Email", etEmail.text.toString())
            put("VINnumber", etVINnumber.text.toString())
            put("DateOfInspection", etDateOfInspection.text.toString())
            put("VehicleType", spinnerTipoVehiculo.selectedItem.toString())
            put("VehicleMake", spinnerMarcaVehiculo.selectedItem.toString())
            put("VehicleModel", spinnerModeloVehiculo.selectedItem.toString())
            put("VehicleYear", etVehicleYear.text.toString())
            put("VehicleColor", spinnerVehicleColor.selectedItem.toString())
            put("Country", spinnerCountry.selectedItem.toString())
            put("State", spinnerState.selectedItem.toString())
            put("City", spinnerCity.selectedItem.toString())
        }
        Log.d("PresupuestoFragment", "JSON de datos de caso preparado: ${jsonObject.toString(2)}")

        val client = OkHttpClient()
        val requestBodyBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("json", jsonObject.toString())

        fotoList.forEachIndexed { index, fotoItem ->
            fotoItem.imagenUri?.let { uri ->
                val file = obtenerArchivoDeUri(uri, fotoItem.tipoFoto)
                if (file != null) {
                    requestBodyBuilder.addFormDataPart(
                        "photo${index + 1}",
                        "${fotoItem.tipoFoto}_${index + 1}.jpg",
                        file.asRequestBody("image/jpeg".toMediaType())
                    )
                    Log.d("PresupuestoFragment", "Agregando foto ${index + 1} (${fotoItem.tipoFoto}) al cuerpo de la solicitud.")
                } else {
                    Log.e("PresupuestoFragment", "El archivo para la foto ${index + 1} es nulo después de obtenerlo de la URI.")
                }
            } ?: Log.w("PresupuestoFragment", "URI de foto nula para el ítem en el índice $index. Saltando esta foto.")
        }

        val request = Request.Builder()
            .url(BASE_URL + API_submit_case)
            .post(requestBodyBuilder.build())
            .build()
        Log.d("PresupuestoFragment", "Solicitud OkHttp construida. URL: ${request.url}")

        CoroutineScope(Dispatchers.IO).launch {
            Log.d("PresupuestoFragment", "Encolando llamada OkHttp en CoroutineScope(Dispatchers.IO).")
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("PresupuestoFragment", "FALLO de la llamada OkHttp: ${e.message}", e) // Log antes de lanzar corutina
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(requireContext(), "Error en la conexión al enviar datos del caso: ${e.message}", Toast.LENGTH_LONG).show()
                        Log.e("PresupuestoFragment", "Error en la conexión al enviar datos del caso (UI Thread)", e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.d("PresupuestoFragment", "RESPUESTA de la llamada OkHttp. Código: ${response.code}. Mensaje: ${response.message}") // Log antes de lanzar corutina
                    CoroutineScope(Dispatchers.Main).launch {
                        if (response.isSuccessful) {
                            val responseBody = response.body?.string()
                            Log.d("PresupuestoFragment", "Cuerpo de respuesta exitosa: $responseBody") // Log del cuerpo
                            val jsonResponse = responseBody?.let {
                                try {
                                    JSONObject(it)
                                } catch (e: JSONException) {
                                    Log.e("PresupuestoFragment", "Error al analizar la respuesta JSON: $it", e)
                                    null
                                }
                            }
                            caseToken = jsonResponse?.optString("case_token")
                            val message = jsonResponse?.optString("message", "Datos enviados correctamente")

                            Toast.makeText(requireContext(), "Éxito: $message", Toast.LENGTH_LONG).show()
                            Log.d("PresupuestoFragment", "Respuesta exitosa del caso: $responseBody")

                            //generateDamageReport() // Envia a la API

                            showDialog("Datos enviados correctamente. Token de caso: $caseToken","Información")
                        } else {
                            val errorBody = response.body?.string()
                            Toast.makeText(requireContext(), "Error al enviar datos del caso: ${response.code} - ${response.message}", Toast.LENGTH_LONG).show()
                            Log.e("PresupuestoFragment", "Error al enviar datos del caso: ${response.code} - ${response.message}. Body: $errorBody")
                        }
                    }
                }
            })
        }
    }

    private fun generateDamageReport() {
        Log.d("GeminiReport", "generateDamageReport() called. Preparando prompt e imágenes.")

        // MOSTRAR EL PROGRESS BAR Y DESHABILITAR EL BOTÓN ANTES DE LA LLAMADA A LA API
        progressBar.visibility = VISIBLE
        btnAceptar.isEnabled = false
        btnCancelar.isEnabled = false

        val vehicleData = mapOf(
            "marca" to spinnerMarcaVehiculo.selectedItem.toString(),
            "modelo" to spinnerModeloVehiculo.selectedItem.toString(),
            "anio" to etVehicleYear.text.toString(),
            "ubicacion" to spinnerCity.selectedItem.toString() + ", " + spinnerCountry.selectedItem.toString(),
            "color" to spinnerVehicleColor.selectedItem.toString()
        )

        val imagesForGemini = mutableListOf<Bitmap>()
        fotoList.forEach { fotoItem ->
            fotoItem.imagenUri?.let { uri ->
                uriToBitmap(uri)?.let { bitmap ->
                    imagesForGemini.add(bitmap)
                    Log.d("GeminiReport", "Imagen agregada para Gemini desde URI: $uri")
                } ?: Log.e("GeminiReport", "No se pudo convertir URI a Bitmap para Gemini: $uri")
            } ?: Log.w("GeminiReport", "URI de imagen nula para Gemini.")
        }

        if (imagesForGemini.isEmpty()) {
            // OCULTAR EL PROGRESS BAR Y HABILITAR EL BOTÓN SI NO HAY IMÁGENES
            progressBar.visibility = GONE
            btnAceptar.isEnabled = true
            btnCancelar.isEnabled = true

            Toast.makeText(requireContext(), "No se encontraron imágenes válidas para generar el informe de daños.", Toast.LENGTH_SHORT).show()
            Log.w("GeminiReport", "No hay imágenes disponibles para la generación del informe de Gemini.")
            return
        }

        val costoHoraManoObra = 20.0 // Asegúrate de que sea Double
        val promptText = """
            Eres un perito automotriz profesional especializado en valoración de daños de vehículos. Tu tarea es generar un informe detallado de daños, indicando si hay partes para reemplazar y reparar para un vehículo chocado.

            Información del vehículo:
            - Marca: ${vehicleData["marca"]}
            - Modelo: ${vehicleData["modelo"]}
            - Año: ${vehicleData["anio"]}
            - Color: ${vehicleData["color"]}
            - Ubicación de Valoración: ${vehicleData["ubicacion"]}
            - Costo por hora de mano de obra: $costoHoraManoObra

            Analiza exhaustivamente las imágenes proporcionadas para identificar todos los daños visibles en la carrocería, estructura y componentes mecánicos.

            El informe debe contener las siguientes secciones estructuradas en formato JSON:
            1.  **"DatosGenerales"**: Con la información del vehículo y el costo por hora de mano de obra utilizada.
            2.  **"DescripcionDanosExistentes"**: Una descripción detallada de los daños por zona (ej. "Parte Trasera Izquierda").
            3.  **"ListadoPiezasAfectadas"**: Una lista de componentes que necesitan reemplazo o reparación, con la acción sugerida en una clave llamada suguerencia, 
                un promedio de horas estimadas para la mano de obra en una clave llamada CantidadEstimadoManoObra a un costo de $costoHoraManoObra por hora de trabajo, 
                desglosado por tareas (desmontaje, reparación estructural, montaje, pintura, etc.), un promedio de costo estimado en USD para las piezas a reemplazar en una clave llamada CostoPieza, 
                ajustado a la ubicación de ${vehicleData["ubicacion"]} y notas..
            4.  **"ConsideracionesAdicionales"**: Puntos importantes a tener en cuenta (daños ocultos, etc.).

            Genera el JSON completo sin ninguna explicación adicional antes o después del código. Asegúrate de que el JSON sea válido.
        """.trimIndent()
        Log.d("GeminiReport", "Prompt de Gemini preparado.")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("GeminiReport", "Llamando a geminiModel.generateContent().")
                val content = content {
                    text(promptText)
                    imagesForGemini.forEach { image(it) }
                }

                val response = geminiModel.generateContent(content)
                Log.d("GeminiReport", "Respuesta recibida de la API de Gemini.")

                withContext(Dispatchers.Main) {
                    // OCULTAR EL PROGRESS BAR Y HABILITAR EL BOTÓN DESPUÉS DE LA RESPUESTA
                    progressBar.visibility = GONE
                    btnAceptar.isEnabled = true
                    btnCancelar.isEnabled = true

                    try {
                        val jsonOutput = response.text
                        Log.d("GeminiReport", "Raw Gemini response text: $jsonOutput")
                        val cleanJsonOutput = if (jsonOutput != null && "```json" in jsonOutput) {
                            jsonOutput.substringAfter("```json").substringBefore("```").trim()
                        } else {
                            jsonOutput?.trim()
                        }
                        Log.d("GeminiReport", "Cleaned Gemini JSON output: $cleanJsonOutput")

                        if (!cleanJsonOutput.isNullOrEmpty()) {
                            val reportJson = JSONObject(cleanJsonOutput)
                            Log.d("GeminiReport", "Informe generado por Gemini:\n${reportJson.toString(2)}")

                            // Información común del usuario y vehículo
                            val commonInfo = """
                                --- Información General ---
                                Número de Caso: ${etCaseNumber.text}
                                Fecha de Inspección: ${etDateOfInspection.text}
                                --- Información Vehiculo ---
                                Marca: ${vehicleData["marca"]}
                                Modelo: ${vehicleData["modelo"]}
                                Número de VIN: ${etVINnumber.text}
                                Año: ${vehicleData["anio"]}
                                Color: ${vehicleData["color"]}
                                --- Ubicación ---
                                Ubicación de Valoración: ${vehicleData["ubicacion"]}
                                --- Información Inspector ---
                                Nombre Completo: ${etFullName.text}
                                Email: ${etEmail.text}
                                --- Información de Costos ---
                                Costo por hora de mano de obra: $costoHoraManoObra
                                Fotos adjuntas: ${imagesForGemini.size}
                                ---------------------------
                            """.trimIndent()

                            // El showDialog original con el JSON completo
                            showDialog("Informe de Daños Generado (JSON Completo):\n${reportJson.toString(2)}",reportJson.toString(2))

                            val bundle = Bundle().apply {
                                putString("input_data", commonInfo) // Tus datos de entrada
                                putString("api_response", reportJson.toString(2)) // La respuesta JSON de la API
                            }
                            findNavController().navigate(R.id.action_nav_presupuestofragment_to_reportDisplayFragment, bundle)
                            //limpiarFormulario()

                        } else {
                            Toast.makeText(requireContext(), "Gemini no generó un JSON válido para el informe de daños.", Toast.LENGTH_LONG).show()
                            Log.e("GeminiReport", "La respuesta de Gemini estaba vacía o no era un JSON válido: $jsonOutput")
                        }
                    } catch (e: JSONException) {
                        Toast.makeText(requireContext(), "Error al decodificar la respuesta JSON de Gemini: ${e.message}", Toast.LENGTH_LONG).show()
                        Log.e("GeminiReport", "Error de análisis JSON en la respuesta de Gemini: ${response.text}", e)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error al procesar la respuesta de Gemini: ${e.message}", Toast.LENGTH_LONG).show()
                        Log.e("GeminiReport", "Error al procesar la respuesta de Gemini: ${response.text}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiReport", "Excepción durante la llamada a la API de Gemini: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    // OCULTAR EL PROGRESS BAR Y HABILITAR EL BOTÓN EN CASO DE ERROR DE CONEXIÓN
                    progressBar.visibility = GONE
                    btnAceptar.isEnabled = true
                    btnCancelar.isEnabled = true

                    Toast.makeText(requireContext(), "Error al conectar con la API de Gemini: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("GeminiReport", "Error al conectar con la API de Gemini (UI Thread)", e)
                }
            }
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e("PresupuestoFragment", "Error al convertir URI a Bitmap para Gemini: ${e.message}", e)
            null
        }
    }

    private fun obtenerArchivoDeUri(uri: Uri, tipoFoto: String): File? {
        return try {
            val photoFile = crearArchivoTemporal(tipoFoto)
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                photoFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
            }
            Log.d("PresupuestoFragment", "Archivo obtenido de URI: ${photoFile.absolutePath}")
            photoFile
        } catch (e: Exception) {
            Log.e("PresupuestoFragment", "Error al convertir URI a archivo: ${e.message}", e)
            null
        }
    }

    private fun bitmapToFile(context: Context, bitmap: Bitmap, fileName: String): File? {
        val file = File(context.cacheDir, fileName) // Usar cacheDir para archivos temporales
        return try {
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) // Comprimir con calidad 90
            }
            Log.d("PresupuestoFragment", "Bitmap convertido a archivo: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("PresupuestoFragment", "Error al convertir Bitmap a archivo: ${e.message}", e)
            null
        }
    }

    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title) // Usar el título pasado como parámetro
            .setMessage(message)
            .setIcon(R.drawable.analyze_list_logs_search_icon)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
        Log.d("PresupuestoFragment", "Diálogo de información '$title' mostrado.")
    }
}