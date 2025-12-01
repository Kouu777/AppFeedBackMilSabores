package com.example.proyectomilsabores

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectomilsabores.data.UserRepository


class MainHome : AppCompatActivity() {

    private lateinit var slide1: View
    private lateinit var slide2: View
    private lateinit var btnComenzar: Button
    private lateinit var btnSiguiente: Button
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_home)

        userRepository = UserRepository(this)

        // FORZAR LIMPIEZA DE DATOS DE SESIÓN
        userRepository.clearUserData()
        Log.d("MainHome", "Datos de sesión limpiados forzadamente")

        // Verificar si el usuario ya está logeado
        val userName = userRepository.getUserName()
        Log.d("MainHome", "UserName guardado después de limpiar: '$userName'")

        if (!userName.isNullOrEmpty() && userName != "null" && userName.trim().isNotEmpty()) {
            // Usuario ya tiene sesión activa, ir a MainActivity
            Log.d("MainHome", "Usuario logeado: $userName - ir a MainActivity")
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        Log.d("MainHome", "Mostrando intro - no hay sesión activa")

        slide1 = findViewById(R.id.slide1)
        btnComenzar = findViewById(R.id.btn_comenzar)


        // solo se muestra el primer slide
        slide1.visibility = View.VISIBLE


        // Long-click
        slide1.setOnLongClickListener {
            userRepository.clearUserData()
            Log.d("MainHome", "Datos de sesión limpiados")
            android.widget.Toast.makeText(this, "Sesión limpiada", android.widget.Toast.LENGTH_SHORT).show()
            true
        }



        btnComenzar.setOnClickListener {
            val intent = Intent(this, MainLogin::class.java)
            startActivity(intent)
            finish()
        }
    }
}
