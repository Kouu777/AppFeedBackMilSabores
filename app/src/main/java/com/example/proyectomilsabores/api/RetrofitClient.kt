package com.example.proyectomilsabores.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit


// ========================================
//           API INTERFACE
// ========================================
interface ApiService {

    // Crear review con Base64
    @POST("api/productos/{id}/reviews")
    suspend fun submitReview(
        @Path("id") productId: Long,
        @Body reviewRequest: ReviewRequest
    ): Response<ReviewResponse>

    // Obtener reviews de un producto
    @GET("api/productos/{productoId}/reviews")
    suspend fun getReviewsForProduct(@Path("productoId") id: Long): Response<List<ReviewResponse>>

    // Categorías
    @GET("api/categorias")
    suspend fun getCategorias(): Response<List<CategoriaResponse>>

    // Productos por categoría
    @GET("api/productos/categoria/{categoriaId}")
    suspend fun getProductosPorCategoria(@Path("categoriaId") categoriaId: Long): Response<List<ProductoResponse>>

    // Obtener producto por ID
    @GET("api/productos/{productoId}")
    suspend fun getProductoById(@Path("productoId") productoId: Long): Response<ProductoResponse>
}


// ========================================
//           DATA CLASSES
// ========================================
data class ReviewRequest(
    val productId: String,
    val productName: String,
    val category: String,
    val userId: String,
    val userName: String,
    val rating: Int,
    val comment: String,
    val sentimentScore: Float = 0.5f,
    val imageBase64: String? = null // <- Aquí viaja la IMAGEN
)

data class ReviewResponse(
    val id: Long,
    val productoId: Long?,
    val usuario: String?,
    val texto: String?,
    val rating: Int?,
    val imageUrl: String?,     // <- tu backend devuelve solo 1 URL, no lista
    val fecha: String?
)

data class CategoriaResponse(
    val id: Long,
    val nombre: String,
    val descripcion: String?,
    val imagenUrl: String?
)

data class ProductoResponse(
    val id: Long,
    val nombre: String,
    val descripcion: String?,
    val precio: Double?,
    val imagenUrl: String?,
    val categoria: String
)


// ========================================
//           RETROFIT CLIENT
// ========================================
object RetrofitClient {

    // Cambia al IP correcto si usas celular físico
    private const val BASE_URL = "http://192.168.1.169:8081/"

    val apiService: ApiService by lazy {

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
