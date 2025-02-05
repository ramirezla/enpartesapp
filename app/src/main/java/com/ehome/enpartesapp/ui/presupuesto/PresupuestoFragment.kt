package com.ehome.enpartesapp.ui.presupuesto

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.icu.util.Calendar
import android.provider.MediaStore
import android.util.Log
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
import androidx.recyclerview.widget.RecyclerView
import com.ehome.enpartesapp.R
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
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 * Que datos son obligatorios?
 * Obligatorio minimo 1 foto?
 * Se puede enviar varias fotos del mismo tipo?
 */

// Creamos un modelo de datos para representar cada línea de fotos.
data class FotoItem(
    var imagenUri: Uri? = null, // URI de la imagen (tomada o subida)
    var tipoFoto: String = "",  // Clave del tipo de foto seleccionada
    var isFotoTomada: Boolean = false // Indica si la foto fue tomada o subida
)

// Implementamos un RecyclerView.Adapter para manejar la lista de FotoItem.
class FotoAdapter(
    private val context: Context,
    private val fotoList: MutableList<FotoItem>,
    private val onAddClickListener: () -> Unit,
    private val onDeleteClickListener: (Int) -> Unit,
    private val onTakePhotoClickListener: (Int) -> Unit,
    private val onUploadPhotoClickListener: (Int) -> Unit
) : RecyclerView.Adapter<FotoAdapter.FotoViewHolder>() {

    var currentPosition: Int = -1 // Posición actual

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_foto, parent, false)
        return FotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        val fotoItem = fotoList[position]

        // Configurar el Spinner
        val tiposFoto = context.resources.getStringArray(R.array.tipos_foto)
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, tiposFoto)
        holder.spinnerTipoFoto.adapter = adapter

        // Seleccionar el tipo de foto actual
        val selectedIndex = tiposFoto.indexOf(fotoItem.tipoFoto)
        if (selectedIndex >= 0) {
            holder.spinnerTipoFoto.setSelection(selectedIndex)
        }

        // Listener para cambios en el Spinner
        holder.spinnerTipoFoto.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                fotoItem.tipoFoto = tiposFoto[pos]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Configurar los botones de tomar/subir foto
        holder.btnTomarFoto.setOnClickListener {
            currentPosition = position
            onTakePhotoClickListener(position)
        }

        holder.btnSubirFoto.setOnClickListener {
            currentPosition = position
            onUploadPhotoClickListener(position)
        }

        // Mostrar la imagen de referencia
        if (fotoItem.imagenUri != null) {
            holder.imgFoto.setImageURI(fotoItem.imagenUri)
        } else {
            holder.imgFoto.setImageResource(R.drawable.ic_menu_gallery)
        }

        // Configurar el botón de borrar
        holder.btnBorrar.setOnClickListener {
            onDeleteClickListener(position)
        }

        // Configurar el botón de agregar
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

    //private val fotoList = mutableListOf(FotoItem())
    private var fotoList: MutableList<FotoItem> = mutableListOf()
    private var caseToken: String? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FotoAdapter
    private var currentPhotoUri: Uri? = null // URI de la foto actual

    // Variables para la línea del VIN Number
    private lateinit var btnTomarFotoVin: ImageButton
    private lateinit var btnSubirFotoVin: ImageButton
    private lateinit var imgFotoVin: ImageView
    private lateinit var btnBorrarVin: ImageButton
    private lateinit var spinnerTipoFotoVin: Spinner // Declarar el Spinner
    private lateinit var spinnerTipoVehiculo: Spinner

    // ActivityResultLauncher para subir la foto del VIN Number
    private lateinit var uploadVinNumberLauncher: ActivityResultLauncher<String>
    private lateinit var takeVinNumberLauncher: ActivityResultLauncher<Uri>

    // Declara los ActivityResultLauncher
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Uri>
    private lateinit var uploadPhotoLauncher: ActivityResultLauncher<String>

    private lateinit var etCaseNumber: TextInputEditText
    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etVINnumber: TextInputEditText
    private lateinit var etLanguage: TextInputEditText
    private lateinit var etDateOfInspection: TextInputEditText

    // Botones de cancelar y aceptar
    private lateinit var btnCancelar: Button
    private lateinit var btnAceptar: Button

    // Variable para almacenar el caseToken
    //private var caseToken: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_presupuesto, container, false)

        // Referencias a los botones
        btnCancelar = view.findViewById(R.id.btnCancelar)
        btnAceptar = view.findViewById(R.id.btnAceptar)

        // Configurar el clic del botón Cancelar
        btnCancelar.setOnClickListener {
            limpiarFormulario()
        }

        // Configurar el clic del botón Aceptar
        btnAceptar.setOnClickListener {
            validarYProcesarDatos()
        }

        // Referencia al campo de fecha
        etDateOfInspection = view.findViewById(R.id.etDateOfInspection)

        // Configurar el DatePicker para el campo de fecha
        etDateOfInspection.setOnClickListener {
            mostrarDatePicker()
        }

        // Referencias a los campos
        etCaseNumber = view.findViewById(R.id.etCaseNumber)
        etFullName = view.findViewById(R.id.etFullName)
        etEmail = view.findViewById(R.id.etEmail)
        etDateOfInspection = view.findViewById(R.id.etDateOfInspection)
        etVINnumber = view.findViewById(R.id.etVINnumber)
        etLanguage = view.findViewById(R.id.etLanguage)
        spinnerTipoVehiculo = view.findViewById(R.id.spinnerTipoVehiculo)
        btnCancelar = view.findViewById(R.id.btnCancelar)
        btnAceptar = view.findViewById(R.id.btnAceptar)
        recyclerView = view.findViewById(R.id.recyclerView)

        // Inicializar vistas del VIN Number
        btnTomarFotoVin = view.findViewById(R.id.btnTomarFotoVin)
        btnSubirFotoVin = view.findViewById(R.id.btnSubirFotoVin)
        imgFotoVin = view.findViewById(R.id.imgFotoVin)
        btnBorrarVin = view.findViewById(R.id.btnBorrarVin)
        spinnerTipoFotoVin = view.findViewById(R.id.spinnerTipoFotoVin) // Inicializar el Spinner
        spinnerTipoVehiculo = view.findViewById(R.id.spinnerTipoVehiculo)

        // Configurar los Spinners
        configurarSpinnerTipoVehiculo()
        configurarSpinnerVinNumber() // Configurar el Spinner del VIN Number

        // Inicializar el RecyclerView y el adaptador
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = FotoAdapter(
            requireContext(),
            fotoList,
            onAddClickListener = { agregarLinea() },
            onDeleteClickListener = { borrarLinea(it) },
            onTakePhotoClickListener = { position -> tomarFoto(position) },
            onUploadPhotoClickListener = { position -> subirFoto(position) }
        )
        recyclerView.adapter = adapter

        // Inicializar los ActivityResultLauncher para el VIN Number
        uploadVinNumberLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                imgFotoVin.setImageURI(uri) // Mostrar la foto en el ImageView
            }
        }

        takeVinNumberLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentPhotoUri?.let { uri ->
                    imgFotoVin.setImageURI(uri) // Mostrar la foto en el ImageView
                }
            }
        }

        // Configurar los botones del VIN Number
        btnTomarFotoVin.setOnClickListener {
            tomarFotoVinNumber()
        }

        btnSubirFotoVin.setOnClickListener {
            uploadVinNumberLauncher.launch("image/*")
        }

        btnBorrarVin.setOnClickListener {
            imgFotoVin.setImageResource(android.R.drawable.ic_menu_gallery) // Restablecer la imagen
        }

        // Inicializa los ActivityResultLauncher
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

        return view
    }

    private fun limpiarFormulario() {
        // Limpiar los campos de texto
        etCaseNumber.text?.clear()
        etFullName.text?.clear()
        etEmail.text?.clear()
        etDateOfInspection.text?.clear()
        etVINnumber.text?.clear()
        etLanguage.text?.clear()

        // Reiniciar el Spinner
        spinnerTipoVehiculo.setSelection(0)
        spinnerTipoFotoVin.setSelection(0)

        // Limpiar la lista de fotos
        fotoList.clear()
        fotoList.add(FotoItem()) // Agregar una línea vacía inicial
        adapter.notifyDataSetChanged() // Notificar al adaptador que los datos han cambiado

        Toast.makeText(requireContext(), "Formulario limpiado", Toast.LENGTH_SHORT).show()
    }

    private fun validarYProcesarDatos() {
        // Validar que todos los campos estén completos
        if (validarCampos() && validarFotoVin() && validarFotosVehiculo()) {
            // Procesar los datos (guardar en base de datos, enviar a servidor, etc.)
            procesarDatos()
        } else {
            Toast.makeText(requireContext(), "Por favor, complete todos los campos y agregue al menos una foto del VIN y una foto del vehículo", Toast.LENGTH_SHORT).show()
        }
    }

