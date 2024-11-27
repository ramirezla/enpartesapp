package com.ehome.enpartesapp

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.ehome.enpartesapp.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var ident: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)
        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Pasando cada ID de menú como un conjunto de ID porque cada
        // opcion del menú debe considerarse como destino de nivel superior.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_consultas_abiertas, R.id.nav_solicitudes_abiertas, R.id.nav_piezas_abiertas, R.id.nav_piezas_por_entregar, R.id.exitMenuItem
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Boton de salida
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        val menu: Menu = navigationView.menu
        val exitMenuItem: MenuItem = menu.findItem(R.id.exitMenuItem)
        exitMenuItem.setOnMenuItemClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirmar salir")
                .setMessage("¿Esta seguro que desea salir?")
                .setPositiveButton("Si") { _, _ ->
                    super.onBackPressed() // Exit the app
                    finish()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss() // Dismiss the dialog
                    /**
                     * Se cancela la salida.
                     *  y no se hace nada.
                     */
                }
                .show()
            true}

        // Se recibe el valor del id del usuario permitido en IDENT.
        val b = intent.extras
        checkNotNull(b)
        ident = b.getString("IDENT")
        title = null
        // Se coloca el usuario en el navegador del menu
        val headerView: View = navigationView.getHeaderView(0) // 0 is the index of the header
        val usernameTextView: TextView = headerView.findViewById(R.id.username)
        usernameTextView.text = ident
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirmar salir")
            .setMessage("¿Esta seguro que desea salir?")
            .setPositiveButton("Si") { _, _ ->
                super.onBackPressed() // Exit the app
                finish()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss() // Dismiss the dialog
                /**
                 * Se cancela la salida.
                 *  y no se hace nada.
                 */
            }
            .show()
    }
}