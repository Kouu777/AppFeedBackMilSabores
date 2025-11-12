package com.example.proyectomilsabores.storage

import android.net.Uri
import java.io.File

class CloudStorageServ {

    suspend fun uploadImage(file: File, userId: String): Result<String> {
        return try {

            Thread.sleep(1000) // Simular delay de red

            val simulatedUrl = "https://storage.googleapis.com/milsabores-bucket/images/$userId/${file.name}"

            Result.success(simulatedUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadImage(uri: Uri, userId: String): Result<String> {
        return try {
            Thread.sleep(1000)

            val simulatedUrl = "https://storage.googleapis.com/milsabores-bucket/images/$userId/${System.currentTimeMillis()}.jpg"

            Result.success(simulatedUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}