//    private fun validarCampos(): Boolean {
//        // Validar que todos los campos de texto estén llenos
//        if (etCaseNumber.text.isNullOrEmpty() ||
//            etFullName.text.isNullOrEmpty() ||
//            etEmail.text.isNullOrEmpty() ||
//            etDateOfInspection.text.isNullOrEmpty() ||
//            etVINnumber.text.isNullOrEmpty() ||
//            etLanguage.text.isNullOrEmpty()) {
//            return false
//        }
//        return true
//    }

    private fun validarCampos(): Boolean {
        val campos = listOf(
            etCaseNumber.text,
            etFullName.text,
            etEmail.text,
            etDateOfInspection.text,
            etVINnumber.text,
            etLanguage.text
        )
        return campos.all { !it.isNullOrEmpty() }
    }

    private fun validarFotoVin(): Boolean {
        // Validar que se haya agregado una foto del VIN
        val defaultDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_menu_gallery)?.constantState
        return imgFotoVin.drawable.constantState != defaultDrawable
    }

//    private fun validarFotosVehiculo(): Boolean {
//        // Validar que se haya agregado al menos una foto del vehículo
//        if (fotoList.any { it.imagenUri != null }) {
//            // Si hay al menos una foto, puedes agregar más validaciones aquí
//            // Por ejemplo, si necesitas que haya una foto de cada tipo:
//            //val tiposDeFoto = listOf("front", "back", "left", "right") // Agrega los tipos que necesitas
//            //return tiposDeFoto.all { tipo -> fotoList.any { it.tipoFoto == tipo && it.imagenUri != null } }
//            val tiposDeFoto = listOf("front_left",
//                "front",
//                "front_right",
//                "left",
//                "right",
//                "back_left",
//                "back_right",
//                "back",
//                "front_side_grilled",
//                "front_side_hood",
//                "front_side_left_bumper",
//                "front_side_left_head_light",
//                "front_side_right_bumper",
//                "front_side_right_head_light",
//                "front_side_windshield",
//                "left_side_fender",
//                "left_side_front_left_door",
//                "left_side_front_left_tyre",
//                "left_side_front_left_window",
//                "left_side_quarter_panel",
//                "left_side_rear_left_door")
//            return tiposDeFoto.all { tipo -> fotoList.any { it.tipoFoto == tipo && it.imagenUri != null } }
//        }
//        return false
//    }

