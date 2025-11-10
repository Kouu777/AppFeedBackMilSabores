package com.example.proyectomilsabores.qr

import com.example.proyectomilsabores.qr.QRData
import com.example.proyectomilsabores.qr.QRType

class QRProcessor {

    fun processQRContent(content: String): QRData {
        return QRData(
            content = content,
            type = determineQRType(content)
        )
    }

    private fun determineQRType(content: String): QRType {
        return when {
            content.startsWith("product:") -> QRType.PRODUCT
            content.startsWith("http://") || content.startsWith("https://") -> QRType.URL
            else -> QRType.TEXT
        }
    }

    fun extractProductIdFromQR(content: String): String? {
        return if (content.startsWith("product:")) {
            content.removePrefix("product:")
        } else {
            null
        }
    }
}