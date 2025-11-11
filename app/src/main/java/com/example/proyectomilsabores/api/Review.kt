package com.example.proyectomilsabores.api

// Estructura recomendada
data class Review(
    val id: String = "",
    val productId: String,
    val userId: String,
    val userName: String,
    val rating: Int, // 1-5
    val comment: String,
    val sentimentScore: Float, // Del Natural Language API
    val imageUrls: List<String> = emptyList(), // URLs de Cloud Storage
    val timestamp: Long = System.currentTimeMillis(),
    val category: String,
    val productName: String
)