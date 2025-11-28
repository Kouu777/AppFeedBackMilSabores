package com.example.proyectomilsabores.ui.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.proyectomilsabores.R
import com.example.proyectomilsabores.ScanQr.ScannerActivity
import com.example.proyectomilsabores.api.RetrofitClient
import com.example.proyectomilsabores.api.ReviewRequest
import com.example.proyectomilsabores.data.UserRepository
import com.example.proyectomilsabores.api.ProductoResponse
import com.example.proyectomilsabores.api.CategoriaResponse
import kotlinx.coroutines.launch
import java.io.File

class MainFeed : AppCompatActivity() {

    private lateinit var spCat: Spinner
    private lateinit var spProd: Spinner
    private lateinit var btnEnviarOp: Button
    private lateinit var btnFto: Button
    private lateinit var etxOpinion: EditText
    private lateinit var ivPreview: ImageView
    private lateinit var btnScanQr: Button

    private var currentProductId: String? = null
    private var currentProductName: String? = null
    private var currentCategory: String? = null
    private var currentImageUri: Uri? = null
    private var currentBitmap: Bitmap? = null

    private var pendingCategoriaNombreFromQr: String? = null
    private var pendingProductoIdFromQr: Long? = null

    // RESULTADO QR
    private val qrScanLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val qrValue = result.data?.getStringExtra("qrResult")
                if (!qrValue.isNullOrEmpty()) {
                    handleQrResult(qrValue)
                }
            }
        }

    // CAMERA Y GALLERY
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) takePhoto()
            else Toast.makeText(this, "Permiso de cámara denegado.", Toast.LENGTH_LONG).show()
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) currentImageUri?.let { processImageUri(it, "Foto tomada") }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { processImageUri(it, "Imagen seleccionada") }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed)
        initializeViews()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        if (spCat.adapter == null || spCat.adapter.count <= 1) {
            lifecycleScope.launch { loadCategorias() }
        }
    }

    private fun initializeViews() {
        spCat = findViewById(R.id.sp_cat)
        spProd = findViewById(R.id.sp_prod)
        btnEnviarOp = findViewById(R.id.btn_enviarop)
        btnFto = findViewById(R.id.btn_fto)
        etxOpinion = findViewById(R.id.etx_opinion)
        ivPreview = findViewById(R.id.iv_preview)
        btnScanQr = findViewById(R.id.btn_scanqr)
    }

    private fun setupClickListeners() {
        btnFto.setOnClickListener { showPhotoOptionsDialog() }
        btnEnviarOp.setOnClickListener { submitReview() }

        btnScanQr.setOnClickListener {
            val intent = Intent(this, ScannerActivity::class.java)
            qrScanLauncher.launch(intent)
        }
    }

 =
    // LÓGICA QR

    private fun handleQrResult(value: String) {
        Log.d("QR_HANDLER", "Valor QR: $value")
        if (!value.contains(";")) {
            Toast.makeText(this, "QR inválido", Toast.LENGTH_LONG).show()
            return
        }

        val parts = value.split(";")
        if (parts.size != 2) {
            Toast.makeText(this, "Formato QR incorrecto", Toast.LENGTH_LONG).show()
            return
        }

        val categoriaNombre = parts[0].trim()
        val productoId = parts[1].trim().toLongOrNull()
        if (productoId == null) {
            Toast.makeText(this, "ID de producto inválido", Toast.LENGTH_LONG).show()
            return
        }

        pendingCategoriaNombreFromQr = categoriaNombre
        pendingProductoIdFromQr = productoId

        lifecycleScope.launch {
            val categorias = loadCategorias() // carga categorías y retorna lista
            val categoria = categorias.find { it.nombre.equals(categoriaNombre, ignoreCase = true) }
            if (categoria != null) {
                val indexCat = categorias.indexOf(categoria)
                spCat.setSelection(indexCat + 1) // dispara onItemSelected

                val productos = loadProductos(categoria.id) // carga productos y espera
                val indexProd = productos.indexOfFirst { it.id == productoId }
                if (indexProd != -1) spProd.setSelection(indexProd + 1)
            } else {
                Toast.makeText(this@MainFeed, "Categoría del QR no encontrada", Toast.LENGTH_LONG).show()
            }

            pendingCategoriaNombreFromQr = null
            pendingProductoIdFromQr = null
        }
    }


    // CARGAR CATEGORÍAS

    private suspend fun loadCategorias(): List<CategoriaResponse> {
        val response = RetrofitClient.apiService.getCategorias()
        val categorias = response.body() ?: emptyList()
        setupCategoriaSpinner(categorias)
        return categorias
    }

    private fun setupCategoriaSpinner(categorias: List<CategoriaResponse>) {
        val nombres = categorias.map { it.nombre }
        val adapter = HintAdapter(this, android.R.layout.simple_spinner_item, nombres, "Selecciona una categoría")
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCat.adapter = adapter

        spCat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val categoria = categorias[position - 1]
                    currentCategory = categoria.nombre
                    lifecycleScope.launch { loadProductos(categoria.id) }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private suspend fun loadProductos(categoriaId: Long): List<ProductoResponse> {
        val response = RetrofitClient.apiService.getProductosPorCategoria(categoriaId)
        val productos = response.body() ?: emptyList()
        setupProductoSpinner(productos)
        return productos
    }

    private fun setupProductoSpinner(productos: List<ProductoResponse>) {
        val nombres = productos.map { it.nombre }
        val adapter = HintAdapter(this, android.R.layout.simple_spinner_item, nombres, "Selecciona un producto")
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spProd.adapter = adapter

        spProd.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val producto = productos[position - 1]
                    currentProductId = producto.id.toString()
                    currentProductName = producto.nombre
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }


    // FOTO, GALERÍA Y REVIEW
    private fun showPhotoOptionsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Subir Foto")
            .setItems(arrayOf("Tomar Foto", "Elegir de Galería")) { _, which ->
                if (which == 0) checkCameraPermissionAndTakePhoto() else chooseFromGallery()
            }
            .show()
    }

    private fun checkCameraPermissionAndTakePhoto() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ->
                takePhoto()
            else -> requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun takePhoto() {
        try {
            val photoFile = File.createTempFile("JPEG_${System.currentTimeMillis()}_", ".jpg", externalCacheDir)
            currentImageUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", photoFile)
            takePictureLauncher.launch(currentImageUri)
        } catch (ex: Exception) {
            Toast.makeText(this, "Error al crear archivo para la foto.", Toast.LENGTH_LONG).show()
        }
    }

    private fun chooseFromGallery() { pickImageLauncher.launch("image/*") }

    private fun processImageUri(uri: Uri, successMessage: String) {
        try {
            currentBitmap = uriToBitmap(uri)
            ivPreview.setImageBitmap(currentBitmap)
            ivPreview.visibility = View.VISIBLE
            Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al procesar la imagen.", Toast.LENGTH_LONG).show()
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(this.contentResolver, uri))
        else
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(this.contentResolver, uri)


    //ENVIAR REVIEW
    private fun submitReview() {
        val opinion = etxOpinion.text.toString().trim()
        if (currentProductId == null) { Toast.makeText(this, "Selecciona un producto", Toast.LENGTH_LONG).show(); return }
        if (opinion.isEmpty()) { Toast.makeText(this, "Escribe tu opinión", Toast.LENGTH_LONG).show(); return }

        btnEnviarOp.isEnabled = false
        btnEnviarOp.text = "Enviando..."

        lifecycleScope.launch {
            try {
                val reviewRequest = ReviewRequest(
                    productId = currentProductId!!,
                    productName = currentProductName!!,
                    category = currentCategory ?: "",
                    userId = getCurrentUserId(),
                    userName = getCurrentUserName(),
                    rating = 5,
                    comment = opinion,
                    sentimentScore = 0.5f,
                    imageUrls = emptyList()
                )

                val response = RetrofitClient.apiService.submitReview(reviewRequest.productId, reviewRequest)
                if (response.isSuccessful) {
                    val reviewResponse = response.body()
                    clearForm()
                    Toast.makeText(this@MainFeed, "¡Gracias! Opinión enviada (ID: ${reviewResponse?.id})", Toast.LENGTH_LONG).show()
                } else Toast.makeText(this@MainFeed, "Error del servidor: ${response.code()}", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Toast.makeText(this@MainFeed, "Fallo de conexión.", Toast.LENGTH_LONG).show()
            } finally {
                btnEnviarOp.isEnabled = true
                btnEnviarOp.text = "Enviar reseña"
            }
        }
    }

    private fun clearForm() {
        etxOpinion.text.clear()
        currentImageUri = null
        currentBitmap = null
        ivPreview.visibility = View.GONE
        spCat.setSelection(0)
    }

    private fun getCurrentUserId(): String = UserRepository(this).getUserId() ?: "user_unknown"
    private fun getCurrentUserName(): String = UserRepository(this).getUserName() ?: "Usuario"


    //HINT

    private class HintAdapter(context: Context, resource: Int, private val items: List<String>, private val hint: String)
        : ArrayAdapter<String>(context, resource, items) {

        override fun getCount(): Int = super.getCount() + 1
        override fun getItem(position: Int): String = if (position == 0) hint else super.getItem(position - 1)!!
        override fun isEnabled(position: Int): Boolean = position != 0

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent) as TextView
            view.setTextColor(if (position == 0) Color.GRAY else Color.BLACK)
            return view
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getDropDownView(position, convertView, parent) as TextView
            view.setTextColor(if (position == 0) Color.GRAY else Color.BLACK)
            return view
        }
    }
}
