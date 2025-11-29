package com.example.proyectomilsabores

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class MainHome : AppCompatActivity() {

    private lateinit var slide1: View
    private lateinit var slide2: View
    private lateinit var btnComenzar: Button
    private lateinit var btnSiguiente: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        slide1 = findViewById(R.id.slide1)
        slide2 = findViewById(R.id.slide2)
        btnComenzar = findViewById(R.id.btn_comenzar)
        btnSiguiente = findViewById(R.id.btn_siguiente)

        // Inicialmente, solo se muestra el primer slide
        slide1.visibility = View.VISIBLE
        slide2.visibility = View.GONE

        btnComenzar.setOnClickListener {
            // Animación slide hacia la izquierda
            slide1.animate()
                .translationX(-slide1.width.toFloat())
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    slide1.visibility = View.GONE
                    slide2.visibility = View.VISIBLE
                    slide2.translationX = slide2.width.toFloat()
                    slide2.alpha = 0f
                    slide2.animate()
                        .translationX(0f)
                        .alpha(1f)
                        .setDuration(300)
                        .start()
                }.start()
        }

        btnSiguiente.setOnClickListener {
            val intent = Intent(this, MainLogin::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
    }
}
