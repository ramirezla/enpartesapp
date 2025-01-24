package com.ehome.enpartesapp

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ehome.enpartesapp.databinding.ActivityLoginBinding
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.view.View
import android.widget.Toast

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import android.provider.Settings
import androidx.activity.OnBackPressedCallback

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // If you detect that there's no network connection
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No network connection available", Toast.LENGTH_LONG).show()
            // Prompt the user to open network settings
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
            startActivity(intent)
        }

        // Set click listener for the login button
        binding.login.setOnClickListener {
            binding.loginProgressBar.visibility = View.VISIBLE
            val accessCode = binding.username.text.toString()
            val cKey = binding.password.text.toString()
            makeApiRequest(accessCode, cKey)
        }

        // Set click listener for the btnRegister button
        binding.btnRegister.setOnClickListener {
            //binding.loginProgressBar.visibility = View.VISIBLE
            // Thread.sleep(3000) // Sleep for 3 seconds (for testing only)
            // Navigate to RegisterActivity
            val intent = Intent(this@LoginActivity, RegistrarActivity::class.java)
            startActivity(intent)
            //finish()
        }

        // Controlando si presiona retroceder para salir de la aplicacion
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val builder = AlertDialog.Builder(this@LoginActivity)
                builder.setTitle("Confirmar salir")
                    .setMessage("¿Esta seguro que desea salir?")
                    .setPositiveButton("Si") { _, _ ->
                        if (isTaskRoot) {finishAffinity()
                        } else {
                            finish()
                        }
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    object NetworkUtils {
        fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)-> true
                else -> false
            }
        }
    }

    // Se configura la clase que guardara la respuesta de integracion cuando se pregunta si el usuario es valido
    data class AuthResponse(
        val code: String,
        val msg: String
    )

    // Interfaz para la solicitud en integracion
    interface ApiService {
        @GET("/integracion") // Endpoint
        fun verifyLogin(@Query("q") query: String): Call<AuthResponse>
    }

    // Crea el servicio para preguntar a integracion
    object RetrofitClient {
        private const val BASE_URL = "http://192.168.1.143/" // ip URL eHome
        // private const val BASE_URL = "http://192.168.220.253/" // ip URL de la oficina

        val instance: ApiService by lazy {
            //Create a logging interceptor
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY // Set the logging level
            }

            // Create an OkHttpClient and add the logging interceptor
            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor) //Add the interceptor here
                .build()

            // Create the Retrofit instance
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()) // Add Gson converter
                .client(client) // Set the OkHttpClient
                .build()

            // Create the ApiService
            retrofit.create(ApiService::class.java)
        }
    }

    // Function to make the API request
    private fun makeApiRequest(accessCode:String, cKey: String) {
        val apiService = RetrofitClient.instance
        // Construct the query string
        val queryString = "{\"action\":\"VERIFYLOGIN\",\"accessCode\":\"$accessCode\",\"cKey\":\"$cKey\"}"
        val call = apiService.verifyLogin(queryString)

        call.enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                binding.loginProgressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    // Thread.sleep(3000) // Sleep for 3 seconds (for testing only)
                    if (authResponse != null) {
                        // Handle the successful response using a when statement
                        when (authResponse.code) {
                            "200" -> {
                                // Handle code 200 (success)
                                // Log.d("API Response", "Code 200: ${authResponse.msg}")
                                // Toast.makeText(this@LoginActivity, "Success: ${authResponse.msg}", Toast.LENGTH_LONG).show()
                                // Navigate to MainActivity
                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                val bundle = Bundle()
                                bundle.putString("username", accessCode)
                                bundle.putString("userpassword", cKey)
                                intent.putExtras(bundle)
                                startActivity(intent)
                                finish() // Close LoginActivity
                            }

                            else -> {
                                // Handle other codes or unknown codes
                                Log.e("API Response", "Code: ${authResponse.code}, Message: ${authResponse.msg}")
                                // Show error dialog
                                showErrorDialog(authResponse.code, authResponse.msg)
                            }
                        }
                    }
                } else {
                    // Handle the error response
                    Log.e("API Error", "Code: ${response.code()}, Message: ${response.message()}")
                    // Show error dialog
                    showErrorDialog(response.code().toString(), response.message())
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                binding.loginProgressBar.visibility = View.GONE
                // Handle the network failure
                Log.e("API Failure", "Error: ${t.message}")
                // Show error dialog
                showErrorDialog("Network Error onFailure...", t.message ?: "Unknown network error")
            }
        })
    }

    private fun showErrorDialog(code: String, message: String) {val dialogFragment = ErrorDialogFragment.newInstance(code, message)
        dialogFragment.show(supportFragmentManager, "errorDialog")
    }
}