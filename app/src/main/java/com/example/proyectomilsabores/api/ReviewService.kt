package com.example.proyectomilsabores.api

import retrofit2.http.Body
import retrofit2.http.POST

interface ReviewService {

    @POST("reviews")
    suspend fun submitReview(@Body review: ReviewRequest): ReviewResponse
}

data class ReviewRequest(
    val productId: String,
    val userId: String,
    val rating: Int,
    val comment: String,
    val imageUrl: String? = null,
    val sentimentScore: Float? = null
)

data class ReviewResponse(
    val success: Boolean,
    val message: String,
    val reviewId: String? = null
)