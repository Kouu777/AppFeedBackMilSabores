package com.example.proyectomilsabores.camara

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

class ImageProcessor {

    fun compressImage(file: File, maxSize: Int = 1024): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            var scale = 1
            while (options.outWidth / scale / 2 >= maxSize &&
                options.outHeight / scale / 2 >= maxSize) {
                scale *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }

            BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
        } catch (e: Exception) {
            null
        }
    }

    fun getImageDimensions(file: File): Pair<Int, Int>? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            Pair(options.outWidth, options.outHeight)
        } catch (e: Exception) {
            null
        }
    }
}