package com.ehome.enpartesapp.ui.presupuesto

import android.Manifest
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConsultaFragment : Fragment() {

    private val requestCodePermissions = 101

    // Usamos ActivityResultLauncher para manejar los permisos
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

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

        // Inicializamos el ActivityResultLauncher para los permisos
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permiso concedido, procedemos a descargar el PDF
                downloadPdf()
            } else {
                // Permiso denegado, mostramos un mensaje al usuario
                showErrorDialog(getString(R.string.permiso_de_almacenamiento_denegado))
            }
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
            showErrorDialog(getString(R.string.ingresar_case_number_token))
            return
        }

        if (caseNumber.isEmpty()) {
            showErrorDialog(getString(R.string.debe_ingresar_case_number_token_para_descargar_el_pdf))
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
                        Log.d("ConsultaFragment", getString(R.string.respuesta_del_servidor, jsonResponse))
                        //Log.d("ConsultaFragment", "Respuesta del servidor: $jsonResponse")

                    } else {
                        // Respuesta con error (400, 404, 500)
                        val errorMessage = when (response.code) {
                            400 -> getString(R.string.solicitud_incorrecta_verifique_los_datos)
                            404 -> getString(R.string.esperar_10_a_15_minutos)
                            500 -> getString(R.string.error_interno_del_servidor)
                            else -> getString(R.string.error_desconocido, response.code)
                        }
                        showErrorDialog(errorMessage)
                        //Log.e("ConsultaFragment", "Error en la solicitud: ${response.code} - $responseBody")
                        Log.e("ConsultaFragment", getString(R.string.error_en_la_solicitud, response.code, responseBody))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorTextView = TextView(requireContext())
                    errorTextView.text = getString(R.string.error_message, e.message)
                    llResultContainer.addView(errorTextView)
                    //Log.e("ConsultaFragment", "Error: ${e.message}", e)
                    Log.e("ConsultaFragment", getString(R.string.error, e.message))
                }
            }
        }
    }

    private fun displayFormattedData(jsonResponse: JSONObject) {
        val mainTitle = TextView(requireContext())
        mainTitle.text = getString(R.string.AI_cloud)
        llResultContainer.addView(mainTitle)

        val data = jsonResponse.getJSONObject("data")
        val caseNumber = data.getString("case_number")
        val vinNumber = data.getString("vin_number")
        val pLaborRate = data.getString("p_labor_rate")
        val laborRate = data.getString("labor_rate")

        val generalInfo = TextView(requireContext())
        //generalInfo.text = "case_number: $caseNumber\nvin_number: $vinNumber\nDetalles: (p_labor_rate: $pLaborRate, labor_rate: $laborRate)"
        generalInfo.text = getString(R.string.general_info_format, caseNumber, vinNumber, pLaborRate, laborRate)
        llResultContainer.addView(generalInfo)

        val separator = TextView(requireContext())
        separator.text = getString(R.string.linea_separador)
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
            detailSeparator.text = getString(R.string.linea_separador)
            llResultContainer.addView(detailSeparator)
        }

        val subTotalPart = data.getString("sub_total_part")
        val subTotalPaint = data.getString("sub_total_paint")
        val subTotalLabor = data.getString("sub_total_labor")
        val subTotal = data.getString("sub_total")
        val tax = data.getString("tax")
        val total = data.getString("total")

        val totalsTextView = TextView(requireContext())
        totalsTextView.text = getString(
            R.string.totals_format,
            subTotalPart,
            subTotalPaint,
            subTotalLabor,
            subTotal,
            tax,
            total
        )
        llResultContainer.addView(totalsTextView)
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.error_texto))
            .setMessage(message)
            .setPositiveButton((R.string.ok_texto)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun downloadPdf() {
        val caseNumber = etCaseNumber.text.toString()
        val caseToken = etCaseToken.text.toString()

        if (caseNumber.isEmpty()) {showErrorDialog(getString(R.string.debe_ingresar_case_number_token_para_descargar_el_pdf))
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
                Log.d("DownloadPDF", getString(R.string.iniciando_descarga))
                //Log.d("DownloadPDF", "URL: $url")
                Log.d("DownloadPDF", getString(R.string.url, url))
                //Log.d("DownloadPDF", "Request Body: $jsonString")
                Log.d("DownloadPDF", getString(R.string.cuerpo_de_la_solicitud, jsonString))

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    //Log.d("DownloadPDF", "Respuesta exitosa: ${response.code}")
                    Log.d("DownloadPDF", getString(R.string.respuesta_exitosa, response.code))

                    // Verificar si el cuerpo de la respuesta no está vacío
                    val responseBody = response.body
                    if (responseBody == null) {
                        withContext(Dispatchers.Main) {
                            showErrorDialog(getString(R.string.la_respuesta_del_servidor_esta_vacia))
                            //Log.e("DownloadPDF", "La respuesta del servidor está vacía.")
                        }
                        return@launch
                    }

                    // Crear el archivo PDF en la carpeta de descargas pública
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    // Obtener la fecha actual
                    val currentDate = SimpleDateFormat("ddMMyyyy", Locale.getDefault()).format(Date())
                    // Crear el nombre del archivo con el caseNumber y la fecha
                    val newFilename = "Caso-$caseNumber-$currentDate.pdf"
                    val file = File(downloadsDir, newFilename)

                    try {
                        // Chequea si el archivo existe, si existe lo borra
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

                        //Log.d("DownloadPDF", "PDF guardado en: ${file.absolutePath}")
                        Log.d("DownloadPDF", getString(R.string.pdf_guardado_en, file.absolutePath))

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
                                getString(R.string.no_hay_aplicación_para_abrir_pdf),
                                Toast.LENGTH_SHORT
                            ).show()
                            }
                        }
                    } catch (e: IOException) {
                        withContext(Dispatchers.Main) {
                            showErrorDialog(getString(R.string.error_al_guardar_el_archivo, e.message))
                            //Log.e("DownloadPDF", "Error al guardar el archivo: ${e.message}", e)
                        }
                    } catch (e: SecurityException) {
                        withContext(Dispatchers.Main) {
                            showErrorDialog(getString(R.string.error_de_seguridad_al_acceder_al_archivo, e.message))
                            //Log.e("DownloadPDF", "Error de seguridad al acceder al archivo: ${e.message}", e)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showErrorDialog("Error desconocido: ${e.message ?: getString(R.string.sin_informacion)}")
                            //Log.e("DownloadPDF", "Error desconocido", e)
                            e.printStackTrace()
                        }
                    }
                } else {
                    // Manejar errores de la respuesta
                    val responseBody = response.body?.string()
                    Log.e("DownloadPDF",getString(R.string.error_en_la_solicitud, response.code, responseBody))
                    //Log.e("DownloadPDF", "Error en la respuesta: ${response.code} - $responseBody")
                    withContext(Dispatchers.Main) {
                        val errorMessage = when (response.code) {
                            400 -> getString(R.string.solicitud_incorrecta_verifique_los_datos)
                            404 -> getString(R.string.no_se_encontr_informacion_para_los_parametros_proporcionados)
                            500 -> getString(R.string.error_interno_del_servidor)
                            else -> getString(R.string.error_desconocido, response.code)
                        }
                        showErrorDialog(errorMessage)
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    showErrorDialog(getString(R.string.error_de_red, e.message))
                    //Log.e("DownloadPDF", "Error de red: ${e.message}", e)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorDialog("Error desconocido: ${e.message ?: getString(R.string.sin_informacion)}")
                    //Log.e("DownloadPDF", "Error desconocido", e)
                    e.printStackTrace()}
            }
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
                requestCodePermissions
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodePermissions) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, retry download
                downloadPdf()
            } else {
                // Permission denied
                showErrorDialog(getString(R.string.permiso_de_almacenamiento_denegado))
            }
        }
    }
}