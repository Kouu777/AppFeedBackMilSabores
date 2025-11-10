package com.example.proyectomilsabores.api

data class Review (
    val id: String? = null,
    val productId: String,
    val userId: String,
    val rating: Int,
    val comment: String,
    val imageUrl: String? = null,
    val sentimentScore: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)