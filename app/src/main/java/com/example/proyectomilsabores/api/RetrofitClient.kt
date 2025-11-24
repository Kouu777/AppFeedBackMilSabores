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

interface ApiService {

    // --- Reviews ---
    @POST("api/productos/{productId}/reviews")
    suspend fun submitReview(
        @Path("productId") productId: String,
        @Body reviewRequest: ReviewRequest
    ): Response<ReviewResponse>

    @GET("api/productos/{productoId}/reviews")
    suspend fun getReviewsForProduct(@Path("productoId") id: Long): Response<List<ReviewResponse>>


    // --- Categorías ---
    @GET("api/categorias")
    suspend fun getCategorias(): Response<List<CategoriaResponse>>


    // --- Productos ---
    @GET("api/productos/categoria/{categoriaId}")
    suspend fun getProductosPorCategoria(@Path("categoriaId") categoriaId: Long): Response<List<ProductoResponse>>

    // ⭐ NECESARIO PARA EL QR ⭐
    @GET("api/productos/{productoId}")
    suspend fun getProductoById(@Path("productoId") productoId: Long): Response<ProductoResponse>
}



data class ReviewRequest(
    val productId: String,
    val productName: String,
    val category: String,
    val userId: String,
    val userName: String,
    val rating: Int,
    val comment: String,
    val sentimentScore: Float = 0.5f,
    val imageUrls: List<String> = emptyList()
)

data class ReviewResponse(
    val id: Long,
    val productId: String,
    val productName: String,
    val category: String,
    val userId: String,
    val userName: String,
    val rating: Int,
    val comment: String,
    val sentimentScore: Float,
    val imageUrls: List<String>,
    val createdAt: String
)

data class CategoriaResponse(
    val id: Long,
    val nombre: String,
    val descripcion: String?, // Añadido
    val imagenUrl: String?
)

data class ProductoResponse(
    val id: Long,
    val nombre: String,
    val descripcion: String?,
    val precio: Double?,
    val imagenUrl: String?,    // Añadido
    val categoria: String
)



object RetrofitClient {

    private const val BASE_URL = "http://192.168.100.8:8081/"

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
