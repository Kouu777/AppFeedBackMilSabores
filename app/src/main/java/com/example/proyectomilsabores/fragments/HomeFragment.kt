package com.example.proyectomilsabores.fragments

import android.Manifest
import android.app.Activity.RESULT_OK
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
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.proyectomilsabores.R
import com.example.proyectomilsabores.ScanQr.ScannerActivity
import com.example.proyectomilsabores.api.CategoriaResponse
import com.example.proyectomilsabores.api.ProductoResponse
import com.example.proyectomilsabores.api.RetrofitClient
import com.example.proyectomilsabores.api.ReviewRequest
import com.example.proyectomilsabores.data.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Date

class HomeFragment : Fragment() {

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

    // RESULTADO QR
    private val qrScanLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d("QR_LAUNCHER", "ResultCode: ${result.resultCode}, RESULT_OK: $RESULT_OK")
            if (result.resultCode == RESULT_OK) {
                val qrValue = result.data?.getStringExtra("qrResult")
                Log.d("QR_LAUNCHER", "QR Value: '$qrValue'")
                if (!qrValue.isNullOrEmpty()) {
                    Log.d("QR_LAUNCHER", "Llamando handleQrResult con: $qrValue")
                    handleQrResult(qrValue)
                } else {
                    Log.e("QR_LAUNCHER", "QR Value es nulo o vacío")
                }
            } else {
                Log.e("QR_LAUNCHER", "ResultCode no es RESULT_OK")
            }
        }

    // CAMERA Y GALLERY
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) takePhoto()
            else Toast.makeText(requireContext(), "Permiso de cámara denegado.", Toast.LENGTH_LONG).show()
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) currentImageUri?.let { processImageUri(it, "Foto tomada") }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { processImageUri(it, "Imagen seleccionada") }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // layout para este Fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Llama a tus funciones de inicialización aquí
        initializeViews(view)
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        if (spCat.adapter == null || spCat.adapter.count <= 1) {
            lifecycleScope.launch { loadCategorias() }
        }
    }

    private fun initializeViews(view: View) {
        spCat = view.findViewById(R.id.sp_cat)
        spProd = view.findViewById(R.id.sp_prod)
        btnEnviarOp = view.findViewById(R.id.btn_enviarop)
        btnFto = view.findViewById(R.id.btn_fto)
        etxOpinion = view.findViewById(R.id.etx_opinion)
        ivPreview = view.findViewById(R.id.iv_preview)
        btnScanQr = view.findViewById(R.id.btn_scanqr)
    }

    private fun setupClickListeners() {
        btnFto.setOnClickListener { showPhotoOptionsDialog() }
        btnEnviarOp.setOnClickListener { submitReview() }

        btnScanQr.setOnClickListener {
            Log.d("SCANNER_BUTTON", "Botón de escaneo presionado")
            val intent = Intent(requireContext(), ScannerActivity::class.java)
            Log.d("SCANNER_BUTTON", "Intent creado, lanzando scanner")
            qrScanLauncher.launch(intent)
            Log.d("SCANNER_BUTTON", "Scanner lanzado")
        }
    }

    // LÓGICA QR
    private fun handleQrResult(value: String) {
        Log.d("QR_HANDLER", "Valor QR: $value")
        if (!value.contains(";")) {
            Toast.makeText(requireContext(), "QR inválido", Toast.LENGTH_LONG).show()
            return
        }

        val parts = value.split(";")
        if (parts.size != 2) {
            Toast.makeText(requireContext(), "Formato QR incorrecto", Toast.LENGTH_LONG).show()
            return
        }

        val categoriaNombre = parts[0].trim()
        val productoId = parts[1].trim().toLongOrNull()
        if (productoId == null) {
            Toast.makeText(requireContext(), "ID de producto inválido", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            try {
                Log.d("QR_HANDLER", "Cargando categorías...")
                val categorias = loadCategorias()
                Log.d("QR_HANDLER", "Categorías cargadas: ${categorias.size}")

                val categoria = categorias.find { it.nombre.equals(categoriaNombre, ignoreCase = true) }
                Log.d("QR_HANDLER", "Categoría encontrada: ${categoria?.nombre}")

                if (categoria != null) {
                    Log.d("QR_HANDLER", "Cargando productos para categoría ${categoria.id}...")
                    val productos = loadProductos(categoria.id)
                    Log.d("QR_HANDLER", "Productos cargados: ${productos.size}")
                    Log.d("QR_HANDLER", "Productos: ${productos.map { "${it.id}:${it.nombre}" }}")

                    val indexCat = categorias.indexOf(categoria)
                    Log.d("QR_HANDLER", "Indice categoría: $indexCat")

                    // asegurar que el spinner de categorías tenga el adaptador listo
                    if (spCat.adapter == null) {
                        Log.d("QR_HANDLER", "Spinner categorías sin adaptador, configurando...")
                        setupCategoriaSpinner(categorias)
                    }

                    // seleccionar la categoría en el hilo ui
                    requireActivity().runOnUiThread {
                        Log.d("QR_HANDLER", "Seleccionando categoría en posición: ${indexCat + 1}")
                        spCat.setSelection(indexCat + 1)
                    }

                    // configurar y seleccionar el producto
                    setupProductoSpinner(productos, productoId)
                    Log.d("QR_HANDLER", "QR procesado exitosamente")
                } else {
                    Log.e("QR_HANDLER", "Categoría no encontrada: $categoriaNombre")
                    Toast.makeText(requireContext(), "Categoría del QR no encontrada", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("QR_HANDLER", "Error procesando QR", e)
                Toast.makeText(requireContext(), "Error al procesar QR: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    // CARGAR CATEGORÍAS
    private suspend fun loadCategorias(): List<CategoriaResponse> {
        try {
            val response = RetrofitClient.apiService.getCategorias()
            if (response.isSuccessful) {
                val categorias = response.body() ?: emptyList()
                setupCategoriaSpinner(categorias)
                return categorias
            }
        } catch (e: Exception) {
            Log.e("HOME_FRAGMENT", "Error al cargar categorías", e)
        }
        return emptyList()
    }

    private fun setupCategoriaSpinner(categorias: List<CategoriaResponse>) {
        Log.d("CAT_SPINNER_SETUP", "Configurando spinner de categorías con ${categorias.size} items")

        val nombres = categorias.map { it.nombre }
        Log.d("CAT_SPINNER_SETUP", "Nombres de categorías: $nombres")

        // asegurar que se ejecuta en el hilo UI
        requireActivity().runOnUiThread {
            val adapter = HintAdapter(requireContext(), android.R.layout.simple_spinner_item, nombres, "Selecciona una categoría")
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spCat.adapter = adapter
            Log.d("CAT_SPINNER_SETUP", "Adaptador configurado para categorías")

            spCat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    Log.d("CAT_SPINNER_SETUP", "Categoría seleccionada en posición: $position")
                    if (position > 0) {
                        val categoria = categorias[position - 1]
                        currentCategory = categoria.nombre
                        Log.d("CAT_SPINNER_SETUP", "Categoría seleccionada: ${categoria.nombre}")
                        lifecycleScope.launch {
                            val productos = loadProductos(categoria.id)
                            setupProductoSpinner(productos)
                        }
                    } else {
                        spProd.adapter = null // limpiar productos si se deselecciona la categoría
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }
    }

    private suspend fun loadProductos(categoriaId: Long): List<ProductoResponse> {
        try {
            val response = RetrofitClient.apiService.getProductosPorCategoria(categoriaId)
            if (response.isSuccessful) {
                return response.body() ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e("HOME_FRAGMENT", "Error al cargar productos", e)
        }
        return emptyList()
    }

    private fun setupProductoSpinner(productos: List<ProductoResponse>, productoIdToSelect: Long? = null) {
        Log.d("SPINNER_SETUP", "Configurando spinner de productos con ${productos.size} items")

        if (productos.isEmpty()) {
            Log.e("SPINNER_SETUP", "Lista de productos vacía")
            Toast.makeText(requireContext(), "No hay productos para esta categoría", Toast.LENGTH_SHORT).show()
            return
        }

        val nombres = productos.map { it.nombre }
        Log.d("SPINNER_SETUP", "Nombres de productos: $nombres")

        // asegurar que se ejecuta en el hilo UI
        requireActivity().runOnUiThread {
            val adapter = HintAdapter(requireContext(), android.R.layout.simple_spinner_item, nombres, "Selecciona un producto")
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spProd.adapter = adapter
            Log.d("SPINNER_SETUP", "Adaptador configurado para productos")

            spProd.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    Log.d("SPINNER_SETUP", "Producto seleccionado en posición: $position")
                    if (position > 0) {
                        val producto = productos[position - 1]
                        currentProductId = producto.id.toString()
                        currentProductName = producto.nombre
                        Log.d("SPINNER_SETUP", "Producto seleccionado: ${producto.nombre} (${producto.id})")
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            // producto específico a seleccionar (del QR)
            productoIdToSelect?.let { id ->
                Log.d("SPINNER_SETUP", "Buscando producto con ID: $id")
                val index = productos.indexOfFirst { it.id == id }
                Log.d("SPINNER_SETUP", "Índice del producto encontrado: $index")

                if (index != -1) {
                    // delay para asegurar que el adaptador está listo
                    spProd.postDelayed({
                        Log.d("SPINNER_SETUP", "Seleccionando producto en posición: ${index + 1}")
                        spProd.setSelection(index + 1)
                    }, 100)
                } else {
                    Log.e("SPINNER_SETUP", "Producto con ID $id no encontrado en la lista")
                    Toast.makeText(requireContext(), "Producto no encontrado en esta categoría", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showPhotoOptionsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Subir Foto")
            .setItems(arrayOf("Tomar Foto", "Elegir de Galería")) { _, which ->
                if (which == 0) checkCameraPermissionAndTakePhoto() else chooseFromGallery()
            }
            .show()
    }

    private fun checkCameraPermissionAndTakePhoto() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ->
                takePhoto()
            else -> requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun takePhoto() {
        try {
            val photoFile = File.createTempFile("JPEG_${System.currentTimeMillis()}_", ".jpg", requireContext().externalCacheDir)
            currentImageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().applicationContext.packageName}.provider", photoFile)
            takePictureLauncher.launch(currentImageUri)
        } catch (ex: Exception) {
            Toast.makeText(requireContext(), "Error al crear archivo para la foto.", Toast.LENGTH_LONG).show()
        }
    }

    private fun chooseFromGallery() { pickImageLauncher.launch("image/*") }

    private fun processImageUri(uri: Uri, successMessage: String) {
        try {
            currentBitmap = uriToBitmap(uri)
            ivPreview.setImageBitmap(currentBitmap)
            ivPreview.visibility = View.VISIBLE
            Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("IMAGEFLOW", "Error procesando imagen", e)
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(requireContext().contentResolver, uri))
        else
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val ratio: Float = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (ratio > 1) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun submitReview() {
        val opinion = etxOpinion.text.toString().trim()
        if (currentProductId == null) {
            Toast.makeText(requireContext(), "Selecciona un producto", Toast.LENGTH_LONG).show()
            return
        }
        if (opinion.isEmpty()) {
            Toast.makeText(requireContext(), "Escribe tu opinión", Toast.LENGTH_LONG).show()
            return
        }
        btnEnviarOp.isEnabled = false
        btnEnviarOp.text = "Enviando..."
        lifecycleScope.launch {
            try {
                val bitmapToSend: Bitmap? = currentBitmap?.let { resizeBitmap(it, 1024) }
                val imageBase64: String? = bitmapToSend?.let {
                    val outputStream = ByteArrayOutputStream()
                    it.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                }
                val reviewRequest = ReviewRequest(
                    productId = currentProductId!!,
                    productName = currentProductName ?: "",
                    category = currentCategory ?: "",
                    userId = getCurrentUserId(),
                    userName = getCurrentUserName(),
                    rating = 5, // Asumo que quieres mantener rating fijo por ahora
                    comment = opinion,
                    imageBase64 = imageBase64
                )
                val response = RetrofitClient.apiService.submitReview(
                    productId = currentProductId!!.toLong(),
                    reviewRequest = reviewRequest
                )
                if (response.isSuccessful) {
                    // Guardar la reseña en Firestore después de enviar al backend exitosamente
                    saveReviewToFirestore(
                        productName = currentProductName ?: "",
                        category = currentCategory ?: "",
                        rating = 5.0,
                        comment = opinion,
                        userEmail = getCurrentUserId()
                    )
                    clearForm()
                    Toast.makeText(requireContext(), "¡Gracias! Opinión enviada", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Error: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Fallo de conexión o envío.", Toast.LENGTH_LONG).show()
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
        spProd.adapter = null
        currentProductId = null
        currentProductName = null
    }

    private fun getCurrentUserId(): String = UserRepository(requireContext()).getUserId() ?: "user_unknown"
    private fun getCurrentUserName(): String = UserRepository(requireContext()).getUserName() ?: "Usuario"

    private fun saveReviewToFirestore(productName: String, category: String, rating: Double, comment: String, userEmail: String) {
        val db = FirebaseFirestore.getInstance()

        val reviewData = hashMapOf(
            "productName" to productName,
            "category" to category,
            "rating" to rating,
            "comment" to comment,
            "userEmail" to userEmail,
            "timestamp" to Date(),
            "userName" to getCurrentUserName()
        )

        db.collection("reviews")
            .add(reviewData)
            .addOnSuccessListener { documentReference ->
                Log.d("HomeFragment", "Reseña guardada en Firestore con ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Error al guardar la reseña en Firestore", e)
            }
    }

    private class HintAdapter(context: Context, resource: Int, private val items: List<String>, private val hint: String)
        : ArrayAdapter<String>(context, resource, items) {

        override fun getCount(): Int = super.getCount() + 1
        override fun getItem(position: Int): String? = if (position == 0) hint else super.getItem(position - 1)
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