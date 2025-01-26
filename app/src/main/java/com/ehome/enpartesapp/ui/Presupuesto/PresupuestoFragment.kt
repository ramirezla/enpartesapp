package com.ehome.enpartesapp.ui.Presupuesto

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ehome.enpartesapp.databinding.FragmentPresupuestoBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import androidx.recyclerview.widget.RecyclerView

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class PresupuestoFragment : Fragment() {

    private var _binding: FragmentPresupuestoBinding? = null
    private val binding get() = _binding!!
    private lateinit var currentPhotoPath: String // Para guardar la ruta de la foto actual
    private val REQUEST_IMAGE_CAPTURE = 1 // Código de solicitud para la cámara
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private lateinit var photoTypeRecyclerView: RecyclerView

    private val photoTypesList = listOf(
        Pair("front_left", "Imagen de la parte delantera izquierda."),
        Pair("front", "Imagen de la parte delantera."),
        Pair("front_right", "Imagen de la parte delantera derecha."),
        // ... (resto de los tipos de fotos) ...
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

      _binding = FragmentPresupuestoBinding.inflate(inflater, container, false)
      return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // ... (other code) ...
        photoTypeRecyclerView = binding.photoTypeRecyclerView // Inicializa la variable
        binding.photoTypeRecyclerView // Inicializa la variable photoTypeRecyclerView
        // Configura el listener para el botón
        binding.buttonTakePhoto.setOnClickListener {
            takePhoto()
        }

        takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // La foto se ha tomado correctamente
                // Muestra la vista previa
                binding.imagePreview.visibility = View.VISIBLE
                binding.imagePreview.setImageURI(Uri.fromFile(File(currentPhotoPath)))
                showPhotoTypeListRecyclerView() // O showPhotoTypeListDialog() si usas AlertDialog
            }
        }
        // ... (other code) ...
    }

    private fun takePhoto() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // El permiso ya está otorgado, abre la cámara
            openCamera()
        } else {
            // El permiso noestá otorgado, solicítalo
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // El permiso ha sido otorgado, abre la cámara
            openCamera()
        } else {
            // El permiso ha sido denegado, muestra un mensaje al usuario
            // o deshabilita la funcionalidad de la cámara
            // ...
        }
    }

    private fun openCamera() {
        // Crea un Intent para abrir la cámara
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // Crea un archivo para guardar la foto
        val photoFile = createImageFile()

        // Guarda la URI del archivo en una variable
        val photoURI = FileProvider.getUriForFile(
            requireContext(),
            "com.ehome.enpartesapp.fileprovider", // Reemplaza con tu authority
            photoFile
        )

        // Agrega la URI del archivo al Intent
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

        // Usa el launcher en lugar de startActivityForResult
        takePictureLauncher.launch(intent)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Crea un nombre de archivo basado en la fecha y hora
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp
        val storageDir = requireContext().getExternalFilesDir(null) // Directorio para guardar las fotos
        val image = File.createTempFile(
            imageFileName, /* prefijo */
            ".jpg", /* sufijo */
            storageDir /* directorio */
        )

        // Guarda la ruta del archivo para usarla después
        currentPhotoPath = image.absolutePath
        return image
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // ...
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            // Muestra la vista previa
            binding.imagePreview.visibility = View.VISIBLE
            binding.imagePreview.setImageURI(Uri.fromFile(File(currentPhotoPath)))
            showPhotoTypeListDialog() // O showPhotoTypeListRecyclerView() si usas RecyclerView
        }
        // ...
    }

    private fun showPhotoTypeListRecyclerView() {
        photoTypeRecyclerView.visibility = View.VISIBLE
        photoTypeRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        val adapter = PhotoTypeAdapter(photoTypesList, ::savePhotoWithSelectedType) // Pasa la función
        photoTypeRecyclerView.adapter = adapter
    }

    private fun showPhotoTypeListDialog() {
        val photoTypes = arrayOf(
            "Imagen de la parte delantera izquierda",
            "Imagen de la parte delantera",
            "Imagen de la parte delantera derecha",
            // ... otros tipos de fotos ...
        )
        AlertDialog.Builder(requireContext())
            .setTitle("Selecciona el tipo de foto")
            .setItems(photoTypes) { _, which ->
                val selectedPhotoType = photoTypes[which]
                savePhotoWithSelectedType(selectedPhotoType)
            }
            .show()
    }

    private fun savePhotoWithSelectedType(selectedPhotoTypeKey: String) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val newFileName = "$selectedPhotoTypeKey-$timeStamp.jpg"

        val currentFile = File(currentPhotoPath)
        val newFile = File(currentFile.parent, newFileName)

        if (currentFile.renameTo(newFile)) {
            // El archivo se renombró correctamente
            // ... (código para actualizar la vista previa o realizar otras acciones) ...
        } else {
            // Error al renombrar el archivo
            // ... (código para manejar el error) ...
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}