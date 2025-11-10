package com.example.proyectomilsabores.qr

data class QRData(
    val content: String,
    val type: QRType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class QRType {
    PRODUCT, URL, TEXT, UNKNOWN
}