package com.example.proyectomilsabores.data

import android.content.Context
import android.content.SharedPreferences

class UserRepository(private val context: Context) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    }

    fun saveUserToken(token: String) {
        prefs.edit().putString("user_token", token).apply()
    }

    fun getUserToken(): String? {
        return prefs.getString("user_token", null)
    }

    fun clearUserData() {
        prefs.edit().clear().apply()
    }

    fun isFirstTimeUser(): Boolean {
        return prefs.getBoolean("is_first_time", true)
    }

    fun setFirstTimeUser(isFirstTime: Boolean) {
        prefs.edit().putBoolean("is_first_time", isFirstTime).apply()
    }
}