//    private fun validarFotosVehiculo(): Boolean {
//        // Validar que se haya agregado al menos una foto del vehículo
//        if (fotoList.any { it.imagenUri != null }) {
//            // Si hay al menos una foto, puedes agregar más validaciones aquí
//            // Por ejemplo, si necesitas que haya una foto de cada tipo:
//            //val tiposDeFoto = listOf("front", "back", "left", "right") // Agrega los tipos que necesitas
//            //return tiposDeFoto.all { tipo -> fotoList.any { it.tipoFoto == tipo && it.imagenUri != null } }
//            return true
//        }
//        return false
//    }

    private fun validarFotosVehiculo(): Boolean {
        val tiposDeFoto = listOf("front_left",
            "front",
            "front_right",
            "left",
            "right",
            "back_left",
            "back_right",
            "back",
            "front_side_grilled",
            "front_side_hood",
            "front_side_left_bumper",
            "front_side_left_head_light",
            "front_side_right_bumper",
            "front_side_right_head_light",
            "front_side_windshield",
            "left_side_fender",
            "left_side_front_left_door",
            "left_side_front_left_tyre",
            "left_side_front_left_window",
            "left_side_quarter_panel",
            "left_side_rear_left_door") // Agrega los tipos que necesitas
        // Retorna true si hay al menos una foto con imagenUri no nulo y si se cumplen los tipos de fotos, false de lo contrario
        return fotoList.any { it.imagenUri != null } && tiposDeFoto.all { tipo -> fotoList.any { it.tipoFoto == tipo && it.imagenUri != null } }
    }

    private fun procesarDatos() {
        // Obtener los datos de los campos
        val caseNumber = etCaseNumber.text.toString()
        val fullName = etFullName.text.toString()
        val email = etEmail.text.toString()
        val dateOfInspection = etDateOfInspection.text.toString()
        val vinNumber= etVINnumber.text.toString()
        val language = etLanguage.text.toString()

        if (caseNumber.isEmpty() || fullName.isEmpty() || email.isEmpty() || dateOfInspection.isEmpty() || vinNumber.isEmpty() || language.isEmpty()) {
            showDialog("Todos los campos son obligatorios.")
            return
        }

        // Format the date by replacing "/" with "."
        val fechaDeInspection = dateOfInspection.replace("/", ".")

        // Crear el JSON
        val jsonObject = JSONObject().apply {
            put("Casenumber", caseNumber)
            put("FullName", fullName)
            put("Email", email)
            put("Dateofinspection", fechaDeInspection)  // Se debe formatear como: 30.01.2025
            put("VINnumber", vinNumber)
            put("Language", language)
        }

        // Convertir el JSON a String
        val jsonString = jsonObject.toString()

        // URL de la API
        val url = "http://209.126.106.199/solmovsa/ApiGestorSiniestros/api/MotionsCloud/submit-case"

        // Crear el cliente OkHttp
        val client = OkHttpClient()

        // Crear el cuerpo de la solicitud
        val mediaType = "application/json".toMediaType()
        val body = jsonString.toRequestBody(mediaType)

        // Crear la solicitud
        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        // Ejecutar la solicitud en un hilo secundario
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        // La solicitud fue exitosa (código 200)
                        val jsonResponse = JSONObject(responseBody ?: "")
                        if (jsonResponse.has("caseToken")) {
                            val caseTokenObject = jsonResponse.getJSONObject("caseToken")
                            caseToken = caseTokenObject.getString("caseTokenValue")

                            // Llamar a la función para enviar las fotos
                            // Crear una variable local para evitar problemas de smart cast
                            val token = caseToken
                            //val token = "GG87jCWGoAqAoykNLxCgYZQ7"  // borrar luego de pasar las pruebas
                            if (token != null) {
                                //showConfirmationDialog(token)
                                enviarFotosAlServidor(token)
                                showDialog("Datos enviados correctamente. caseToken: $caseToken")
                            } else {
                                Toast.makeText(requireContext(), "Error: caseToken es nulo", Toast.LENGTH_SHORT).show()
                            }
                            limpiarFormulario()
                        } else {
                            // La respuesta no contiene caseToken
                            Toast.makeText(
                                requireContext(),
                                "Respuesta del servidor incompleta",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.e("PresupuestoFragment", "Respuesta del servidor incompleta: $responseBody")
                        }
                    } else {
                        // La solicitud falló (códigos 400, 500, etc.)
                        val errorMessage = when (response.code) {
                            400 -> "Datos inválidos"
                            500 -> "Error interno del servidor"
                            else -> "Error al enviar los datos: ${response.code}"
                        }
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                        Log.e(
                            "PresupuestoFragment",
                            "Error en la solicitud: ${response.code} - $responseBody"
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Ocurrió un error
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("PresupuestoFragment", "Error: ${e.message}", e)
                }
            }
        }
    }

