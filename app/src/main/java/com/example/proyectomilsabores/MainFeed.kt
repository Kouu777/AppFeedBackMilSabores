package com.example.proyectomilsabores.ui.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectomilsabores.R
import com.example.proyectomilsabores.api.RetrofitClient
import com.example.proyectomilsabores.api.ReviewRequest
import com.example.proyectomilsabores.data.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainFeed : AppCompatActivity() {

    private lateinit var spCat: Spinner
    private lateinit var spProd: Spinner
    private lateinit var btnEnviarOp: Button
    private lateinit var btnFto: Button
    private lateinit var etxOpinion: EditText

    // Variables para datos del formulario
    private var currentProductId: String? = null
    private var currentProductName: String? = null
    private var currentCategory: String? = null
    private var currentImageUri: Uri? = null

    // Launcher para resultados de cámara y galería
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentImageUri?.let { uri ->
                Toast.makeText(this, "Foto tomada exitosamente", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            currentImageUri = it
            Toast.makeText(this, "Imagen seleccionada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed)

        initializeViews()
        setupClickListeners()
        testApiConnection()
        loadCategorias()
    }

    private fun initializeViews() {
        spCat = findViewById(R.id.sp_cat)
        spProd = findViewById(R.id.sp_prod)
        btnEnviarOp = findViewById(R.id.btn_enviarop)
        btnFto = findViewById(R.id.btn_fto)
        etxOpinion = findViewById(R.id.etx_opinion)
    }

    private fun setupClickListeners() {
        // Botón Subir Foto
        btnFto.setOnClickListener {
            showPhotoOptionsDialog()
        }

        // Botón Enviar Opinión
        btnEnviarOp.setOnClickListener {
            submitReview()
        }
    }

    // Adaptador personalizado para mostrar hint en gris y no seleccionable
    private class HintAdapter(
        context: Context,
        resource: Int,
        private val items: List<String>,
        private val hint: String
    ) : ArrayAdapter<String>(context, resource, items) {

        override fun getCount(): Int {
            return super.getCount() + 1 // +1 para el hint
        }

        override fun getItem(position: Int): String? {
            return if (position == 0) {
                hint
            } else {
                super.getItem(position - 1)
            }
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)
            val textView = view.findViewById<TextView>(android.R.id.text1)

            if (position == 0) {
                // Hint - texto gris
                textView.setTextColor(Color.GRAY)
            } else {
                // Items normales - texto negro
                textView.setTextColor(Color.BLACK)
            }
            return view
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getDropDownView(position, convertView, parent)
            val textView = view.findViewById<TextView>(android.R.id.text1)

            if (position == 0) {
                // Hint - texto gris y deshabilitado
                textView.setTextColor(Color.GRAY)
                view.isEnabled = false
                view.isClickable = false
            } else {
                // Items normales - texto negro y habilitado
                textView.setTextColor(Color.BLACK)
                view.isEnabled = true
                view.isClickable = true
            }
            return view
        }

        override fun isEnabled(position: Int): Boolean {
            // Solo los items que no son el hint son seleccionables
            return position != 0
        }
    }

    private fun showPhotoOptionsDialog() {
        val options = arrayOf("Tomar Foto", "Elegir de Galería", "Cancelar")

        AlertDialog.Builder(this)
            .setTitle("Subir Foto")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> takePhoto()
                    1 -> chooseFromGallery()
                    // 2 es Cancelar
                }
            }
            .show()
    }

    private fun takePhoto() {
        val photoFile = File.createTempFile(
            "JPEG_${System.currentTimeMillis()}_",
            ".jpg",
            externalCacheDir
        )

        currentImageUri = Uri.fromFile(photoFile)
        takePictureLauncher.launch(currentImageUri)
    }

    private fun chooseFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun submitReview() {
        val opinion = etxOpinion.text.toString().trim()

        // Validaciones
        if (currentProductId == null || currentProductName == null) {
            Toast.makeText(this, "Por favor selecciona un producto", Toast.LENGTH_LONG).show()
            return
        }

        if (opinion.isEmpty()) {
            Toast.makeText(this, "Por favor escribe tu opinión", Toast.LENGTH_LONG).show()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            btnEnviarOp.isEnabled = false
            btnEnviarOp.text = "Enviando..."

            try {
                // 1. Analizar sentimiento
                val sentimentScore = analyzeSentiment(opinion)

                // 2. Crear objeto Review para enviar a la API
                val reviewRequest = ReviewRequest(
                    productId = currentProductId!!,
                    productName = currentProductName!!,
                    category = currentCategory ?: "",
                    userId = getCurrentUserId(),
                    userName = getCurrentUserName(),
                    rating = calculateRatingFromSentiment(sentimentScore),
                    comment = opinion,
                    sentimentScore = sentimentScore,
                    imageUrls = emptyList()
                )

                // 3. Enviar a la API
                val response = RetrofitClient.apiService.submitReview(reviewRequest)

                // 4. Éxito - limpiar formulario y mostrar mensaje
                clearForm()
                Toast.makeText(
                    this@MainFeed,
                    "¡Gracias! Valoramos tu opinión",
                    Toast.LENGTH_LONG
                ).show()

                Log.d("REVIEW", "✅ Reseña enviada exitosamente: ${response.id}")

            } catch (e: Exception) {
                Log.e("REVIEW", "❌ Error enviando reseña: ${e.message}")
                Toast.makeText(
                    this@MainFeed,
                    "Error al enviar la reseña: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                btnEnviarOp.isEnabled = true
                btnEnviarOp.text = "Enviar reseña"
            }
        }
    }

    private fun analyzeSentiment(text: String): Float {
        val lowerText = text.lowercase()

        // Análisis mejorado de sentimiento
        return when {
            containsStrongPositiveWords(lowerText) -> 0.95f
            containsPositiveWords(lowerText) && !containsNegativeWords(lowerText) -> 0.8f
            containsNegativeWords(lowerText) && !containsPositiveWords(lowerText) -> 0.2f
            containsStrongNegativeWords(lowerText) -> 0.05f
            containsPositiveWords(lowerText) && containsNegativeWords(lowerText) -> 0.5f
            else -> 0.5f // Neutral
        }
    }

    private fun containsStrongPositiveWords(text: String): Boolean {
        val strongPositiveWords = listOf(
            "excelente", "increíble", "maravilloso", "perfecto", "impresionante",
            "espectacular", "fantástico", "asombroso", "sorprendente"
        )
        return strongPositiveWords.any { text.contains(it) }
    }

    private fun containsPositiveWords(text: String): Boolean {
        val positiveWords = listOf(
            "bueno", "genial", "rico", "delicioso", "recomendado",
            "agradable", "sabroso", "contento", "satisfecho", "gusta"
        )
        return positiveWords.any { text.contains(it) }
    }

    private fun containsNegativeWords(text: String): Boolean {
        val negativeWords = listOf(
            "malo", "regular", "decepcionante", "insatisfecho", "caro",
            "feo", "desagradable", "mejorable", "normal"
        )
        return negativeWords.any { text.contains(it) }
    }

    private fun containsStrongNegativeWords(text: String): Boolean {
        val strongNegativeWords = listOf(
            "horrible", "terrible", "pésimo", "odio", "asco",
            "deplorable", "indignante", "pésimo", "desastroso"
        )
        return strongNegativeWords.any { text.contains(it) }
    }

    private fun calculateRatingFromSentiment(sentimentScore: Float): Int {
        return when {
            sentimentScore >= 0.9f -> 5
            sentimentScore >= 0.7f -> 4
            sentimentScore >= 0.5f -> 3
            sentimentScore >= 0.3f -> 2
            else -> 1
        }
    }

    private fun clearForm() {
        // Limpiar todos los campos del formulario
        etxOpinion.text.clear()
        currentImageUri = null
        spCat.setSelection(0)
        spProd.setSelection(0)
        currentProductId = null
        currentProductName = null
        currentCategory = null

        Toast.makeText(this, "Formulario limpiado", Toast.LENGTH_SHORT).show()
    }

    // Funciones para obtener datos de usuario
    private fun getCurrentUserId(): String {
        val userRepository = UserRepository(this)
        return userRepository.getUserId() ?: run {
            Log.e("Review", "Usuario no logueado - ID no encontrado")
            "user_unknown"
        }
    }

    private fun getCurrentUserName(): String {
        val userRepository = UserRepository(this)
        return userRepository.getUserName() ?: run {
            Log.e("Review", "Usuario no logueado - nombre no encontrado")
            "Usuario"
        }
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
                Toast.makeText(this@MainFeed, "Usando datos de ejemplo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupCategoriaSpinner(categorias: List<com.example.proyectomilsabores.api.CategoriaResponse>) {
        val categoriaNombres = categorias.map { it.nombre }
        val adapter = HintAdapter(this, android.R.layout.simple_spinner_item, categoriaNombres, "Selecciona tu categoría")
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCat.adapter = adapter

        spCat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                if (position > 0) {
                    val categoria = categorias[position - 1]
                    currentCategory = categoria.nombre
                    loadProductos(categoria.id)
                    Log.d("SPINNER", "Categoría seleccionada: ${categoria.nombre}")
                } else {
                    currentCategory = null
                    currentProductId = null
                    currentProductName = null
                    // Limpiar spinner de productos con hint
                    setupProductoSpinner(emptyList())
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
                Log.d("SPINNER", "Productos cargados: ${productos.size}")
            } catch (e: Exception) {
                Log.e("API_TEST", "Error cargando productos: ${e.message}")
                // Fallback con productos de ejemplo basados en la categoría
                val productosEjemplo = when (categoriaId) {
                    1L -> listOf(
                        com.example.proyectomilsabores.api.ProductoResponse(1, "Pastel de Chocolate", "Delicioso pastel de chocolate", 25.99, null),
                        com.example.proyectomilsabores.api.ProductoResponse(2, "Tarta de Manzana", "Clásica tarta de manzana", 20.50, null)
                    )
                    2L -> listOf(
                        com.example.proyectomilsabores.api.ProductoResponse(3, "Croissants", "Croissants artesanales", 2.99, null),
                        com.example.proyectomilsabores.api.ProductoResponse(4, "Donuts", "Donuts glaseados", 1.99, null)
                    )
                    else -> emptyList()
                }
                setupProductoSpinner(productosEjemplo)
                Toast.makeText(this@MainFeed, "Usando productos de ejemplo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupProductoSpinner(productos: List<com.example.proyectomilsabores.api.ProductoResponse>) {
        val productNombres = productos.map { it.nombre }
        val adapter = HintAdapter(this, android.R.layout.simple_spinner_item, productNombres, "Selecciona tu producto")
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spProd.adapter = adapter

        spProd.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                if (position > 0) {
                    val producto = productos[position - 1]
                    currentProductId = producto.id.toString()
                    currentProductName = producto.nombre
                    Log.d("SPINNER", "Producto seleccionado: ${producto.nombre} (ID: ${producto.id})")
                } else {
                    currentProductId = null
                    currentProductName = null
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    // Método para verificar estado actual del formulario
    fun getFormStatus(): String {
        return "Producto: $currentProductName, Categoría: $currentCategory, Opinión: ${etxOpinion.text.length} chars"
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainFeed", "Aplicación lista para recibir reseñas")
    }
}