package com.example.proyectomilsabores.api


data class Review(
    val id: String = "",
    val productId: String,
    val userId: String,
    val userName: String,
    val rating: Int,
    val comment: String,
    val sentimentScore: Float,
    val imageUrls: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val category: String,
    val productName: String
)