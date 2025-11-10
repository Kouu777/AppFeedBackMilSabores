package com.example.proyectomilsabores.data

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(private val context: Context) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    fun saveCameraQuality(quality: String) {
        prefs.edit().putString("camera_quality", quality).apply()
    }

    fun getCameraQuality(): String {
        return prefs.getString("camera_quality", "high") ?: "high"
    }

    fun saveAutoUploadEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_upload", enabled).apply()
    }

    fun isAutoUploadEnabled(): Boolean {
        return prefs.getBoolean("auto_upload", true)
    }
}