//    private fun showConfirmationDialog(token: String) {
//        val builder = AlertDialog.Builder(requireContext())
//        builder.setTitle("Confirmar envío de fotos")
//
//        // Crear un mensaje con la información de las fotosval message = StringBuilder()
//        val message = StringBuilder()
//        message.append("Fotos a enviar:\n")
//        message.append("--------------------\n")
//
//        // Agregar la foto del VIN
//        if (imgFotoVin.drawable != null) {
//            message.append("vin_number: vin_number.jpg\n")
//        }
//
//        // Agregar las fotos del vehículo
//        fotoList.forEachIndexed { index, fotoItem ->
//            if (fotoItem.imagenUri != null) {
//                message.append("${fotoItem.tipoFoto}: foto_$index.jpg\n")
//            }}
//
//        // Configurar el mensaje del diálogo
//        builder.setMessage(message.toString())
//
//        // Configurar el botón positivo (Enviar)
//        builder.setPositiveButton("Enviar") { dialog, _ ->
//            // Enviar las fotos al servidor
//            enviarFotosAlServidor(token)
//            dialog.dismiss()
//        }
//
//        // Configurar el botón negativo (Cancelar)
//        builder.setNegativeButton("Cancelar") { dialog, _ ->
//            // Cancelar el envío
//            dialog.dismiss()
//        }
//
//        // Mostrar el diálogo
//        builder.show()
//    }

    private fun mostrarDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                etDateOfInspection.setText(selectedDate)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun tomarFotoVinNumber() {
        val photoFile: File? = try {
            crearArchivoTemporal("vin_number")
        } catch (ex: IOException) {
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
            takeVinNumberLauncher.launch(photoURI)
        }
    }

    private fun configurarSpinnerVinNumber() {
        val tiposFoto = arrayOf("vin_number") // Solo un valor por ahora
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tiposFoto) // Especifica el tipo <String>
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipoFotoVin.adapter = adapter
    }

    private fun configurarSpinnerTipoVehiculo() {
        val tiposVehiculo = arrayOf("Sedan", "SUV", "Camioneta", "Motocicleta", "Otro")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tiposVehiculo)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipoVehiculo.adapter = adapter

        // Listener para manejar la selección del tipo de vehículo
        spinnerTipoVehiculo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                //val tipoSeleccionado = tiposVehiculo[position]
                // Aquí puedes guardar el tipo de vehículo seleccionado si es necesario
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun agregarLinea() {
        fotoList.add(FotoItem()) // Añade un nuevo FotoItem a la lista
        adapter.notifyItemInserted(fotoList.size - 1) // Notifica al adaptador que se ha añadido un nuevo elemento
    }

    private fun borrarLinea(position: Int) {
        if (fotoList.size > 1) { // Asegúrate de que siempre haya al menos un elemento en la lista
            fotoList.removeAt(position) // Elimina el elemento en la posición especificada
            adapter.notifyItemRemoved(position) // Notifica al adaptador que se ha eliminado un elemento
        } else {
            Toast.makeText(requireContext(), "Debe haber al menos una línea", Toast.LENGTH_SHORT).show()
        }
    }

    // Vamos a crear un archivo con un nombre específico basado en el tipo de foto.
    // TODO: crear el nombre del vin_number asignandole un timestamp para evitar colisiones
    private fun crearArchivoTemporal(tipoFoto: String): File {
        val storageDir: File? = requireContext().getExternalFilesDir(null)
        val nombreArchivo = when (tipoFoto) {
            "vin_number" -> "vin_number.jpg"
            else -> "${tipoFoto}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
        }
        return File(storageDir, nombreArchivo)
    }

    private fun tomarFoto(position: Int) {
        val tipoFoto = fotoList[position].tipoFoto
        val photoFile: File? = try {
            crearArchivoTemporal(tipoFoto)
        } catch (ex: IOException) {
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
            takePhotoLauncher.launch(photoURI) // Usar el launcher para tomar la foto
        }
    }

    private fun subirFoto(position: Int) {
        uploadPhotoLauncher.launch("image/*") // Usar el launcher para subir la imagen
    }

    // usar el método enqueue() en lugar de execute() para realizar la llamada de
    // forma asíncrona. enqueue() no bloquea el hilo y utiliza un callback para
    // manejar la respuesta cuando esté disponible.
    private fun enviarFotosAlServidor(caseToken: String) {
        // Obtener el tipo de vehículo seleccionado
        val carType = spinnerTipoVehiculo.selectedItem.toString()

        // Crear el cuerpo de la solicitud multipart/form-data
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("car_type", carType) // Campo obligatorio: tipo de vehículo
            .apply {
                // Agregar la foto del VIN
                if (imgFotoVin.drawable is BitmapDrawable) {
                    val vinBitmap = (imgFotoVin.drawable as BitmapDrawable).bitmap
                    val vinPhotoFile = obtenerArchivoDeUri(getImageUri(requireContext(), vinBitmap), "vin_number")
                    if (vinPhotoFile != null) {
                        addFormDataPart(
                            "vin_number", // Clave para la foto del VIN
                            "vin_number.jpg", // Nombre del archivo
                            vinPhotoFile.asRequestBody("image/jpeg".toMediaType())
                        )
                    }
                }

                // Agregar las fotos del vehículo
//                fotoList.forEachIndexed { index, fotoItem ->
//                    if (fotoItem.imagenUri != null) {
//                        val photoFile = obtenerArchivoDeUri(fotoItem.imagenUri!!, fotoItem.tipoFoto)
//                        if (photoFile != null) {
//                            addFormDataPart(
//                                fotoItem.tipoFoto, // Clave para la foto (ej: front, left, right)
//                                photoFile.name, // Nombre del archivo
//                                photoFile.asRequestBody("image/jpeg".toMediaType())
//                            )
//                        }
//                    }
//                }

//                El índice no se va a usar.
//                fotoList.forEachIndexed { _, fotoItem ->
//                    if (fotoItem.imagenUri != null) {
//                        val photoFile = obtenerArchivoDeUri(fotoItem.imagenUri!!, fotoItem.tipoFoto)
//                        if (photoFile != null) {
//                            addFormDataPart(
//                                fotoItem.tipoFoto, // Clave para la foto (ej: front, left, right)
//                                photoFile.name, // Nombre del archivo
//                                photoFile.asRequestBody("image/jpeg".toMediaType())
//                            )
//                        }
//                    }
//                }

                // iterar sobre una lista cuando solo necesitas el elemento y no el índice.
                fotoList.forEach { fotoItem ->
                    if (fotoItem.imagenUri != null) {
                        val photoFile = obtenerArchivoDeUri(fotoItem.imagenUri!!, fotoItem.tipoFoto)
                        if (photoFile != null) {
                            addFormDataPart(
                                fotoItem.tipoFoto, // Clave para la foto (ej: front, left, right)
                                photoFile.name, // Nombre del archivo
                                photoFile.asRequestBody("image/jpeg".toMediaType())
                            )
                        }
                    }
                }
            }
            .build()

        // Crear la solicitud
        val request = Request.Builder()
            .url("http://209.126.106.199/solmovsa/ApiGestorSiniestros/api/MotionsCloud/upload_photos?token=$caseToken")
            .post(body)
            .build()

        // Crear el cliente OkHttp
        val client = OkHttpClient()

        // Ejecutar la solicitud de forma asíncrona
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Manejar el error en el hilo principal
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("PresupuestoFragment", "Error: ${e.message}", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                // Manejar la respuesta en el hilo principal
                requireActivity().runOnUiThread {
                    if (response.isSuccessful) {
                        // La solicitud fue exitosa (código 200)
                        val responseBody = response.body?.string()
                        Toast.makeText(requireContext(), "Fotos enviadas correctamente, Ten en cuenta que la evaluación puede tardar entre 10 y 15 minutos.", Toast.LENGTH_SHORT).show()
                        Log.d("PresupuestoFragment", "Respuesta del servidor: $responseBody")
                    } else {
                        // La solicitud falló (códigos 400, 500, etc.)
                        val errorMessage = when (response.code) {
                            400 -> "Datos inválidos o faltantes"
                            500 -> "Error interno del servidor"
                            else -> "Error al enviar las fotos: ${response.code}"
                        }
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                        Log.e("PresupuestoFragment", "Error en la solicitud: ${response.code} - ${response.body?.string()}")
                    }
                }
            }
        })
    }

    private fun obtenerArchivoDeUri(uri: Uri, tipoFoto: String): File? {
        return try {
            val photoFile = crearArchivoTemporal(tipoFoto)
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            photoFile.outputStream().use { output ->
                inputStream?.copyTo(output)
            }
            photoFile
        } catch (e: Exception) {
            Log.e("PresupuestoFragment", "Error al convertir URI a archivo: ${e.message}", e)
            null
        }
    }

    private fun getImageUri(inContext: Context, inImage: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }

    // Función para convertir un Bitmap en un archivo
