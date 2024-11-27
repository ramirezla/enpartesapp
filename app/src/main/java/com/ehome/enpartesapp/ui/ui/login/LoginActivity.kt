package com.ehome.enpartesapp.ui.ui.login

/*
 * @autho Luis Alberto Ramirez Guerrero
 * @versión V1 - 01 Noviembre 2024
 * @since 21
 *
 * Activity Principal,
 * Primera pantalla que se le muestra al usuario, se muestra:
 * 1.- Colocar el usuario (correo)
 * 2.- Se pide el password (clave debe ser mayor a 5 caracteres)
 * 3.- Se envia via URL a integración para verificar si existe el usuario y es valido el password
 * 4.- Retorna si es un usario valido y correcto el password.
 *
 */

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ehome.enpartesapp.MainActivity
import com.ehome.enpartesapp.R
import com.ehome.enpartesapp.databinding.ActivityLoginBinding

@Suppress("SpellCheckingInspection")
class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var binding: ActivityLoginBinding
//    private final lateinit var userID: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val username = binding.username
        val password = binding.password
        val login = binding.login
        val loading = binding.loading

        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                username.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
        })

        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)
            }
            if (loginResult.success != null) {
                updateUiWithUser(loginResult.success)
            }
            setResult(Activity.RESULT_OK)

            //Complete and destroy login activity once successful
            finish()
        })

        username.afterTextChanged {
            loginViewModel.loginDataChanged(
                username.text.toString(),
                password.text.toString()
            )
        }

        password.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    username.text.toString(),
                    password.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        loginViewModel.login(
                            username.text.toString(),
                            password.text.toString()
                        )
                }
                false
            }

            login.setOnClickListener {
                loading.visibility = View.VISIBLE
                loginViewModel.login(username.text.toString(), password.text.toString())
            }
        }
    }

    private fun updateUiWithUser(model: LoggedInUserView) {
//        val welcome = getString(R.string.welcome)
        val editTextPassword = binding.password
        val passwordUsuario: String = editTextPassword.text.toString()
        val editTextUsername = binding.username
        val nombreUsuario = editTextUsername.text.toString()

        // TODO : initiate successful logged in experience
        // Servidor gratis para trucking
        // TODO: Encrytando el url
        // val encodedCiphertext = Base64.encodeToString(ciphertext, Base64.DEFAULT)
        // val url = "https://tu-url-de-integracion?cuenta=${cuenta}&password=${encodedCiphertext}"
        // val url = "http://ec2-3-134-95-99.us-east-2.compute.amazonaws.com/api/integracion?q={\"action\":\"VERIFYLOGIN\",\"userCode\":\"$nombreUsuario\",\"cKey\":\"$passwordUsuario\"}"

        // Verificando si se tiene conexion a internet
        if (isOnlineNet()) {
            // TODO: Verificar la cuenta y el password del usuario
            // si esta todo bien
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("IDENT", nombreUsuario)
            startActivity(intent)

            //Complete and destroy login activity once successful
            finish() // Opcional: cierra LoginActivity
        } else {
            /**
             * Error de conexion de internet...
             */
            Toast.makeText(
                applicationContext,
                "Error de conexion a internet",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * La función isOnLineNet.
     * <p>
     * Esta función retorna true si hay conexión a internet.
     * Se puede cambiar el sitio web.
     * </p>
     */
    private fun isOnlineNet(): Boolean {
        try {
            val p = Runtime.getRuntime().exec("ping -c 1 www.google.es")

            val `val` = p.waitFor()
            val reachable = (`val` == 0)
            return reachable
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}