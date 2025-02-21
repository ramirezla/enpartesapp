package com.ehome.enpartesapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class PermissionsActivity : AppCompatActivity() {

    private lateinit var requestMultiplePermissionsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>

    private val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        requestMultiplePermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            // This callback is invoked AFTER the user has interacted with the permission dialog.
            val allPermissionsGranted = permissions.all { it.value }
            if (allPermissionsGranted) {
                // All permissions granted, proceed to the login activity
                navigateToLoginActivity()
            } else {
                // Some permissions denied, show a message or handle it accordingly
                showPermissionDeniedDialog()
            }
        }

        requestCameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your app.
                navigateToLoginActivity()
            } else {
                // Explain to the user that the feature is unavailable because the
                // feature requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
                showPermissionDeniedDialog()
            }
        }

        // Check if permissions are already granted
        if (checkPermissions()) {
            // Permissions are already granted, proceed to the login activity
            navigateToLoginActivity()
        } else {
            // Permissions are not granted, launch the permission request
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, proceder a tomar la foto
                requestMultiplePermissionsLauncher.launch(permissions)
                navigateToLoginActivity()  // solo para pruebas, se debe borrar
            } else {
                // Permiso no concedido, solicitarlo al usuario
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                navigateToLoginActivity()  // solo para pruebas, se debe borrar
            }
        }
    }

    private fun checkPermissions(): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false // At least one permission is not granted
            }
        }
        return true // All permissions are granted
    }

    private fun navigateToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permisos_denegados))
            .setMessage(getString(R.string.para_usar_la_aplicacion_debes_conceder_todos_los_permisos))
            .setPositiveButton("Reintentar") { _, _ ->
                // Re-launch the permission request.
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    // Permiso concedido, proceder a tomar la foto
                    requestMultiplePermissionsLauncher.launch(permissions)
                } else {
                    // Permiso no concedido, solicitarlo al usuario
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
            .setNegativeButton("Salir") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
}

        /*
        import androidx.core.content.ContextCompat


        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Permiso concedido, proceder a tomar la foto
            }
        } else {
            // Permiso no concedido, solicitarlo al usuario
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        */