//    private fun obtenerArchivoDeUri(bitmap: Bitmap): File? {
//        return try {
//            val file = File.createTempFile("temp_photo", ".jpg", requireContext().cacheDir)
//            file.outputStream().use { output ->
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
//            }
//            file
//        } catch (e: Exception) {
//            Log.e("PresupuestoFragment", "Error al convertir Bitmap a archivo: ${e.message}")
//            null
//        }
//    }

    private fun showDialog(message: String) {
        AlertDialog.Builder(requireContext()) // Usar requireContext() directamente
            .setTitle("Información") // Título del diálogo
            .setMessage(message)     // Mensaje a mostrar
            .setIcon(R.drawable.analyze_list_logs_search_icon) // Ícono personalizado
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()     // Cerrar el diálogo al hacer clic en "OK"
            }
//            .setNegativeButton("Cancelar") { dialog, _ ->
//                dialog.dismiss()     // Cerrar el diálogo al hacer clic en "Cancelar"
//            }
            .show() // Mostrar el diálogo
    }

    // Función para mostrar un diálogo de error
//    private fun showErrorDialog(message: String) {
//        AlertDialog.Builder(requireContext())
//            .setTitle("Error")
//            .setMessage(message)
//            .setPositiveButton("Aceptar") { dialog, _ -> dialog.dismiss() }
//            .show()
//    }
}

/*
Datos de ejemplo:
"Casenumber":"OLIM3",
"FullName":"OLIMPICO",
"Email":"jperez@enpartes.com",
"Dateofinspection":"01.01.2025", "30/1/2025"
"VINnumber":"YV1MV36V1K2601947",
"Language":"en"

Casenumber: OLIM4-03022025
caseToken: 2tiWaxsBZbN39fQ4yPFsDCDE

GG87jCWGoAqAoykNLxCgYZQ7

Casenumber: OLIM4-03022025-2
caseToken: vwZxPxpkpzb3HasGtc3JZfn4

Casenumber: OLIM4-03022025-3
caseToken: Fa6j3spsARhqRFb7AYiry1iA

CaseNumber: OLIM4-Duster
casetoken; bPq2r6Sr5vDYs1N9mDJQess1
 */

