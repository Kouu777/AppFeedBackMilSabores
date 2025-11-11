package com.example.proyectomilsabores.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface ApiService {
    @POST("api/reviews")
    suspend fun submitReview(@Body reviewRequest: ReviewRequest): ReviewResponse

    @GET("api/categorias")
    suspend fun getCategorias(): List<CategoriaResponse>

    @GET("api/productos/categoria/{categoriaId}")
    suspend fun getProductosPorCategoria(@Path("categoriaId") categoriaId: Long): List<ProductoResponse>
}

// Modelos para las reviews
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
    val nombre: String
)

data class ProductoResponse(
    val id: Long,
    val nombre: String,
    val descripcion: String? = null,
    val precio: Double? = null,
    val categoria: CategoriaResponse? = null
)

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080/"

    val instance: Retrofit by lazy {
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
    }

    val apiService: ApiService by lazy {
        instance.create(ApiService::class.java)
    }
}