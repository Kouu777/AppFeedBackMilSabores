package com.example.proyectomilsabores.ui.activities

import android.Manifest
import android.content.Context
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
import com.example.proyectomilsabores.R
import com.example.proyectomilsabores.api.*
import com.example.proyectomilsabores.data.UserRepository
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
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
    private lateinit var ivPreview: ImageView
    private lateinit var btnScanQr: ImageButton

    private var currentProductId: String? = null
    private var currentProductName: String? = null
    private var currentCategory: String? = null
    private var currentImageUri: Uri? = null
    private var currentBitmap: Bitmap? = null

    // Scanner QR
    private val qrLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) return@registerForActivityResult

        val scannedProductId = result.contents.trim()
        handleScannedProductId(scannedProductId)
    }

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
            loadCategorias()
        }
    }

    private fun initializeViews() {
        spCat = findViewById(R.id.sp_cat)
        spProd = findViewById(R.id.sp_prod)
        btnEnviarOp = findViewById(R.id.btn_enviarop)
        btnFto = findViewById(R.id.btn_fto)
        etxOpinion = findViewById(R.id.etx_opinion)
        ivPreview = findViewById(R.id.iv_preview)

        btnScanQr = findViewById(R.id.btn_scan_qr)  // AÑADIR ESTE BOTÓN EN TU XML
    }

    private fun setupClickListeners() {
        btnFto.setOnClickListener { showPhotoOptionsDialog() }
        btnEnviarOp.setOnClickListener { submitReview() }
        btnScanQr.setOnClickListener { openQrScanner() }
    }

    // Abrir lector QR
    private fun openQrScanner() {
        val options = ScanOptions()
        options.setPrompt("Escanea el código QR del producto")
        options.setBeepEnabled(true)
        options.setOrientationLocked(true)
        qrLauncher.launch(options)
    }

    // Procesar ID escaneado
    private fun handleScannedProductId(productId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = RetrofitClient.apiService.getProductoById(productId.toLong())

                if (response.isSuccessful) {
                    val producto = response.body() ?: return@launch

                    currentProductId = producto.id.toString()
                    currentProductName = producto.nombre

                    // Cargar categorías y seleccionar automáticamente
                    loadCategorias {
                        selectCategoryAndProduct(producto)
                    }

                    Toast.makeText(this@MainFeed, "Producto detectado: ${producto.nombre}", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainFeed, "Producto no encontrado", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainFeed, "Error al buscar producto", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun loadCategorias(onLoaded: (() -> Unit)? = null) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = RetrofitClient.apiService.getCategorias()
                if (response.isSuccessful) {
                    val categorias = response.body() ?: emptyList()
                    setupCategoriaSpinner(categorias)
                    onLoaded?.invoke()
                }
            } catch (_: Exception) {}
        }
    }

    // Selecciona automaticamente CAT + PRODUCTO
    private fun selectCategoryAndProduct(producto: ProductoResponse) {

        val categoriaNombre = producto.categoria

        val adapter = spCat.adapter
        if (adapter != null) {

            // Seleccionar categoría comparando Strings
            for (i in 1 until adapter.count) {
                if (adapter.getItem(i) == categoriaNombre) {
                    spCat.setSelection(i)
                    break
                }
            }

            CoroutineScope(Dispatchers.Main).launch {
                // Buscar categoría por nombre
                val categoriasResp = RetrofitClient.apiService.getCategorias()
                val categorias = categoriasResp.body() ?: emptyList()

                val categoriaObj = categorias.find { it.nombre == categoriaNombre }
                if (categoriaObj != null) {
                    val prodsResponse = RetrofitClient.apiService.getProductosPorCategoria(categoriaObj.id)
                    val productos = prodsResponse.body() ?: emptyList()

                    setupProductoSpinner(productos)

                    for (i in 1 until spProd.adapter.count) {
                        if (spProd.adapter.getItem(i) == producto.nombre) {
                            spProd.setSelection(i)
                            break
                        }
                    }
                }
            }
        }
    }


    private fun showPhotoOptionsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Subir Foto")
            .setItems(arrayOf("Tomar Foto", "Elegir de Galería")) { _, which ->
                if (which == 0) checkCameraPermissionAndTakePhoto() else chooseFromGallery()
            }.show()
    }

    private fun checkCameraPermissionAndTakePhoto() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> takePhoto()

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

    private fun chooseFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun processImageUri(uri: Uri, successMessage: String) {
        try {
            currentBitmap = uriToBitmap(uri)
            ivPreview.setImageBitmap(currentBitmap)
            ivPreview.visibility = View.VISIBLE
            Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Error al procesar la imagen.", Toast.LENGTH_LONG).show()
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(this.contentResolver, uri))
        else
            MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
    }

    private fun submitReview() {
        val opinion = etxOpinion.text.toString().trim()

        if (currentProductId == null) {
            Toast.makeText(this, "Por favor selecciona un producto", Toast.LENGTH_LONG).show()
            return
        }

        if (opinion.isEmpty()) {
            Toast.makeText(this, "Por favor escribe tu opinión", Toast.LENGTH_LONG).show()
            return
        }

        btnEnviarOp.isEnabled = false
        btnEnviarOp.text = "Enviando..."

        CoroutineScope(Dispatchers.Main).launch {
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

                val response = RetrofitClient.apiService.submitReview(
                    reviewRequest.productId,
                    reviewRequest
                )

                if (response.isSuccessful) {
                    val reviewResponse = response.body()
                    clearForm()
                    Toast.makeText(
                        this@MainFeed,
                        "¡Gracias! Opinión enviada (ID: ${reviewResponse?.id})",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainFeed,
                        "Error del servidor: ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@MainFeed,
                    "Fallo de conexión. Revisa tu red.",
                    Toast.LENGTH_LONG
                ).show()
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

    private fun setupCategoriaSpinner(categorias: List<CategoriaResponse>) {
        val categoriaNombres = categorias.map { it.nombre }
        val adapter = HintAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categoriaNombres,
            "Selecciona una categoría"
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCat.adapter = adapter

        spCat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val categoria = categorias[position - 1]
                    currentCategory = categoria.nombre
                    loadProductos(categoria.id)
                } else {
                    setupProductoSpinner(emptyList())
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadProductos(categoriaId: Long) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = RetrofitClient.apiService.getProductosPorCategoria(categoriaId)
                if (response.isSuccessful) {
                    val productos = response.body() ?: emptyList()
                    setupProductoSpinner(productos)
                }
            } catch (_: Exception) {
                setupProductoSpinner(emptyList())
            }
        }
    }

    private fun setupProductoSpinner(productos: List<ProductoResponse>) {
        val productNombres = productos.map { it.nombre }
        val adapter = HintAdapter(
            this,
            android.R.layout.simple_spinner_item,
            productNombres,
            "Selecciona un producto"
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spProd.adapter = adapter

        spProd.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val producto = productos[position - 1]
                    currentProductId = producto.id.toString()
                    currentProductName = producto.nombre
                } else {
                    currentProductId = null
                    currentProductName = null
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private class HintAdapter(
        context: Context, resource: Int,
        private val items: List<String>, private val hint: String
    ) : ArrayAdapter<String>(context, resource, items) {

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
