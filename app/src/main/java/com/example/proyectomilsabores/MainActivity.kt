package com.example.proyectomilsabores

import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController

class MainActivity : AppCompatActivity() {

    private var navInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("MainActivity", "onCreate llamado")
    }

    override fun onStart() {
        super.onStart()
        Log.d("MainActivity", "onStart llamado")
        if (!navInitialized) {
            initializeNavController()
        }
    }

    private fun initializeNavController() {
        try {
            if (navInitialized) return

            Log.d("MainActivity", "Intentando inicializar NavController...")
            val navView: BottomNavigationView = findViewById(R.id.bottom_nav_view)
            val navController = findNavController(R.id.nav_host_fragment)

            // Solo configurar el BottomNavigationView, sin ActionBar
            navView.setupWithNavController(navController)
            navInitialized = true
            Log.d("MainActivity", "NavController inicializado correctamente")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error configurando NavController: ${e.message}", e)
            e.printStackTrace()
        }
    }
}