package com.example.proyectomilsabores.camara

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    fun startCamera(previewView: PreviewView) {
        cameraExecutor = Executors.newSingleThreadExecutor()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // variable local no nula para evitar error de compilación
            val localImageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            
            // asignar a la variable
            this.imageCapture = localImageCapture

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                // pasar variable local no nula
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    localImageCapture
                )
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    suspend fun takePicture(): File? {
        // comprobar si imageCapture es nulo al principio
        val localImageCapture = imageCapture ?: return null

        return try {
            // comprobar que el archivo se pudo crear
            val photoFile = createImageFile() ?: return null

            suspendCancellableCoroutine { continuation ->
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                localImageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            if (continuation.isActive) {
                                continuation.resume(photoFile)
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            photoFile.delete() // limpiar si hay un error
                            if (continuation.isActive) {
                                continuation.resumeWithException(exception)
                            }
                        }
                    }
                )

                continuation.invokeOnCancellation {
                    photoFile.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun createImageFile(): File? {
        val storageDir: File = context.getExternalFilesDir("photos") ?: run {
            //null si no se puede acceder al directorio
            return null
        }
        storageDir.mkdirs()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    fun cleanup() {
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }
}
