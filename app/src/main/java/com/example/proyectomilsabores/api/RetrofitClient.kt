package com.example.proyectomilsabores.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface ApiService {

    @GET("api/categorias")
    suspend fun getCategorias(): List<CategoriaResponse>

    @GET("api/productos/categoria/{categoriaId}")
    suspend fun getProductosPorCategoria(@Path("categoriaId") categoriaId: Long): List<ProductoResponse>
}

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
    // Elige la URL correcta para tu setup:
    private const val BASE_URL = "http://10.0.2.2:8080/"  // ← Para Emulador Android

    val instance: Retrofit by lazy {
        // Interceptor para ver logs de las requests
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