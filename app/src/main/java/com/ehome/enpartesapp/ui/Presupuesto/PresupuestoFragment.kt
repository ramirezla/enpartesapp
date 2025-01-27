package com.ehome.enpartesapp.ui.Presupuesto

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
import android.icu.util.Calendar
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.ehome.enpartesapp.R
import com.google.android.material.textfield.TextInputEditText

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
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

    private val fotoList = mutableListOf(FotoItem())
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
    private lateinit var etLanguage: TextInputEditText
    private lateinit var etDateOfInspection: TextInputEditText

    private lateinit var btnCancelar: Button
    private lateinit var btnAceptar: Button

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

        // Configurar el clic del botón Cancelar
//        btnCancelar.setOnClickListener {
//            redirigirAConsultasAbiertas()
//        }

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
        etLanguage.text?.clear()

        // Reiniciar el Spinner
        spinnerTipoVehiculo.setSelection(0)

        // Limpiar la lista de fotos
        fotoList.clear()
        fotoList.add(FotoItem()) // Agregar una línea vacía inicial
        adapter.notifyDataSetChanged() // Notificar al adaptador que los datos han cambiado

        Toast.makeText(requireContext(), "Formulario limpiado", Toast.LENGTH_SHORT).show()
    }

//    private fun redirigirAConsultasAbiertas() {
//        // Navegar al fragmento ConsultasAbiertasFragment
//        val navController = findNavController()
//        navController.navigate(R.id.action_presupuestofragment_to_nav_consultas_abiertas)
//    }

    private fun validarYProcesarDatos() {
        // Validar que todos los campos estén completos
        if (validarCampos()) {
            // Procesar los datos (guardar en base de datos, enviar a servidor, etc.)
            procesarDatos()
        } else {
            Toast.makeText(requireContext(), "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validarCampos(): Boolean {
        // Aquí puedes agregar la lógica para validar que todos los campos estén completos
        return true // Cambia esto según tu lógica de validación
    }

    private fun procesarDatos() {
        // Aquí puedes agregar la lógica para procesar los datos
        Toast.makeText(requireContext(), "Datos procesados correctamente", Toast.LENGTH_SHORT).show()
    }

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
            crearArchivoTemporal()
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
        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, tiposFoto) // Especifica el tipo <String>
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
                val tipoSeleccionado = tiposVehiculo[position]
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

    private fun crearArchivoTemporal(): File {
        val storageDir: File? = requireContext().getExternalFilesDir(null)
        return File.createTempFile(
            "JPEG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}_",
            ".jpg",
            storageDir
        )
    }

    private fun tomarFoto(position: Int) {
        val photoFile: File? = try {
            crearArchivoTemporal()
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

    companion object {
        private const val REQUEST_TAKE_PHOTO = 1
        private const val REQUEST_UPLOAD_PHOTO = 2
    }
}