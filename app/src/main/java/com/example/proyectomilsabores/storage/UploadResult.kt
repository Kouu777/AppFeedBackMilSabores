package com.example.proyectomilsabores.storage

data class UploadResult(
    val success: Boolean,
    val url: String? = null,
    val error: String? = null
)