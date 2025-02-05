package com.ehome.enpartesapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.ehome.enpartesapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navigationView: NavigationView
    private var isSiniestroExpanded = false // Estado del menú Siniestro

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Se recibe el valor del id del usuario y el password permitido.
        val nombreUsuario = intent.extras?.getString("username")
//        val passwordUsuario = intent.extras?.getString("userpassword")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        // mostrar el icono flotante del correo
//        binding.appBarMain.fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null)
//                .setAnchorView(R.id.fab).show()
//        }

        // Se crea un bundle con los parametros que se desea pasar al fragment
//        val bundle = Bundle().apply {
//            putString("username", nombreUsuario)        // Pass nombreUsuario in the bundle
//            putString("userpassword", passwordUsuario)  // Pass passwordUsuario in the bundle
//        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        navigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.

        // Configurar el AppBarConfiguration
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_consultas_abiertas,
                R.id.nav_gallery,
                R.id.nav_siniestro,
                R.id.exitMenuItem
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        //navView.setupWithNavController(navController)
        // Se activa el NavigationItemSelectedListener para obtener la opcion seleccionada
        // Manejar la selección de ítems del menú
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_consultas_abiertas -> {
                    // Navegar al fragmento de consultas abiertas
                    navController.navigate(R.id.nav_consultas_abiertas)
                }
                R.id.nav_siniestro -> {
                    // Expandir o colapsar el submenú de Siniestro
                    toggleSiniestroSubmenu()
                    return@setNavigationItemSelectedListener false // No navegar
                }
                R.id.nav_reportar_siniestro -> {
                    // Navegar al fragmento de reportar siniestro
                    navController.navigate(R.id.nav_presupuestofragment)
                }
                R.id.nav_consultar_siniestro -> {
                    // Navegar al fragmento de consultar siniestro
                    navController.navigate(R.id.nav_consultafragment)
                }
//                R.id.nav_descargar_pdf_siniestro -> {
//                    // Lógica para descargar PDF
//                    Toast.makeText(this, "Descargar PDF", Toast.LENGTH_SHORT).show()
//                }
//                else -> false // Manejar otros ítems del menú
            }
            drawerLayout.closeDrawer(GravityCompat.START) // Cerrar el drawer después de la navegación
            true // Indicar que el ítem fue manejado
        }

        // Botón de salida
        val navigationView: NavigationView = findViewById(R.id.nav_view)
//        val menu: Menu = navigationView.menu
        // Botón de salida
        val exitMenuItem = navigationView.menu.findItem(R.id.exitMenuItem)
        exitMenuItem.setOnMenuItemClickListener {
            // Lógica para salir de la aplicación
            true
        }

        exitMenuItem.setOnMenuItemClickListener {
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("Confirmar salir")
                .setMessage("¿Está seguro que desea salir?")
                .setPositiveButton("Sí") { _, _ ->
                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish() // Opcional: Finalizar la actividad actual
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
            true
        }

// Controlando si presiona retroceder para salir de la aplicación
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val builder = AlertDialog.Builder(this@MainActivity)
                builder.setTitle("Confirmar salir")
                    .setMessage("¿Está seguro que desea salir?")
                    .setPositiveButton("Sí") { _, _ ->
                        val intent = Intent(this@MainActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish() // Opcional: Finalizar la actividad actual
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun toggleSiniestroSubmenu() {
        val menu = navigationView.menu
        val mainMenuItem = menu.findItem(R.id.nav_siniestro) // Obtén el elemento principal de Siniestro

        // Obtén el submenú del elemento principal
        val submenu = mainMenuItem.subMenu

        // Busca el elemento del grupo dentro del submenú
        val groupItem = submenu?.findItem(R.id.group_siniestro_submenu)

        if (groupItem != null) {
            val isVisible = groupItem.isVisible // Obtén la visibilidad actual
            submenu.setGroupVisible(R.id.group_siniestro_submenu, !isVisible) // Cambia la visibilidad
            isSiniestroExpanded = !isVisible // Actualiza el estado

            // Si el menú principal de Siniestro está colapsado, oculta el submenú
            if (!mainMenuItem.isChecked) {
                submenu.setGroupVisible(R.id.group_siniestro_submenu, false)
                isSiniestroExpanded = false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}