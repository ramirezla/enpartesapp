package com.ehome.enpartesapp.ui.Presupuesto

import android.Manifest
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.ehome.enpartesapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient

import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConsultaFragment : Fragment() {

    private val REQUEST_CODE_PERMISSIONS = 101

    private lateinit var etCaseNumber: EditText
    private lateinit var etCaseToken: EditText
    private lateinit var btnConsultar: Button
    private lateinit var llResultContainer: LinearLayout
    private lateinit var btnDownloadPdf: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Use the new layout file name here
        val view = inflater.inflate(R.layout.fragment_consulta_siniestro, container, false)

        etCaseNumber = view.findViewById(R.id.etCaseNumber)
        etCaseToken = view.findViewById(R.id.etCaseToken)
        btnConsultar = view.findViewById(R.id.btnConsultar)
        llResultContainer = view.findViewById(R.id.llResultContainer)
        btnDownloadPdf = view.findViewById(R.id.btnDownloadPdf)

        btnConsultar.setOnClickListener {
            consultarCaso()
        }

        btnDownloadPdf.setOnClickListener {
            downloadPdf()
        }

        return view
    }

    private fun consultarCaso() {
        // Clear the container before each query
        llResultContainer.removeAllViews()

        val caseNumber = etCaseNumber.text.toString()
        val caseToken = etCaseToken.text.toString()

        // Validar que al menos uno de los campos esté lleno
        if (caseNumber.isEmpty() && caseToken.isEmpty()) {
            //Toast.makeText(requireContext(), "Debe ingresar un Case Number o un Case Token", Toast.LENGTH_SHORT).show()
            showErrorDialog("Debe ingresar un Case Number o un Case Token.")
            return
        }

        if (caseNumber.isEmpty()) {
            showErrorDialog("Debe ingresar un Case Number para descargar el PDF.")
            return
        }

        val jsonObject = JSONObject().apply {
            put("case_number", caseNumber)
            put("case_token", caseToken)
        }

        val jsonString = jsonObject.toString()
        val url = "http://209.126.106.199/solmovsa/ApiGestorSiniestros/api/Webhook/webhook-query"

        val client = OkHttpClient()
        val mediaType = "application/json".toMediaType()
        val body = jsonString.toRequestBody(mediaType)
        //val body = "{\n    \"case_number\": \"OLIM3\",\n    \"case_token\": \"\"\n}".toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        // Respuesta exitosa (200 OK)
                        val jsonResponse = JSONObject(responseBody ?: "")
                        displayFormattedData(jsonResponse)
                        Log.d("ConsultaFragment", "Respuesta del servidor: $jsonResponse")
                        // Procesar el JSON y mostrar los datos
                        // Ejemplo:
                        // val pdfUrl = jsonResponse.getString("pdf_url")
                        // val data = jsonResponse.getJSONObject("data")
                        // ...
                    } else {
                        // Respuesta con error (400, 404, 500)
                        val errorMessage = when (response.code) {
                            400 -> "Solicitud incorrecta. Verifique los datos ingresados."
                            404 -> "No se encontró información del caso. Ten en cuenta que la evaluación puede tardar entre 10 y 15 minutos. Por favor, intenta nuevamente más tarde."
                            500 -> "Error interno del servidor."
                            else -> "Error desconocido: ${response.code}"
                        }
                        showErrorDialog(errorMessage)
                        Log.e("ConsultaFragment", "Error en la solicitud: ${response.code} - $responseBody")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorTextView = TextView(requireContext())
                    errorTextView.text = "Error: ${e.message}"
                    llResultContainer.addView(errorTextView)
                    Log.e("ConsultaFragment", "Error: ${e.message}", e)
                }
            }
        }
    }

    private fun displayFormattedData(jsonResponse: JSONObject) {
        val mainTitle = TextView(requireContext())
        mainTitle.text = "MotionsCloud Vehicle Assessment Report\nPowered by AI Computer Vision Technologies"
        llResultContainer.addView(mainTitle)

        val data = jsonResponse.getJSONObject("data")
        val caseNumber = data.getString("case_number")
        val vinNumber = data.getString("vin_number")
        val pLaborRate = data.getString("p_labor_rate")
        val laborRate = data.getString("labor_rate")

        val generalInfo = TextView(requireContext())
        generalInfo.text = "case_number: $caseNumber\nvin_number: $vinNumber\nDetalles: (p_labor_rate: $pLaborRate, labor_rate: $laborRate)"
        llResultContainer.addView(generalInfo)

        val separator = TextView(requireContext())
        separator.text = "_________________________________________________"
        llResultContainer.addView(separator)

        val details = data.getJSONArray("details")
        for (i in 0 until details.length()) {
            val detail = details.getJSONObject(i)
            val detailText = """
                car_part: ${detail.getString("car_part")}
                side_1: ${detail.getString("side_1")}
                side_2: ${detail.getString("side_2")}
                damage: ${detail.getString("damage")}
                confidence: ${detail.getString("confidence")}
                treatment: ${detail.getString("treatment")}
                part_cost: ${detail.getString("part_cost")}
                paint_hour: ${detail.getString("paint_hour")}
                paint_material_cost: ${detail.getString("paint_material_cost")}
                labour_hour: ${detail.getString("labour_hour")}
                labour_cost: ${detail.getString("labour_cost")}
            """.trimIndent()

            val detailTextView = TextView(requireContext())
            detailTextView.text = detailText
            llResultContainer.addView(detailTextView)

            val detailSeparator = TextView(requireContext())
            detailSeparator.text = "_________________________________________________"
            llResultContainer.addView(detailSeparator)
        }
        val subTotalPart = data.getString("sub_total_part")
        val subTotalPaint = data.getString("sub_total_paint")
        val subTotalLabor = data.getString("sub_total_labor")
        val subTotal = data.getString("sub_total")
        val tax = data.getString("tax")
        val total = data.getString("total")

        val totalsTextView = TextView(requireContext())
        totalsTextView.text = """
            sub_total_part: $subTotalPart
            sub_total_paint: $subTotalPaint
            sub_total_labor: $subTotalLabor
            sub_total: $subTotal
            tax: $tax
            total: $total
        """.trimIndent()
        llResultContainer.addView(totalsTextView)
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun downloadPdf() {
        val caseNumber = etCaseNumber.text.toString()
        val caseToken = etCaseToken.text.toString()

        if (caseNumber.isEmpty()) {showErrorDialog("Debe ingresar un Case Number para descargar el PDF.")
            return
        }

        // Check for permissions
        if (!checkPermissions()) {
            requestPermissions()
            return
        }

        val jsonObject = JSONObject().apply {
            put("case_number", caseNumber)
            put("case_token", caseToken)
        }

        val jsonString = jsonObject.toString()
        val url = "http://209.126.106.199/solmovsa/ApiGestorSiniestros/api/Webhook/generate-pdf"

        val client = OkHttpClient()
        val mediaType = "application/json".toMediaType()
        val body = jsonString.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("DownloadPDF", "Iniciando descarga...")
                Log.d("DownloadPDF", "URL: $url")
                Log.d("DownloadPDF", "Request Body: $jsonString")

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    Log.d("DownloadPDF", "Respuesta exitosa: ${response.code}")

                    // Verificar si el cuerpo de la respuesta no está vacío
                    val responseBody = response.body
                    if (responseBody == null) {
                        withContext(Dispatchers.Main) {
                            showErrorDialog("La respuesta del servidor está vacía.")
                            Log.e("DownloadPDF", "La respuesta del servidor está vacía.")
                        }
                        return@launch
                    }

                    // Obtener el nombre del archivo (si no viene en el header, usar el caseNumber)
                    val contentDisposition = response.header("Content-Disposition")
                    val filename = extractFilenameFromContentDisposition(contentDisposition, caseNumber)

                    // Crear el archivo PDF en la carpeta de descargas pública
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    // Obtener la fecha actual
                    val currentDate = SimpleDateFormat("ddMMyyyy", Locale.getDefault()).format(Date())
                    //val file = File(downloadsDir, filename)
                    // Crear el nombre del archivo con el caseNumber y la fecha
                    val newFilename = "Caso-$caseNumber-$currentDate.pdf"
                    val file = File(downloadsDir, newFilename)

                    try {
                        // Check if the file exists, and delete it if it does
                        if (file.exists()) {
                            file.delete()
                        }

                        // Guardar el archivo PDF (ahora en Dispatchers.IO)
                        withContext(Dispatchers.IO) {
                            file.outputStream().use { output ->
                                responseBody.byteStream().use { input ->
                                    input.copyTo(output)
                                }
                            }
                        }

                        Log.d("DownloadPDF", "PDF guardado en: ${file.absolutePath}")

                        // Abrir el archivo PDF con una aplicación externa (en Dispatchers.Main)
                        withContext(Dispatchers.Main) {
                            val uri = FileProvider.getUriForFile(
                                requireContext(),
                                "com.ehome.enpartesapp.provider", // Debe coincidir con el manifest
                                file
                            )
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }
                            try {
                                startActivity(intent)
                            } catch (e: ActivityNotFoundException) {Toast.makeText(
                                requireContext(),
                                "No hay aplicación para abrir PDF",
                                Toast.LENGTH_SHORT
                            ).show()
                            }
                        }
                    } catch (e: IOException) {
                        withContext(Dispatchers.Main) {
                            showErrorDialog("Error al guardar el archivo: ${e.message}")
                            Log.e("DownloadPDF", "Error al guardar el archivo: ${e.message}", e)
                        }
                    } catch (e: SecurityException) {
                        withContext(Dispatchers.Main) {
                            showErrorDialog("Error de seguridad al acceder al archivo: ${e.message}")
                            Log.e("DownloadPDF", "Error de seguridad al acceder al archivo: ${e.message}", e)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showErrorDialog("Error desconocido: ${e.message ?: "No message"}")
                            Log.e("DownloadPDF", "Error desconocido", e)
                            e.printStackTrace()
                        }
                    }
                } else {
                    // Manejar errores de la respuesta
                    val responseBody = response.body?.string()
                    Log.e("DownloadPDF", "Error en la respuesta: ${response.code} - $responseBody")
                    withContext(Dispatchers.Main) {
                        val errorMessage = when (response.code) {
                            400 -> "Solicitud incorrecta. Verifique los datos ingresados."
                            404 -> "No se encontró información para los parámetros proporcionados."
                            500 -> "Error interno del servidor."
                            else -> "Error desconocido: ${response.code}"
                        }
                        showErrorDialog(errorMessage)
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    showErrorDialog("Error de red: ${e.message}")
                    Log.e("DownloadPDF", "Error de red: ${e.message}", e)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorDialog("Error desconocido: ${e.message ?: "No message"}")
                    Log.e("DownloadPDF", "Error desconocido", e)
                    e.printStackTrace()}
            }
        }
    }

    private fun extractFilenameFromContentDisposition(contentDisposition: String?, caseNumber: String): String {
        if (contentDisposition == null) {
            return "Caso-$caseNumber.pdf"
        }
        val filenameRegex = Regex("filename\\s*=\\s*\"?([^\";]+)\"?")
        val filenameMatch = filenameRegex.find(contentDisposition)

        return if (filenameMatch != null) {
            filenameMatch.groupValues[1]
        } else {
            "Caso-$caseNumber.pdf"
        }
    }

    // Verifica si los permisos están concedidos
    private fun checkPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Para versiones anteriores a Android 6.0, los permisos se otorgan en la instalación
        }
    }

    // Solicita los permisos en tiempo de ejecución
    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, retry download
                downloadPdf()
            } else {
                // Permission denied
                showErrorDialog("Permiso de almacenamiento denegado.")
            }
        }
    }

//    // Guarda el archivo PDF en la carpeta de Descargas
//    private fun savePdfToDownloads(fileName: String, pdfBytes: ByteArray) {
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
//            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
//            }
//        }
//
//        val resolver = context?.contentResolver
//
//        val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            resolver?.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
//        } else {
//            TODO("VERSION.SDK_INT < Q")
//        }
//
//        uri?.let {
//            try {
//                if (resolver != null) {
//                    resolver.openOutputStream(uri)?.use { outputStream: OutputStream? ->
//                        outputStream?.write(pdfBytes)
//                    }
//                }
//                showToast("Archivo guardado en Descargas")
//            } catch (e: IOException) {
//                e.printStackTrace()
//                showToast("Error al guardar el archivo")
//            }
//        } ?: run {
//            showToast("No se pudo crear el archivo")
//        }
//    }

    // Muestra un Toast con un mensaje
    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}