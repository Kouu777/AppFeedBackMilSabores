package com.example.proyectomilsabores.ui.activities

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectomilsabores.R
import com.example.proyectomilsabores.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainFeed : AppCompatActivity() {

    private lateinit var spCat: Spinner
    private lateinit var spProd: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed)

        initializeViews()
        testApiConnection()
        loadCategorias()
    }

    private fun initializeViews() {
        spCat = findViewById(R.id.sp_cat)
        spProd = findViewById(R.id.sp_prod)
    }

    private fun testApiConnection() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val categorias = RetrofitClient.apiService.getCategorias()
                Log.d("API_TEST", "✅ CONEXIÓN EXITOSA! Categorías: ${categorias.size}")

                runOnUiThread {
                    Toast.makeText(this@MainFeed, "API conectada: ${categorias.size} categorías", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("API_TEST", "❌ ERROR: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@MainFeed, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadCategorias() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val categorias = RetrofitClient.apiService.getCategorias()
                setupCategoriaSpinner(categorias)
            } catch (e: Exception) {
                // Fallback con datos de ejemplo
                val categoriasEjemplo = listOf(
                    com.example.proyectomilsabores.api.CategoriaResponse(1, "Tortas y Pasteles"),
                    com.example.proyectomilsabores.api.CategoriaResponse(2, "Bolleria y Masas dulces"),
                    com.example.proyectomilsabores.api.CategoriaResponse(3, "Panes Especiales"),
                    com.example.proyectomilsabores.api.CategoriaResponse(4, "Galletas y Pequeños Dulces")
                )
                setupCategoriaSpinner(categoriasEjemplo)
            }
        }
    }

    private fun setupCategoriaSpinner(categorias: List<com.example.proyectomilsabores.api.CategoriaResponse>) {
        val categoriaNombres = listOf("Selecciona categoría") + categorias.map { it.nombre }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoriaNombres)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCat.adapter = adapter

        spCat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                if (position > 0) {
                    val categoriaId = categorias[position - 1].id
                    loadProductos(categoriaId)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadProductos(categoriaId: Long) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val productos = RetrofitClient.apiService.getProductosPorCategoria(categoriaId)
                setupProductoSpinner(productos)
            } catch (e: Exception) {
                Log.e("API_TEST", "Error cargando productos: ${e.message}")
            }
        }
    }

    private fun setupProductoSpinner(productos: List<com.example.proyectomilsabores.api.ProductoResponse>) {
        val productNombres = listOf("Selecciona producto") + productos.map { it.nombre }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, productNombres)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spProd.adapter = adapter
    }
}