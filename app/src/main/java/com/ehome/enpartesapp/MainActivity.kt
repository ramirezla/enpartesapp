package com.ehome.enpartesapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Se recibe el valor del id del usuario y el password permitido.
        val nombreUsuario = intent.extras?.getString("username")
        val passwordUsuario = intent.extras?.getString("userpassword")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }

        // Se crea un bundle con los parametros que se desea pasar al fragment
        val bundle = Bundle().apply {
            putString("username", nombreUsuario)        // Pass nombreUsuario in the bundle
            putString("userpassword", passwordUsuario)  // Pass passwordUsuario in the bundle
        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_consultas_abiertas,
                R.id.nav_reclamos,
                R.id.nav_presupuestofragment,
                R.id.exitMenuItem
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        //navView.setupWithNavController(navController)
        // Se activa el NavigationItemSelectedListener para obtener la opcion seleccionada
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_consultas_abiertas -> {
                    navController.navigate(R.id.nav_consultas_abiertas, bundle)
                }
                R.id.nav_presupuestofragment -> {
                    navController.navigate(R.id.action_nav_consultas_abiertas_to_nav_presupuestofragment, bundle)
                }
//                R.id.nav_solicitudes_abiertas -> {
//                    navController.navigate(R.id.nav_solicitudes_abiertas, bundle)
//                }
                // ... other menu items ...
                else -> false // Handle other menu items or return false if not handled
            }
            drawerLayout.closeDrawer(GravityCompat.START) // Close the drawer after navigation
            true// Indicate that the item was handled
        }
        // Boton de salida
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        val menu: Menu = navigationView.menu
        val exitMenuItem: MenuItem = menu.findItem(R.id.exitMenuItem)

        exitMenuItem.setOnMenuItemClickListener {
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("Confirmar salir")
                .setMessage("¿Esta seguro que desea salir?")
                .setPositiveButton("Si") { _, _ ->
                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish() // Optional: Finish the current activity
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
            true}

        // Controlando si presiona retroceder para salir de la aplicacion
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val builder = AlertDialog.Builder(this@MainActivity)
                builder.setTitle("Confirmar salir")
                    .setMessage("¿Esta seguro que desea salir?")
                    .setPositiveButton("Si") { _, _ ->
                        val intent = Intent(this@MainActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish() // Optional: Finish the current activity
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}